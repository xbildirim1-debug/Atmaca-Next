package com.buse.app.automation

import android.graphics.Rect
import com.buse.app.domain.engine.QueueResultPolicy
import com.buse.app.domain.engine.TaskQueuePlanner
import com.buse.app.domain.model.*
import com.buse.app.data.repository.TaskProgressWritePolicy
import org.junit.Assert.*
import org.junit.Test

class BatchCommentReturn26_53Test {
    private fun n(text: String?, top: Int, left: Int = 100, width: Int = 380, height: Int = 45,
                  enabled: Boolean = true, clickable: Boolean = false, desc: String? = null,
                  id: String? = null, clazz: String = "android.widget.TextView") = NodeSnapshot(
        text, desc, id, clazz, clickable, enabled,
        Rect().apply { this.left = left; this.top = top; right = left + width; bottom = top + height })

    private fun detail(action: String? = "Mesaj gönder", identity: Boolean = true) = buildList {
        add(n("Gönderi", 80, width = 780, height = 50))
        if (identity) add(n("Sessiz İstila @sessizistila58", 180, width = 420))
        if (action != null) add(n(action, 180, left = 700, width = 300, clickable = true, clazz = "android.widget.Button"))
        add(n("Altın 10 binciler neredela sesleri kesildi?", 255, width = 850))
        add(n("11:24 · 05 Eki 26 · 6,8K Görüntülenme", 320, width = 850))
        add(n("Alakalı", 390))
        add(n("zeynepimm @zeynepsek161616 · 1 sa", 445, width = 560))
        add(n("Takip et", 450, left = 700, width = 300, clickable = true))
        add(n("Martta 10 bin olacak diyenler", 510, width = 850))
    }

    @Test fun messageButtonAndNestedFollowCannotFollowTheNestedAuthor() {
        val nodes = detail()
        assertEquals("sessizistila58", CommentDetailEvidence.header(nodes)?.handle)
        assertNull(CommentDetailEvidence.relationshipActionIndex(nodes, "sessizistila58", VerifiedFollowPolicy.plainFollowLabels))
        assertNull(CommentDetailEvidence.headerActionIndex(nodes, VerifiedFollowPolicy.plainFollowLabels))
    }
    @Test fun omittedTopIdentityDoesNotBorrowCloseNestedFollow() {
        assertNull(CommentDetailEvidence.relationshipActionIndex(detail(identity = false), "sessizistila58", VerifiedFollowPolicy.plainFollowLabels))
    }
    @Test fun missingButtonStillCannotUseNestedFollow() {
        assertNull(CommentDetailEvidence.relationshipActionIndex(detail(action = null), "sessizistila58", VerifiedFollowPolicy.plainFollowLabels))
    }
    @Test fun otherTopControlsRemainIneligibleForFollow() {
        for (label in listOf("Mesaj gönder", "Send message", "Abone ol", "Subscribe", "Daha fazla"))
            assertNull(label, CommentDetailEvidence.relationshipActionIndex(detail(label), "sessizistila58", VerifiedFollowPolicy.plainFollowLabels))
    }
    @Test fun correctTopButtonStillWorksWithNestedRepliesPresent() {
        assertEquals(2, CommentDetailEvidence.relationshipActionIndex(detail("Takip et"), "sessizistila58", VerifiedFollowPolicy.plainFollowLabels))
    }
    @Test fun mismatchedTopAuthorCannotBeFollowedViaFallback() {
        assertNull(CommentDetailEvidence.relationshipActionIndex(detail("Takip et"), "someoneelse", VerifiedFollowPolicy.plainFollowLabels))
    }
    @Test fun splitTimedNestedHeaderAlsoStopsTopRightFallback() {
        val nodes = detail(identity = false).filter { it.text != "zeynepimm @zeynepsek161616 · 1 sa" && it.text != "Alakalı" } +
            n("@nested", 440, width = 300) + n("1 sa", 440, left = 450, width = 80)
        assertNull(CommentDetailEvidence.header(nodes))
        assertNull(CommentDetailEvidence.headerActionIndex(nodes, VerifiedFollowPolicy.plainFollowLabels))
    }
    @Test fun topFollowingAndRequestedReadEvenWhenDisabled() {
        for (label in listOf("Takip ediliyor", "Beklemede", "Requested")) {
            val nodes = detail(label).mapIndexed { i, node -> if (i == 2) node.copy(enabled = false) else node }
            assertNotNull(CommentDetailEvidence.relationshipActionIndex(nodes, "sessizistila58", XUiVocabulary.followingActions + XUiVocabulary.requestedActions))
            assertNull(CommentDetailEvidence.relationshipActionIndex(nodes, "sessizistila58", VerifiedFollowPolicy.plainFollowLabels))
        }
    }
    @Test fun nestedFollowingCannotVerifyTheOpenedCommenter() {
        val nodes = detail().map { if (it.text == "Takip et") it.copy(text = "Takip ediliyor") else it }
        assertNull(CommentDetailEvidence.relationshipActionIndex(nodes, "sessizistila58", XUiVocabulary.followingActions + XUiVocabulary.requestedActions))
    }
    @Test fun disabledRequestedIsReadableAndNotClickable() {
        val node = n("Beklemede", 400, left = 700, enabled = false, clazz = "android.widget.Button")
        assertTrue(RelationshipActionEvidence.matches(node, observation = true))
        assertFalse(RelationshipActionEvidence.matches(node))
        assertEquals(VerifiedFollowOutcome.SUCCESS, VerifiedFollowPolicy.outcome(false, false, false, 0, 200, requestedNow = true))
    }
    @Test fun requestedPlainTextStatusStillReadsInConfirmedRow() {
        assertTrue(RelationshipActionEvidence.matches(n("Requested", 400, left = 700), observation = true))
        assertFalse(RelationshipActionEvidence.matches(n("Requested", 400, left = 700)))
    }
    @Test fun requestedDescriptionIsRecognizedWhenTextIsEmpty() {
        assertTrue(RelationshipActionEvidence.matches(n(null, 400, left = 700, desc = "Requested @private", clazz = "Button"), observation = true))
    }
    @Test fun requestedFromHiddenRowsAndTabsIsNotAResult() {
        assertFalse(RelationshipActionEvidence.matches(n("Beklemede", 400).copy(visible = false), observation = true))
        assertFalse(RelationshipActionEvidence.matches(n("Following", 400, id = "relationship_tab", clazz = "Button"), observation = true))
        assertFalse(RelationshipActionEvidence.matches(n("Takip ediliyor, sekme 2/4", 400, clazz = "Button"), observation = true))
    }
    @Test fun disabledFollowCannotAuthorizeANewActionOrProveSuccess() {
        val node = n("Takip et", 400, enabled = false, clazz = "Button")
        assertFalse(RelationshipActionEvidence.matches(node))
        assertFalse(RelationshipActionEvidence.matches(node, observation = true))
    }
    @Test fun requestAcknowledgementBreaksRevertStreakAndNeverWaitsForApproval() {
        val outcome = VerifiedFollowPolicy.outcome(false, false, false, 0, 300, requestedNow = true)
        assertEquals(VerifiedFollowOutcome.SUCCESS, outcome)
        assertEquals(0, VerifiedFollowPolicy.nextStreak(2, outcome))
        assertFalse(VerifiedFollowPolicy.isPlainFollow(listOf("Beklemede")))
    }
    @Test fun previousPostListsAndComposerUseBackForEveryTaskHandoff() {
        for (screen in listOf(XScreen.TWEET_DETAIL, XScreen.PROFILE, XScreen.ENGAGEMENT_LIST,
            XScreen.FOLLOWERS_LIST, XScreen.FOLLOWING_LIST, XScreen.VERIFIED_FOLLOWERS_LIST, XScreen.COMPOSER))
            assertEquals(AccountVerificationReturnPolicy.Decision.BACK,
                AccountVerificationReturnPolicy.decide(screen, true, 0, 0, Long.MAX_VALUE))
    }
    @Test fun accountReturnNeverBacksFromHomeDrawerOrAccountSheet() {
        for (screen in listOf(XScreen.HOME, XScreen.ACCOUNT_DRAWER, XScreen.ACCOUNT_SWITCHER, XScreen.DIALOG))
            assertEquals(AccountVerificationReturnPolicy.Decision.WAIT,
                AccountVerificationReturnPolicy.decide(screen, false, 0, 0, Long.MAX_VALUE))
    }
    @Test fun unknownSurfaceNeedsVisibleBackOrBoundedRecovery() {
        assertEquals(AccountVerificationReturnPolicy.Decision.WAIT, AccountVerificationReturnPolicy.decide(XScreen.UNKNOWN, false, 0, 500, Long.MAX_VALUE))
        assertEquals(AccountVerificationReturnPolicy.Decision.BACK, AccountVerificationReturnPolicy.decide(XScreen.UNKNOWN, true, 0, 500, Long.MAX_VALUE))
        assertEquals(AccountVerificationReturnPolicy.Decision.RECOVER, AccountVerificationReturnPolicy.decide(XScreen.UNKNOWN, false, 0, 12_000, Long.MAX_VALUE))
    }
    @Test fun accountReturnWaitsForBackAndBoundsRepeatedAttempts() {
        assertEquals(AccountVerificationReturnPolicy.Decision.WAIT, AccountVerificationReturnPolicy.decide(XScreen.TWEET_DETAIL, true, 1, 500, 500))
        assertEquals(AccountVerificationReturnPolicy.Decision.BACK, AccountVerificationReturnPolicy.decide(XScreen.TWEET_DETAIL, true, 1, 1100, 1100))
        assertEquals(AccountVerificationReturnPolicy.Decision.RECOVER, AccountVerificationReturnPolicy.decide(XScreen.TWEET_DETAIL, true, 6, 7000, 1100))
    }
    @Test fun missingFollowReturnsToParentThenStopsBacking() {
        assertEquals(CommenterReturnPolicy.Decision.BACK, CommenterReturnPolicy.decide(false, false, true, 0, 0, Long.MAX_VALUE))
        assertEquals(CommenterReturnPolicy.Decision.WAIT, CommenterReturnPolicy.decide(false, false, true, 1, 500, 500))
        assertEquals(CommenterReturnPolicy.Decision.PARENT, CommenterReturnPolicy.decide(true, false, true, 1, 700, 700))
    }
    @Test fun targetProfileReachedCannotReceiveExtraCommenterBack() {
        assertEquals(CommenterReturnPolicy.Decision.TARGET, CommenterReturnPolicy.decide(false, true, true, 3, 9000, 1000))
    }
    @Test fun uncertainCommentReturnDoesNotEnterAnotherReplyOrBlindlyBack() {
        assertEquals(CommenterReturnPolicy.Decision.WAIT, CommenterReturnPolicy.decide(false, false, false, 0, 2000, Long.MAX_VALUE))
        assertEquals(CommenterReturnPolicy.Decision.PAUSE, CommenterReturnPolicy.decide(false, false, false, 0, 6000, Long.MAX_VALUE))
        assertEquals(CommenterReturnPolicy.Decision.PAUSE, CommenterReturnPolicy.decide(false, false, true, 3, 6000, 1000))
    }
    @Test fun selectedReplyKeyAloneCannotProveParentWhileItsNestedRepliesAreOpen() {
        assertFalse(DiscoveryViewportEvidence.returnedToParent("before", "after", listOf("selected", "other"),
            listOf("selected", "nested"), "nestedauthor", "sessizistila58", "selected", "pusholder"))
        assertFalse(DiscoveryViewportEvidence.returnedToParent("before", "after", listOf("selected", "other"),
            listOf("selected", "nested"), null, "sessizistila58", "selected"))
    }
    @Test fun actualParentAuthorWithLastVisibleCommentCanRestorePosition() {
        assertTrue(DiscoveryViewportEvidence.returnedToParent("before", "rebuilt", listOf("selected"),
            listOf("selected"), "pusholder", "sessizistila58", "selected", "pusholder"))
    }
    @Test fun everySupportedTaskRunsSecondAccountAndOnlyThenCompletesQueue() {
        val types = listOf(TaskType.UNFOLLOW, TaskType.VERIFIED_FOLLOW, TaskType.COMMENTER_FOLLOW,
            TaskType.RETWEETER_FOLLOW, TaskType.FOLLOW, TaskType.LIKE, TaskType.RETWEET, TaskType.BOOKMARK,
            TaskType.COMMENT, TaskType.QUOTE, TaskType.COMMENT_QUOTE_TARGETS, TaskType.TEXT_TWEET, TaskType.IMAGE_TWEET)
        val accounts = (1..2).map { Account("$it", "@account$it", "Hesap", "0", "0", "0", 100, AccountAccent.BLUE) }
        for (type in types) {
            val tasks = (1..2).map { ScheduledTask("$type-$it", "$it", "@account$it", type = type, limit = 1,
                quoteTargets = "pusholder", contentText = "Metin", targetUrl = "https://x.com/pusholder/status/1", mediaUri = "content://image") }
            val queue = TaskQueuePlanner.build(accounts, tasks)
            assertEquals(type.toString(), 2, queue.size)
            val firstDone = queue.mapIndexed { i, row -> if (i == 0) row.copy(status = QueueItemStatus.COMPLETED) else row }
            assertEquals(type.toString(), 1, TaskQueuePlanner.nextRunnableIndex(firstDone, 0))
            assertEquals(QueueStatus.PARTIAL, QueueResultPolicy.finalStatus(firstDone))
            val allDone = firstDone.map { it.copy(status = QueueItemStatus.COMPLETED) }
            assertEquals(-1, TaskQueuePlanner.nextRunnableIndex(allDone, 1))
            assertEquals(QueueStatus.COMPLETED, QueueResultPolicy.finalStatus(allDone))
        }
    }
    @Test fun nextAccountClearsBothReturnStacksForAllTaskTypes() {
        val controller = AutomationController
        fun field(name: String) = controller.javaClass.getDeclaredField(name).apply { isAccessible = true }
        try {
            for (type in TaskType.entries.filter { it !in setOf(TaskType.SYNC, TaskType.PUBLISH, TaskType.TREND, TaskType.COMMUNITY) }) {
                controller.stop()
                field("accountReturnStartedAt").setLong(controller, 1234)
                field("accountReturnLastBackAt").setLong(controller, 1300)
                field("accountReturnAttempts").setInt(controller, 5)
                field("engagerReturnLastBackAt").setLong(controller, 1400)
                field("engagerParentAuthor").set(controller, "oldparent")
                val started = controller.start(ScheduledTask("new-$type", "2", "account2", type = type,
                    contentText = "Metin", quoteTargets = "pusholder", targetUrl = "https://x.com/pusholder/status/1", mediaUri = "content://image"), listOf("pusholder"))
                assertTrue(type.toString(), started)
                assertFalse(controller.state.value.accountVerified)
                assertEquals(0L, field("accountReturnStartedAt").getLong(controller))
                assertEquals(0L, field("accountReturnLastBackAt").getLong(controller))
                assertEquals(0, field("accountReturnAttempts").getInt(controller))
                assertEquals(0L, field("engagerReturnLastBackAt").getLong(controller))
                assertNull(field("engagerParentAuthor").get(controller))
            }
        } finally { controller.stop() }
    }
    @Test fun linkedTasksAcceptValidXHostsInTheSameJvmAndAndroidCodePath() {
        for (url in listOf("https://x.com/user", "http://twitter.com/user/status/123", "https://www.x.com/user/status/123?ref=1",
            "https://www.twitter.com/user/status/123", " https://X.COM/user ")) assertTrue(url, AutomationController.validXUrl(url))
    }
    @Test fun malformedAndForeignLinksCannotStartLinkedTask() {
        for (url in listOf(null, "", "user", "https://example.com/user", "https://x.com.example.com/user",
            "ftp://x.com/user", "https://x.com/a b", "https://")) assertFalse(url, AutomationController.validXUrl(url))
    }
    @Test fun allLinkedTaskTypesStartAndRetainRequestedAccountBeforeVerification() {
        try {
            for (type in listOf(TaskType.FOLLOW, TaskType.LIKE, TaskType.RETWEET, TaskType.BOOKMARK, TaskType.COMMENT, TaskType.QUOTE)) {
                AutomationController.stop()
                assertTrue(type.toString(), AutomationController.start(ScheduledTask("link-$type", "2", "account2", type = type,
                    targetUrl = "https://x.com/pusholder/status/123", contentText = "Metin")))
                assertEquals("account2", AutomationController.state.value.username)
                assertFalse(AutomationController.state.value.accountVerified)
            }
        } finally { AutomationController.stop() }
    }
    @Test fun latePausedSnapshotCannotReopenCompletedAccountTaskAtSameCounter() {
        assertFalse(TaskProgressWritePolicy.accepts(15, true, 15, false))
        assertTrue(TaskProgressWritePolicy.accepts(15, true, 15, true))
    }
    @Test fun lowerSnapshotCannotEraseVerifiedCounterInAnyTaskStatus() {
        assertFalse(TaskProgressWritePolicy.accepts(15, false, 14, false))
        assertFalse(TaskProgressWritePolicy.accepts(15, true, 14, true))
        assertTrue(TaskProgressWritePolicy.accepts(14, false, 15, true))
    }
    @Test fun explicitTaskResetAllowsNewRunAndNormalWaitingSnapshot() {
        assertTrue(TaskProgressWritePolicy.accepts(0, false, 0, false))
        assertTrue(TaskProgressWritePolicy.accepts(5, false, 5, false))
    }
}
