package com.atmacanext.app.data.settings

import android.content.Context
import com.atmacanext.app.automation.FollowerWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

enum class StatsRange(val title: String) { DAY("Günlük"), WEEK("Haftalık"), MONTH("Aylık"), YEAR("Yıllık") }

data class StatsTotals(val followers: Long, val comments: Int, val tweets: Int)

data class RangeChart(val gained: List<Long>, val tweets: List<Int>, val comments: List<Int>, val bucketStart: List<Long>)

/** Counts that survive restarts. Daily, weekly and monthly use the event log. */
object ActivityStats {
    private val revisionState = MutableStateFlow(0)
    val revision = revisionState.asStateFlow()

    fun noteFollowers(context: Context, username: String?, count: Long) = append(context, "F", username, count)

    fun noteComment(context: Context, username: String?) = append(context, "C", username, 1)

    fun noteTweet(context: Context, username: String?) = append(context, "T", username, 1)

    fun seedBaselines(context: Context, accounts: List<String>, now: Long = System.currentTimeMillis()) {
        val start = startOf(StatsRange.DAY, now)
        val known = read(context).filter { it.kind == "F" }
        var wrote = false
        accounts.forEach { username ->
            val user = username.removePrefix("@").trim().lowercase()
            val count = AccountPulse.read(context, username).followers ?: return@forEach
            val mine = known.filter { it.user == user }
            if (mine.any { it.time < start } || mine.any { it.time >= start }) return@forEach
            file(context).appendText("${start}\tF\t$user\t$count\n")
            wrote = true
        }
        if (wrote) revisionState.value = revisionState.value + 1
    }

    fun totals(context: Context, range: StatsRange, accounts: List<String>, now: Long = System.currentTimeMillis()): StatsTotals {
        val events = read(context)
        val start = startOf(range, now)
        val followers = events.filter { it.kind == "F" }
            .groupBy { it.user }
            .values
            .sumOf { FollowerWindow.gained(it.map { event -> event.time to event.value }, start) }
        val comments = events.count { it.kind == "C" && it.time >= start }
        val tweets = events.count { it.kind == "T" && it.time >= start }
        return StatsTotals(followers, comments, tweets)
    }

    /** Follower change for one account since the start of today. */
    fun gainedFor(context: Context, username: String, now: Long = System.currentTimeMillis()): Long {
        val user = username.removePrefix("@").trim().lowercase()
        val points = read(context).filter { it.kind == "F" && it.user == user }.map { it.time to it.value }
        return FollowerWindow.gained(points, startOf(StatsRange.DAY, now))
    }

    /** Absolute follower totals over the selected range, one point per scan. */
    fun followerSeries(context: Context, range: StatsRange, accounts: List<String>, now: Long = System.currentTimeMillis()): List<Long> {
        val wanted = accounts.map { it.removePrefix("@").trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val events = read(context).filter { it.kind == "F" && it.user in wanted }
        val current = accounts.sumOf { AccountPulse.read(context, it).followers ?: 0L }
        if (events.isEmpty()) return if (current > 0L) listOf(current) else emptyList()
        val start = startOf(range, now)
        val byUser = events.groupBy { it.user }.mapValues { (_, points) -> points.sortedBy { it.time } }
        val marks = buildList {
            if (start > 0L) add(start)
            events.map { it.time }.filter { it >= start }.distinct().sorted().forEach(::add)
        }.distinct()
        val series = marks.map { time ->
            byUser.values.sumOf { points -> points.lastOrNull { it.time <= time }?.value ?: 0L }
        }
        if (series.isEmpty()) return if (current > 0L) listOf(current) else emptyList()
        return series.dropLast(1) + current
    }

    /** One slot per hour, day or month. Follower change is the rise inside that slot. */
    fun chart(context: Context, range: StatsRange, accounts: List<String>, now: Long = System.currentTimeMillis()): RangeChart {
        val count = when (range) {
            StatsRange.DAY -> 24
            StatsRange.WEEK -> 7
            StatsRange.MONTH -> 30
            StatsRange.YEAR -> 12
        }
        val span = when (range) {
            StatsRange.DAY -> 24L * 3_600_000L
            StatsRange.WEEK -> 7L * 86_400_000L
            StatsRange.MONTH -> 30L * 86_400_000L
            StatsRange.YEAR -> 365L * 86_400_000L
        }
        val start = startOf(range, now)
        val wanted = accounts.map { it.removePrefix("@").trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val gained = LongArray(count)
        val tweets = IntArray(count)
        val comments = IntArray(count)
        fun slot(time: Long) = (((time - start).coerceAtLeast(0L) * count) / span).toInt().coerceIn(0, count - 1)
        val events = read(context).filter { it.time >= start && it.user in wanted }
        events.filter { it.kind == "F" }.groupBy { it.user }.values.forEach { points ->
            val ordered = points.sortedBy { it.time }
            for (index in 1 until ordered.size) {
                gained[slot(ordered[index].time)] += ordered[index].value - ordered[index - 1].value
            }
        }
        events.forEach { event ->
            when (event.kind) {
                "T" -> tweets[slot(event.time)] += 1
                "C" -> comments[slot(event.time)] += 1
            }
        }
        val starts = List(count) { start + span * it / count }
        return RangeChart(gained.toList(), tweets.toList(), comments.toList(), starts)
    }

    fun clear(context: Context) {
        file(context).delete()
        revisionState.value = revisionState.value + 1
    }

    fun startOf(range: StatsRange, now: Long): Long {
        val zone = ZoneId.systemDefault()
        return when (range) {
            StatsRange.DAY -> LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
            StatsRange.WEEK -> now - 7L * 24 * 60 * 60 * 1000
            StatsRange.MONTH -> now - 30L * 24 * 60 * 60 * 1000
            StatsRange.YEAR -> now - 365L * 24 * 60 * 60 * 1000
        }
    }

    private fun append(context: Context, kind: String, username: String?, value: Long) {
        val user = username?.removePrefix("@")?.trim()?.lowercase()?.ifBlank { null } ?: return
        val file = file(context)
        val lines = file.takeIf { it.exists() }?.readLines().orEmpty().takeLast(3_000).toMutableList()
        lines += "${System.currentTimeMillis()}\t$kind\t$user\t$value"
        file.writeText(lines.joinToString("\n"))
        revisionState.value = revisionState.value + 1
    }

    private data class Event(val time: Long, val kind: String, val user: String, val value: Long)

    private fun read(context: Context): List<Event> =
        file(context).takeIf { it.exists() }?.readLines().orEmpty().mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size < 4) return@mapNotNull null
            Event(parts[0].toLongOrNull() ?: return@mapNotNull null, parts[1], parts[2], parts[3].toLongOrNull() ?: 0L)
        }

    private fun file(context: Context) = File(context.applicationContext.filesDir, "activity_stats.log")
}
