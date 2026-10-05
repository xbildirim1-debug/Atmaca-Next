package com.buse.app.data.repository

/** Late flow snapshots must not erase verified progress or reopen a finished task. */
internal object TaskProgressWritePolicy {
    fun accepts(storedProgress: Int, storedCompleted: Boolean, incomingProgress: Int, incomingCompleted: Boolean): Boolean =
        incomingProgress >= storedProgress && (!storedCompleted || incomingCompleted)
}
