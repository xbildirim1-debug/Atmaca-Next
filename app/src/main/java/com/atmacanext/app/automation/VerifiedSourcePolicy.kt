package com.atmacanext.app.automation

import android.graphics.Rect
import kotlin.random.Random

/** Choose a real visible username on the exhausted list, independent of Follow state. */
internal object VerifiedSourcePolicy {
    fun visible(nodes: List<NodeSnapshot>, own: String, visited: Set<String>, viewport: Rect? = null): List<String> =
        RecentFollowerSelector.orderedHandles(nodes.filter { node ->
            node.enabled && node.bounds.bottom > maxOf(0, node.bounds.top) &&
                node.bounds.right > maxOf(0, node.bounds.left) &&
                (viewport == null || (node.bounds.bottom > viewport.top && node.bounds.top < viewport.bottom &&
                    node.bounds.right > viewport.left && node.bounds.left < viewport.right)) &&
                listOfNotNull(node.text, node.contentDescription).none {
                    VerifiedFollowPolicy.matchesAction(it, XUiVocabulary.followActions +
                        XUiVocabulary.followingActions + XUiVocabulary.requestedActions)
                }
        }, visited + own)

    fun choose(visible: Collection<String>, previous: Collection<String>, own: String, visited: Set<String>,
               random: Random = Random.Default): String? =
        VerifiedFollowPolicy.nextSource(visible, own, visited, random)
            ?: VerifiedFollowPolicy.nextSource(previous, own, visited, random)
}
