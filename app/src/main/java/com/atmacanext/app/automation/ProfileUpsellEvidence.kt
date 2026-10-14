package com.atmacanext.app.automation

/** X's inline "account isn't verified" card. Its close control is not a dialog. */
internal object ProfileUpsellEvidence {
    fun present(nodes: List<NodeSnapshot>): Boolean {
        val labels = visibleLabels(nodes)
        return labels.any(::isTitle) && labels.any(::isPitch)
    }

    /** Profile stays behind this card, so the screen is not a modal dialog. */
    fun isInlineCard(nodes: List<NodeSnapshot>): Boolean =
        present(nodes) && nodes.any { node ->
            node.visible && XUiVocabulary.normalize(node.text ?: node.contentDescription) in setOf("gönderiler", "posts")
        }

    fun closeIndex(nodes: List<NodeSnapshot>): Int? {
        if (!present(nodes)) return null
        val titles = nodes.filter { it.visible && labels(it).any(::isTitle) }
        if (titles.isEmpty()) return null
        val top = titles.minOf { it.bounds.top } - 36
        val titleBottom = titles.maxOf { it.bounds.bottom } + 36
        val labeled = nodes.indices.filter { index ->
            val node = nodes[index]
            node.visible && node.enabled && node.bounds.top >= top && node.bounds.top <= titleBottom &&
                labels(node).any(::isClose)
        }
        if (labeled.isNotEmpty()) return labeled.maxBy { nodes[it].bounds.right }
        return nodes.indices.filter { index ->
            val node = nodes[index]
            val width = node.bounds.width()
            val height = node.bounds.height()
            node.visible && node.enabled && node.clickable &&
                width in 16..140 && height in 16..140 &&
                node.bounds.top >= top && node.bounds.bottom <= titleBottom &&
                labels(node).none { label ->
                    val text = XUiVocabulary.normalize(label)
                    text.contains("doğrula") || text.contains("verify") || text.contains("abone")
                }
        }.maxByOrNull { nodes[it].bounds.right }
    }

    private fun visibleLabels(nodes: List<NodeSnapshot>) =
        nodes.filter { it.visible }.flatMap(::labels).map(::plain)

    private fun labels(node: NodeSnapshot) = listOfNotNull(node.text, node.contentDescription)

    private fun plain(raw: String) = XUiVocabulary.normalize(raw).replace('’', '\'').replace('‘', '\'')

    private fun isTitle(raw: String): Boolean {
        val text = plain(raw)
        return text.contains("hesabın onaylı değil") || text.contains("hesabin onayli degil") ||
            text.contains("account isn't verified") || text.contains("account is not verified")
    }

    private fun isPitch(raw: String): Boolean {
        val text = plain(raw)
        return text.contains("abone ol") || text.contains("onay işareti") || text.contains("get verified") ||
            text.contains("subscribe")
    }

    private fun isClose(raw: String): Boolean {
        val text = plain(raw)
        return text == "kapat" || text == "close" || text.startsWith("kapat") || text.startsWith("close")
    }
}
