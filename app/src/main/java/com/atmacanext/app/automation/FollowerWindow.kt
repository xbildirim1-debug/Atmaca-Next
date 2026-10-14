package com.atmacanext.app.automation

/** Follower change inside a time window. Points are (time, count) for one account. */
internal object FollowerWindow {
    fun gained(points: List<Pair<Long, Long>>, start: Long): Long {
        if (points.size < 2 && start > 0L) return 0L
        val ordered = points.sortedBy { it.first }
        if (ordered.isEmpty()) return 0L
        val latest = ordered.last().second
        if (start <= 0L) return latest - ordered.first().second
        val before = ordered.lastOrNull { it.first < start }?.second
        if (before != null) return latest - before
        val inside = ordered.filter { it.first >= start }
        if (inside.size < 2) return 0L
        return inside.last().second - inside.first().second
    }
}
