package com.nihonix.mobile.core.offline

import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/** In-flight requests cannot restore a projection invalidated by a newer manifest. */
private object CacheOrdering {
    class State(var epoch:Long=0,var sequence:Long=0,val writes:MutableMap<String,Long> = mutableMapOf())
    private val states=java.util.WeakHashMap<CatalogStore,State>()
    @Synchronized fun state(store:CatalogStore)=states.getOrPut(store){State()}
    fun invalidate(store:CatalogStore,url:String) {
        synchronized(store) {
            state(store).epoch++
            store.remove(url)
        }
    }
}

class CatalogCacheInterceptor(private val store:CatalogStore,private val baseUrl:String):Interceptor {
    private fun allowed(request:Request):Boolean = request.method=="GET" && request.header("Authorization")==null &&
        request.url.toString().startsWith(baseUrl) &&
        Regex("^"+Regex.escape(baseUrl)+"(catalog|levels|topics)(/|\\?|$)").containsMatchIn(request.url.toString())
    private fun restored(request:Request,row:CatalogCache,offline:Boolean)=Response.Builder().request(request)
        .protocol(Protocol.HTTP_1_1).code(200).message(if(offline) "Saved offline content" else "Not modified")
        .header("Content-Type","application/json").header("X-Nihonix-Cache",if(offline) "offline" else "validated")
        .apply { row.etag?.let { header("ETag",it) } }.body(row.payload.toResponseBody("application/json".toMediaType())).build()
    override fun intercept(chain:Interceptor.Chain):Response {
        val request=chain.request()
        if(!allowed(request)) return chain.proceed(request)
        val url=request.url.toString()
        val ordering=CacheOrdering.state(store)
        val (saved,epoch,sequence)=synchronized(store) { Triple(store.get(url),ordering.epoch,++ordering.sequence) }
        val started=System.currentTimeMillis()
        fun fallback():Response? = synchronized(store) {
            if(ordering.epoch!=epoch) null else store.get(url)?.let { restored(request,it,true) }
        }
        val conditional=request.newBuilder().apply {
            if(saved?.etag!=null) header("If-None-Match",saved.etag) else removeHeader("If-None-Match")
        }.build()
        var response=try { chain.proceed(conditional) } catch(error:IOException) {
            return fallback() ?: throw error
        }
        if(response.code==304) {
            response.close()
            val current=synchronized(store) {
                if(ordering.epoch!=epoch) throw IOException("Catalog changed during validation. Retry.")
                val row=store.get(url)
                if(row!=null && (ordering.writes[url] ?: 0)<=sequence) {
                    store.put(row.copy(checkedAt=started));ordering.writes[url]=sequence
                }
                row
            }
            if(current!=null) return restored(request,current,false)
            response=chain.proceed(request.newBuilder().removeHeader("If-None-Match").build())
        }
        if(response.code in 500..599) fallback()?.let { response.close();return it }
        if(response.code==404 || response.code==410) {
            CacheOrdering.invalidate(store,url)
            val path=request.url.encodedPath
            store.all().filter { row ->
                val other=row.url.substringBefore('?')
                val root=path.substringBeforeLast('/')
                other.contains(root) || other.contains("/topics/")
            }.forEach { CacheOrdering.invalidate(store,it.url) }
        }
        if(response.code==200) {
            val bytes=response.body?.bytes() ?: return response
            val text=bytes.toString(Charsets.UTF_8)
            if(bytes.size<=8*1024*1024 && runCatching { JsonParser.parseString(text).asJsonObject["success"].asBoolean }.getOrDefault(false)) {
                synchronized(store) {
                    if(ordering.epoch!=epoch) throw IOException("Catalog changed during download. Retry.")
                    val current=store.get(url)
                    if((ordering.writes[url] ?: 0)>sequence && current!=null) return restored(request,current,false)
                    store.put(CatalogCache(url,text,response.header("ETag"),started))
                    ordering.writes[url]=sequence
                }
            }
            return response.newBuilder().body(bytes.toResponseBody(response.body?.contentType())).build()
        }
        return response
    }
}

/** Full authoritative manifest: absence is a deletion, not a failed partial page. */
fun pruneCatalog(store:CatalogStore,manifestPayload:String) { synchronized(store) {
    val data=runCatching { JsonParser.parseString(manifestPayload).asJsonObject["data"].asJsonObject }.getOrNull() ?: return
    if(data["schema_version"]?.asInt!=1) return
    if(data["collections"]?.isJsonArray!=true) return
    val collections=data["collections"].asJsonArray.associate { collection ->
        val value=collection.asJsonObject
        value["kind"].asString to value["entries"].asJsonArray.flatMap { entry ->
            val row=entry.asJsonObject
            listOf(row["id"].asString,row["slug"].asString)
        }.toSet()
    }
    if(!collections.keys.containsAll(listOf("levels","topics","vocabulary-items","vocabulary-sets","grammar-sub-topics","grammar-patterns"))) return
    CacheOrdering.state(store).epoch++
    store.all().forEach { row ->
        if(row.url.substringBefore('?').endsWith("/catalog/manifest")) return@forEach
        val path=java.net.URI(row.url).path
        val kind=collections.keys.firstOrNull { path.contains("/catalog/$it") } ?:
            when { path.contains("/topics") -> "topics";path.contains("/levels") -> "levels";else->null } ?: return@forEach
        val payload=runCatching { JsonParser.parseString(row.payload).asJsonObject["data"] }.getOrNull() ?: return@forEach
        val entries=when { payload.isJsonArray->payload.asJsonArray.toList()
            payload.isJsonObject && payload.asJsonObject.has("items")->payload.asJsonObject["items"].asJsonArray.toList()
            kind=="topics" && payload.isJsonObject && payload.asJsonObject.has("topic")->listOf(payload.asJsonObject["topic"])
            else->listOf(payload) }
        val deleted=entries.any { entry ->
            val objectValue=entry.asJsonObject
            val id=listOf("id","slug","code").firstNotNullOfOrNull { key->objectValue.get(key)?.takeUnless{it.isJsonNull}?.asString }
            id!=null && id !in collections.getValue(kind)
        }
        if(deleted) CacheOrdering.invalidate(store,row.url)
    }
} }
