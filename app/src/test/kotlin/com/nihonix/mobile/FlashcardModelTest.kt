package com.nihonix.mobile
import com.nihonix.mobile.feature.flashcards.domain.*
import org.junit.Test
import org.junit.Assert.*
class FlashcardModelTest {
    private val card=CardDraft("00000000-0000-4000-8000-000000000001",CardFace("こんにちは"),CardFace("Hello"))
    private val set=SetDraft("00000000-0000-4000-8000-000000000002","00000000-0000-4000-8000-000000000003","Greetings",listOf(card))
    @Test fun duplicateIdsCannotSilentlyOverwriteCards(){assertNotNull(validateSet(set.copy(cards=listOf(card,card))))}
    @Test fun blankOrOversizedFacesAreRejected() {
        val blank=card.copy(front=CardFace("  "))
        val long=card.copy(back=CardFace("あ".repeat(4001)))
        assertNotNull(validateSet(set.copy(cards=listOf(blank))))
        assertNotNull(validateSet(set.copy(cards=listOf(long))))
    }
    @Test fun namesAndBulkSizeAreBounded() {
        assertNotNull(validateSet(set.copy(name="")))
        assertNotNull(validateSet(set.copy(cards=List(201){card})))
    }
    @Test fun emptyNamedSetsAndJapaneseTextAreValid(){assertNull(validateSet(set));assertNull(validateSet(set.copy(cards=emptyList())))}
    @Test fun reviewNavigationResetsSideAndStopsAtTheEnd(){
        val state=ReviewState(listOf("a","b")).flip().next()
        assertEquals(1,state.position);assertFalse(state.back)
        assertEquals(2,state.next().next().position)
        assertEquals(0,state.previous().previous().position)
    }
}
