package com.atmacanext.app.automation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationWatchdogTest {
    @Test
    fun repeatedEventsDoNotCountAsProgress() {
        val watchdog = AutomationWatchdog(timeoutMs = 1_000L)
        watchdog.reset(0L)
        assertTrue(watchdog.observe("same", 100L))
        assertFalse(watchdog.observe("same", 500L))
        assertTrue(watchdog.isStuck(1_101L))
    }

    @Test
    fun signatureChangeResetsTimeout() {
        val watchdog = AutomationWatchdog(timeoutMs = 1_000L)
        watchdog.observe("a", 100L)
        watchdog.observe("b", 900L)
        assertFalse(watchdog.isStuck(1_500L))
        assertTrue(watchdog.isStuck(1_901L))
    }
}
