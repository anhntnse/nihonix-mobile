package com.nihonix.mobile
import com.nihonix.mobile.feature.activity.EngagedClock
import com.nihonix.mobile.feature.notifications.NotificationPayload
import org.junit.Test
import org.junit.Assert.*
class ActivityModelTest{
 @Test fun recordsOnlyForegroundEngagedTimeAndCheckpointsOnce(){
  val c=EngagedClock();c.foreground(true,0);c.interact(0)
  assertEquals(15,c.takeSeconds(15000));assertEquals(0,c.takeSeconds(15000))
  assertEquals(45,c.takeSeconds(90000));assertEquals(0,c.takeSeconds(120000))
  c.interact(120000);c.foreground(false,125000)
  assertEquals(5,c.takeSeconds(180000));assertEquals(0,c.takeSeconds(200000))
 }
 @Test fun reopeningForegroundDoesNotCountBackgroundTime(){
  val c=EngagedClock();c.foreground(true,0);c.interact(0);c.foreground(false,10000)
  assertEquals(10,c.takeSeconds(100000));c.foreground(true,110000);c.interact(110000)
  assertEquals(5,c.takeSeconds(115000))
 }
 @Test fun fractionalCheckpointsKeepTheOriginalOccurrenceBeforeMidnight(){
  val c=EngagedClock();c.foreground(true,0);c.interact(0)
  assertTrue(c.drain(600).isEmpty())
  val slice=c.drain(1200).single()
  assertEquals(0L,slice.startedAt);assertEquals(1,slice.durationSeconds)
 }
 @Test fun notificationPayloadRejectsOtherOwnersBindingsAndDestinations(){
  val data=mapOf("schema_version" to "1","type" to "learning_reminder","destination" to "activity","user_id" to "a","binding_id" to "binding","notification_id" to "id")
  assertNotNull(NotificationPayload.parse(data,"a","binding"))
  assertNull(NotificationPayload.parse(data,"b","binding"))
  assertNull(NotificationPayload.parse(data,"a","new-binding"))
  assertNull(NotificationPayload.parse(data+("destination" to "https://example.test"),"a","binding"))
 }
}
