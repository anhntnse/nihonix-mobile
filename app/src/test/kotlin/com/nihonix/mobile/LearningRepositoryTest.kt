package com.nihonix.mobile

import com.google.gson.Gson
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.learning.data.*
import com.nihonix.mobile.feature.learning.domain.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LearningRepositoryTest {
    private val server=MockWebServer()
    private lateinit var repo:LearningRepositoryImpl
    private val version="a".repeat(64)
    @Before fun setup() {
        server.start()
        repo=LearningRepositoryImpl(Retrofit.Builder().baseUrl(server.url("/api/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(LearningApi::class.java),Gson())
    }
    @After fun close() { server.shutdown() }
    private fun page(items:String,total:Int=1,number:Int=1) =
        """{"success":true,"data":{"schema_version":1,"items":$items,"total":$total,"page":$number,"page_size":20,"has_more":false,"version":"$version"}}"""
    private val word="""{"slug":"hello","kana":"こんにちは","kanji":"今日は","meaning":{"vi":"Xin chào","en":"Hello"},"examples":[{"sentence_jp":"こんにちは","sentence_en":"Hello"}]}"""
    @Test fun usesPublicPagedEndpointAndEscapesJapaneseSearch()=runBlocking {
        server.enqueue(MockResponse().setBody(page("[$word]")))
        val result=repo.page(ContentKind.WORDS,LearningFilter("日本","topic-id","N5","set-id"),1) as Resource.Success
        assertEquals("こんにちは",result.data.items.single().kana)
        val request=server.takeRequest()
        assertEquals("/api/catalog/vocabulary-items",request.requestUrl!!.encodedPath)
        assertEquals("日本",request.requestUrl!!.queryParameter("q"))
        assertEquals("set-id",request.requestUrl!!.queryParameter("set_id"))
        assertNull(request.requestUrl!!.queryParameter("sub_topic_slug"))
        assertEquals("20",request.requestUrl!!.queryParameter("page_size"))
        assertNull(request.getHeader("Authorization"))
    }
    @Test fun grammarMapsStructureExamplesAndConjugation()=runBlocking {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"schema_version":1,"version":"$version","slug":"desu","pattern_jp":"です","pattern_romaji":"desu","explanation":{"en":"Polite ending"},"usage_examples":[{"sentence_jp":"学生です","sentence_en":"I am a student"}],"conjugation_rules":{"present":"です"},"grammar_points":["polite"]}}"""))
        val entry=(repo.detail(ContentKind.PATTERNS,"desu") as Resource.Success).data
        assertEquals("です",entry.pattern);assertEquals("です",entry.rules["present"])
        assertEquals("I am a student",entry.examples.single().en)
        assertEquals("/api/catalog/grammar-patterns/desu",server.takeRequest().path)
    }
    @Test fun setKeepsCountAndTopicContext()=runBlocking {
        val json="""{"success":true,"data":{"schema_version":1,"version":"$version","id":"set-id","slug":"greetings","title":{"en":"Greetings"},"items_count":22,"topic":{"id":"topic","slug":"greetings","level_code":"N5","title":{"en":"Greetings"}}}}"""
        server.enqueue(MockResponse().setBody(json))
        val entry=(repo.detail(ContentKind.SETS,"set-id") as Resource.Success).data
        assertEquals(22,entry.itemCount);assertEquals("topic",entry.topicId);assertEquals("N5",entry.levelCode)
    }
    @Test fun rejectsMalformedWordRatherThanShowingInventedContent()=runBlocking {
        server.enqueue(MockResponse().setBody(page("""[{"slug":"broken"}]""")))
        assertTrue(repo.page(ContentKind.WORDS,LearningFilter(),1) is Resource.Error)
    }
    @Test fun rejectsUnknownSchema()=runBlocking {
        server.enqueue(MockResponse().setBody(page("[$word]").replace("\"schema_version\":1","\"schema_version\":2")))
        assertTrue(repo.page(ContentKind.WORDS,LearningFilter(),1) is Resource.Error)
    }
    @Test fun rejectsDuplicateIdsWithinAPage()=runBlocking {
        server.enqueue(MockResponse().setBody(page("[$word,$word]",2)))
        assertTrue(repo.page(ContentKind.WORDS,LearningFilter(),1) is Resource.Error)
    }
    @Test fun missingContentPreserves404ForRetryUI()=runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"Learning content not found","data":null}"""))
        val result=repo.detail(ContentKind.WORDS,"missing") as Resource.Error
        assertEquals(404,result.statusCode);assertEquals("Learning content not found",result.message)
    }
    @Test fun emptyCatalogMapsToAnEmptySuccessfulPage()=runBlocking {
        server.enqueue(MockResponse().setBody(page("[]",0)))
        val result=repo.page(ContentKind.WORDS,LearningFilter(),1) as Resource.Success
        assertTrue(result.data.items.isEmpty());assertFalse(result.data.hasMore)
    }
}
