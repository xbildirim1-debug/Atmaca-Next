package com.atmacanext.app.automation

/** Returning from a commenter must not enter that commenter's own reply thread. */
internal object CommenterReturnPolicy {
    enum class Decision { PARENT, TARGET, BACK, WAIT, PAUSE }

    fun decide(parentVisible: Boolean, targetVisible: Boolean, childVisible: Boolean,
               attempts: Int, elapsedMs: Long, sinceBackMs: Long): Decision = when {
        parentVisible -> Decision.PARENT
        targetVisible -> Decision.TARGET
        sinceBackMs < 1_000L -> Decision.WAIT
        childVisible && attempts < 3 -> Decision.BACK
        elapsedMs >= 6_000L -> Decision.PAUSE
        else -> Decision.WAIT
    }
}
