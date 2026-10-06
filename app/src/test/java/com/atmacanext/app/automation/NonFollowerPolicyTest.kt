package com.atmacanext.app.automation

import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskType
import org.junit.Assert.*
import org.junit.Test

class NonFollowerPolicyTest {
    private fun row(handle: String = "older", labels: List<String> = listOf("Older", "@older", "Takip ediliyor"),
                    complete: Boolean = true, visible: Boolean = true, enabled: Boolean = true,
                    following: Boolean = true, treeComplete: Boolean = true, handles: Set<String> = setOf(handle)) =
        NonFollowerRowEvidence(handle, handles, labels, complete, visible, enabled, following, treeComplete)
    private fun prepared(): NonFollowerFollowingRun = NonFollowerFollowingRun().also { run ->
        assertTrue(run.observe((1..100).map { "user$it" }))
        assertTrue(run.observe(listOf("user100", "older", "mutual")))
    }

    @Test fun turkishBadgeProtectsItsOwner() { assertEquals(NonFollowerRelationship.FOLLOWS_YOU, NonFollowerPolicy.relationship(row(labels = listOf("Seni takip ediyor", "@older")))) }
    @Test fun englishBadgeProtectsItsOwner() { assertEquals(NonFollowerRelationship.FOLLOWS_YOU, NonFollowerPolicy.relationship(row(labels = listOf("Follows you")))) }
    @Test fun badgeWithAccessibilityStateProtectsItsOwner() { assertEquals(NonFollowerRelationship.FOLLOWS_YOU, NonFollowerPolicy.relationship(row(labels = listOf("Seni takip ediyor, metin")))) }
    @Test fun whitespaceAndCaseDoNotRemoveMutualFollower() { assertEquals(NonFollowerRelationship.FOLLOWS_YOU, NonFollowerPolicy.relationship(row(labels = listOf("  SENİ   TAKİP EDİYOR  ")))) }
    @Test fun mergedRowDescriptionStillProtectsBadgeOwner() { assertEquals(NonFollowerRelationship.FOLLOWS_YOU, NonFollowerPolicy.relationship(row(labels = listOf("Seni takip ediyor. Older @older Takip ediliyor")))) }
    @Test fun completeNonMutualRowIsEligible() { assertEquals(NonFollowerRelationship.DOES_NOT_FOLLOW, NonFollowerPolicy.relationship(row())) }
    @Test fun topClippedBadgeCannotBecomeAbsentBadge() { assertEquals(NonFollowerRelationship.UNKNOWN, NonFollowerPolicy.relationship(row(complete = false))) }
    @Test fun bottomClippedRowCannotProveAbsence() { assertFalse(prepared().mayUnfollow(row(complete = false), "own", emptySet())) }
    @Test fun mergedNeighborsCannotProveAbsence() { assertEquals(NonFollowerRelationship.UNKNOWN, NonFollowerPolicy.relationship(row(handles = setOf("older", "neighbor")))) }
    @Test fun neighborBadgeDoesNotAlterACompleteOwnerRow() {
        assertEquals(NonFollowerRelationship.FOLLOWS_YOU, NonFollowerPolicy.relationship(row(handle = "mutual", labels = listOf("Seni takip ediyor"))))
        assertEquals(NonFollowerRelationship.DOES_NOT_FOLLOW, NonFollowerPolicy.relationship(row()))
    }
    @Test fun siblingBadgeAboveCardBelongsToNextPerson() { assertTrue(NonFollowerBadgeAssociation.belongsToRow(605, 628, 684, 582, 257)) }
    @Test fun nextPersonsBadgeDoesNotBelongToPreviousPerson() { assertFalse(NonFollowerBadgeAssociation.belongsToRow(605, 628, 448, 379, 257)) }
    @Test fun previousPersonsBadgeDoesNotBelongToNextPerson() { assertFalse(NonFollowerBadgeAssociation.belongsToRow(605, 628, 913, 809, 257)) }
    @Test fun badgeClippedByHeaderCannotBeBorrowed() { assertFalse(NonFollowerBadgeAssociation.belongsToRow(230, 250, 290, 220, 257)) }
    @Test fun offscreenOrEmptyBadgeBoundsCannotBeBorrowed() { assertFalse(NonFollowerBadgeAssociation.belongsToRow(605, 605, 684, 582, 257)) }
    @Test fun invisibleRowIsUnknown() { assertEquals(NonFollowerRelationship.UNKNOWN, NonFollowerPolicy.relationship(row(visible = false))) }
    @Test fun disabledRowIsUnknown() { assertEquals(NonFollowerRelationship.UNKNOWN, NonFollowerPolicy.relationship(row(enabled = false))) }
    @Test fun truncatedTreeCannotProveAbsence() { assertEquals(NonFollowerRelationship.UNKNOWN, NonFollowerPolicy.relationship(row(treeComplete = false))) }
    @Test fun alreadyUnfollowedRowIsNotAnAction() { assertEquals(NonFollowerRelationship.UNKNOWN, NonFollowerPolicy.relationship(row(following = false))) }
    @Test fun invalidHandleIsUnknown() { assertEquals(NonFollowerRelationship.UNKNOWN, NonFollowerPolicy.relationship(row(handle = "not a handle"))) }
    @Test fun differentHandleInSubtreeIsUnknown() { assertEquals(NonFollowerRelationship.UNKNOWN, NonFollowerPolicy.relationship(row(handles = setOf("different")))) }
    @Test fun fewerThan100PeopleNeverProduceAnAction() {
        val run = NonFollowerFollowingRun(); run.observe((1..99).map { "user$it" })
        assertFalse(run.ready); assertFalse(run.mayUnfollow(row("user99"), "own", emptySet()))
    }
    @Test fun person100StaysProtected() { assertFalse(prepared().mayUnfollow(row("user100"), "own", emptySet())) }
    @Test fun person101IsEligibleAfterBoundary() { assertTrue(prepared().mayUnfollow(row(), "own", emptySet())) }
    @Test fun boundaryInsideViewportProtectsExactly100() {
        val run = NonFollowerFollowingRun()
        run.observe((1..98).map { "user$it" }); run.observe(listOf("user98", "user99", "user100", "older", "mutual"))
        assertEquals(100, run.protectedHandles.size); assertEquals(102, run.seenCount)
        assertFalse(run.mayUnfollow(row("user99"), "own", emptySet())); assertTrue(run.mayUnfollow(row(), "own", emptySet()))
    }
    @Test fun overlappingViewportsCountPeopleOnce() {
        val run = NonFollowerFollowingRun()
        for (start in 1..96 step 5) assertTrue(run.observe((start..start + 9).map { "user$it" }))
        assertEquals(105, run.seenCount); assertEquals(100, run.protectedHandles.size)
    }
    @Test fun repeatedViewportDoesNotAdvanceBoundary() {
        val run = NonFollowerFollowingRun(); repeat(100) { assertTrue(run.observe(listOf("first", "second"))) }
        assertEquals(2, run.seenCount); assertFalse(run.ready)
    }
    @Test fun caseAndAtPrefixDoNotCountTheSamePersonTwice() {
        val run = NonFollowerFollowingRun(); run.observe(listOf("@First", "first", "FIRST", "second"))
        assertEquals(2, run.seenCount)
    }
    @Test fun skippedViewportPausesBeforeGuessing100() {
        val run = NonFollowerFollowingRun(); assertTrue(run.observe(listOf("first", "second")))
        assertFalse(run.observe(listOf("distant", "older"))); assertEquals(2, run.seenCount)
    }
    @Test fun reorderedViewportCannotExtendProtectionBoundary() {
        val run = NonFollowerFollowingRun(); run.observe(listOf("first", "second"))
        assertFalse(run.observe(listOf("second", "new", "first"))); assertEquals(2, run.seenCount)
    }
    @Test fun emptyAndInvalidRowsCannotAdvanceBoundary() {
        val run = NonFollowerFollowingRun(); assertFalse(run.observe(listOf("", "bad name"))); assertEquals(0, run.seenCount)
    }
    @Test fun ownAccountNeverGetsUnfollowed() { assertFalse(prepared().mayUnfollow(row(), "@OLDER", emptySet())) }
    @Test fun completedAndUnconfirmedPeopleAreNotClickedAgain() { assertFalse(prepared().mayUnfollow(row(), "own", setOf("older"))) }
    @Test fun nonVisibleUnseenPersonIsNotEligible() { assertFalse(prepared().mayUnfollow(row("unseen"), "own", emptySet())) }
    @Test fun mutualFollowerBelowBoundaryIsProtected() { assertFalse(prepared().mayUnfollow(row("mutual", labels = listOf("Seni takip ediyor")), "own", emptySet())) }
    @Test fun first100RemainProtectedWhenListShrinks() {
        val run = prepared(); run.observe(listOf("user99", "user100", "mutual"))
        assertEquals(100, run.protectedHandles.size); assertFalse(run.mayUnfollow(row("user100"), "own", emptySet()))
    }
    @Test fun eachAccountGetsAnIndependentBoundary() { assertTrue(prepared().ready); assertFalse(NonFollowerFollowingRun().ready) }
}
