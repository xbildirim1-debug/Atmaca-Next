package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

object AccessibilityTree {
    fun snapshots(root: AccessibilityNodeInfo?, maxNodes: Int = 800): List<NodeSnapshot> {
        if (root == null) return emptyList()
        val out = ArrayList<NodeSnapshot>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty() && out.size < maxNodes) {
            val node = queue.removeFirst()
            if (node.isPassword) continue
            out += node.toSnapshot()
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::addLast)
            }
        }
        return out
    }

    fun nodes(root: AccessibilityNodeInfo?, maxNodes: Int = 800): List<AccessibilityNodeInfo> {
        if (root == null) return emptyList()
        val out = ArrayList<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty() && out.size < maxNodes) {
            val node = queue.removeFirst()
            if (node.isPassword) continue
            out += node
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::addLast)
            }
        }
        return out
    }
}
