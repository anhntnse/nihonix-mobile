package com.nihonix.mobile.feature.activity
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Entity(tableName="activity_cache",primaryKeys=["account","key"])
data class ActivityCache(val account:String,val key:String,val payload:String,val savedAt:Long)
@Dao interface ActivityDao{
 @Query("SELECT * FROM activity_cache WHERE account=:owner")fun observe(owner:String):Flow<List<ActivityCache>>
 @Query("SELECT * FROM activity_cache WHERE account=:owner AND `key`=:key")suspend fun get(owner:String,key:String):ActivityCache?
 @Insert(onConflict=OnConflictStrategy.REPLACE)suspend fun put(value:ActivityCache)
 @Query("DELETE FROM activity_cache WHERE account=:owner")suspend fun purge(owner:String)
}
