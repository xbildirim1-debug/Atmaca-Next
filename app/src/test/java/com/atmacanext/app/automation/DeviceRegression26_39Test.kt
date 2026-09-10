package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceRegression26_39Test {
    private fun n(
        text: String?,
        y: Int,
        h: Int = 48,
        editable: Boolean = false,
        selected: Boolean = false,
    ) = NodeSnapshot(
        text,
        null,
        null,
        "TextView",
        true,
        true,
        Rect().apply { left = 20; top = y; right = 900; bottom = y + h },
    ).copy(editable = editable, selected = selected)

    @Test
    fun turkishRepostedTabWithoutCountIsRecognized() {
        assertTrue(EngagementListEvidence.isReposts("Yeniden gönderildi"))
        assertTrue(EngagementListEvidence.isReposts("Yeniden gönderildi, sekme 2/2, seçili"))
    }

    @Test
    fun quoteEntryVariantsOpenTheEngagementSurface() {
        assertTrue(EngagementListEvidence.openLabel("Alıntı"))
        assertTrue(EngagementListEvidence.openLabel("Alıntılar"))
        assertTrue(EngagementListEvidence.openLabel("33 Alıntı"))
        assertTrue(EngagementListEvidence.openLabel("Alıntıları görüntüle"))
    }

    @Test
    fun selectedRepostedTabIsVerifiedBeforeUsersAreProcessed() {
        val nodes = listOf(
            n("Gönderi etkileşimleri", y = 80),
            n("Alıntılar", y = 150),
            n("Yeniden gönderildi, sekme 2/2, seçili", y = 150, selected = true),
            n("@example", y = 300),
            n("Takip et", y = 350),
        )
        assertTrue(EngagementListEvidence.selected(nodes))
    }

    @Test
    fun replySwipeStartsAboveLiveComposerAndKeepsViewportOverlap() {
        val screen = Rect().apply { left = 0; top = 0; right = 1080; bottom = 2400 }
        val nodes = listOf(
            n("Bir yorum", y = 650, h = 120),
            n("Başka bir yorum", y = 1120, h = 160),
            n("Yanıtını gönder", y = 2140, h = 48),
        )
        val path = DiscoveryScrollGesturePolicy.verticalPath(screen, nodes, forward = true)
        assertNotNull(path)
        path!!
        assertTrue(path.startY < 2140f)
        assertTrue(path.startY > path.endY)
        assertTrue(path.startY - path.endY <= 2400f * 0.42f + 1f)
    }

    @Test
    fun profileScanWithoutReplyComposerKeepsNormalLongGesture() {
        val screen = Rect().apply { left = 0; top = 0; right = 1080; bottom = 2400 }
        val path = DiscoveryScrollGesturePolicy.verticalPath(
            screen,
            listOf(n("Gönderi", y = 700, h = 200)),
            forward = true,
        )
        assertNotNull(path)
        assertEquals(2400f * DiscoveryScrollGesturePolicy.FORWARD_START_Y_RATIO, path!!.startY, 0.5f)
        assertEquals(2400f * DiscoveryScrollGesturePolicy.FORWARD_END_Y_RATIO, path.endY, 0.5f)
    }
}
