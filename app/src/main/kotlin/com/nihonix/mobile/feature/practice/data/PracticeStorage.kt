package com.nihonix.mobile.feature.practice.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Entity(tableName="practice_cache",primaryKeys=["account","key"])
data class PracticeCache(val account:String,val key:String,val kind:String,val payload:String,val savedAt:Long)
@Dao interface PracticeDao{
 @Query("SELECT * FROM practice_cache WHERE account=:account") fun observe(account:String):Flow<List<PracticeCache>>
 @Query("SELECT * FROM practice_cache WHERE account=:account") suspend fun all(account:String):List<PracticeCache>
 @Query("SELECT * FROM practice_cache WHERE account=:account AND `key`=:key") suspend fun get(account:String,key:String):PracticeCache?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun put(row:PracticeCache)
 @Query("DELETE FROM practice_cache WHERE account=:account AND `key`=:key") suspend fun remove(account:String,key:String)
 @Query("DELETE FROM practice_cache WHERE account=:account") suspend fun purge(account:String)
}
