package com.buse.app.automation

import com.buse.app.domain.model.ScheduledTask
import com.buse.app.domain.model.TaskType
import com.buse.app.domain.policy.BuseTaskMode
import com.buse.app.domain.policy.BuseTaskPolicy
import org.junit.Assert.*
import org.junit.Test

class BuseNonFollowerPolicyTest {
    private fun row(handle: String = "older", labels: List<String> = listOf("Older", "@older", "Takip ediliyor"),
                    complete: Boolean = true, visible: Boolean = true, enabled: Boolean = true,
                    following: Boolean = true, treeComplete: Boolean = true, handles: Set<String> = setOf(handle)) =
        BuseRowEvidence(handle, handles, labels, complete, visible, enabled, following, treeComplete)
    private fun prepared(): BuseFollowingRun = BuseFollowingRun().also { run ->
        assertTrue(run.observe((1..200).map { "user$it" }))
        assertTrue(run.observe(listOf("user200", "older", "mutual")))
    }

    @Test fun turkishBadgeProtectsItsOwner() { assertEquals(BuseRelationship.FOLLOWS_YOU, BuseNonFollowerPolicy.relationship(row(labels = listOf("Seni takip ediyor", "@older")))) }
    @Test fun englishBadgeProtectsItsOwner() { assertEquals(BuseRelationship.FOLLOWS_YOU, BuseNonFollowerPolicy.relationship(row(labels = listOf("Follows you")))) }
    @Test fun badgeWithAccessibilityStateProtectsItsOwner() { assertEquals(BuseRelationship.FOLLOWS_YOU, BuseNonFollowerPolicy.relationship(row(labels = listOf("Seni takip ediyor, metin")))) }
    @Test fun whitespaceAndCaseDoNotRemoveMutualFollower() { assertEquals(BuseRelationship.FOLLOWS_YOU, BuseNonFollowerPolicy.relationship(row(labels = listOf("  SENİ   TAKİP EDİYOR  ")))) }
    @Test fun mergedRowDescriptionStillProtectsBadgeOwner() { assertEquals(BuseRelationship.FOLLOWS_YOU, BuseNonFollowerPolicy.relationship(row(labels = listOf("Seni takip ediyor. Older @older Takip ediliyor")))) }
    @Test fun completeNonMutualRowIsEligible() { assertEquals(BuseRelationship.DOES_NOT_FOLLOW, BuseNonFollowerPolicy.relationship(row())) }
    @Test fun topClippedBadgeCannotBecomeAbsentBadge() { assertEquals(BuseRelationship.UNKNOWN, BuseNonFollowerPolicy.relationship(row(complete = false))) }
    @Test fun bottomClippedRowCannotProveAbsence() { assertFalse(prepared().mayUnfollow(row(complete = false), "own", emptySet())) }
    @Test fun mergedNeighborsCannotProveAbsence() { assertEquals(BuseRelationship.UNKNOWN, BuseNonFollowerPolicy.relationship(row(handles = setOf("older", "neighbor")))) }
    @Test fun neighborBadgeDoesNotAlterACompleteOwnerRow() {
        assertEquals(BuseRelationship.FOLLOWS_YOU, BuseNonFollowerPolicy.relationship(row(handle = "mutual", labels = listOf("Seni takip ediyor"))))
        assertEquals(BuseRelationship.DOES_NOT_FOLLOW, BuseNonFollowerPolicy.relationship(row()))
    }
    @Test fun siblingBadgeAboveCardBelongsToNextPerson() { assertTrue(BuseBadgeAssociation.belongsToRow(605, 628, 684, 582, 257)) }
    @Test fun nextPersonsBadgeDoesNotBelongToPreviousPerson() { assertFalse(BuseBadgeAssociation.belongsToRow(605, 628, 448, 379, 257)) }
    @Test fun previousPersonsBadgeDoesNotBelongToNextPerson() { assertFalse(BuseBadgeAssociation.belongsToRow(605, 628, 913, 809, 257)) }
    @Test fun badgeClippedByHeaderCannotBeBorrowed() { assertFalse(BuseBadgeAssociation.belongsToRow(230, 250, 290, 220, 257)) }
    @Test fun offscreenOrEmptyBadgeBoundsCannotBeBorrowed() { assertFalse(BuseBadgeAssociation.belongsToRow(605, 605, 684, 582, 257)) }
    @Test fun invisibleRowIsUnknown() { assertEquals(BuseRelationship.UNKNOWN, BuseNonFollowerPolicy.relationship(row(visible = false))) }
    @Test fun disabledRowIsUnknown() { assertEquals(BuseRelationship.UNKNOWN, BuseNonFollowerPolicy.relationship(row(enabled = false))) }
    @Test fun truncatedTreeCannotProveAbsence() { assertEquals(BuseRelationship.UNKNOWN, BuseNonFollowerPolicy.relationship(row(treeComplete = false))) }
    @Test fun alreadyUnfollowedRowIsNotAnAction() { assertEquals(BuseRelationship.UNKNOWN, BuseNonFollowerPolicy.relationship(row(following = false))) }
    @Test fun invalidHandleIsUnknown() { assertEquals(BuseRelationship.UNKNOWN, BuseNonFollowerPolicy.relationship(row(handle = "not a handle"))) }
    @Test fun differentHandleInSubtreeIsUnknown() { assertEquals(BuseRelationship.UNKNOWN, BuseNonFollowerPolicy.relationship(row(handles = setOf("different")))) }
    @Test fun fewerThan200PeopleNeverProduceAnAction() {
        val run = BuseFollowingRun(); run.observe((1..199).map { "user$it" })
        assertFalse(run.ready); assertFalse(run.mayUnfollow(row("user199"), "own", emptySet()))
    }
    @Test fun person200StaysProtected() { assertFalse(prepared().mayUnfollow(row("user200"), "own", emptySet())) }
    @Test fun person201IsEligibleAfterBoundary() { assertTrue(prepared().mayUnfollow(row(), "own", emptySet())) }
    @Test fun boundaryInsideViewportProtectsExactly200() {
        val run = BuseFollowingRun()
        run.observe((1..198).map { "user$it" }); run.observe(listOf("user198", "user199", "user200", "older", "mutual"))
        assertEquals(200, run.protectedHandles.size); assertEquals(202, run.seenCount)
        assertFalse(run.mayUnfollow(row("user199"), "own", emptySet())); assertTrue(run.mayUnfollow(row(), "own", emptySet()))
    }
    @Test fun overlappingViewportsCountPeopleOnce() {
        val run = BuseFollowingRun()
        for (start in 1..196 step 5) assertTrue(run.observe((start..start + 9).map { "user$it" }))
        assertEquals(205, run.seenCount); assertEquals(200, run.protectedHandles.size)
    }
    @Test fun repeatedViewportDoesNotAdvanceBoundary() {
        val run = BuseFollowingRun(); repeat(200) { assertTrue(run.observe(listOf("first", "second"))) }
        assertEquals(2, run.seenCount); assertFalse(run.ready)
    }
    @Test fun caseAndAtPrefixDoNotCountTheSamePersonTwice() {
        val run = BuseFollowingRun(); run.observe(listOf("@First", "first", "FIRST", "second"))
        assertEquals(2, run.seenCount)
    }
    @Test fun skippedViewportPausesBeforeGuessing200() {
        val run = BuseFollowingRun(); assertTrue(run.observe(listOf("first", "second")))
        assertFalse(run.observe(listOf("distant", "older"))); assertEquals(2, run.seenCount)
    }
    @Test fun reorderedViewportCannotExtendProtectionBoundary() {
        val run = BuseFollowingRun(); run.observe(listOf("first", "second"))
        assertFalse(run.observe(listOf("second", "new", "first"))); assertEquals(2, run.seenCount)
    }
    @Test fun emptyAndInvalidRowsCannotAdvanceBoundary() {
        val run = BuseFollowingRun(); assertFalse(run.observe(listOf("", "bad name"))); assertEquals(0, run.seenCount)
    }
    @Test fun ownAccountNeverGetsUnfollowed() { assertFalse(prepared().mayUnfollow(row(), "@OLDER", emptySet())) }
    @Test fun completedAndUnconfirmedPeopleAreNotClickedAgain() { assertFalse(prepared().mayUnfollow(row(), "own", setOf("older"))) }
    @Test fun nonVisibleUnseenPersonIsNotEligible() { assertFalse(prepared().mayUnfollow(row("unseen"), "own", emptySet())) }
    @Test fun mutualFollowerBelowBoundaryIsProtected() { assertFalse(prepared().mayUnfollow(row("mutual", labels = listOf("Seni takip ediyor")), "own", emptySet())) }
    @Test fun first200RemainProtectedWhenListShrinks() {
        val run = prepared(); run.observe(listOf("user199", "user200", "mutual"))
        assertEquals(200, run.protectedHandles.size); assertFalse(run.mayUnfollow(row("user200"), "own", emptySet()))
    }
    @Test fun eachAccountGetsAnIndependentBoundary() { assertTrue(prepared().ready); assertFalse(BuseFollowingRun().ready) }
    @Test fun policyExportsOnlyTwoTaskModes() { assertEquals(2, BuseTaskMode.entries.size) }
    @Test fun taskMarkerSurvivesNormalTaskModelCopy() {
        val task = task().copy(contentPrompt = BuseTaskPolicy.NON_FOLLOWER_MARKER)
        assertTrue(BuseTaskPolicy.accepts(task.copy(progress = 5))); assertTrue(BuseTaskPolicy.isNonFollower(task))
        assertEquals(BuseTaskMode.NON_FOLLOWERS, BuseTaskPolicy.mode(task))
    }
    @Test fun normalUnfollowUsesExistingFlow() { assertTrue(BuseTaskPolicy.accepts(task())); assertFalse(BuseTaskPolicy.isNonFollower(task())) }
    @Test fun otherTaskTypesCannotBeSavedOrQueuedInBuse() {
        TaskType.entries.filter { it != TaskType.UNFOLLOW }.forEach { assertFalse(BuseTaskPolicy.accepts(task().copy(type = it))) }
    }
    @Test fun malformedTaskConfigurationCannotBeQueued() {
        listOf(task().copy(contentPrompt = "arbitrary"), task().copy(useGemini = true), task().copy(limit = 36),
            task().copy(limit = 0), task().copy(repeatCount = 0), task().copy(intervalMinutes = 0),
            task().copy(mediaUri = "other"), task().copy(targetUrl = "https://x.com/other"), task().copy(contentText = "post"))
            .forEach { assertFalse(BuseTaskPolicy.accepts(it)) }
    }
    private fun task() = ScheduledTask("task", "account", "@own", type = TaskType.UNFOLLOW, limit = 20)
}
