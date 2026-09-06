package com.atmacanext.app.automation

/** Small deterministic loop detector; it never makes an X action itself. */
class LoopGuard(private val capacity: Int = 20) {
    data class Step(val screen: XScreen, val fingerprint: String, val action: String, val target: String?)

    private val steps = ArrayDeque<Step>()

    fun record(step: Step): Boolean {
        steps.addLast(step)
        while (steps.size > capacity) steps.removeFirst()
        if (steps.size < 3) return false
        val last = steps.takeLast(3)
        val exact = last.distinct().size == 1
        val pingPong = if (steps.size >= 6) {
            val six = steps.takeLast(6)
            six[0] == six[2] && six[2] == six[4] && six[1] == six[3] && six[3] == six[5]
        } else false
        return exact || pingPong
    }

    fun clear() = steps.clear()
}
