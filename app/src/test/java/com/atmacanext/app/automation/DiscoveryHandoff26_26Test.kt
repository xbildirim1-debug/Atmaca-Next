package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class DiscoveryHandoff26_26Test {
    private fun n(text: String, x: Int, y: Int, w: Int = 200, h: Int = 40) = NodeSnapshot(
        text, null, null, "TextView", true, true,
        Rect().apply { left = x; top = y; right = x + w; bottom = y + h })

    private fun detail() = listOf(
        n("Gönderi", 105, 90), n("Zeyn-app", 105, 190), n("@zeynepdemir92", 105, 235),
        n("Takip et", 500, 207, 140, 45), n("Gönderi metni", 20, 290, 620, 70),
        n("Yanıtını gönder", 100, 1350).copy(editable = true))

    private fun profile(handle: String = "second_target") = listOf(
        n("@$handle", 20, 180), n("Gönderiler", 20, 540), n("Yanıtlar", 230, 540), n("Medya", 440, 540))

    @Test fun duplicateFullPageTitleCannotHideTheRealAuthor() {
        val nodes = listOf(n("Gönderi", 0, 0, 691, 1500)) + detail()
        assertEquals("zeynepdemir92", CommentDetailEvidence.header(nodes)?.handle)
        assertEquals(4, CommentDetailEvidence.actionIndex(nodes, "zeynepdemir92", VerifiedFollowPolicy.plainFollowLabels))
    }

    @Test fun headingRoleSuffixStillAllowsExactAuthorFollow() {
        for (label in listOf("Gönderi, Başlık", "Post, heading", "Tweet")) {
            val nodes = detail().mapIndexed { i, node -> if (i == 0) node.copy(text = label) else node }
            assertEquals(XScreen.TWEET_DETAIL, ScreenDetector.detect(nodes))
            assertEquals(3, CommentDetailEvidence.actionIndex(nodes, "zeynepdemir92", VerifiedFollowPolicy.plainFollowLabels))
        }
    }

    @Test fun mergedLargeFollowContainerDoesNotBecomeTapTarget() {
        val nodes = detail() + n("Takip et", 0, 140, 691, 1200)
        assertEquals(3, CommentDetailEvidence.actionIndex(nodes, "zeynepdemir92", VerifiedFollowPolicy.plainFollowLabels))
    }

    @Test fun nearbyReplyFollowIsNotTheOpenedAuthorsFollow() {
        val nodes = detail().filterIndexed { i, _ -> i != 3 } +
            n("Other @other · 2 sa", 100, 300, 390) + n("Takip et", 500, 305, 140)
        assertNull(CommentDetailEvidence.actionIndex(nodes, "zeynepdemir92", VerifiedFollowPolicy.plainFollowLabels))
    }

    @Test fun wrongAuthorIsRejectedEvenWithVisibleFollow() {
        assertNull(CommentDetailEvidence.actionIndex(detail(), "someone_else", VerifiedFollowPolicy.plainFollowLabels))
    }

    @Test fun loadingProfileDoesNotRequireAVisiblePostOrCount() {
        assertEquals(XScreen.PROFILE, ScreenDetector.detect(profile()))
        assertTrue(DiscoveryProfileEvidence.matches(profile(), "second_target", "second_target"))
    }

    @Test fun hiddenPreviousAccountAndComposerCannotOverrideSecondProfile() {
        val nodes = listOf(n("@first_account", 20, 60).copy(visible = false),
            n("Neler oluyor", 20, 100).copy(editable = true, viewId = "tweet_box", visible = false)) + profile()
        assertEquals(XScreen.PROFILE, ScreenDetector.detect(nodes))
        assertTrue(DiscoveryProfileEvidence.matches(nodes, null, "second_target"))
    }

    @Test fun differentProfileCannotBeConfirmedByItsRepostOfTarget() {
        val nodes = profile("wrong_profile") + n("Target @second_target · 2 sa", 100, 640, 460) + n("Metin", 20, 700)
        assertFalse(DiscoveryProfileEvidence.matches(nodes, "wrong_profile", "second_target"))
    }

    @Test fun searchQueryOrBioMentionAloneIsNeverTargetConfirmation() {
        assertFalse(DiscoveryProfileEvidence.matches(listOf(n("@second_target", 20, 100).copy(editable = true)), "second_target", "second_target"))
        assertFalse(DiscoveryProfileEvidence.matches(listOf(n("Arkadaşım @second_target", 20, 100)), "second_target", "second_target"))
    }

    @Test fun searchResultTapWaitsThenRetriesOnlyTheStillOpenQuery() {
        assertEquals(SearchRecovery.WAIT, DiscoverySearchRecovery.decide(2499, 1, true))
        assertEquals(SearchRecovery.RETRY_RESULT, DiscoverySearchRecovery.decide(2500, 1, true))
        assertEquals(SearchRecovery.RETRY_RESULT, DiscoverySearchRecovery.decide(2500, 2, true))
        assertEquals(SearchRecovery.PAUSE, DiscoverySearchRecovery.decide(2500, 3, true))
        assertEquals(SearchRecovery.WAIT, DiscoverySearchRecovery.decide(5000, 1, false))
    }

    @Test fun secondAccountStartsWithItsOwnTargetAndFreshNavigation() {
        val controller = AutomationController
        fun set(name: String, value: Any) {
            controller.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(controller, value)
        }
        fun get(name: String): Any? = controller.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(controller)
        val first = com.atmacanext.app.domain.model.ScheduledTask("first", "account1", "first_account",
            type = com.atmacanext.app.domain.model.TaskType.RETWEETER_FOLLOW, progress = 1, limit = 1)
        controller.stop()
        try {
            assertTrue(controller.start(first, listOf("first_target")))
            val oldSession = controller.state.value.sessionId
            set("discoverySearchStep", 3)
            set("discoverySearchSubmitted", true)
            set("discoveryResultAttempts", 3)
            set("listScrolls", 90)
            set("engagerHandle", "old_commenter")
            set("nextActionNotBefore", Long.MAX_VALUE)
            assertTrue(controller.start(first.copy(id = "second", accountId = "account2", username = "second_account", progress = 0), listOf("second_target")))
            assertNotEquals(oldSession, controller.state.value.sessionId)
            assertEquals("second_account", controller.state.value.username)
            assertEquals(0, controller.state.value.verifiedCount)
            assertFalse(controller.state.value.accountVerified)
            assertEquals(listOf("second_target"), get("discoveryTargets"))
            assertEquals(0, get("discoverySearchStep"))
            assertEquals(false, get("discoverySearchSubmitted"))
            assertEquals(0, get("discoveryResultAttempts"))
            assertEquals(0, get("listScrolls"))
            assertEquals(0L, get("nextActionNotBefore"))
            assertNull(get("engagerHandle"))
        } finally { controller.stop() }
    }
}
