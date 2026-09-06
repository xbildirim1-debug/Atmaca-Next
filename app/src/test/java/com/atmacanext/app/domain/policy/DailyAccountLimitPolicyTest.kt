package com.atmacanext.app.domain.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyAccountLimitPolicyTest {
    @Test
    fun `allows task only when whole remaining work fits today's account budget`() {
        val allowed = DailyAccountLimitPolicy.evaluate(taskProgress = 20, taskLimit = 35, usedToday = 20, dailyLimit = 35)
        assertTrue(allowed.allowed)
        assertEquals(15, allowed.remainingToday)
        assertEquals(15, allowed.taskRemaining)

        val blocked = DailyAccountLimitPolicy.evaluate(taskProgress = 0, taskLimit = 35, usedToday = 10, dailyLimit = 35)
        assertFalse(blocked.allowed)
        assertEquals(25, blocked.remainingToday)
        assertEquals(35, blocked.taskRemaining)
    }
}
