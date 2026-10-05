package com.buse.app.automation

/** Idle time during an unfollow confirmation is never evidence of a list boundary. */
internal class BuseScrollBoundary {
    private var beforeScroll: String? = null
    var unchangedAttempts = 0
        private set

    fun reset() {
        beforeScroll = null
        unchangedAttempts = 0
    }

    fun afterScroll(signature: String) { beforeScroll = signature }

    /** Called only with a fresh, settled viewport after a completed drag/release. */
    fun observe(signature: String): Boolean {
        val before = beforeScroll ?: return false
        beforeScroll = null
        unchangedAttempts = if (signature == before) unchangedAttempts + 1 else 0
        return unchangedAttempts >= 3
    }
}
