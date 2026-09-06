package com.atmacanext.app.automation

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Finds the own-profile Following counter semantically. The counter can move
 * vertically between profiles, so only the selected node's real bounds are
 * used; there is no screen coordinate fallback.
 */
object UnfollowProfileStatSelector {
    private val followingTokens = setOf("takip edilen", "takip ediliyor", "following")
    private val digit = Regex("\\d")

    fun click(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        val target = find(root) ?: return false
        val clicked = GestureClick.click(service, target)
        if (clicked) OperationLog.i("NAV", "Kendi profilindeki Takip ediliyor sayacı semantik olarak tıklandı")
        return clicked
    }

    internal fun find(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        val allNodes = AccessibilityTree.nodes(root, maxNodes = 900)
        val isOwnProfile = allNodes.any { node ->
            labels(node).any { it in XUiVocabulary.ownProfileSignals }
        }
        if (!isOwnProfile) return null

        return allNodes.asSequence()
            .mapNotNull { node ->
                val target = safeClickableAncestor(node) ?: node.takeIf { it.isEnabled } ?: return@mapNotNull null
                val subtree = AccessibilityTree.nodes(target, maxNodes = 36)
                if (subtree.size >= 36) return@mapNotNull null
                val subtreeLabels = subtree.flatMap(::labels)
                val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
                val score = candidateScore(subtreeLabels, viewId) ?: return@mapNotNull null
                val bounds = Rect().also(target::getBoundsInScreen)
                if (bounds.isEmpty) return@mapNotNull null
                target to score
            }
            .distinctBy { System.identityHashCode(it.first) }
            .maxByOrNull { it.second }
            ?.first
    }

    internal fun candidateScore(rawLabels: List<String>, rawViewId: String): Int? {
        val normalized = rawLabels.map(XUiVocabulary::normalize).filter(String::isNotBlank)
        val corpus = normalized.joinToString(" ")
        val viewId = rawViewId.lowercase(Locale.ROOT)
        val hasForbidden = normalized.any { label ->
            XUiVocabulary.forbiddenProfilePhrases.any(label::contains)
        }
        if (hasForbidden) return null

        val tokenMatches = followingTokens.count { token -> containsPhrase(corpus, token) }
        val idMatches = listOf("following", "following_count").count(viewId::contains)
        val hasNumber = digit.containsMatchIn(corpus)
        if (tokenMatches == 0 && idMatches == 0) return null
        if (!hasNumber) return null

        val actionOnly = normalized.none(digit::containsMatchIn) && normalized.any { label ->
            label in XUiVocabulary.followingActions
        }
        if (actionOnly) return null
        return tokenMatches * 220 + idMatches * 180 + 80 - normalized.size.coerceAtMost(20)
    }

    private fun containsPhrase(corpus: String, token: String): Boolean =
        corpus == token || corpus.startsWith("$token ") || corpus.endsWith(" $token") || corpus.contains(" $token ")

    private fun labels(node: AccessibilityNodeInfo): List<String> =
        listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            .map(XUiVocabulary::normalize)

    private fun safeClickableAncestor(start: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = start
        repeat(6) {
            val node = current ?: return@repeat
            if (node.isEnabled && node.isClickable && !TargetVerifier.containsForbiddenProfilePhrase(node)) return node
            current = node.parent
        }
        return null
    }
}
