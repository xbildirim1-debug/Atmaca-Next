package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class UnfollowOutcomePolicyTest {
    private val timeout = 5_000L

    @Test fun confirmedRowBecomesFollowIsSuccess() {
        assertEquals(UnfollowOutcome.SUCCESS, UnfollowOutcomePolicy.evaluate(true, 100, true, false, timeout))
    }

    @Test fun unchangedFollowingIsNotDailyLimitEvidence() {
        assertEquals(UnfollowOutcome.UNKNOWN, UnfollowOutcomePolicy.evaluate(true, timeout, false, true, timeout))
    }

    @Test fun missingConfirmationIsNotDailyLimit() {
        assertEquals(UnfollowOutcome.UNCONFIRMED, UnfollowOutcomePolicy.evaluate(false, timeout, false, true, timeout))
    }

    @Test fun confirmedUnknownStateWaitsThenBecomesUnknown() {
        assertEquals(UnfollowOutcome.WAIT, UnfollowOutcomePolicy.evaluate(true, timeout, false, false, timeout))
        assertEquals(UnfollowOutcome.UNKNOWN, UnfollowOutcomePolicy.evaluate(true, timeout * 2, false, false, timeout))
    }
}
