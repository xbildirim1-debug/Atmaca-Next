package com.buse.app.automation

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/** A badge must come from the owning row's subtree, never from a neighboring row. */
object BuseFollowingInspector {
    data class Row(val evidence: BuseRowEvidence, val bounds: Rect)

    fun rows(root: AccessibilityNodeInfo?): List<Row> {
        if (root == null) return emptyList()
        val nodes = AccessibilityTree.nodes(root, maxNodes = 2000)
        val rootBounds = Rect().also(root::getBoundsInScreen)
        val headerBottom = nodes.filter { node ->
            (node.isSelected || node.isChecked) && node.isVisibleToUser && RelationshipTabInspector.classifySelectedLabels(labels(node)) == RelationshipTabInspector.FOLLOWING
        }.maxOfOrNull { Rect().also(it::getBoundsInScreen).bottom } ?: rootBounds.top
        val rows = nodes.mapNotNull { handleNode ->
            if (!handleNode.isVisibleToUser) return@mapNotNull null
            val handle = labels(handleNode).firstNotNullOfOrNull(AccountSwitcherInspector::dedicatedHandle) ?: return@mapNotNull null
            var candidate: AccessibilityNodeInfo? = handleNode
            var owning: AccessibilityNodeInfo? = null
            repeat(9) {
                val current = candidate ?: return@repeat
                val descendants = AccessibilityTree.nodes(current, maxNodes = 160)
                val handles = descendants.flatMap(::labels).mapNotNull(AccountSwitcherInspector::dedicatedHandle).toSet()
                if (descendants.size >= 160 || handles != setOf(handle)) { candidate = null; return@repeat }
                val rect = Rect().also(current::getBoundsInScreen)
                // Keep the outermost single-person row, including its badge above the name.
                if (!rect.isEmpty && rect.height() < rootBounds.height() * .8f) owning = current
                candidate = current.parent
            }
            val row = owning ?: return@mapNotNull null
            val descendants = AccessibilityTree.nodes(row, maxNodes = 160)
            val raw = descendants.flatMap(::labels)
            val bounds = Rect().also(row::getBoundsInScreen)
            val handleBounds = Rect().also(handleNode::getBoundsInScreen)
            if (handleBounds.isEmpty || handleBounds.top < headerBottom || handleBounds.bottom > rootBounds.bottom) return@mapNotNull null
            val actions = descendants.filter { node ->
                RelationshipActionEvidence.matches(node.toSnapshot(), node.parent?.isClickable == true, observation = true)
            }
            fun hasAction(vocabulary: Set<String>) = actions.any { node -> labels(node).any { label ->
                val value = XUiVocabulary.normalize(label)
                vocabulary.any { value == it || value.startsWith("$it @") || value.startsWith("$it, @") }
            } }
            val following = hasAction(XUiVocabulary.followingActions)
            if (!following && !hasAction(VerifiedFollowPolicy.availableFollowLabels + XUiVocabulary.requestedActions)) return@mapNotNull null
            // A handle-only or button-only subtree cannot prove that an absent badge is absent.
            val hasName = descendants.any { node ->
                val rect = Rect().also(node::getBoundsInScreen)
                rect.bottom <= handleBounds.top && labels(node).any { text ->
                    text.isNotBlank() && AccountSwitcherInspector.dedicatedHandle(text) == null &&
                        XUiVocabulary.normalize(text) !in XUiVocabulary.followingActions
                }
            }
            val complete = bounds.top >= headerBottom && bounds.bottom <= rootBounds.bottom &&
                handleBounds.top - headerBottom >= handleBounds.height() * 3 &&
                bounds.height() >= handleBounds.height() * 2 && hasName &&
                descendants.all { child ->
                    val rect = Rect().also(child::getBoundsInScreen)
                    !child.isVisibleToUser || rect.isEmpty ||
                        (rect.top >= headerBottom && rect.bottom <= rootBounds.bottom)
                }
            Row(BuseRowEvidence(handle, descendants.flatMap(::labels).mapNotNull(AccountSwitcherInspector::dedicatedHandle).toSet(),
                raw, complete, row.isVisibleToUser, row.isEnabled, following, nodes.size < 2000 && descendants.size < 160), bounds)
        }.distinctBy { it.evidence.handle }.sortedBy { it.bounds.top }
        val badges = nodes.filter { it.isVisibleToUser && labels(it).any(BuseNonFollowerPolicy::hasFollowsYouLabel) }
        return rows.mapIndexed { index, row ->
            val handleTop = nodes.filter { labels(it).any { value -> AccountSwitcherInspector.dedicatedHandle(value) == row.evidence.handle } }
                .minOfOrNull { Rect().also(it::getBoundsInScreen).top } ?: row.bounds.top
            val previousBottom = rows.getOrNull(index - 1)?.bounds?.bottom ?: headerBottom
            val ownBadges = badges.filter { badge ->
                val rect = Rect().also(badge::getBoundsInScreen)
                rect.left >= row.bounds.left && rect.right <= row.bounds.right &&
                    BuseBadgeAssociation.belongsToRow(rect.top, rect.bottom, handleTop, previousBottom, headerBottom)
            }.flatMap(::labels)
            row.copy(evidence = row.evidence.copy(labels = row.evidence.labels + ownBadges))
        }
    }

    private fun labels(node: AccessibilityNodeInfo): List<String> =
        listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
}
