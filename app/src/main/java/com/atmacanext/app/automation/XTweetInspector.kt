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
        val authorTruncated: Boolean = false,
        /** True when a card-only post must be opened through the age side of its combined header. */
        val openViaHeaderTrailing: Boolean = false,
    )

    fun visibleTweets(root: AccessibilityNodeInfo?, nowMillis: Long = System.currentTimeMillis()): List<TweetRow> {
        if (root == null) return emptyList()
        val flatNodes = AccessibilityTree.nodes(root, maxNodes = 1_200)
        val flatRows = FeedRowEvidence.rows(flatNodes.map { it.toSnapshot() }).map { r ->
            val header = flatNodes[r.headerIndex]
            val body = r.bodyIndex?.let(flatNodes::get)
            val rowNode = tweetRowAncestor(header) ?: header
            val ageNode = if (body == null) explicitAgeNode(flatNodes, header, nowMillis) else null
            val combinedHeader = labels(header).any { TweetContentEvidence.header(it) != null }
            val detailTarget = body ?: ageNode ?: header.takeIf { combinedHeader }
            TweetRow(
                r.key,
                r.age,
                rowNode,
                Rect().also(rowNode::getBoundsInScreen),
                r.author,
                detailTarget,
                header,
                r.authorTruncated,
                openViaHeaderTrailing = body == null && ageNode == null && combinedHeader,
            )
        }

        // X/Compose can expose mixed row shapes in the very same viewport: the
        // parent post or one reply can be a flat sibling row while other replies
        // are only recoverable from their semantic ancestor. Never let the
        // presence of one flat row suppress the fallback parser for every other
        // visible reply. Merge both sources and de-duplicate the same visual row.
        val fallbackRows = flatNodes.asSequence()
            .mapNotNull { node ->
                val id = node.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
                val raw = listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" ")
                val hasTweetId = id.contains("tweet") || id.contains("status") || raw.contains("/status/")
                if (!node.isVisibleToUser) return@mapNotNull null
                val age = listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).firstNotNullOfOrNull {
                    TweetContentEvidence.header(it)?.ageMinutes ?: parseAgeMinutes(it, nowMillis)
                }
                if (!hasTweetId && age == null) return@mapNotNull null
                val row = tweetRowAncestor(node) ?: return@mapNotNull null
                val corpus = AccessibilityTree.nodes(row, maxNodes = 180)
                    .flatMap { listOfNotNull(it.text?.toString(), it.contentDescription?.toString()) }
                    .joinToString(" ")
                val rowLabels = AccessibilityTree.nodes(row, 180)
                    .flatMap { listOfNotNull(it.text?.toString(), it.contentDescription?.toString()) }
                    .map(XUiVocabulary::normalize)
                if (rowLabels.any { it in setOf("pinned", "sabitlendi", "sabitlenmiş", "promoted", "reklam") }) return@mapNotNull null
                val key = statusKey(corpus) ?: stableTextKey(rowLabels.joinToString(" | "))
                if (key.isBlank()) return@mapNotNull null
                val bounds = Rect().also(row::getBoundsInScreen)
                if (bounds.isEmpty) return@mapNotNull null
                val rowNodes = AccessibilityTree.nodes(row, 180)

                var author: String? = null
                var authorTruncated = false
                run authorSearch@{
                    for (child in rowNodes) {
                        if (!child.isVisibleToUser) continue
                        for (label in listOfNotNull(child.text?.toString(), child.contentDescription?.toString())) {
                            val combined = TweetContentEvidence.header(label)
                            if (combined != null) {
                                author = combined.handle
                                authorTruncated = combined.truncated
                                return@authorSearch
                            }
                            val dedicated = AccountSwitcherInspector.dedicatedHandle(label)
                            if (dedicated != null) {
                                author = dedicated
                                authorTruncated = false
                                return@authorSearch
                            }
                        }
                    }
                }

                val timestampNode = rowNodes.asSequence().filter { it.isVisibleToUser }.firstOrNull { child ->
                    labels(child).any { value ->
                        value.length < 100 && !value.contains("@") &&
                            parseAgeMinutes(value.trim().trimStart('·', '•', ',').trim(), nowMillis) != null
                    }
                }
                val timestampAge = timestampNode?.let { child ->
                    labels(child).firstNotNullOfOrNull { value ->
                        parseAgeMinutes(value.trim().trimStart('·', '•', ',').trim(), nowMillis)
                    }
                }
                val headerNode = rowNodes.firstOrNull { child ->
                    listOfNotNull(child.text?.toString(), child.contentDescription?.toString()).any {
                        TweetContentEvidence.header(it) != null ||
                            (!authorTruncated && AccountSwitcherInspector.dedicatedHandle(it) == author)
                    }
                }
                val headerBottom = headerNode?.let { Rect().also(it::getBoundsInScreen).bottom } ?: bounds.top
                val textIndex = TweetContentEvidence.bodyIndex(rowNodes.map { it.toSnapshot() }, headerBottom)
                val bodyTarget = textIndex?.let(rowNodes::get)
                val combinedHeader = headerNode?.let(::labels)?.any { TweetContentEvidence.header(it) != null } == true
                val detailTarget = bodyTarget ?: timestampNode ?: headerNode?.takeIf { combinedHeader }
                TweetRow(
                    key,
                    timestampAge ?: age,
                    row,
                    bounds,
                    author,
                    detailTarget,
                    headerNode,
                    authorTruncated,
                    openViaHeaderTrailing = bodyTarget == null && timestampNode == null && combinedHeader,
                )
            }
            .distinctBy(TweetRow::key)
            .toList()

        return mergeVisibleRows(flatRows, fallbackRows)
    }

    /** Only complete author identities of actual reply rows are actionable. */
    fun visibleReplyAuthors(root: AccessibilityNodeInfo?): List<String> = visibleTweets(root)
        .asSequence()
        .filter { !it.authorTruncated }
        .mapNotNull { it.author }
        .distinct()
        .toList()

    /** A shortened feed handle may match only an already verified target account. */
    fun authorMatches(row: TweetRow, expected: String): Boolean {
        val author = row.author ?: return false
        val target = XIdentityDetector.normalizeUsername(expected)
        return if (row.authorTruncated) author.length >= 4 && target.startsWith(author) else author == target
    }

    fun clickReplyAuthor(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, handle: String): Boolean {
        if (root == null) return false
        val rows = visibleTweets(root)
        val rowIndex = rows.indexOfFirst { !it.authorTruncated && it.author == handle }
        if (rowIndex < 0) return false
        val row = rows[rowIndex]

        // Product rule: commenter-follow never opens image/video/GIF replies. Bound
        // media detection to this reply's visible vertical slice so media in another
        // reply cannot cause the wrong author to be skipped.
        val allNodes = AccessibilityTree.snapshots(root)
        val nextTop = rows.drop(rowIndex + 1).map { it.bounds.top }.filter { it > row.bounds.top }.minOrNull()
        val rowBottom = nextTop ?: row.bounds.bottom.coerceAtLeast(row.bounds.top + 320)
        if (ReplyMediaEvidence.hasMedia(allNodes, row.bounds.top, rowBottom)) {
            OperationLog.i("COMMENT_SKIP", "@$handle resimli/medyalı yorum; açılmadan atlandı")
            return false
        }

        row.authorTarget?.let { target ->
            // Compose can expose "Name @handle · age" as one wide header. Its
            // centre belongs to the reply card and opens the reply as a tweet;
            // the leading name/handle area opens the user's profile/reply target.
            return GestureClick.gestureTapLeading(service, target)
        }
        val node = AccessibilityTree.nodes(row.row, 180).firstOrNull { child ->
            child.isVisibleToUser && listOfNotNull(child.text?.toString(), child.contentDescription?.toString()).any {
                val header = TweetContentEvidence.header(it)
                (header != null && !header.truncated && header.handle == handle) ||
                    AccountSwitcherInspector.dedicatedHandle(it) == handle
            }
        } ?: return false
        return GestureClick.gestureTapLeading(service, node)
    }

    fun eligibleLatestFive(
        rows: Collection<TweetRow>,
        processedKeys: Set<String>,
        minimumAgeMinutes: Long = 120L,
    ): List<TweetRow> {
        // Discovery is never allowed to weaken the product rule below two hours.
        // Older runtime callers used 90 minutes; clamp that legacy value here so a
        // 1h30–1h59 post can never be selected even if a stale caller passes 90.
        val effectiveMinimum = minimumAgeMinutes.coerceAtLeast(120L)
        return rows.asSequence()
            .filter { it.key !in processedKeys }
            .filter { (it.ageMinutes ?: -1L) >= effectiveMinimum }
            .toList()
    }

    fun text(row: TweetRow): String = row.textTarget?.let { it.text ?: it.contentDescription }?.toString().orEmpty()

    fun click(service: AtmacaAccessibilityService, row: TweetRow, retryAttempt: Int = 0): Boolean {
        val node = row.textTarget ?: return false
        if (!node.isVisibleToUser || !node.isEnabled) return false

        // A video/photo/card-only tweet can have no separate body Text node. 26.38
        // treated that as a fatal condition and stopped on the target profile. When
        // the timestamp is a separate node it is the click target directly. When X
        // combines author + age into one header, tap only the trailing age side; the
        // leading side is the profile. All points come from live node rectangles.
        if (row.openViaHeaderTrailing) {
            val accepted = GestureClick.gestureTapTrailing(service, node)
            OperationLog.i(
                "DISCOVERY_CLICK",
                "key=${row.key} author=@${row.author}${if (row.authorTruncated) "…" else ""} mode=header-age bounds=${Rect().also(node::getBoundsInScreen)} retry=$retryAttempt accepted=$accepted; detay doğrulaması bekleniyor",
            )
            return accepted
        }

        val native = retryAttempt == 0 && node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        val accepted = native || if (retryAttempt == 0) GestureClick.gestureTapText(service, node)
            else GestureClick.gestureTapTextRetry(service, node, retryAttempt)
        OperationLog.i(
            "DISCOVERY_CLICK",
            "key=${row.key} author=@${row.author}${if (row.authorTruncated) "…" else ""} mode=body-or-age bounds=${Rect().also(node::getBoundsInScreen)} native=$native retry=$retryAttempt accepted=$accepted; detay doğrulaması bekleniyor",
        )
        return accepted
    }

    private fun explicitAgeNode(
        nodes: List<AccessibilityNodeInfo>,
        header: AccessibilityNodeInfo,
        nowMillis: Long,
    ): AccessibilityNodeInfo? {
        val headerBounds = Rect().also(header::getBoundsInScreen)
        return nodes.asSequence()
            .filter { it !== header && it.isVisibleToUser && it.isEnabled }
            .filter { child ->
                val bounds = Rect().also(child::getBoundsInScreen)
                maxOf(headerBounds.top, bounds.top) < minOf(headerBounds.bottom, bounds.bottom) &&
                    bounds.left >= headerBounds.left
            }
            .firstOrNull { child ->
                labels(child).any { value ->
                    value.length < 100 && !value.contains("@") &&
                        parseAgeMinutes(value.trim().trimStart('·', '•', ',').trim(), nowMillis) != null
                }
            }
    }

    private fun labels(node: AccessibilityNodeInfo): List<String> =
        listOfNotNull(node.text?.toString(), node.contentDescription?.toString())

    private fun mergeVisibleRows(primary: List<TweetRow>, fallback: List<TweetRow>): List<TweetRow> {
        val merged = mutableListOf<TweetRow>()
        (primary + fallback).sortedBy { it.bounds.top }.forEach { candidate ->
            val duplicate = merged.indexOfFirst { existing -> sameVisibleRow(existing, candidate) }
            if (duplicate < 0) {
                merged += candidate
            } else if (rowEvidenceScore(candidate) > rowEvidenceScore(merged[duplicate])) {
                merged[duplicate] = candidate
            }
        }
        return merged.sortedBy { it.bounds.top }
    }

    private fun sameVisibleRow(a: TweetRow, b: TweetRow): Boolean {
        if (a.key == b.key) return true
        val aAuthor = a.author ?: return false
        val bAuthor = b.author ?: return false
        if (aAuthor != bAuthor) return false
        // One parser may expose only the header rectangle while the other exposes
        // the whole reply card. Geometric overlap is used only for de-duplication,
        // never to discover or click a user.
        return maxOf(a.bounds.top, b.bounds.top) < minOf(a.bounds.bottom, b.bounds.bottom)
    }

    private fun rowEvidenceScore(row: TweetRow): Int =
        (if (!row.authorTruncated) 4 else 0) +
            (if (row.textTarget != null) 2 else 0) +
            (if (row.authorTarget != null) 1 else 0)

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
                    ?: runCatching {
                        LocalDate.parse("$candidate ${today.year}", DateTimeFormatter.ofPattern("d MMM yyyy", Locale("tr", "TR")))
                    }.getOrNull()
                    ?: runCatching {
                        LocalDate.parse("$candidate, ${today.year}", DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
                    }.getOrNull()
                if (date != null) {
                    // Date-only labels do not reveal the time. Use the latest possible
                    // time on that date, so a new post is never treated as two hours old.
                    return Duration.between(
                        date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        Instant.ofEpochMilli(nowMillis),
                    ).toMinutes().coerceAtLeast(0L)
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
            val corpus = descendants
                .flatMap { listOfNotNull(it.text?.toString(), it.contentDescription?.toString()) }
                .joinToString(" ")
                .lowercase()
            val hasActions = listOf(
                setOf("reply", "yanıt", "yorum"),
                setOf("like", "beğeni"),
                setOf("retweet", "repost", "yeniden gönder"),
                setOf("bookmark", "yer işareti"),
            ).count { words -> words.any { token -> ids.any { it.contains(token) } || corpus.contains(token) } }
            val authors = descendants.filter { it.isVisibleToUser }.mapNotNull { child ->
                val labels = listOfNotNull(child.text?.toString(), child.contentDescription?.toString())
                labels.firstNotNullOfOrNull {
                    TweetContentEvidence.header(it)?.handle ?: AccountSwitcherInspector.dedicatedHandle(it)
                }?.let { handle -> handle to Rect().also(child::getBoundsInScreen).top }
            }.distinct()
            if (descendants.size < 180 && hasActions >= 2 && authors.size == 1) return current
            node = current.parent
        }
        return null
    }

    private fun statusKey(corpus: String): String? =
        Regex("(?:x|twitter)\\.com/[^/\\s]+/status/(\\d+)", RegexOption.IGNORE_CASE)
            .find(corpus)?.groupValues?.getOrNull(1)

    internal fun eligibleAge(minutes: Long?): Boolean = minutes != null && minutes >= 120L

    internal fun stableTextKey(corpus: String): String {
        val stable = corpus.split(" | ").filter { label ->
            parseAgeMinutes(label) == null &&
                !Regex(
                    "[0-9., kmb]+(?:yanıt|replies|beğeni|likes|retweets|reposts|görüntüleme|views).*",
                    RegexOption.IGNORE_CASE,
                ).matches(label)
        }.distinct().joinToString(" | ").trim()
        return stable.takeIf { it.isNotBlank() }?.hashCode()?.toString().orEmpty()
    }
}
