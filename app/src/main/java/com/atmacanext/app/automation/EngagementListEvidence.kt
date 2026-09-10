package com.atmacanext.app.automation

/** Counts are presentation data, not selectors. Never match the repost action button. */
internal object EngagementListEvidence {
    private val competingTabs = setOf("alıntılar", "quotes", "beğeniler", "likes")

    fun isReposts(raw: String): Boolean {
        val s = XUiVocabulary.normalize(raw)
            .substringBefore(", selected")
            .substringBefore(", seçili")
            .substringBefore(", active")
            .substringBefore(", aktif")
            .trim()
        return s in setOf("yeniden gönderenler", "yeniden gönderiler", "retweetler", "reposts", "reposted by") ||
            Regex("[0-9., kmb]+ (?:kişi )?tarafından yeniden gönderildi").matches(s) ||
            Regex("(?:reposts [0-9.,kmb ]+|[0-9.,kmb ]+ reposts|reposted by [0-9.,kmb ]+)").matches(s)
    }

    fun selected(root: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
        val explicit = AccessibilityTree.nodes(root).any { node ->
            node.isVisibleToUser && (node.isSelected || node.isChecked || node.contentDescription?.toString().orEmpty().let {
                it.contains("selected", true) || it.contains("seçili", true) || it.contains("active", true) || it.contains("aktif", true)
            }) && AccessibilityTree.snapshots(node, 12).filter { it.visible }.let { children ->
                val labels = children.flatMap { listOfNotNull(it.text, it.contentDescription) }
                labels.any(::isReposts) && labels.none { XUiVocabulary.normalize(it) in competingTabs }
            }
        }
        return explicit || repostOnlySurface(AccessibilityTree.snapshots(root))
    }

    fun selected(nodes: List<NodeSnapshot>): Boolean {
        val explicit = nodes.any { n ->
            n.visible && (n.selected || n.checked || n.contentDescription.orEmpty().let {
                it.contains("selected", true) || it.contains("seçili", true) || it.contains("active", true) || it.contains("aktif", true)
            }) && listOfNotNull(n.text, n.contentDescription).any(::isReposts)
        }
        return explicit || repostOnlySurface(nodes)
    }

    fun title(nodes: List<NodeSnapshot>): Boolean = nodes.any { n -> n.visible &&
        listOfNotNull(n.text, n.contentDescription).any {
            XUiVocabulary.normalize(it) in setOf("gönderi etkileşimleri", "post engagements", "tweet engagements")
        }
    }

    fun openLabel(raw: String): Boolean {
        val s = XUiVocabulary.normalize(raw)
            .substringBefore(", selected")
            .substringBefore(", seçili")
            .trim()
        val tokens = setOf(
            "alıntıları görüntüle",
            "view quotes",
            "view post engagements",
            "gönderi etkileşimlerini görüntüle",
            "post engagements",
        )
        return tokens.any { token ->
            s == token || s.startsWith("$token ") || s.startsWith("$token,") || s.startsWith("$token (")
        }
    }

    /**
     * Some X/Compose builds expose the repost surface without selected/checked state.
     * Accept that fallback only when the engagement title is present and no competing
     * Quotes/Likes tab is visible, so quote users can never be mistaken for reposters.
     */
    private fun repostOnlySurface(nodes: List<NodeSnapshot>): Boolean {
        if (!title(nodes)) return false
        val labels = nodes.filter { it.visible }
            .flatMap { listOfNotNull(it.text, it.contentDescription) }
            .map(XUiVocabulary::normalize)
        return labels.any(::isReposts) && labels.none { it in competingTabs }
    }
}
