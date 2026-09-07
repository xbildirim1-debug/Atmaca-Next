package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class VerifiedFollowFlowTest {
    private fun n(text: String, selected: Boolean = false, visible: Boolean = true) =
        NodeSnapshot(text, null, null, "TextView", false, true, Rect(), selected = selected, visible = visible)
    @Test fun followsOnlyPlainButtonsInBothLanguages() {
        for (label in listOf("Takip et", "Follow", "Follow @someone", "Takip et, @someone"))
            assertTrue(VerifiedFollowPolicy.isPlainFollow(listOf(label)))
        for (label in listOf("Geri takip et", "Follow back", "Follow back @someone", "Sen de takip et", "Takip ediliyor", "Following"))
            assertFalse(VerifiedFollowPolicy.isPlainFollow(listOf(label)))
    }
    @Test fun conflictingDescriptionCannotFollowBack() {
        assertFalse(VerifiedFollowPolicy.isPlainFollow(listOf("Follow", "Follow back @someone")))
        assertFalse(VerifiedFollowPolicy.isPlainFollow(listOf("Takip et", "Takip ediliyor")))
    }
    @Test fun selectedVerifiedTabHasItsOwnIdentity() {
        for (label in listOf("Verified Followers", "Onaylı Takipçiler, sekme 1/4", "Doğrulanmış Takipçiler"))
            assertEquals(RelationshipTabInspector.VERIFIED, RelationshipTabInspector.classifySelectedLabels(listOf(label)))
    }
    @Test fun visibleUnselectedVerifiedHeaderIsNotPermission() {
        val nodes = listOf(n("Verified Followers"), n("Followers", true), n("@one"), n("@two"))
        assertEquals(XScreen.FOLLOWERS_LIST, ScreenDetector.detect(nodes))
    }
    @Test fun selectedVerifiedWinsOverNeighborFollowersHeader() {
        assertEquals(XScreen.VERIFIED_FOLLOWERS_LIST, ScreenDetector.detect(listOf(
            n("Verified Followers", true), n("Followers"), n("Following"), n("@one"))))
    }
    @Test fun hiddenVerifiedSelectionCannotStartFollowing() {
        assertNotEquals(XScreen.VERIFIED_FOLLOWERS_LIST, ScreenDetector.detect(listOf(
            n("Verified Followers", true, false), n("Followers", true), n("@one"))))
    }
    @Test fun successWaitsForStableFollowing() {
        assertEquals(VerifiedFollowOutcome.WAIT, VerifiedFollowPolicy.outcome(true, true, false, 1999, 2500))
        assertEquals(VerifiedFollowOutcome.SUCCESS, VerifiedFollowPolicy.outcome(true, true, false, 2000, 2500))
    }
    @Test fun unchangedFollowDoesNotProveReversion() {
        assertEquals(VerifiedFollowOutcome.WAIT, VerifiedFollowPolicy.outcome(false, false, true, 0, 1000))
        assertEquals(VerifiedFollowOutcome.UNKNOWN, VerifiedFollowPolicy.outcome(false, false, true, 0, 7000))
    }
    @Test fun threeRealConsecutiveRevertsStopAccount() {
        var streak = 0
        repeat(3) { index ->
            val result = VerifiedFollowPolicy.outcome(true, false, true, 0, 900)
            assertEquals(VerifiedFollowOutcome.REVERTED, result)
            streak = VerifiedFollowPolicy.nextStreak(streak, result)
            assertEquals(index == 2, VerifiedFollowPolicy.stopAccount(streak))
        }
    }
    @Test fun successfulOrUnknownAttemptBreaksConsecutiveStreak() {
        assertEquals(0, VerifiedFollowPolicy.nextStreak(2, VerifiedFollowOutcome.SUCCESS))
        assertEquals(0, VerifiedFollowPolicy.nextStreak(2, VerifiedFollowOutcome.UNKNOWN))
        assertEquals(2, VerifiedFollowPolicy.nextStreak(2, VerifiedFollowOutcome.WAIT))
    }
    @Test fun nextSourceUsesOpenListAndAvoidsOwnAndVisitedProfiles() {
        val picked = (0..40).map { seed -> VerifiedFollowPolicy.nextSource(
            listOf("self", "old", "next", "later"), "self", setOf("old"), kotlin.random.Random(seed)) }.toSet()
        assertEquals(setOf("next", "later"), picked)
        assertNull(VerifiedFollowPolicy.nextSource(listOf("self", "old"), "self", setOf("old")))
    }
}
