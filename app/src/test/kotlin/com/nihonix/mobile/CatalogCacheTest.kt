package com.nihonix.mobile

import com.nihonix.mobile.core.offline.*
import okhttp3.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*

class CatalogCacheTest {
    private val server=MockWebServer()
    private class Store:CatalogStore {
        val rows=linkedMapOf<String,CatalogCache>()
        override fun get(url:String)=rows[url]
        override fun put(row:CatalogCache) { rows[row.url]=row }
        override fun remove(url:String) { rows.remove(url) }
        override fun all()=rows.values.toList()
    }
    private val store=Store()
    private val session=com.nihonix.mobile.core.network.interceptor.SessionRequest("account-a")
    private lateinit var client:OkHttpClient
    private val payload="""{"success":true,"data":{"items":[{"slug":"hello"}]}}"""
    @Before fun setup() {
        server.start()
        client=OkHttpClient.Builder().addInterceptor(CatalogCacheInterceptor(store,server.url("/api/").toString()){session}).build()
    }
    @After fun close() { server.shutdown() }
    private fun request(path:String="/api/catalog/vocabulary-items")=Request.Builder().url(server.url(path)).header("Authorization","Bearer fixture")
        .tag(com.nihonix.mobile.core.network.interceptor.SessionRequest::class.java,session).build()
    @Test fun delayed304CannotOverwriteNewerPayload() {
        val url=request().url.toString()
        store.put(CatalogCache(url,payload,"\"v1\"",0))
        val entered=java.util.concurrent.CountDownLatch(1)
        val release=java.util.concurrent.CountDownLatch(1)
        val index=java.util.concurrent.atomic.AtomicInteger()
        val updated=payload.replace("hello","updated")
        server.dispatcher=object:okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request:RecordedRequest):MockResponse =
                if(index.getAndIncrement()==0) {
                    entered.countDown()
                    check(release.await(5,java.util.concurrent.TimeUnit.SECONDS))
                    MockResponse().setResponseCode(304)
                } else MockResponse().setHeader("ETag","\"v2\"").setBody(updated)
        }
        val executor=java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            val first=executor.submit<String> { client.newCall(request()).execute().use { it.body!!.string() } }
            assertTrue(entered.await(5,java.util.concurrent.TimeUnit.SECONDS))
            client.newCall(request()).execute().use { assertEquals(updated,it.body!!.string()) }
            release.countDown()
            assertEquals(updated,first.get(5,java.util.concurrent.TimeUnit.SECONDS))
            assertEquals(updated,store.get(url)!!.payload)
        } finally { release.countDown();executor.shutdownNow() }
    }
    @Test fun invalidationCannotBeUndoneByOldDownload() {
        val url=request().url.toString()
        store.put(CatalogCache(url,payload,"\"v1\"",0))
        val entered=java.util.concurrent.CountDownLatch(1);val release=java.util.concurrent.CountDownLatch(1)
        server.dispatcher=object:okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request:RecordedRequest):MockResponse {
                entered.countDown();check(release.await(5,java.util.concurrent.TimeUnit.SECONDS))
                return MockResponse().setBody(payload)
            }
        }
        val executor=java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            val first=executor.submit { runCatching { client.newCall(request()).execute().close() } }
            assertTrue(entered.await(5,java.util.concurrent.TimeUnit.SECONDS))
            val collections=listOf("levels","topics","vocabulary-items","vocabulary-sets","grammar-sub-topics","grammar-patterns")
                .joinToString(","){"""{"kind":"$it","entries":[]}"""}
            pruneCatalog(store,"""{"data":{"schema_version":1,"collections":[$collections]}}""")
            release.countDown();first.get(5,java.util.concurrent.TimeUnit.SECONDS)
            assertNull(store.get(url))
        } finally {release.countDown();executor.shutdownNow()}
    }
    @Test fun conditional304RestoresExactPayload() {
        server.enqueue(MockResponse().setHeader("ETag","\"v1\"").setBody(payload))
        server.enqueue(MockResponse().setResponseCode(304))
        client.newCall(request()).execute().use { assertEquals(payload,it.body!!.string()) }
        client.newCall(request()).execute().use { assertEquals(200,it.code);assertEquals(payload,it.body!!.string());assertEquals("validated",it.header("X-Nihonix-Cache")) }
        assertNull(server.takeRequest().getHeader("If-None-Match"))
        assertEquals("\"v1\"",server.takeRequest().getHeader("If-None-Match"))
        assertEquals(1,store.rows.size)
    }
    @Test fun missingPayloadRetries304Unconditionally() {
        server.enqueue(MockResponse().setResponseCode(304))
        server.enqueue(MockResponse().setBody(payload))
        client.newCall(request()).execute().use { assertEquals(payload,it.body!!.string()) }
        assertNull(server.takeRequest().getHeader("If-None-Match"));assertNull(server.takeRequest().getHeader("If-None-Match"))
    }
    @Test fun serverFailureUsesSavedContentAndOfflineMarker() {
        store.put(CatalogCache(request().url.toString(),payload,"\"v1\"",0))
        server.enqueue(MockResponse().setResponseCode(503))
        client.newCall(request()).execute().use { assertEquals(payload,it.body!!.string());assertEquals("offline",it.header("X-Nihonix-Cache")) }
    }
    @Test fun onlyAuthenticatedCatalogReadsAreCachedAndCredentialsAreNotStored() {
        server.enqueue(MockResponse().setBody(payload));server.enqueue(MockResponse().setBody(payload))
        client.newCall(request()).execute().close()
        client.newCall(request("/api/users/me")).execute().close()
        assertEquals(1,store.rows.size)
        assertFalse(store.rows.values.single().toString().contains("Bearer"))
    }
    @Test fun notFoundEvictsPreviouslySavedDetail() {
        val request=request("/api/catalog/vocabulary-items/hello")
        store.put(CatalogCache(request.url.toString(),payload,null,0))
        server.enqueue(MockResponse().setResponseCode(404))
        client.newCall(request).execute().use { assertEquals(404,it.code) }
        assertNull(store.get(request.url.toString()))
    }
    @Test fun authoritativeManifestEvictsDeletedRowsButIncompleteManifestCannot() {
        val url=request().url.toString()
        store.put(CatalogCache(url,payload,null,0))
        pruneCatalog(store,"""{"data":{"collections":[]}}""")
        assertNotNull(store.get(url))
        val collections=listOf("levels","topics","vocabulary-items","vocabulary-sets","grammar-sub-topics","grammar-patterns")
            .joinToString(","){"""{"kind":"$it","entries":[]}"""}
        pruneCatalog(store,"""{"data":{"schema_version":1,"collections":[$collections]}}""")
        assertNull(store.get(url))
    }
}
