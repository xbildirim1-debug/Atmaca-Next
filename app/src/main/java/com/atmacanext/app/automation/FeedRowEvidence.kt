package com.atmacanext.app.automation

/** Compose may expose author, time and body as siblings, without a tweet container. */
internal object FeedRowEvidence {
    data class Row(val author: String, val age: Long, val headerIndex: Int, val bodyIndex: Int?, val key: String)
    private fun labels(n: NodeSnapshot) = listOfNotNull(n.text, n.contentDescription)
    private fun usable(n: NodeSnapshot) = n.visible && !n.editable &&
        n.bounds.right > n.bounds.left && n.bounds.bottom > n.bounds.top
    private fun sameLine(a: NodeSnapshot, b: NodeSnapshot) =
        maxOf(a.bounds.top, b.bounds.top) < minOf(a.bounds.bottom, b.bounds.bottom)
    fun rows(nodes: List<NodeSnapshot>): List<Row> {
        val headers = nodes.indices.mapNotNull { i ->
            val n = nodes[i]
            if (!usable(n)) return@mapNotNull null
            val combined = labels(n).firstNotNullOfOrNull(TweetContentEvidence::header)
            if (combined != null) return@mapNotNull Triple(i, combined.handle, combined.ageMinutes)
            val handle = labels(n).firstNotNullOfOrNull(AccountSwitcherInspector::dedicatedHandle)
                ?: return@mapNotNull null
            // Time must be a separate, short label on the author's actual visual line.
            val times = nodes.filter { usable(it) && sameLine(n, it) && it.bounds.left >= n.bounds.left }
                .flatMap(::labels).mapNotNull { XTweetInspector.parseAgeMinutes(it.trim().trimStart('·', '•', ',').trim()) }.distinct()
            if (times.size != 1) null else Triple(i, handle, times.single())
        }.sortedBy { nodes[it.first].bounds.top }.fold(mutableListOf<Triple<Int, String, Long>>()) { out, item ->
            if (out.none { it.second == item.second && sameLine(nodes[it.first], nodes[item.first]) }) out.add(item)
            out
        }
        return headers.mapIndexedNotNull { position, (index, author, age) ->
            val header = nodes[index]
            val nextTop = headers.getOrNull(position + 1)?.let { nodes[it.first].bounds.top } ?: Int.MAX_VALUE
            val barriers = nodes.filter { usable(it) && it.bounds.top >= header.bounds.bottom &&
                labels(it).any { s -> XUiVocabulary.normalize(s) in setOf("kimi takip etmeli", "who to follow", "daha fazla keşfet", "discover more") } }
            val mediaOrActions = nodes.filter { usable(it) && it.bounds.top >= header.bounds.bottom &&
                listOf("video", "media", "toolbar", "tweet_image", "tweet_photo").any { token -> it.viewId.orEmpty().contains(token, true) } }
            val end = minOf(nextTop, barriers.minOfOrNull { it.bounds.top } ?: Int.MAX_VALUE,
                mediaOrActions.minOfOrNull { it.bounds.top } ?: Int.MAX_VALUE)
            val band = nodes.indices.filter { usable(nodes[it]) && nodes[it].bounds.top >= header.bounds.top && nodes[it].bounds.bottom <= end }
            // Markers immediately above a header belong to that card, not the next card.
            val marked = nodes.any { n -> usable(n) && n.bounds.bottom <= header.bounds.bottom &&
                n.bounds.top >= header.bounds.top - (header.bounds.bottom - header.bounds.top) * 2 &&
                labels(n).any { XUiVocabulary.normalize(it) in setOf("pinned", "sabitlendi", "sabitlenmiş", "promoted", "reklam") } }
            if (marked) return@mapIndexedNotNull null
            val body = TweetContentEvidence.bodyIndex(band.map(nodes::get), header.bounds.bottom)?.let(band::get)
            val text = body?.let { nodes[it].text ?: nodes[it].contentDescription }.orEmpty().trim()
            // Counts, relative age, scroll position and toolbar state are not post identity.
            val key = java.security.MessageDigest.getInstance("SHA-256")
                .digest("$author|$text".toByteArray()).joinToString("") { "%02x".format(it) }
            Row(author, age, index, body, key)
        }
    }

    fun profileFeed(nodes: List<NodeSnapshot>): Boolean {
        val labels = nodes.filter { it.visible }.flatMap(::labels).map(XUiVocabulary::normalize)
        val posts = labels.any { it == "gönderiler" || it == "posts" || it.startsWith("gönderiler,") || it.startsWith("posts,") }
        val count = labels.any { Regex("[0-9.,bk m]+ (?:gönderileri|gönderi|posts)").matches(it) }
        return posts && (count || rows(nodes).isNotEmpty())
    }
}
