package com.nihonix.mobile.core.offline

import androidx.room.*
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
    @Query("SELECT * FROM local_progress WHERE account=:account") fun observe(account:String):Flow<List<LocalProgress>>
    @Query("SELECT * FROM local_progress WHERE account=:account") suspend fun all(account:String):List<LocalProgress>
    @Query("SELECT * FROM local_progress WHERE account=:account AND kind=:kind AND itemId=:id")
    suspend fun get(account:String,kind:String,id:String):LocalProgress?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun put(row:LocalProgress)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun enqueue(message:OutboxMessage)
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
@Database(entities=[CatalogCache::class,LocalProgress::class,OutboxMessage::class,SyncCheckpoint::class],version=2,exportSchema=true)
abstract class OfflineDatabase:RoomDatabase() {
    companion object {
        val MIGRATION_1_2 = object: androidx.room.migration.Migration(1,2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_outbox_account_state ON outbox(account,state)")
            }
        }
    }
    abstract fun catalog():CatalogDao
    abstract fun progress():ProgressDao
}
