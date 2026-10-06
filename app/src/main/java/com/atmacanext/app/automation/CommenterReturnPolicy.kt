package com.atmacanext.app.automation

/** Returning from a commenter must not enter that commenter's own reply thread. */
internal object CommenterReturnPolicy {
    enum class Decision { PARENT, TARGET, BACK, WAIT, RESTART }

    /** An unrecognized detail can already be the parent whose lazy rows have not hydrated. */
    fun provenChildDetail(headerAuthor: String?, childAuthor: String?, parentAuthor: String?): Boolean =
        !headerAuthor.isNullOrBlank() && headerAuthor == childAuthor && headerAuthor != parentAuthor

    fun decide(parentVisible: Boolean, targetVisible: Boolean, childVisible: Boolean,
               attempts: Int, elapsedMs: Long, sinceBackMs: Long): Decision = when {
        parentVisible -> Decision.PARENT
        targetVisible -> Decision.TARGET
        sinceBackMs < 1_000L -> Decision.WAIT
        elapsedMs >= AutomationStallPolicy.TIMEOUT_MS -> Decision.RESTART
        childVisible && attempts < 3 -> Decision.BACK
        else -> Decision.WAIT
    }
}
