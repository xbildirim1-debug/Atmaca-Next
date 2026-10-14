package com.atmacanext.app.automation

import org.junit.Assert.*
import org.junit.Test

class NonFollowerGesture27_65Test {
    @Test fun stableOldRowsDuringThe280MsDragCannotStartAnotherDrag() {
        val gate = NonFollowerViewportGate()
        gate.gestureStarted(1_000L)
        for (at in 1_016L..1_272L step 16L) {
            assertEquals(NonFollowerViewportGate.Decision.WAIT, gate.observe("unchanged rows", at))
        }
        gate.gestureFinished(true, 1_280L)
        assertEquals(NonFollowerViewportGate.Decision.WAIT, gate.observe("new rows", 1_300L))
        assertEquals(NonFollowerViewportGate.Decision.WAIT, gate.observe("new rows", 1_312L))
        assertEquals(NonFollowerViewportGate.Decision.READY, gate.observe("new rows", 1_344L))
    }

    @Test fun cancellationNeverCreatesACompletedUnchangedScrollBoundary() {
        val gate = NonFollowerViewportGate()
        val boundary = NonFollowerScrollBoundary()
        gate.gestureStarted(1_000L)
        boundary.afterScroll("old rows")
        gate.gestureFinished(false, 1_050L)
        assertEquals(NonFollowerViewportGate.Decision.CANCELLED, gate.observe("old rows", 1_100L))
        boundary.reset()
        assertFalse(boundary.observe("old rows"))
        assertEquals(0, boundary.unchangedAttempts)
    }

    @Test fun missingCompletionTimesOutInsteadOfApprovingTheOldViewport() {
        val gate = NonFollowerViewportGate()
        gate.gestureStarted(1_000L)
        assertEquals(NonFollowerViewportGate.Decision.WAIT, gate.observe("old rows", 2_000L))
        assertEquals(NonFollowerViewportGate.Decision.TIMED_OUT, gate.observe("old rows", 3_000L))
    }

    @Test fun lateCompletionAfterResetCannotAffectTheNextAccount() {
        val gate = NonFollowerViewportGate()
        gate.gestureStarted(1_000L)
        gate.reset()
        gate.gestureFinished(false, 1_200L)
        assertEquals(NonFollowerViewportGate.Decision.READY, gate.observe("next account", 1_300L))
    }

    @Test fun movementAfterReleaseRequiresNewStableRows() {
        val gate = NonFollowerViewportGate()
        gate.gestureStarted(1_000L)
        gate.gestureFinished(true, 1_280L)
        gate.observe("position 1", 1_312L)
        gate.observe("position 2", 1_344L)
        assertEquals(NonFollowerViewportGate.Decision.WAIT, gate.observe("position 2", 1_360L))
        assertEquals(NonFollowerViewportGate.Decision.READY, gate.observe("position 2", 1_376L))
    }

    @Test fun semanticScrollAlsoWaitsForItsTransitionDelay() {
        val gate = NonFollowerViewportGate()
        gate.afterScroll(1_000L, settleMs = 450L)
        gate.observe("old rows", 1_016L)
        assertEquals(NonFollowerViewportGate.Decision.WAIT, gate.observe("old rows", 1_448L))
        assertEquals(NonFollowerViewportGate.Decision.WAIT, gate.observe("new rows", 1_450L))
        assertEquals(NonFollowerViewportGate.Decision.READY, gate.observe("new rows", 1_482L))
    }

    @Test fun watchdogWaitsTenSecondsNotThreeBeforeInterruptingTheList() {
        val watchdog = AutomationWatchdog(AutomationStallPolicy.TIMEOUT_MS)
        watchdog.observe("following list", 1_000L)
        assertFalse(watchdog.isStuck(4_005L))
        assertFalse(watchdog.isStuck(10_999L))
        assertTrue(watchdog.isStuck(11_000L))
    }

    @Test fun realListProgressRestartsTheTenSecondDeadline() {
        val watchdog = AutomationWatchdog(AutomationStallPolicy.TIMEOUT_MS)
        watchdog.observe("first 8 users", 1_000L)
        watchdog.observe("first 12 users", 9_000L)
        assertFalse(watchdog.isStuck(11_000L))
        assertTrue(watchdog.isStuck(19_000L))
    }
}
