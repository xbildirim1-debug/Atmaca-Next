package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommenterViewportPolicyTest {
    @Before fun resetGuard() {
        CommenterViewportPolicy.clearScrollGuard()
    }

    @Test fun handledTopCommenterDoesNotForceScrollPastOtherVisibleUsers() {
        val visible = listOf("first", "second", "third")
        val excluded = setOf("first")
        assertEquals("second", CommenterViewportPolicy.nextVisible(visible, excluded))
        assertFalse(CommenterViewportPolicy.shouldScroll(visible, excluded, nowMillis = 1_000L))
    }

    @Test fun oneEmptySnapshotAfterBackNeverScrollsImmediately() {
        val visible = listOf("first")
        val excluded = setOf("first")
        assertNull(CommenterViewportPolicy.nextVisible(visible, excluded))
        assertFalse(CommenterViewportPolicy.shouldScroll(visible, excluded, nowMillis = 1_000L))
        assertFalse(CommenterViewportPolicy.allowScrollAfterStableEmpty(visible, "viewport-a", 1_100L))
    }

    @Test fun viewportScrollsOnlyAfterStableEmptyEvidence() {
        val visible = listOf("first", "second", "third")
        val excluded = setOf("first", "second", "third")
        assertFalse(CommenterViewportPolicy.shouldScroll(visible, excluded, nowMillis = 1_000L))
        assertFalse(CommenterViewportPolicy.allowScrollAfterStableEmpty(visible, "viewport-a", 1_100L))
        assertFalse(CommenterViewportPolicy.allowScrollAfterStableEmpty(visible, "viewport-a", 1_300L))
        assertTrue(CommenterViewportPolicy.allowScrollAfterStableEmpty(visible, "viewport-a", 1_500L))
    }

    @Test fun commenterAppearingDuringSettleCancelsScroll() {
        val excluded = setOf("first")
        assertFalse(CommenterViewportPolicy.shouldScroll(listOf("first"), excluded, nowMillis = 1_000L))
        assertFalse(CommenterViewportPolicy.allowScrollAfterStableEmpty(listOf("first"), "viewport-a", 1_100L))
        assertFalse(CommenterViewportPolicy.allowScrollAfterStableEmpty(listOf("first", "second"), "viewport-a", 1_300L))
        assertEquals("second", CommenterViewportPolicy.nextVisible(listOf("first", "second"), excluded))
        // Guard is cancelled; a future unrelated discovery scroll is transparent.
        assertTrue(CommenterViewportPolicy.allowScrollAfterStableEmpty(emptyList(), "other", 1_500L))
    }

    @Test fun changedViewportRestartsEmptyConfirmation() {
        val visible = listOf("first")
        val excluded = setOf("first")
        assertFalse(CommenterViewportPolicy.shouldScroll(visible, excluded, nowMillis = 1_000L))
        assertFalse(CommenterViewportPolicy.allowScrollAfterStableEmpty(visible, "viewport-a", 1_100L))
        assertFalse(CommenterViewportPolicy.allowScrollAfterStableEmpty(visible, "viewport-a", 1_300L))
        assertFalse(CommenterViewportPolicy.allowScrollAfterStableEmpty(visible, "viewport-b", 1_500L))
        assertFalse(CommenterViewportPolicy.allowScrollAfterStableEmpty(visible, "viewport-b", 1_700L))
        assertTrue(CommenterViewportPolicy.allowScrollAfterStableEmpty(visible, "viewport-b", 1_900L))
    }

    @Test fun ownAccountAndTargetCanBeExcludedWithoutSkippingOtherCommenters() {
        val visible = listOf("target", "own", "commenter")
        assertEquals("commenter", CommenterViewportPolicy.nextVisible(visible, setOf("target", "own")))
    }
}
