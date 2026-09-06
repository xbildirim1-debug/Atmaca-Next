package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimitPolicyTest {
    @Test
    fun onlyOneAutomaticRetryIsAllowed() {
        assertTrue(RateLimitPolicy.canAutoRetry(0))
        assertFalse(RateLimitPolicy.canAutoRetry(1))
    }

    @Test
    fun remainingNeverGoesNegative() {
        assertEquals(500L, RateLimitPolicy.remainingMillis(1_000L, 1_500L))
        assertEquals(0L, RateLimitPolicy.remainingMillis(2_000L, 1_500L))
    }
}
