package com.nihonix.mobile.feature.activity
data class EngagedSlice(val startedAt:Long,val durationSeconds:Int)
/** Carries occurrence time with accumulated milliseconds; idle gaps are never merged. */
class EngagedClock{
 private data class Range(var start:Long,var end:Long)
 private val ranges=ArrayDeque<Range>()
 private var active=false;private var last=0L;private var interaction=0L
 private fun advance(now:Long){
  if(active){
   val end=minOf(now,interaction+60000)
   if(end>last){
    val tail=ranges.lastOrNull()
    if(tail?.end==last)tail.end=end else ranges.addLast(Range(last,end))
   }
  }
  last=now
 }
 @Synchronized fun foreground(active:Boolean,now:Long){advance(now);this.active=active}
 @Synchronized fun interact(now:Long){advance(now);interaction=now}
 @Synchronized fun drain(now:Long):List<EngagedSlice>{
  advance(now);val output=mutableListOf<EngagedSlice>();val retained=ArrayDeque<Range>()
  for(range in ranges){
   val seconds=((range.end-range.start)/1000).toInt()
   if(seconds>0){output+=EngagedSlice(range.start,seconds);range.start+=seconds*1000L}
   // Keep fractional time only while this continuous engagement can still grow.
   if(range.end>range.start&&active&&range.end==last&&last<=interaction+60000)retained.addLast(range)
  }
  ranges.clear();ranges.addAll(retained);return output
 }
 @Synchronized fun takeSeconds(now:Long)=drain(now).sumOf{it.durationSeconds}
}
