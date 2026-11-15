package com.nihonix.mobile.feature.flashcards.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="flashcard_folders_local",primaryKeys=["account","id"])
data class LocalFolder(val account:String,val id:String,val name:String,val description:String="",val position:Int=1,
    val version:String="0",val mutationId:String?=null,val deleted:Boolean=false)
@Entity(tableName="flashcard_sets_local",primaryKeys=["account","id"],indices=[Index(value=["account","folderId"])])
data class LocalSet(val account:String,val id:String,val folderId:String,val name:String,val position:Int=1,
    val version:String="0",val mutationId:String?=null,val deleted:Boolean=false)
@Entity(tableName="flashcards_local",primaryKeys=["account","id"],indices=[Index(value=["account","setId"])])
data class LocalCard(val account:String,val id:String,val setId:String,val front:String,val back:String,val position:Int=1)
@Entity(tableName="flashcard_server_copies",primaryKeys=["account","kind","id"])
data class ServerCopy(val account:String,val kind:String,val id:String,val version:String,val payload:String)
@Entity(tableName="flashcard_checkpoints")
data class FlashcardCheckpoint(@PrimaryKey val account:String,val cursor:String)
@Entity(tableName="flashcard_editor_drafts",primaryKeys=["account","id"])
data class EditorDraft(val account:String,val id:String,val payload:String)

@Dao
interface FlashcardDao {
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putPayload(rows:List<FlashcardPayloadChunk>)
    @Query("SELECT * FROM flashcard_payload_chunks WHERE account=:account AND `key`=:key ORDER BY position") suspend fun payload(account:String,key:String):List<FlashcardPayloadChunk>
    @Query("DELETE FROM flashcard_payload_chunks WHERE account=:account AND `key`=:key") suspend fun deletePayload(account:String,key:String)
    @Query("DELETE FROM flashcard_payload_chunks WHERE account=:account") suspend fun purgePayloads(account:String)
    @Query("SELECT * FROM flashcard_folders_local WHERE account=:account ORDER BY position,id") fun observeFolders(account:String):Flow<List<LocalFolder>>
    @Query("SELECT * FROM flashcard_sets_local WHERE account=:account ORDER BY position,id") fun observeSets(account:String):Flow<List<LocalSet>>
    @Query("SELECT * FROM flashcards_local WHERE account=:account ORDER BY position,id") fun observeCards(account:String):Flow<List<LocalCard>>
    @Query("SELECT * FROM flashcard_folders_local WHERE account=:account") suspend fun folders(account:String):List<LocalFolder>
    @Query("SELECT * FROM flashcard_sets_local WHERE account=:account") suspend fun sets(account:String):List<LocalSet>
    @Query("SELECT * FROM flashcards_local WHERE account=:account AND setId=:setId ORDER BY position,id") suspend fun cards(account:String,setId:String):List<LocalCard>
    @Query("SELECT * FROM flashcard_folders_local WHERE account=:account AND id=:id") suspend fun folder(account:String,id:String):LocalFolder?
    @Query("SELECT * FROM flashcard_sets_local WHERE account=:account AND id=:id") suspend fun set(account:String,id:String):LocalSet?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun put(row:LocalFolder)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun put(row:LocalSet)
    @Query("SELECT * FROM flashcards_local WHERE account=:account AND id IN (:ids)") suspend fun cardsByIds(account:String,ids:List<String>):List<LocalCard>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putCards(rows:List<LocalCard>)
    @Query("DELETE FROM flashcards_local WHERE account=:account AND setId=:setId") suspend fun deleteCards(account:String,setId:String)
    @Query("DELETE FROM flashcard_sets_local WHERE account=:account AND id=:id") suspend fun deleteSet(account:String,id:String)
    @Query("DELETE FROM flashcard_folders_local WHERE account=:account AND id=:id") suspend fun deleteFolder(account:String,id:String)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun put(row:ServerCopy)
    @Query("SELECT * FROM flashcard_server_copies WHERE account=:account AND kind=:kind AND id=:id") suspend fun server(account:String,kind:String,id:String):ServerCopy?
    @Query("SELECT * FROM flashcard_server_copies WHERE account=:account") suspend fun servers(account:String):List<ServerCopy>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun checkpoint(row:FlashcardCheckpoint)
    @Query("SELECT * FROM flashcard_checkpoints WHERE account=:account") suspend fun checkpoint(account:String):FlashcardCheckpoint?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun draft(row:EditorDraft)
    @Query("SELECT * FROM flashcard_editor_drafts WHERE account=:account AND id=:id") suspend fun draft(account:String,id:String):EditorDraft?
    @Query("DELETE FROM flashcard_editor_drafts WHERE account=:account AND id=:id") suspend fun deleteDraft(account:String,id:String)
    @Query("DELETE FROM flashcard_folders_local WHERE account=:account") suspend fun purgeFolders(account:String)
    @Query("DELETE FROM flashcard_sets_local WHERE account=:account") suspend fun purgeSets(account:String)
    @Query("DELETE FROM flashcards_local WHERE account=:account") suspend fun purgeCards(account:String)
    @Query("DELETE FROM flashcard_server_copies WHERE account=:account") suspend fun purgeServers(account:String)
    @Query("DELETE FROM flashcard_checkpoints WHERE account=:account") suspend fun purgeCheckpoint(account:String)
    @Query("DELETE FROM flashcard_editor_drafts WHERE account=:account") suspend fun purgeDrafts(account:String)
}
