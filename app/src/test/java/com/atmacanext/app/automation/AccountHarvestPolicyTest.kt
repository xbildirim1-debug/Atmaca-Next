package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountHarvestPolicyTest {
    @Test fun firstPaintWaitsAndAFullListFinishes() {
        assertEquals(AccountHarvestPolicy.Step.WAIT, AccountHarvestPolicy.step(2, 0, 0, true))
        assertEquals(AccountHarvestPolicy.Step.SCROLL, AccountHarvestPolicy.step(2, 1, 0, true))
        assertEquals(AccountHarvestPolicy.Step.FINISH, AccountHarvestPolicy.step(10, 0, 0, true))
        assertEquals(AccountHarvestPolicy.Step.REVERSE, AccountHarvestPolicy.step(2, 2, 3, true))
        assertEquals(AccountHarvestPolicy.Step.SCROLL, AccountHarvestPolicy.step(5, 2, 2, false))
        assertEquals(AccountHarvestPolicy.Step.FINISH, AccountHarvestPolicy.step(5, 2, 3, false))
    }

    @Test fun swipeStillInFlightIsNotTheEndOfTheList() {
        assertTrue(UnfollowScrollPace.inFlight(1_000L, 1_200L))
        assertEquals(false, UnfollowScrollPace.inFlight(0L, 5_000L))
        assertEquals(false, UnfollowScrollPace.inFlight(1_000L, 1_000L + UnfollowScrollPace.SETTLE_MS))
    }

    private fun assertTrue(value: Boolean) = org.junit.Assert.assertTrue(value)
}
