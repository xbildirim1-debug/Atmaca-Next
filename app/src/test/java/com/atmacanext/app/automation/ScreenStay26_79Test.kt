package com.atmacanext.app.automation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenStay26_79Test {
    @Test fun screenStaysOnOnlyWhileTheTaskIsRunning() {
        assertTrue(ScreenStayPolicy.hold(taskLive = true, userPaused = false))
        assertFalse(ScreenStayPolicy.hold(taskLive = true, userPaused = true))
        assertFalse(ScreenStayPolicy.hold(taskLive = false, userPaused = false))
        assertTrue(ScreenStayPolicy.MAX_HOLD_MS >= 60 * 60 * 1000L)
    }
}
