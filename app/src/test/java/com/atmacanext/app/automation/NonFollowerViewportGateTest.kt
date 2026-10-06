package com.atmacanext.app.automation

import org.junit.Assert.*
import org.junit.Test

class NonFollowerViewportGateTest {
    @Test fun movingRowsCannotTriggerCountingOrAnotherScroll() {
        val gate = NonFollowerViewportGate()
        gate.afterScroll(1_000L)
        for (step in 0..12) {
            assertEquals(NonFollowerViewportGate.Decision.WAIT,
                gate.observe("first:${500 - step * 20}|second:${700 - step * 20}", 1_000L + step * 32L))
        }
        assertTrue(gate.pending)
    }

    @Test fun freshUnchangedViewportCanStillProveAListBoundary() {
        val gate = NonFollowerViewportGate()
        gate.afterScroll(1_000L)
        assertEquals(NonFollowerViewportGate.Decision.WAIT, gate.observe("first:500|second:700", 1_500L))
        assertEquals(NonFollowerViewportGate.Decision.WAIT, gate.observe("first:500|second:700", 1_516L))
        assertEquals(NonFollowerViewportGate.Decision.READY, gate.observe("first:500|second:700", 1_532L))
        assertFalse(gate.pending)
    }

    @Test fun missingTreeBetweenReadsBreaksTheStabilityProof() {
        val gate = NonFollowerViewportGate()
        gate.afterScroll(1_000L)
        gate.observe("first:500", 1_020L)
        gate.observe(null, 1_070L)
        assertEquals(NonFollowerViewportGate.Decision.WAIT, gate.observe("first:500", 1_090L))
        assertEquals(NonFollowerViewportGate.Decision.WAIT, gate.observe("first:500", 1_120L))
        assertEquals(NonFollowerViewportGate.Decision.READY, gate.observe("first:500", 1_154L))
    }

    @Test fun endlessMovementTimesOutWithoutInventingASafeViewport() {
        val gate = NonFollowerViewportGate()
        gate.afterScroll(1_000L)
        for (step in 0..19) assertEquals(NonFollowerViewportGate.Decision.WAIT,
            gate.observe("row:$step", 1_000L + step * 100L))
        assertEquals(NonFollowerViewportGate.Decision.TIMED_OUT, gate.observe("row:20", 3_000L))
    }

    @Test fun newScrollRequiresNewProofEvenAfterTheLastOneSettled() {
        val gate = NonFollowerViewportGate()
        gate.afterScroll(1_000L)
        gate.observe("row:500", 1_020L)
        assertEquals(NonFollowerViewportGate.Decision.READY, gate.observe("row:500", 1_084L))
        gate.afterScroll(1_200L)
        assertEquals(NonFollowerViewportGate.Decision.WAIT, gate.observe("row:500", 1_300L))
    }

    @Test fun newAccountDoesNotInheritPendingScroll() {
        val gate = NonFollowerViewportGate()
        gate.afterScroll(1_000L)
        gate.observe(null, 1_020L)
        gate.reset()
        assertEquals(NonFollowerViewportGate.Decision.READY, gate.observe("own:500", 1_030L))
    }

    @Test fun transientDisjointRowsDoNotPauseOrAdvanceTheProtectionCount() {
        val gate = NonFollowerViewportGate()
        val run = NonFollowerFollowingRun()
        assertTrue(run.observe((1..8).map { "user$it" }))
        gate.afterScroll(1_000L)
        fun read(handles: List<String>, at: Long): Boolean {
            if (gate.observe(handles.joinToString("|"), at) != NonFollowerViewportGate.Decision.READY) return true
            return run.observe(handles)
        }
        assertTrue(read((50..55).map { "user$it" }, 1_020L))
        assertEquals(8, run.seenCount)
        assertTrue(read((5..12).map { "user$it" }, 1_052L))
        assertTrue(read((5..12).map { "user$it" }, 1_116L))
        assertEquals(12, run.seenCount)
        assertFalse(run.ready)
    }

    @Test fun stableOverlappingWindowsProtectExactlyTheFirst100People() {
        val gate = NonFollowerViewportGate()
        val run = NonFollowerFollowingRun()
        assertTrue(run.observe((1..8).map { "user$it" }))
        for (start in 5..97 step 4) {
            val handles = (start..start + 7).map { "user$it" }
            val signature = handles.joinToString("|")
            val now = start * 1_000L
            gate.afterScroll(now)
            assertEquals(NonFollowerViewportGate.Decision.WAIT, gate.observe(signature, now + 32L))
            assertEquals(NonFollowerViewportGate.Decision.READY, gate.observe(signature, now + 96L))
            assertTrue(run.observe(handles))
            assertTrue(run.observe(handles))
        }
        assertEquals(104, run.seenCount)
        assertEquals((1..100).map { "user$it" }.toSet(), run.protectedHandles)
        assertTrue(run.ready)
    }
}
