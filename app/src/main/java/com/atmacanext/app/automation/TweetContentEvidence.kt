package com.atmacanext.app.automation

/** Feed header and body selectors, separate from media and relationship controls. */
internal object TweetContentEvidence {
    data class Header(val handle: String, val ageMinutes: Long)
    fun header(raw: String): Header? {
        if (raw.length > 140 || raw.contains('\n')) return null
        val match = Regex("^(?:[^@\\n]{1,70}\\s)?@([A-Za-z0-9_]{1,15})\\s*[·•,]\\s*(.+)$").matchEntire(raw.trim()) ?: return null
        val age = XTweetInspector.parseAgeMinutes(match.groupValues[2]) ?: return null
        return Header(match.groupValues[1].lowercase(), age)
    }
    fun bodyIndex(nodes: List<NodeSnapshot>, headerBottom: Int): Int? {
        val candidates = nodes.indices.filter { i ->
            val n = nodes[i]
            val t = (n.text ?: n.contentDescription).orEmpty().trim()
            val id = n.viewId.orEmpty().lowercase()
            n.visible && n.enabled && n.bounds.top >= headerBottom &&
                n.bounds.right > n.bounds.left && n.bounds.bottom > n.bounds.top &&
                t.length >= 4 && t.any(Char::isLetter) && !n.editable &&
                !n.className.orEmpty().contains("button", true) &&
                listOf("avatar", "media", "video", "image", "toolbar", "follow", "reply", "retweet", "like", "bookmark").none(id::contains) &&
                header(t) == null && XTweetInspector.parseAgeMinutes(t) == null &&
                !t.startsWith("@") && !t.startsWith("http", true) &&
                !Regex("^[0-9., kmb]+(?:yanıt|replies|beğeni|likes|yeniden gönder|reposts|görüntüleme|views).*", RegexOption.IGNORE_CASE).matches(t) &&
                !Regex("^(?:video|fotoğraf|resim|image|photo|play|oynat|beğen|yanıtla|reply|repost|retweet|bookmark)(?:\\s.*)?$", RegexOption.IGNORE_CASE).matches(t) &&
                XUiVocabulary.normalize(t) !in setOf("daha fazlasını göster", "show more", "takip et", "follow", "yanıtını gönder", "alıntıları görüntüle")
        }
        return candidates.firstOrNull { nodes[it].viewId.orEmpty().let { id -> id.contains("tweet_text") || id.contains("tweet_content") || id.contains("status_text") } }
            ?: candidates.minByOrNull { nodes[it].bounds.top }
    }
}
