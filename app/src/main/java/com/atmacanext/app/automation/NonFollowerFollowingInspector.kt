package com.atmacanext.app.automation

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/** Capture each live node once; all row/ancestor/badge checks then use that same fresh tree. */
object NonFollowerFollowingInspector {
    data class Row(val evidence: NonFollowerRowEvidence, val bounds: Rect)
    data class Reading(val rows: List<Row>, val snapshots: List<NodeSnapshot>)
    internal data class TreeNode(val snapshot: NodeSnapshot, val parentIndex: Int?)

    fun rows(root: AccessibilityNodeInfo?): List<Row> = read(root).rows

    fun read(root: AccessibilityNodeInfo?): Reading {
        if (root == null) return Reading(emptyList(), emptyList())
        val captured = ArrayList<TreeNode>()
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int?>>()
        queue.add(root to null)
        while (queue.isNotEmpty() && captured.size < 2000) {
            val (node, parent) = queue.removeFirst()
            if (node.isPassword) continue
            val index = captured.size
            captured += TreeNode(node.toSnapshot(), parent)
            for (child in 0 until node.childCount) node.getChild(child)?.let { queue.add(it to index) }
        }
        return Reading(inspectCaptured(captured), captured.map { it.snapshot })
    }

    internal fun inspectCaptured(nodes: List<TreeNode>): List<Row> {
        if (nodes.isEmpty() || nodes[0].parentIndex != null || nodes.indices.drop(1).any { index ->
                nodes[index].parentIndex?.let { it !in 0 until index } != false
            }) return emptyList()
        val snapshots = nodes.map { it.snapshot }
        val labels = snapshots.map { listOfNotNull(it.text, it.contentDescription) }
        val directHandles = labels.map { values -> values.mapNotNull(AccountSwitcherInspector::dedicatedHandle).toSet() }
        val children = Array(nodes.size) { ArrayList<Int>() }
        nodes.forEachIndexed { index, node -> node.parentIndex?.let { children[it].add(index) } }
        val subtreeCounts = IntArray(nodes.size) { 1 }
        val subtreeHandles = Array(nodes.size) { directHandles[it].toMutableSet() }
        for (index in nodes.indices.reversed()) {
            nodes[index].parentIndex?.let { parent ->
                subtreeCounts[parent] = (subtreeCounts[parent] + subtreeCounts[index]).coerceAtMost(160)
                subtreeHandles[parent].addAll(subtreeHandles[index])
            }
        }
        fun descendants(index: Int): List<Int> {
            val found = ArrayList<Int>()
            val pending = ArrayDeque<Int>()
            pending.add(index)
            while (pending.isNotEmpty() && found.size < 160) {
                val next = pending.removeFirst()
                found += next
                children[next].forEach(pending::addLast)
            }
            return found
        }
        fun nonEmpty(bounds: Rect) = bounds.right > bounds.left && bounds.bottom > bounds.top
        val rootBounds = snapshots[0].bounds
        val rootHeight = rootBounds.bottom - rootBounds.top
        val headerBottom = snapshots.indices.filter { index ->
            val node = snapshots[index]
            (node.selected || node.checked) && node.visible &&
                RelationshipTabInspector.classifySelectedLabels(labels[index]) == RelationshipTabInspector.FOLLOWING
        }.maxOfOrNull { snapshots[it].bounds.bottom } ?: rootBounds.top
        val handleTops = HashMap<String, Int>()
        directHandles.forEachIndexed { index, handles -> handles.forEach { handle ->
            handleTops[handle] = minOf(handleTops[handle] ?: Int.MAX_VALUE, snapshots[index].bounds.top)
        } }
        val rows = snapshots.indices.mapNotNull { handleIndex ->
            val handleNode = snapshots[handleIndex]
            if (!handleNode.visible) return@mapNotNull null
            val handle = labels[handleIndex].firstNotNullOfOrNull(AccountSwitcherInspector::dedicatedHandle) ?: return@mapNotNull null
            var candidate: Int? = handleIndex
            var owning: Int? = null
            repeat(9) {
                val index = candidate ?: return@repeat
                if (subtreeCounts[index] >= 160 || subtreeHandles[index] != setOf(handle)) {
                    candidate = null
                    return@repeat
                }
                val bounds = snapshots[index].bounds
                if (nonEmpty(bounds) && bounds.bottom - bounds.top < rootHeight * .8f) owning = index
                candidate = nodes[index].parentIndex
            }
            val rowIndex = owning ?: return@mapNotNull null
            val row = snapshots[rowIndex]
            val descendants = descendants(rowIndex)
            val bounds = row.bounds
            val handleBounds = handleNode.bounds
            if (!nonEmpty(handleBounds) || handleBounds.top < headerBottom || handleBounds.bottom > rootBounds.bottom) return@mapNotNull null
            val actions = descendants.filter { index ->
                RelationshipActionEvidence.matches(snapshots[index],
                    nodes[index].parentIndex?.let { snapshots[it].clickable } == true, observation = true)
            }
            fun hasAction(vocabulary: Set<String>) = actions.any { index -> labels[index].any { label ->
                val value = XUiVocabulary.normalize(label)
                vocabulary.any { value == it || value.startsWith("$it @") || value.startsWith("$it, @") }
            } }
            val following = hasAction(XUiVocabulary.followingActions)
            if (!following && !hasAction(VerifiedFollowPolicy.availableFollowLabels + XUiVocabulary.requestedActions)) return@mapNotNull null
            val hasName = descendants.any { index ->
                snapshots[index].bounds.bottom <= handleBounds.top && labels[index].any { text ->
                    text.isNotBlank() && AccountSwitcherInspector.dedicatedHandle(text) == null &&
                        XUiVocabulary.normalize(text) !in XUiVocabulary.followingActions
                }
            }
            val handleHeight = handleBounds.bottom - handleBounds.top
            val complete = bounds.top >= headerBottom && bounds.bottom <= rootBounds.bottom &&
                handleBounds.top - headerBottom >= handleHeight * 3 &&
                bounds.bottom - bounds.top >= handleHeight * 2 && hasName &&
                descendants.all { index ->
                    val child = snapshots[index]
                    !child.visible || !nonEmpty(child.bounds) ||
                        (child.bounds.top >= headerBottom && child.bounds.bottom <= rootBounds.bottom)
                }
            Row(NonFollowerRowEvidence(handle, subtreeHandles[rowIndex].toSet(), descendants.flatMap { labels[it] },
                complete, row.visible, row.enabled, following, nodes.size < 2000 && descendants.size < 160), bounds)
        }.distinctBy { it.evidence.handle }.sortedBy { it.bounds.top }
        val badges = snapshots.indices.filter { index ->
            snapshots[index].visible && labels[index].any(NonFollowerPolicy::hasFollowsYouLabel)
        }
        return rows.mapIndexed { index, row ->
            val handleTop = handleTops[row.evidence.handle] ?: row.bounds.top
            val previousBottom = rows.getOrNull(index - 1)?.bounds?.bottom ?: headerBottom
            val ownBadges = badges.filter { badge ->
                val bounds = snapshots[badge].bounds
                bounds.left >= row.bounds.left && bounds.right <= row.bounds.right &&
                    NonFollowerBadgeAssociation.belongsToRow(bounds.top, bounds.bottom, handleTop, previousBottom, headerBottom)
            }.flatMap { labels[it] }
            row.copy(evidence = row.evidence.copy(labels = row.evidence.labels + ownBadges))
        }
    }
}
