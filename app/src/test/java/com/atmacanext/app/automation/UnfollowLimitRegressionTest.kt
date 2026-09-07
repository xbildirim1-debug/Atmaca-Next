package com.atmacanext.app.automation

import org.junit.Assert.*
import org.junit.Test

class UnfollowLimitRegressionTest {
    @Test fun followBackOutcomesCountForBothLanguages() {
        for (label in listOf("Geri Takip Et", "Sen de takip et", "Follow back", "Takip et", "Follow")) {
            val unfollowed = XUiVocabulary.containsExact(listOf(label), XUiVocabulary.followActions)
            assertEquals(label, UnfollowOutcome.SUCCESS,
                UnfollowOutcomePolicy.evaluate(true, 700, unfollowed, false, 5000))
        }
    }
    @Test fun followingAndMissingLabelsDoNotCount() {
        for (label in listOf("Takip ediliyor", "Following", "", "Takipçiler")) {
            assertFalse(XUiVocabulary.containsExact(listOf(label), XUiVocabulary.followActions))
        }
    }
    @Test fun fiveAttemptsCannotStartSixthEvenWhenResultsAreUnread() {
        for (issued in 0..4) assertTrue(UnfollowAttemptBudget.mayIssue(issued, 5, 0, 5))
        assertFalse(UnfollowAttemptBudget.mayIssue(5, 5, 0, 5))
        assertFalse(UnfollowAttemptBudget.mayIssue(6, 5, 0, 5))
    }
    @Test fun eachAccountHasItsOwnFiveAttemptBudget() {
        for (account in 1..2) {
            var issued = 0
            while (UnfollowAttemptBudget.mayIssue(issued, 5, 0, 5)) issued++
            assertEquals("Account $account", 5, issued)
        }
    }
    @Test fun nextCycleCannotBeUsedBeforeItStarts() {
        assertFalse(UnfollowAttemptBudget.mayIssue(5, 10, 0, 5))
        assertTrue(UnfollowAttemptBudget.mayIssue(5, 10, 1, 5))
        assertFalse(UnfollowAttemptBudget.mayIssue(10, 10, 1, 5))
    }
    @Test fun resumedProgressLeavesOnlyRemainingAttempts() {
        assertTrue(UnfollowAttemptBudget.mayIssue(4, 5, 0, 5))
        assertFalse(UnfollowAttemptBudget.mayIssue(5, 5, 0, 5))
    }
}
