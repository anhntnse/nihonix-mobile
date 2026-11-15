package com.nihonix.mobile.core.offline

import androidx.room.*
import com.nihonix.mobile.feature.flashcards.data.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="catalog_cache")
data class CatalogCache(@PrimaryKey val url:String,val payload:String,val etag:String?,val checkedAt:Long)
@Entity(tableName="local_progress",primaryKeys=["account","kind","itemId"])
data class LocalProgress(val account:String,val kind:String,val itemId:String,val status:String,val mutationId:String,
    val clientUpdatedAt:String,val topicId:String,val levelCode:String,val serverVersion:Long=0)
@Entity(tableName="outbox",primaryKeys=["account","mutationId"],indices=[Index(value=["account","state"])])
data class OutboxMessage(val account:String,val mutationId:String,val entityKey:String,val producer:String,
    val payload:String,val state:String="pending",val error:String?=null,val attempts:Int=0)
@Entity(tableName="sync_checkpoints")
data class SyncCheckpoint(@PrimaryKey val account:String,val cursor:String)

interface CatalogStore {
    fun get(url:String):CatalogCache?
    fun put(row:CatalogCache)
    fun remove(url:String)
    fun all():List<CatalogCache>
}
@Dao
interface CatalogDao:CatalogStore {
    @Query("SELECT * FROM catalog_cache") fun observeAll():Flow<List<CatalogCache>>
    @Query("SELECT * FROM catalog_cache WHERE url=:url") override fun get(url:String):CatalogCache?
    @Insert(onConflict=OnConflictStrategy.REPLACE) override fun put(row:CatalogCache)
    @Query("DELETE FROM catalog_cache WHERE url=:url") override fun remove(url:String)
    @Query("SELECT * FROM catalog_cache") override fun all():List<CatalogCache>
}
@Dao
interface ProgressDao {
    @Query("SELECT * FROM outbox WHERE account=:account AND producer=:producer AND state='pending' ORDER BY rowid LIMIT 50") suspend fun pendingProducer(account:String,producer:String):List<OutboxMessage>
    @Query("SELECT * FROM outbox WHERE account=:account AND producer=:producer ORDER BY rowid") suspend fun messagesProducer(account:String,producer:String):List<OutboxMessage>
    @Query("SELECT * FROM outbox WHERE account=:account AND producer=:producer ORDER BY rowid") fun observeProducer(account:String,producer:String):Flow<List<OutboxMessage>>
    @Query("DELETE FROM outbox WHERE account=:account AND producer=:producer") suspend fun purgeProducer(account:String,producer:String)
    @Query("SELECT * FROM local_progress WHERE account=:account") fun observe(account:String):Flow<List<LocalProgress>>
    @Query("SELECT * FROM local_progress WHERE account=:account") suspend fun all(account:String):List<LocalProgress>
    @Query("SELECT * FROM local_progress WHERE account=:account AND kind=:kind AND itemId=:id")
    suspend fun get(account:String,kind:String,id:String):LocalProgress?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun put(row:LocalProgress)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun enqueue(message:OutboxMessage)
    // UPDATE keeps rowid stable: retry metadata must not reorder dependent mutations.
    @Query("UPDATE outbox SET state=:state,error=:error,attempts=:attempts WHERE account=:account AND mutationId=:id")
    suspend fun updateMessageState(account:String,id:String,state:String,error:String?,attempts:Int)
    @Query("SELECT * FROM outbox WHERE account=:account AND state='pending' ORDER BY rowid LIMIT 50") suspend fun pending(account:String):List<OutboxMessage>
    @Query("SELECT * FROM outbox WHERE account=:account") fun observeOutbox(account:String):Flow<List<OutboxMessage>>
    @Query("SELECT * FROM outbox WHERE account=:account") suspend fun messages(account:String):List<OutboxMessage>
    @Query("SELECT * FROM outbox WHERE account=:account AND mutationId=:id") suspend fun message(account:String,id:String):OutboxMessage?
    @Query("DELETE FROM outbox WHERE account=:account AND mutationId=:id") suspend fun acknowledge(account:String,id:String)
    @Query("DELETE FROM outbox WHERE account=:account AND entityKey=:key") suspend fun discardKey(account:String,key:String)
    @Query("SELECT * FROM sync_checkpoints WHERE account=:account") suspend fun checkpoint(account:String):SyncCheckpoint?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun checkpoint(value:SyncCheckpoint)
    @Query("DELETE FROM local_progress WHERE account=:account AND mutationId=:mutation") suspend fun discardProgress(account:String,mutation:String)
    @Query("DELETE FROM local_progress WHERE account=:account") suspend fun purgeProgress(account:String)
    @Query("DELETE FROM outbox WHERE account=:account") suspend fun purgeOutbox(account:String)
    @Query("DELETE FROM sync_checkpoints WHERE account=:account") suspend fun purgeCheckpoint(account:String)
}
@Database(entities=[CatalogCache::class,LocalProgress::class,OutboxMessage::class,SyncCheckpoint::class,
    LocalFolder::class,LocalSet::class,LocalCard::class,ServerCopy::class,FlashcardCheckpoint::class,EditorDraft::class,FlashcardPayloadChunk::class],version=4,exportSchema=true)
abstract class OfflineDatabase:RoomDatabase() {
    companion object {
        val MIGRATION_3_4 = object: androidx.room.migration.Migration(3,4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS flashcard_payload_chunks (account TEXT NOT NULL,`key` TEXT NOT NULL,position INTEGER NOT NULL,text TEXT NOT NULL,PRIMARY KEY(account,`key`,position))")
            }
        }
        val MIGRATION_2_3 = object: androidx.room.migration.Migration(2,3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS flashcard_folders_local (account TEXT NOT NULL,id TEXT NOT NULL,name TEXT NOT NULL,description TEXT NOT NULL,position INTEGER NOT NULL,version TEXT NOT NULL,mutationId TEXT,deleted INTEGER NOT NULL,PRIMARY KEY(account,id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS flashcard_sets_local (account TEXT NOT NULL,id TEXT NOT NULL,folderId TEXT NOT NULL,name TEXT NOT NULL,position INTEGER NOT NULL,version TEXT NOT NULL,mutationId TEXT,deleted INTEGER NOT NULL,PRIMARY KEY(account,id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_flashcard_sets_local_account_folderId ON flashcard_sets_local(account,folderId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS flashcards_local (account TEXT NOT NULL,id TEXT NOT NULL,setId TEXT NOT NULL,front TEXT NOT NULL,back TEXT NOT NULL,position INTEGER NOT NULL,PRIMARY KEY(account,id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_flashcards_local_account_setId ON flashcards_local(account,setId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS flashcard_server_copies (account TEXT NOT NULL,kind TEXT NOT NULL,id TEXT NOT NULL,version TEXT NOT NULL,payload TEXT NOT NULL,PRIMARY KEY(account,kind,id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS flashcard_checkpoints (account TEXT NOT NULL PRIMARY KEY,cursor TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS flashcard_editor_drafts (account TEXT NOT NULL,id TEXT NOT NULL,payload TEXT NOT NULL,PRIMARY KEY(account,id))")
            }
        }
        val MIGRATION_1_2 = object: androidx.room.migration.Migration(1,2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_outbox_account_state ON outbox(account,state)")
            }
        }
    }
    abstract fun flashcards():FlashcardDao
    abstract fun catalog():CatalogDao
    abstract fun progress():ProgressDao
}
