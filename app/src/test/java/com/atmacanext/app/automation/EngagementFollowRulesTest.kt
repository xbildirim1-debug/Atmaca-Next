package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class EngagementFollowRulesTest {
    private fun tab(text: String, selected: Boolean) = NodeSnapshot(text, null, null, "TextView", true, true, Rect(), selected = selected)
    @Test fun dynamicRepostCountsAreNotHardcoded() {
        for (label in listOf("30 tarafından yeniden gönderildi", "1.234 kişi tarafından yeniden gönderildi", "0 tarafından yeniden gönderildi", "45 reposts", "Reposts 900"))
            assertTrue(label, EngagementListEvidence.isReposts(label))
    }
    @Test fun quoteTabAndRepostActionAreNotThePeopleList() {
        for (label in listOf("Alıntılar 5", "Retweet", "Repost", "Yeniden gönder", "35 beğeni"))
            assertFalse(label, EngagementListEvidence.isReposts(label))
    }
    @Test fun visibleRepostsAreInsufficientUntilSelected() {
        assertFalse(EngagementListEvidence.selected(listOf(tab("30 tarafından yeniden gönderildi", false))))
        assertTrue(EngagementListEvidence.selected(listOf(tab("31 tarafından yeniden gönderildi", true))))
    }
    @Test fun requestedIsCompletedWithoutWaitingForApproval() {
        assertEquals(VerifiedFollowOutcome.SUCCESS, VerifiedFollowPolicy.outcome(false, false, false, 0, 100, requestedNow = true))
        assertFalse(VerifiedFollowPolicy.isPlainFollow(listOf("Beklemede")))
        assertFalse(VerifiedFollowPolicy.isPlainFollow(listOf("Follow", "Requested")))
        assertTrue(VerifiedFollowPolicy.matchesAction("Beklemede", XUiVocabulary.requestedActions))
        assertTrue(VerifiedFollowPolicy.matchesAction("Requested @private", XUiVocabulary.requestedActions))
    }
    @Test fun contradictoryRequestedAndFollowIsNotSuccess() {
        assertEquals(VerifiedFollowOutcome.WAIT, VerifiedFollowPolicy.outcome(false, false, true, 0, 100, requestedNow = true))
    }
    @Test fun twoHourBoundaryAndUnknownTime() {
        assertFalse(XTweetInspector.eligibleAge(119))
        assertTrue(XTweetInspector.eligibleAge(120))
        assertTrue(XTweetInspector.eligibleAge(720))
        assertFalse(XTweetInspector.eligibleAge(null))
        assertEquals(119L, XTweetInspector.parseAgeMinutes("119 dk"))
        assertEquals(120L, XTweetInspector.parseAgeMinutes("2 sa"))
        assertEquals(180L, XTweetInspector.parseAgeMinutes("3 h"))
        assertNull(XTweetInspector.parseAgeMinutes("Bu iş 2 saat sürdü"))
        assertNull(XTweetInspector.parseAgeMinutes("Yeni gönderi"))
    }
    @Test fun ageAndCountChangesDoNotCreateANewTweetKey() {
        assertEquals(XTweetInspector.stableTextKey("@target | 2 sa | Haber metni | 30 likes"),
            XTweetInspector.stableTextKey("@target | 3 sa | Haber metni | 31 likes"))
    }
    @Test fun pendingBreaksRevertStreakAndDoesNotCountAsRevert() {
        val outcome = VerifiedFollowPolicy.outcome(false, false, false, 0, 100, true)
        assertEquals(0, VerifiedFollowPolicy.nextStreak(2, outcome))
    }
    @Test fun engagementEntryOpensQuotesViewWithoutReposting() {
        assertTrue(EngagementListEvidence.openLabel("Alıntıları görüntüle"))
        assertFalse(EngagementListEvidence.openLabel("Yeniden gönder"))
    }
}
