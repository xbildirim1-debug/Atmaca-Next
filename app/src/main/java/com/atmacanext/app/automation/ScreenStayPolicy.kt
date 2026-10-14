package com.atmacanext.app.automation

/** Keep the screen awake only while a task is actually in progress. A user pause releases it. */
internal object ScreenStayPolicy {
    const val MAX_HOLD_MS = 8 * 60 * 60 * 1000L

    fun hold(taskLive: Boolean, userPaused: Boolean): Boolean = taskLive && !userPaused
}
