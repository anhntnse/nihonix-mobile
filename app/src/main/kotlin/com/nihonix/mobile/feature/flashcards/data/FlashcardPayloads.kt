package com.nihonix.mobile.feature.flashcards.data

import androidx.room.*

@Entity(tableName="flashcard_payload_chunks",primaryKeys=["account","key","position"])
data class FlashcardPayloadChunk(val account:String,val key:String,val position:Int,val text:String)

/** Keep each SQLite row well below CursorWindow limits, including multi-byte Japanese text.
 * Call writes inside the same Room transaction as the referring row.
 * References are local storage details and are always expanded before Retrofit/Gson.
 */
internal class FlashcardPayloads(private val dao:FlashcardDao) {
    private val prefix="@nihonix-payload:"
    suspend fun store(owner:String,key:String,json:String):String {
        dao.deletePayload(owner,key)
        if(json.length<=32_000)return json
        val chunks=mutableListOf<FlashcardPayloadChunk>()
        var start=0
        while(start<json.length) {
            var end=minOf(start+32_000,json.length)
            // SQLite encodes each row separately: never split a UTF-16 surrogate pair.
            if(end<json.length&&json[end-1].isHighSurrogate()&&json[end].isLowSurrogate())end--
            chunks.add(FlashcardPayloadChunk(owner,key,chunks.size,json.substring(start,end)))
            start=end
        }
        dao.putPayload(chunks)
        return prefix+key
    }
    suspend fun read(owner:String,value:String):String {
        if(!value.startsWith(prefix))return value
        val chunks=dao.payload(owner,value.removePrefix(prefix))
        check(chunks.isNotEmpty()&&chunks.withIndex().all{it.index==it.value.position}) { "Incomplete local flashcard payload" }
        return chunks.joinToString(""){it.text}
    }
    suspend fun remove(owner:String,key:String)=dao.deletePayload(owner,key)
}
