package com.nihonix.mobile
import com.nihonix.mobile.feature.practice.domain.*
import org.junit.Test
import org.junit.Assert.*
class PracticeModelTest{
 @Test fun quotaUnknownIsNotZeroAndReservedAllowanceIsNotReusable(){
  assertFalse(canGenerate(null,"practice"))
  val quota=AiQuota("2026-11-15","2026-11-15T17:00:00Z","Asia/Ho_Chi_Minh",15,AiProvider(true),mapOf("practice" to AiBucket(3,2,1,0)))
  assertFalse(canGenerate(quota,"practice"))
  assertTrue(canGenerate(quota.copy(buckets=mapOf("practice" to AiBucket(3,1,0,2))),"practice"))
 }
 @Test fun everyQuestionMustBeAnsweredExactlyOnceWithoutLocalGrading(){
  val set=PracticeSet("s","t",mapOf("en" to "Animals"),"N5","fill_blank",1,"in_progress",exercises=listOf(PracticeQuestion("q","fill_blank",QuestionText("____"),emptyList(),1)))
  assertNotNull(validateAnswers(set,emptyMap()))
  assertNull(validateAnswers(set,mapOf("q" to "猫")))
  assertNotNull(validateAnswers(set,mapOf("q" to "猫","foreign" to "answer")))
 }
 @Test fun multipleChoiceAcceptsOnlyAnOptionId(){
  val q=PracticeQuestion("q","multiple_choice",QuestionText("Cat"),listOf(PracticeOption(1,"猫"),PracticeOption(2,"犬")),1)
  val set=PracticeSet("s","t",emptyMap(),"N5","multiple_choice",1,"in_progress",exercises=listOf(q))
  assertNull(validateAnswers(set,mapOf("q" to 1)))
  assertNotNull(validateAnswers(set,mapOf("q" to "1")))
  assertNotNull(validateAnswers(set,mapOf("q" to 3)))
 }
}
