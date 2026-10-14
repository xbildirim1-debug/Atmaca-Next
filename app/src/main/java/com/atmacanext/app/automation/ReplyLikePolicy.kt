package com.atmacanext.app.automation

/** After a verified reply, like the same tweet, then leave. A missed like does not undo the reply. */
internal object ReplyLikePolicy {
    enum class Step { TAP, LOOK_UP, DONE, GIVE_UP }

    fun step(liked: Boolean, attempts: Int): Step = when {
        liked -> Step.DONE
        attempts >= 4 -> Step.GIVE_UP
        attempts % 2 == 0 -> Step.TAP
        else -> Step.LOOK_UP
    }
}
