package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/** X/Twitter profile identity reader. A handle is trusted only with explicit @ syntax or a strong username resource id. */
object XIdentityDetector {
    private val explicitHandleRegex = Regex("@([A-Za-z0-9_]{1,15})(?![A-Za-z0-9_])")
    private val bareHandleRegex = Regex("[A-Za-z0-9_]{1,15}")

    fun inspectOwnProfile(root: AccessibilityNodeInfo?, expectedUsername: String): OwnProfileIdentity {
        if (root == null) return OwnProfileIdentity(false, false)
        val nodes = AccessibilityTree.nodes(root)
        val labels = nodes.flatMap(::labels)
        val own = labels.any { it in XUiVocabulary.ownProfileSignals }
        val detected = detectProfileHandle(root)
        if (!own) return OwnProfileIdentity(false, false, detected?.let { "@$it" })

        val expected = normalizeUsername(expectedUsername)
        // A mention in the bio or a tweet must never override a different profile header.
        val expectedVisible = detected == expected
        return OwnProfileIdentity(
            isOwnProfile = true,
            expectedAccountVisible = expectedVisible,
            detectedUsername = detected?.let { "@$it" },
        )
    }

    fun detectProfileHandle(root: AccessibilityNodeInfo?): String? {
        if (root == null) return null
        val nodes = AccessibilityTree.nodes(root)
        ProfileSurfaceEvidence.read(nodes.map { it.toSnapshot() })?.let { return it.handle }
        return nodes.asSequence().filter { it.isVisibleToUser && !it.isEditable && !it.isPassword }
            .mapNotNull { node ->
                val id = node.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
                val nodeLabels = labels(node)
                val explicit = nodeLabels.mapNotNull(::extractHandle).firstOrNull()
                val bare = if (explicit == null && isStrongUsernameId(id)) {
                    nodeLabels.mapNotNull(::extractBareHandleCandidate).firstOrNull()
                } else null
                val handle = explicit ?: bare ?: return@mapNotNull null

                var score = 0
                if (explicit != null) score += 100
                if (node.text?.toString()?.trim()?.startsWith("@") == true) score += 80
                if (isStrongUsernameId(id)) score += 140
                if (id.contains("toolbar") || id.contains("title")) score += 20
                handle to score
            }
            .maxByOrNull { it.second }
            ?.first
    }

    fun handleVisible(root: AccessibilityNodeInfo?, username: String): Boolean {
        if (root == null) return false
        val wanted = normalizeUsername(username)
        if (wanted.isBlank()) return false
        return AccessibilityTree.nodes(root, maxNodes = 700).any { node ->
            val id = node.viewIdResourceName?.lowercase(Locale.ROOT).orEmpty()
            labels(node).any { label ->
                extractHandle(label) == wanted ||
                    (isStrongUsernameId(id) && extractBareHandleCandidate(label) == wanted)
            }
        }
    }

    fun looksLikeProfile(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val labels = AccessibilityTree.nodes(root).flatMap(::labels)
        val corpus = labels.joinToString(" ")
        val statSignals = listOf(
            XUiVocabulary.followersHeaders.any { corpus.contains(it) },
            XUiVocabulary.followingHeaders.any { corpus.contains(it) },
            XUiVocabulary.joinedSignals.any { corpus.contains(it) },
        ).count { it }
        return detectProfileHandle(root) != null && statSignals >= 2
    }

    /**
     * Extracts only an explicit X handle.  CP9 treated any short word (for example "following")
     * as a username; that could corrupt screen detection. Bare handles are accepted only when a
     * node has a strong username/screen_name/handle resource id via detectProfileHandle().
     */
    fun extractHandle(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return explicitHandleRegex.find(raw)?.groupValues?.getOrNull(1)?.lowercase(Locale.ROOT)
    }

    fun normalizeUsername(raw: String): String = raw
        .lowercase(Locale.ROOT)
        .trim()
        .removePrefix("@")

    internal fun extractBareHandleCandidate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val normalized = normalizeUsername(raw)
        return normalized.takeIf { bareHandleRegex.matches(it) }
    }

    internal fun isStrongUsernameId(id: String): Boolean =
        id.contains("username") || id.contains("screen_name") || id.contains("screenname") || id.contains("handle")

    private fun labels(node: AccessibilityNodeInfo): List<String> = listOfNotNull(
        node.text?.toString(),
        node.contentDescription?.toString(),
    ).map(XUiVocabulary::normalize)
}
