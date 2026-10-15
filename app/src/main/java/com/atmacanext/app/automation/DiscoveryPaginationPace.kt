package com.atmacanext.app.automation

/** A page is read after finger release and stable geometry, never during a fling. */
internal class DiscoveryPaginationPace {
    enum class Reading { WAIT, MOVED, STILL, CANCELLED, TIMEOUT }
    var pending = false; private set
    private var start = 0L
    private var base = ""
    private var released = 0L
    private var completed: Boolean? = null
    private var seen = ""
    private var stableAt = 0L
    fun reset() { pending = false; completed = null; seen = "" }
    fun arm(signature: String, now: Long) {
        pending = true; start = now; base = signature; completed = null; seen = ""; stableAt = now
    }
    fun finished(success: Boolean, now: Long) { if (pending) { completed = success; released = now; stableAt = now; seen = "" } }
    fun read(signature: String, now: Long): Reading {
        if (!pending) return Reading.STILL
        if (completed == false) { reset(); return Reading.CANCELLED }
        if (now - start >= 2_500L) { reset(); return Reading.TIMEOUT }
        if (completed == null || now - released < READ_MS || signature.isBlank()) return Reading.WAIT
        if (signature != seen) { seen = signature; stableAt = now; return Reading.WAIT }
        if (now - stableAt < READ_MS) return Reading.WAIT
        val result = if (signature != base) Reading.MOVED else Reading.STILL
        reset(); return result
    }
    companion object {
        const val MOVE_MS = 320L
        const val HOLD_MS = 120L
        const val READ_MS = 120L
        const val TRAVEL_RATIO = .24f
        /** Ignore autoplay time labels; retain real post/user identities and every visible rectangle. */
        fun signature(nodes: List<NodeSnapshot>): String = nodes.asSequence().filter { it.visible && !it.editable }
            .joinToString("|") { n ->
                val handles = listOfNotNull(n.text, n.contentDescription).flatMap {
                    Regex("@[A-Za-z0-9_]{1,15}").findAll(it).map { m -> m.value.lowercase() }.toList()
                }.distinct().joinToString(",")
                "$handles:${n.viewId}:${n.bounds.left},${n.bounds.top},${n.bounds.right},${n.bounds.bottom}"
            }.hashCode().toString(16)
    }
}
