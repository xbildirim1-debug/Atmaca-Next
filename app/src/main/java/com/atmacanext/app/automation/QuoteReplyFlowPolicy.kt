package com.atmacanext.app.automation

import com.atmacanext.app.domain.model.TaskType

/** Decisions use a fresh visible frame; a dispatched write is never proof of its text. */
internal object QuoteReplyFlowPolicy {
    enum class Decision { OPEN, FILL, WRITE, PREPARE_SUBMIT, SUBMIT, WAIT, RESTART }

    fun detailReady(nodes: List<NodeSnapshot>, screen: XScreen, expected: DiscoveryTweetOpenRecovery.Attempt?): Boolean =
        expected != null && screen in setOf(XScreen.TWEET_DETAIL, XScreen.UNKNOWN, XScreen.COMPOSER) &&
            (screen == XScreen.TWEET_DETAIL || ReplyComposerEvidence.postHeader(nodes) != null) &&
            ReplyComposerEvidence.matchesPost(nodes, expected)

    fun openedIsProcessed(type: TaskType?): Boolean = type != TaskType.COMMENT_QUOTE_TARGETS

    fun sameThread(nodes: List<NodeSnapshot>, screen: XScreen, expected: DiscoveryTweetOpenRecovery.Attempt?, pendingKey: String?): Boolean =
        expected != null && expected.key == pendingKey &&
            (detailReady(nodes, screen, expected) ||
                (screen == XScreen.TWEET_DETAIL && CommentDetailEvidence.header(nodes) == null))

    fun newReplyVisible(nodes: List<NodeSnapshot>, screen: XScreen, expected: DiscoveryTweetOpenRecovery.Attempt?,
        pendingKey: String?, username: String, content: String, before: Set<String>): Boolean =
        sameThread(nodes, screen, expected, pendingKey) &&
            (ReplyComposerEvidence.ownReplyKeys(nodes, username, content) - before).isNotEmpty()

    fun decide(stage: XFlowStage, ready: Boolean, textVerified: Boolean, openAvailable: Boolean,
        submitAvailable: Boolean, attempts: Int, elapsedMs: Long, targetConflict: Boolean = false): Decision {
        if (targetConflict) return if (elapsedMs >= 10_000L) Decision.RESTART else Decision.WAIT
        return when (stage) {
            XFlowStage.OPEN_COMPOSER -> when {
                ready -> Decision.FILL
                elapsedMs >= 10_000L -> Decision.RESTART
                openAvailable && attempts < 3 && (attempts == 0 || elapsedMs >= attempts * 1_000L) -> Decision.OPEN
                else -> Decision.WAIT
            }
            XFlowStage.FILL_COMPOSER -> when {
                ready && textVerified -> Decision.PREPARE_SUBMIT
                elapsedMs >= 10_000L -> Decision.RESTART
                ready && attempts < 3 -> Decision.WRITE
                else -> Decision.WAIT
            }
            XFlowStage.SUBMIT_COMPOSER -> when {
                ready && textVerified && submitAvailable -> Decision.SUBMIT
                elapsedMs >= 10_000L -> Decision.RESTART
                else -> Decision.WAIT
            }
            else -> Decision.WAIT
        }
    }
}
