package com.atmacanext.app.automation

import org.junit.Assert.*
import org.junit.Test

class NonFollowerScrollProgressTest {
    @Test fun idleConfirmationReadsNeverProveListEnd() {
        val boundary = NonFollowerScrollBoundary()
        repeat(100) { assertFalse(boundary.observe("same rows after an unfollow")) }
        assertEquals(0, boundary.unchangedAttempts)
    }

    @Test fun onlyThreeCompletedUnchangedScrollsProveABoundary() {
        val boundary = NonFollowerScrollBoundary()
        repeat(3) { attempt ->
            boundary.afterScroll("viewport")
            assertEquals(attempt == 2, boundary.observe("viewport"))
        }
    }

    @Test fun additionalReadsDoNotInventAdditionalScrollAttempts() {
        val boundary = NonFollowerScrollBoundary()
        boundary.afterScroll("viewport")
        assertFalse(boundary.observe("viewport"))
        repeat(100) { assertFalse(boundary.observe("viewport")) }
        assertEquals(1, boundary.unchangedAttempts)
    }

    @Test fun movementBreaksTheBoundaryProof() {
        val boundary = NonFollowerScrollBoundary()
        repeat(2) { boundary.afterScroll("old"); assertFalse(boundary.observe("old")) }
        boundary.afterScroll("old")
        assertFalse(boundary.observe("older"))
        assertEquals(0, boundary.unchangedAttempts)
        repeat(2) { boundary.afterScroll("older"); assertFalse(boundary.observe("older")) }
    }

    @Test fun anActionClearsOldScrollHistoryBeforeTheNextCandidate() {
        val boundary = NonFollowerScrollBoundary()
        repeat(2) { boundary.afterScroll("viewport"); boundary.observe("viewport") }
        boundary.reset()
        assertFalse(boundary.observe("viewport"))
        boundary.afterScroll("viewport")
        assertFalse(boundary.observe("viewport"))
        assertEquals(1, boundary.unchangedAttempts)
    }

    @Test fun anotherAccountCannotInheritABoundary() {
        val boundary = NonFollowerScrollBoundary()
        repeat(3) { boundary.afterScroll("account-one"); boundary.observe("account-one") }
        boundary.reset()
        assertFalse(boundary.observe("account-two"))
        assertEquals(0, boundary.unchangedAttempts)
    }

    @Test fun denseViewportAdvancesMuchMoreThanOneOrTwoPeopleWithTwoRowsRetained() {
        val tops = (100..1700 step 200).toList()
        val distance = NonFollowerScrollStride.distance(100, 1900, tops)
        assertTrue(distance > 1_000f)
        assertTrue(tops.count { it - distance >= 100 } >= 2)
    }

    @Test fun variableHeightRowsStillRetainTwoActualPeople() {
        val tops = listOf(100, 300, 450, 800, 1700)
        val distance = NonFollowerScrollStride.distance(100, 2200, tops)
        assertTrue(distance > 0)
        assertTrue(tops.count { it - distance >= 100 } >= 2)
    }

    @Test fun twoRowViewportRetainsOneCountingAnchor() {
        val tops = listOf(100, 500)
        val distance = NonFollowerScrollStride.distance(100, 900, tops)
        assertTrue(distance > 0)
        assertTrue(tops.any { it - distance >= 100 })
    }

    @Test fun oneLargeRowOnlyMovesPartOfItsHeight() {
        val distance = NonFollowerScrollStride.distance(100, 900, listOf(100))
        assertTrue(distance > 0 && distance < 400)
    }

    @Test fun emptyOrInvalidGeometryCannotProduceADrag() {
        assertEquals(0f, NonFollowerScrollStride.distance(100, 100, listOf(100)), 0f)
        assertEquals(0f, NonFollowerScrollStride.distance(100, 900, emptyList()), 0f)
    }
}
