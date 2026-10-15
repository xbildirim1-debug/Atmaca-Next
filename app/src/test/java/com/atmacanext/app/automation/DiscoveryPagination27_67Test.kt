package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class DiscoveryPagination27_67Test {
    private fun row(text: String, top: Int = 200) = NodeSnapshot(text, null, null, "TextView", false, true,
        Rect().apply { left = 100; this.top = top; right = 800; bottom = top + 100 })
    @Test fun movingFingerCannotBecomeAReadyEmptyList() {
        val pace = DiscoveryPaginationPace(); pace.arm("old", 0)
        assertEquals(DiscoveryPaginationPace.Reading.WAIT, pace.read("old", 1000))
    }
    @Test fun heldReleaseRequiresFreshStableGeometry() {
        val pace = DiscoveryPaginationPace(); pace.arm("old", 0); pace.finished(true, 440)
        assertEquals(DiscoveryPaginationPace.Reading.WAIT, pace.read("new", 500))
        assertEquals(DiscoveryPaginationPace.Reading.WAIT, pace.read("new", 560))
        assertEquals(DiscoveryPaginationPace.Reading.MOVED, pace.read("new", 680)); assertFalse(pace.pending)
    }
    @Test fun bottomOfListIsStillOnlyAfterACompletedHeldDrag() {
        val pace = DiscoveryPaginationPace(); pace.arm("same", 0); pace.finished(true, 440)
        pace.read("same", 560)
        assertEquals(DiscoveryPaginationPace.Reading.STILL, pace.read("same", 680))
    }
    @Test fun cancellationIsNeverMistakenForListEnd() {
        val pace = DiscoveryPaginationPace(); pace.arm("same", 0); pace.finished(false, 100)
        assertEquals(DiscoveryPaginationPace.Reading.CANCELLED, pace.read("same", 300))
    }
    @Test fun absentCallbackHasABoundedRecovery() {
        val pace = DiscoveryPaginationPace(); pace.arm("same", 0)
        assertEquals(DiscoveryPaginationPace.Reading.TIMEOUT, pace.read("same", 2500)); assertFalse(pace.pending)
    }
    @Test fun recyclingRowsRestartTheReadWindow() {
        val pace = DiscoveryPaginationPace(); pace.arm("old", 0); pace.finished(true, 440)
        pace.read("a", 560); pace.read("b", 600)
        assertEquals(DiscoveryPaginationPace.Reading.WAIT, pace.read("b", 700))
        assertEquals(DiscoveryPaginationPace.Reading.MOVED, pace.read("b", 720))
    }
    @Test fun autoplayTimerDoesNotInventListMovement() {
        assertEquals(DiscoveryPaginationPace.signature(listOf(row("Video 00:10"))), DiscoveryPaginationPace.signature(listOf(row("Video 00:11"))))
    }
    @Test fun realNewUserAndGeometryRemainMovementEvidence() {
        assertNotEquals(DiscoveryPaginationPace.signature(listOf(row("@first"))), DiscoveryPaginationPace.signature(listOf(row("@second"))))
        assertNotEquals(DiscoveryPaginationPace.signature(listOf(row("@first"))), DiscoveryPaginationPace.signature(listOf(row("@first", 400))))
    }
    @Test fun verifiedRepostListSurvivesItsTabScrollingOffScreen() {
        assertTrue(EngagementListEvidence.continuesVerifiedList(true, true, XScreen.UNKNOWN, true, listOf(row("@user"))))
        assertTrue(EngagementListEvidence.continuesVerifiedList(true, true, XScreen.ENGAGEMENT_LIST, true, listOf(row("@user"))))
    }
    @Test fun wrongTabWrongWindowAndProfileCannotInheritRetweetIdentity() {
        assertFalse(EngagementListEvidence.continuesVerifiedList(true, false, XScreen.ENGAGEMENT_LIST, true, emptyList()))
        assertFalse(EngagementListEvidence.continuesVerifiedList(false, true, XScreen.ENGAGEMENT_LIST, true, emptyList()))
        assertFalse(EngagementListEvidence.continuesVerifiedList(true, true, XScreen.PROFILE, true, emptyList()))
        assertFalse(EngagementListEvidence.continuesVerifiedList(true, true, XScreen.ENGAGEMENT_LIST, true, listOf(row("Alıntılar").copy(selected = true))))
    }
    @Test fun discoverMoreMarkerIsAnExplicitBoundary() {
        assertTrue(ReplyThreadEndEvidence.visible(listOf(row("Daha fazlasını keşfet"), row("@suggested", 400))))
    }
    @Test fun twoHourEligibilityIsPreservedWhileScrollingChanges() {
        assertFalse(XTweetInspector.eligibleAge(119)); assertTrue(XTweetInspector.eligibleAge(120)); assertTrue(XTweetInspector.eligibleAge(150))
    }
}
