package com.atmacanext.app.automation

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Parses the freshest visible X tweet rows without relying on screen coordinates. */
object XTweetInspector {
    data class TweetRow(
        val key: String,
        val ageMinutes: Long?,
        val row: AccessibilityNodeInfo,
        val bounds: Rect,
        val author: String? = null,
        val textTarget: AccessibilityNodeInfo? = null,
        val authorTarget: AccessibilityNodeInfo? = null,
    )

    fun visibleTweets(root: AccessibilityNodeInfo?, nowMillis: Long = System.currentTimeMillis()): List<TweetRow> {
        if (root == null) return emptyList()
        val flatNodes = AccessibilityTree.nodes(root, maxNodes = 1_200)
        val flatRows = FeedRowEvidence.rows(flatNodes.map { it.toSnapshot() }).map { r ->
            val header = flatNodes[r.headerIndex]
            TweetRow(r.key, r.age, header, Rect().also(header::getBoundsInScreen), r.author,
                r.bodyIndex?.let(flatNodes::get), header)
        }
        // Do not require offscreen reply/like controls, legacy IDs, or a common parent.
        if (flatRows.isNotEmpty()) return flatRows
        return AccessibilityTree.nodes(root, maxNodes = 1_200).asSequence()
            .mapNotNull { node ->
                val id = node.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
                val raw = listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" ")
                val hasTweetId = id.contains("tweet") || id.contains("status") || raw.contains("/status/")
                if (!node.isVisibleToUser) return@mapNotNull null
                val age = listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).firstNotNullOfOrNull { TweetContentEvidence.header(it)?.ageMinutes ?: parseAgeMinutes(it, nowMillis) }
                if (!hasTweetId && age == null) return@mapNotNull null
                val row = tweetRowAncestor(node) ?: return@mapNotNull null
                val corpus = AccessibilityTree.nodes(row, maxNodes = 180)
                    .flatMap { listOfNotNull(it.text?.toString(), it.contentDescription?.toString()) }
                    .joinToString(" ")
                val rowLabels = AccessibilityTree.nodes(row, 180).flatMap { listOfNotNull(it.text?.toString(), it.contentDescription?.toString()) }.map(XUiVocabulary::normalize)
                if (rowLabels.any { it in setOf("pinned", "sabitlendi", "sabitlenmiş", "promoted", "reklam") }) return@mapNotNull null
                val key = statusKey(corpus) ?: stableTextKey(rowLabels.joinToString(" | "))
                if (key.isBlank()) return@mapNotNull null
                val bounds = Rect().also(row::getBoundsInScreen)
                if (bounds.isEmpty) return@mapNotNull null
                val rowNodes = AccessibilityTree.nodes(row, 180)
                val author = rowNodes.asSequence().filter { it.isVisibleToUser }.firstNotNullOfOrNull { child ->
                    listOfNotNull(child.text?.toString(), child.contentDescription?.toString())
                        .firstNotNullOfOrNull { TweetContentEvidence.header(it)?.handle ?: AccountSwitcherInspector.dedicatedHandle(it) }
                }
                val timestampAge = rowNodes.asSequence().filter { it.isVisibleToUser }.mapNotNull { child ->
                    val t = child.text?.toString().orEmpty()
                    val d = child.contentDescription?.toString().orEmpty()
                    listOf(t, d).filter { it.length < 100 && !it.contains("@") }
                        .firstNotNullOfOrNull { parseAgeMinutes(it, nowMillis) }
                }.firstOrNull()
                val headerNode = rowNodes.firstOrNull { child ->
                    listOfNotNull(child.text?.toString(), child.contentDescription?.toString()).any {
                        TweetContentEvidence.header(it) != null || AccountSwitcherInspector.dedicatedHandle(it) == author
                    }
                }
                val headerBottom = headerNode?.let { Rect().also(it::getBoundsInScreen).bottom } ?: bounds.top
                val textIndex = TweetContentEvidence.bodyIndex(rowNodes.map { it.toSnapshot() }, headerBottom)
                TweetRow(key, timestampAge ?: age, row, bounds, author, textIndex?.let(rowNodes::get))
            }
            .distinctBy(TweetRow::key)
            .sortedBy { it.bounds.top }
            .toList()
    }

    /** Only the author field of an actual reply row is evidence; body mentions are excluded. */
    fun visibleReplyAuthors(root: AccessibilityNodeInfo?): List<String> = visibleTweets(root).mapNotNull { it.author }.distinct()

    fun clickReplyAuthor(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, handle: String): Boolean {
        val row = visibleTweets(root).firstOrNull { it.author == handle } ?: return false
        row.authorTarget?.let { target ->
            // Compose can expose "Name @handle · age" as one wide header. Its
            // centre belongs to the reply card and opens the reply as a tweet;
            // the leading name/handle area opens the user's profile.
            return GestureClick.gestureTapLeading(service, target)
        }
        val node = AccessibilityTree.nodes(row.row, 180).firstOrNull { child ->
            child.isVisibleToUser && listOfNotNull(child.text?.toString(), child.contentDescription?.toString()).any {
                TweetContentEvidence.header(it)?.handle == handle || AccountSwitcherInspector.dedicatedHandle(it) == handle
            }
        } ?: return false
        return GestureClick.gestureTapLeading(service, node)
    }

    fun eligibleLatestFive(
        rows: Collection<TweetRow>,
        processedKeys: Set<String>,
        minimumAgeMinutes: Long = 120L,
    ): List<TweetRow> = rows.asSequence()
        .filter { it.key !in processedKeys }
        .filter { (it.ageMinutes ?: -1L) >= minimumAgeMinutes }
        .toList()

    fun click(service: AtmacaAccessibilityService, row: TweetRow): Boolean =
        row.textTarget?.let { GestureClick.gestureTap(service, it) } ?: false

    internal fun parseAgeMinutes(raw: String?, nowMillis: Long = System.currentTimeMillis()): Long? {
        val text = raw.orEmpty().trim().lowercase(Locale("tr", "TR"))
        Regex("^(\\d{1,4})\\s*(?:dk|dak|dakika|min|mins|minutes)(?:\\s*(?:ago|önce))?$").find(text)?.let {
            return it.groupValues[1].toLongOrNull()
        }
        Regex("^(\\d{1,3})\\s*(?:sa|saat|h|hr|hrs|hours)(?:\\s*(?:ago|önce))?$").find(text)?.let {
            return it.groupValues[1].toLongOrNull()?.times(60L)
        }
        Regex("^(\\d{1,3})\\s*(?:g|gün|d|day|days)(?:\\s*(?:ago|önce))?$").find(text)?.let {
            return it.groupValues[1].toLongOrNull()?.times(1_440L)
        }

        val today = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val formats = listOf(
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale("tr", "TR")),
            DateTimeFormatter.ofPattern("d MMM", Locale("tr", "TR")),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US),
            DateTimeFormatter.ofPattern("MMM d", Locale.US),
        )
        for (format in formats) {
            val candidates = listOf(raw.orEmpty().trim())
            for (candidate in candidates) {
                val date = runCatching { LocalDate.parse(candidate, format) }.getOrNull()
                    ?: runCatching { LocalDate.parse("$candidate ${today.year}", DateTimeFormatter.ofPattern("d MMM yyyy", Locale("tr", "TR"))) }.getOrNull()
                    ?: runCatching { LocalDate.parse("$candidate, ${today.year}", DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)) }.getOrNull()
                if (date != null) {
                    // Date-only labels do not reveal the time. Use the latest possible
                    // time on that date, so a new post is never treated as two hours old.
                    return Duration.between(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant(), Instant.ofEpochMilli(nowMillis)).toMinutes().coerceAtLeast(0L)
                }
            }
        }
        return null
    }

    private fun tweetRowAncestor(start: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var node: AccessibilityNodeInfo? = start
        repeat(8) {
            val current = node ?: return@repeat
            val descendants = AccessibilityTree.nodes(current, maxNodes = 180)
            val ids = descendants.mapNotNull { it.viewIdResourceName?.lowercase(Locale.ROOT) }
            val corpus = descendants.flatMap { listOfNotNull(it.text?.toString(), it.contentDescription?.toString()) }.joinToString(" ").lowercase()
            val hasActions = listOf(setOf("reply", "yanıt", "yorum"), setOf("like", "beğeni"), setOf("retweet", "repost", "yeniden gönder"), setOf("bookmark", "yer işareti")).count { words -> words.any { token -> ids.any { it.contains(token) } || corpus.contains(token) } }
            val authors = descendants.filter { it.isVisibleToUser }.mapNotNull { child ->
                val labels = listOfNotNull(child.text?.toString(), child.contentDescription?.toString())
                labels.firstNotNullOfOrNull { TweetContentEvidence.header(it)?.handle ?: AccountSwitcherInspector.dedicatedHandle(it) }
                    ?.let { handle -> handle to Rect().also(child::getBoundsInScreen).top }
            }.distinct()
            if (descendants.size < 180 && hasActions >= 2 && authors.size == 1) return current
            node = current.parent
        }
        return null
    }

    private fun statusKey(corpus: String): String? = Regex("(?:x|twitter)\\.com/[^/\\s]+/status/(\\d+)", RegexOption.IGNORE_CASE)
        .find(corpus)?.groupValues?.getOrNull(1)

    internal fun eligibleAge(minutes: Long?): Boolean = minutes != null && minutes >= 120L

    internal fun stableTextKey(corpus: String): String {
        val stable = corpus.split(" | ").filter { label ->
            parseAgeMinutes(label) == null &&
                !Regex("[0-9., kmb]+(?:yanıt|replies|beğeni|likes|retweets|reposts|görüntüleme|views).*", RegexOption.IGNORE_CASE).matches(label)
        }.distinct().joinToString(" | ").trim()
        return stable.takeIf { it.isNotBlank() }?.hashCode()?.toString().orEmpty()
    }
}
