package com.atmacanext.app.automation

/** Keep the pointer down briefly at the end; releasing a moving pointer flings past the overlap. */
internal class HeldDragSequence(
    private val move: ((Boolean) -> Unit) -> Boolean,
    private val hold: ((Boolean) -> Unit) -> Boolean,
    private val finished: (Boolean) -> Unit,
) {
    enum class Phase { NEW, MOVING, HOLDING, FINISHED }
    var phase = Phase.NEW
        private set

    fun start(): Boolean {
        if (phase != Phase.NEW) return false
        phase = Phase.MOVING
        val accepted = runCatching { move(::moveFinished) }.getOrDefault(false)
        if (!accepted) finish(false)
        return accepted
    }

    private fun moveFinished(completed: Boolean) {
        if (phase != Phase.MOVING) return
        if (!completed) { finish(false); return }
        phase = Phase.HOLDING
        if (!runCatching { hold(::holdFinished) }.getOrDefault(false)) finish(false)
    }

    private fun holdFinished(completed: Boolean) {
        if (phase == Phase.HOLDING) finish(completed)
    }

    private fun finish(completed: Boolean) {
        if (phase == Phase.FINISHED) return
        phase = Phase.FINISHED
        finished(completed)
    }

    companion object {
        const val MOVE_MS = 280L
        const val HOLD_MS = 140L
    }
}
