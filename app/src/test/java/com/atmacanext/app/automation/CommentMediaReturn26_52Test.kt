package com.atmacanext.app.automation

import android.graphics.Rect
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskType
import org.junit.Assert.*
import org.junit.Test

class CommentMediaReturn26_52Test {
    private fun n(text: String? = null, top: Int, width: Int = 900, height: Int = 70,
                  id: String? = null, desc: String? = null, clazz: String = "android.widget.TextView") =
        NodeSnapshot(text, desc, id, clazz, false, true,
            Rect().apply { left = 120; this.top = top; right = left + width; bottom = top + height }, visible = true)

    @Test fun textMentioningVideoPhotoOrGifIsStillText() {
        for (text in listOf("video paylaşımı hakkında bir yorum", "fotoğraf çok güzel", "gif yerine yazıyorum", "Bu image dosyasını konuştuk")) {
            assertFalse(ReplyMediaEvidence.hasMedia(listOf(n(text, 600, height = 240)), 500, 1000))
        }
    }
    @Test fun descriptionOfTextIsNotAnAttachment() {
        assertFalse(ReplyMediaEvidence.hasMedia(listOf(n(top = 600, height = 240, desc = "Video hakkında yorum")), 500, 1000))
    }
    @Test fun unlabelledSmallPhotoAvatarIsNotTweetMedia() {
        val nodes = listOf(n("metin", 600), n(top = 550, width = 110, height = 110, desc = "Fotoğraf", clazz = "android.widget.ImageView"))
        assertFalse(ReplyMediaEvidence.hasMedia(nodes, 500, 1000))
    }
    @Test fun previousImageOverlappingTheCurrentBandDoesNotBelongToIt() {
        val nodes = listOf(n(top = 200, height = 800, id = "tweet_photo", clazz = "android.widget.ImageView"), n("Saye metin yorumu", 850))
        assertFalse(ReplyMediaEvidence.hasMedia(nodes, 800, 1200))
    }
    @Test fun pageSizedMediaAncestorDoesNotHideAllCommenters() {
        val nodes = listOf(n(top = 0, height = 2400, id = "media_container"), n("Ender metin yorumu", 850))
        assertFalse(ReplyMediaEvidence.hasMedia(nodes, 800, 1200))
    }
    @Test fun smallInlineGifAndPhotoButtonsAreNotAttachments() {
        val nodes = listOf(n("son metin yorumu", 850), n(top = 1000, width = 60, height = 60, desc = "GIF"),
            n(top = 1000, width = 60, height = 60, desc = "Fotoğraf", clazz = "android.widget.ImageView"))
        assertFalse(ReplyMediaEvidence.hasMedia(nodes, 800, 1100))
    }
    @Test fun actualPhotoVideoAndGifAttachmentsAreSkipped() {
        for (id in listOf("tweet_photo", "video_player", "gif_view")) {
            assertTrue(ReplyMediaEvidence.hasMedia(listOf(n(top = 600, height = 340, id = id)), 500, 1000))
        }
    }
    @Test fun largeComposeImageWithoutResourceIdIsMedia() {
        assertTrue(ReplyMediaEvidence.hasMedia(listOf(n(top = 600, height = 340, clazz = "android.widget.ImageView")), 500, 1000))
    }
    @Test fun hiddenAndEditableMediaNodesAreIgnored() {
        val image = n(top = 600, height = 340, id = "tweet_photo")
        assertFalse(ReplyMediaEvidence.hasMedia(listOf(image.copy(visible = false)), 500, 1000))
        assertFalse(ReplyMediaEvidence.hasMedia(listOf(image.copy(editable = true)), 500, 1000))
    }
    @Test fun videoTextCommentersAllRemainDistinctFeedCandidates() {
        val handles = listOf("AVG1689282", "siyahkutup_", "envytr355", "Mustafa849037", "sessizistila58", "Egeliyikbiz")
        val nodes = handles.flatMapIndexed { i, handle -> listOf(n("İsim @$handle · 2 sa", 150 + i * 300),
            n("Bu kullanıcının metin yorumu $i", 230 + i * 300, id = "tweet_text")) }
        assertEquals(handles.map(String::lowercase), FeedRowEvidence.rows(nodes).map { it.author })
    }
    @Test fun reachingTargetAlwaysStopsBackEvenWithOldScreenOrExpiredBudget() {
        for (screen in XScreen.entries) assertEquals(DiscoveryReturnPolicy.Decision.SCAN,
            DiscoveryReturnPolicy.decide(true, screen, 3, 9000, 0))
    }
    @Test fun retweeterStackReturnsListThenPostThenStopsAtProfile() {
        assertEquals(DiscoveryReturnPolicy.Decision.BACK, DiscoveryReturnPolicy.decide(false, XScreen.ENGAGEMENT_LIST, 0, 0, 0))
        assertEquals(DiscoveryReturnPolicy.Decision.WAIT, DiscoveryReturnPolicy.decide(false, XScreen.TWEET_DETAIL, 1, 700, 700))
        assertEquals(DiscoveryReturnPolicy.Decision.BACK, DiscoveryReturnPolicy.decide(false, XScreen.TWEET_DETAIL, 1, 1200, 1200))
        assertEquals(DiscoveryReturnPolicy.Decision.SCAN, DiscoveryReturnPolicy.decide(true, XScreen.PROFILE, 2, 2000, 800))
    }
    @Test fun unconfirmedProfileHomeSearchAndUnknownNeverReceiveBlindBack() {
        for (screen in listOf(XScreen.PROFILE, XScreen.HOME, XScreen.UNKNOWN, XScreen.COMPOSER))
            assertEquals(DiscoveryReturnPolicy.Decision.WAIT, DiscoveryReturnPolicy.decide(false, screen, 0, 1000, 1000))
    }
    @Test fun unresolvedReturnPausesInsteadOfSearchingTargetAgain() {
        assertEquals(DiscoveryReturnPolicy.Decision.PAUSE, DiscoveryReturnPolicy.decide(false, XScreen.UNKNOWN, 0, 8000, 8000))
        assertEquals(DiscoveryReturnPolicy.Decision.WAIT, DiscoveryReturnPolicy.decide(false, XScreen.TWEET_DETAIL, 3, 4000, 500))
        assertEquals(DiscoveryReturnPolicy.Decision.PAUSE, DiscoveryReturnPolicy.decide(false, XScreen.TWEET_DETAIL, 3, 4500, 1000))
    }
    @Test fun actualTargetProfileBlocksBackWithStaleTweetScreenClassification() {
        val nodes = listOf(n("Geri", 40), n("Gönderiler", 150), n("Pusholder @pusholder · 2 sa", 300), n("Hedef gönderi", 390))
        assertTrue(DiscoveryProfileEvidence.matches(nodes, null, "pusholder"))
        assertNull(NavigationSurfaceEvidence.backIndex(nodes, XFlowStage.RETURN_DISCOVERY_TARGET, XScreen.TWEET_DETAIL))
    }
    @Test fun twoHoursRuleForOriginalTargetPostIsUnchanged() {
        assertFalse(XTweetInspector.eligibleAge(119)); assertTrue(XTweetInspector.eligibleAge(120))
    }
    @Test fun nextAccountClearsCommentSkipRetryAndReturnNavigationState() {
        val controller = AutomationController
        fun field(name: String) = controller.javaClass.getDeclaredField(name).apply { isAccessible = true }
        try {
            controller.stop()
            @Suppress("UNCHECKED_CAST")
            val skipped = field("skippedReplyKeys").get(controller) as MutableSet<String>
            @Suppress("UNCHECKED_CAST")
            val attempts = field("replyOpenAttempts").get(controller) as MutableMap<String, Int>
            for (type in listOf(TaskType.COMMENTER_FOLLOW, TaskType.RETWEETER_FOLLOW)) {
                skipped.add("old-media-reply"); attempts["old-comment"] = 2
                field("discoveryReturnLastBackAt").setLong(controller, 1234L)
                assertTrue(controller.start(ScheduledTask("new-$type", "new-account", "account", type = type), listOf("pusholder")))
                assertTrue(skipped.isEmpty()); assertTrue(attempts.isEmpty())
                assertEquals(0L, field("discoveryReturnLastBackAt").getLong(controller))
                controller.stop()
            }
        } finally { controller.stop() }
    }
}
