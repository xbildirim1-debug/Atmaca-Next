package com.atmacanext.app.domain.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskLimitPolicyTest {
    @Test
    fun hardLimitNeverExceeds35() {
        assertEquals(35, TaskLimitPolicy.sanitizeLimit(200))
    }

    @Test
    fun verifiedCounterNeverExceedsLimit() {
        assertEquals(35, TaskLimitPolicy.nextVerifiedCount(35, 35))
    }

    @Test
    fun negativeProgressIsSanitized() {
        assertEquals(0, TaskLimitPolicy.sanitizeProgress(-4, 35))
    }
}
