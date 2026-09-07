package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class TaskAccountAndUnfollowRegressionTest {
    private fun n(text: String, checked: Boolean = false, selected: Boolean = false, visible: Boolean = true) =
        NodeSnapshot(text, null, null, "TextView", false, true, Rect(), checked = checked,
            selected = selected, visible = visible)

    @Test fun anotherAccountCheckmarkDoesNotSelectRequestedAccount() {
        val sharedParent = listOf(n("@xhesaplar1"), n("@bildirimhaber1"), n("checkmark", checked = true))
        assertFalse(AccountRowSelectionEvidence.isSelected(sharedParent, "xhesaplar1"))
    }
    @Test fun selectedWholeAccountListIsNotRowProof() {
        assertFalse(AccountRowSelectionEvidence.isSelected(listOf(n("Accounts", selected = true),
            n("@one"), n("@two")), "one"))
    }
    @Test fun checkmarkWithinOnlyTargetRowIsValid() {
        assertTrue(AccountRowSelectionEvidence.isSelected(listOf(n("@One"), n("checkmark")), "@one"))
    }
    @Test fun unselectedRowAndHiddenCheckmarkAreNotProof() {
        assertFalse(AccountRowSelectionEvidence.isSelected(listOf(n("@one")), "one"))
        assertFalse(AccountRowSelectionEvidence.isSelected(listOf(n("@one"), n("checkmark", visible = false)), "one"))
    }
    @Test fun displayNameMentioningSelectionIsNotCheckmark() {
        assertFalse(AccountRowSelectionEvidence.isSelected(listOf(n("@one"), n("Selected News")), "one"))
    }
    @Test fun sameRowButtonMatchesAtDifferentScales() {
        assertTrue(RelationshipRowGeometry.matches(10, 110, 90, 128, 200, 100, 280, 140))
        assertTrue(RelationshipRowGeometry.matches(20, 220, 180, 256, 400, 200, 560, 280))
    }
    @Test fun precedingFollowingTabCannotAttachToFirstUser() {
        assertFalse(RelationshipRowGeometry.matches(10, 110, 90, 128, 200, 60, 280, 100))
    }
    @Test fun nextRowsFollowingButtonCannotFakeTargetReversion() {
        assertFalse(RelationshipRowGeometry.matches(10, 110, 90, 128, 200, 140, 280, 180))
    }
    @Test fun overlappingFullWidthContainerIsNotUsernameEvidence() {
        assertFalse(RelationshipRowGeometry.matches(0, 100, 400, 300, 200, 110, 280, 150))
    }
    @Test fun confirmationClickedOnlyOnceDuringDismissalAnimation() {
        assertEquals(UnfollowConfirmationDecision.CLICK, UnfollowConfirmationPolicy.decide(false, 0, 5000))
        for (elapsed in listOf(0L, 500L, 1500L, 4999L)) {
            assertEquals(UnfollowConfirmationDecision.WAIT, UnfollowConfirmationPolicy.decide(true, elapsed, 5000))
        }
    }
    @Test fun stuckConfirmationPausesAfterDeadline() {
        assertEquals(UnfollowConfirmationDecision.PAUSE, UnfollowConfirmationPolicy.decide(true, 5000, 5000))
    }
    @Test fun conflictingRelationshipLabelsNeverCountAsSuccess() {
        assertEquals(UnfollowOutcome.WAIT, UnfollowOutcomePolicy.evaluate(true, 100, true, true, 5000))
        assertEquals(UnfollowOutcome.UNKNOWN, UnfollowOutcomePolicy.evaluate(true, 5000, true, true, 5000))
    }
    @Test fun targetVisibleInSwitcherIsNotPermissionToUnfollow() {
        val result = XAccountSwitchGuard.evaluate("one", "one", XScreen.ACCOUNT_SWITCHER, false)
        assertFalse(XAccountSwitchGuard.mayPerformMutation(result))
        assertFalse(XAccountSwitchGuard.mayPerformMutation(XAccountSwitchGuard.evaluate("one", "two", XScreen.PROFILE, true)))
    }
}
