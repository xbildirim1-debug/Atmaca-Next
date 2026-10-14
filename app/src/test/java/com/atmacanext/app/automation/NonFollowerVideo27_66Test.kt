package com.atmacanext.app.automation

import com.atmacanext.app.ai.PilotPolicy
import org.junit.Assert.*
import org.junit.Test

class NonFollowerVideo27_66Test {
    @Test fun releaseWaitsForTheStationarySecondSegment() {
        var move: ((Boolean) -> Unit)? = null
        var hold: ((Boolean) -> Unit)? = null
        val results = mutableListOf<Boolean>()
        val drag = HeldDragSequence({ move = it; true }, { hold = it; true }, results::add)
        assertTrue(drag.start()); assertEquals(HeldDragSequence.Phase.MOVING, drag.phase)
        move!!(true); assertEquals(HeldDragSequence.Phase.HOLDING, drag.phase); assertTrue(results.isEmpty())
        hold!!(true); assertEquals(listOf(true), results)
        hold!!(true); move!!(true); assertEquals(listOf(true), results)
    }
    @Test fun rejectedMovementDoesNotDispatchHoldOrReportSuccess() {
        var holds = 0; val results = mutableListOf<Boolean>()
        val drag = HeldDragSequence({ false }, { holds++; true }, results::add)
        assertFalse(drag.start()); assertFalse(drag.start()); assertEquals(0, holds); assertEquals(listOf(false), results)
    }
    @Test fun cancelledMovementEndsThePendingPointerOnce() {
        var move: ((Boolean) -> Unit)? = null; var holds = 0; val results = mutableListOf<Boolean>()
        HeldDragSequence({ move = it; true }, { holds++; true }, results::add).start()
        move!!(false); move!!(true); assertEquals(0, holds); assertEquals(listOf(false), results)
    }
    @Test fun rejectedContinuationCannotBecomeACompletedScroll() {
        var move: ((Boolean) -> Unit)? = null; val results = mutableListOf<Boolean>()
        HeldDragSequence({ move = it; true }, { false }, results::add).start()
        move!!(true); assertEquals(listOf(false), results)
    }
    @Test fun cancelledHoldCannotCountAsCompletedMovement() {
        var move: ((Boolean) -> Unit)? = null; var hold: ((Boolean) -> Unit)? = null; val results = mutableListOf<Boolean>()
        HeldDragSequence({ move = it; true }, { hold = it; true }, results::add).start()
        move!!(true); hold!!(false); hold!!(true); assertEquals(listOf(false), results)
    }
    @Test fun flingIsReversedWithoutCountingItsDistantPeople() {
        val run = NonFollowerFollowingRun(); run.observe((1..8).map { "user$it" })
        assertFalse(run.observe((14..20).map { "user$it" }))
        assertFalse(run.overlapsPrevious(listOf("user14"))); assertFalse(run.recoveryForward(listOf("user14")))
        assertEquals(8, run.seenCount)
        assertTrue(run.observe((8..14).map { "user$it" })); assertEquals(14, run.seenCount)
    }
    @Test fun restoredTopMovesForwardToThePreviousRange() {
        val run = NonFollowerFollowingRun(); run.observe((1..8).map { "user$it" }); run.observe((8..15).map { "user$it" })
        assertTrue(run.recoveryForward((1..7).map { "user$it" }))
        assertFalse(run.overlapsPrevious((1..7).map { "user$it" })); assertEquals(15, run.seenCount)
        assertTrue(run.overlapsPrevious(listOf("@USER8")))
    }
    @Test fun overlapRecoveryPreservesExactlyTheFirstHundred() {
        val run = NonFollowerFollowingRun()
        for (start in 1..96 step 5) run.observe((start..start + 9).map { "user$it" })
        val protected = run.protectedHandles
        assertTrue(run.recoveryForward((1..7).map { "user$it" })); assertEquals(protected, run.protectedHandles)
        assertEquals((1..100).map { "user$it" }.toSet(), protected)
    }
    @Test fun listStallDoesNotBackOutToTheProfileEvenWithOldFailureMemory() {
        for (attempt in 1..2) assertEquals(PilotPolicy.Move.REFRESH,
            PilotPolicy.stallMove(attempt, false, knownStreak = 9, screen = "FOLLOWING_LIST",
                failedRecoveries = 3, lastFailed = "REFRESH", taskType = "UNFOLLOW_NON_FOLLOWERS"))
    }
    @Test fun persistentListStallHasABoundedRestart() {
        assertEquals(PilotPolicy.Move.HOME, PilotPolicy.stallMove(3, false,
            screen = "FOLLOWING_LIST", taskType = "UNFOLLOW_NON_FOLLOWERS"))
    }
    @Test fun pendingUnfollowCannotBeRestartedAndIssuedTwice() {
        assertEquals(PilotPolicy.Move.CONTINUE, PilotPolicy.stallMove(9, true,
            screen = "FOLLOWING_LIST", taskType = "UNFOLLOW_NON_FOLLOWERS"))
    }
}
