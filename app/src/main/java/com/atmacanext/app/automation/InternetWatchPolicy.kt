package com.atmacanext.app.automation

/** Minute-by-minute internet watch while a task is running or waiting for the link to return. */
internal object InternetWatchPolicy {
    const val CHECK_MS = 60_000L
    enum class Event { LOST, RESTORED }

    fun decide(wasOnline: Boolean, online: Boolean, taskRunning: Boolean, waitingForInternet: Boolean): Event? = when {
        taskRunning && !waitingForInternet && !online -> Event.LOST
        waitingForInternet && online -> Event.RESTORED
        else -> null
    }
}
