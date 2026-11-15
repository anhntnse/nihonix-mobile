package com.nihonix.mobile.feature.activity
import android.os.SystemClock
import kotlinx.coroutines.*
import java.time.Instant
import java.util.UUID
/** One foreground lease across study destinations; immutable checkpoints prevent duplicate credit. */
class StudyTracker(private val repository:ActivityRepository,private val scope:CoroutineScope){
 private data class Target(val lease:String,val owner:String,val kind:String,val item:String,val timezone:String)
 private var target:Target?=null;private var clock=EngagedClock();
 private var wall=Instant.EPOCH;private var origin=0L
 @Synchronized fun start(owner:String,kind:String,item:String,timezone:String):String{
  stop(target?.lease);val now=SystemClock.elapsedRealtime()
  val lease=UUID.randomUUID().toString();target=Target(lease,owner,kind,item,timezone);clock=EngagedClock()
  origin=now;wall=Instant.now();clock.foreground(true,now);clock.interact(now);return lease
 }
 @Synchronized fun interact(){if(target==null)return;checkpoint(target?.lease);clock.interact(SystemClock.elapsedRealtime())}
 @Synchronized fun checkpoint(lease:String?){
  val current=target?:return;if(lease!=current.lease)return
  val slices=clock.drain(SystemClock.elapsedRealtime())
  if(slices.isEmpty()||repository.account()!=current.owner)return
  val capturedWall=wall;val capturedOrigin=origin
  scope.launch{
   for(slice in slices){
    var remaining=slice.durationSeconds;var at=capturedWall.plusMillis(slice.startedAt-capturedOrigin)
    while(remaining>0){
     val duration=minOf(remaining,300)
     runCatching{repository.record(current.owner,ActivitySegment(UUID.randomUUID().toString(),current.kind,at.toString(),duration,current.timezone,current.item))}
     remaining-=duration;at=at.plusSeconds(duration.toLong())
    }
   }
  }
 }
 @Synchronized fun stop(lease:String?){if(target?.lease!=lease)return;checkpoint(lease);clock.foreground(false,SystemClock.elapsedRealtime());target=null}
}
