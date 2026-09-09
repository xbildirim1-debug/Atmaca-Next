package com.atmacanext.app.automation

import android.graphics.Rect
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class DiscoveryVideo27Test {
    private fun n(text: String?, x: Int = 20, y: Int = 100, w: Int = 300, h: Int = 40) = NodeSnapshot(
        text, null, null, "TextView", true, true,
        Rect().apply { left=x; top=y; right=x+w; bottom=y+h })

    @Test fun profileFollowCanBeBelowCountersButNotInsidePosts() {
        val nodes = listOf(n("@someone", y=100), n("55 Takip ediliyor", y=300), n("1,3M Takipçiler", x=360, y=300),
            n("Takip et", x=400, y=480), n("Gönderiler", y=570), n("Takip et", x=400, y=800))
        val boundary=NavigationSurfaceEvidence.profileActionEnd(nodes,300)
        assertEquals(570,boundary)
        assertTrue(nodes[3].bounds.bottom<=boundary)
        assertFalse(nodes[5].bounds.bottom<=boundary)
    }
    @Test fun selectedRepostTabSurvivesMissingEngagementTitleAndFollowingRows() {
        val nodes=listOf(n("307 tarafından yeniden gönderildi",y=80).copy(selected=true),
            n("@a",y=200),n("Takip ediliyor",x=400,y=200),n("@b",y=300),n("Takip ediliyor",x=400,y=300))
        assertEquals(XScreen.ENGAGEMENT_LIST,ScreenDetector.detect(nodes))
    }
    @Test fun visibleButUnselectedRepostTabIsNotEnough() {
        val nodes=listOf(n("307 tarafından yeniden gönderildi",y=80),n("@a",y=200),n("Takip ediliyor",x=400,y=200))
        assertFalse(EngagementListEvidence.selected(nodes))
    }
    @Test fun screenshotSparseLoadingSurfaceIsRecoverable() {
        val nodes=listOf(n("Geri",y=50),n(null,y=700).copy(className="android.widget.ProgressBar",clickable=false))
        assertTrue(NavigationSurfaceEvidence.loading(nodes))
    }
    @Test fun ordinaryEmptyComposerProfileAndPermissionAreNotLoading() {
        for (body in listOf(n("@someone"),n("İzin ver"),n("Gönderi"),n("Metin").copy(editable=true)))
            assertFalse(NavigationSurfaceEvidence.loading(listOf(n("Geri",y=50),body)))
        assertFalse(NavigationSurfaceEvidence.loading(emptyList()))
    }
    @Test fun resourceBackWorksWithoutTextButHiddenBackDoesNot() {
        val back=n(null,y=50).copy(viewId="com.twitter.android:id/toolbar_back")
        assertTrue(NavigationSurfaceEvidence.loading(listOf(back)))
        assertFalse(NavigationSurfaceEvidence.loading(listOf(back.copy(visible=false))))
    }
    @Test fun exactDetailTimeEnforces119And120MinuteBoundary() {
        val now=LocalDateTime.of(2026,9,9,12,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(119L,TweetAgeEvidence.parseDetailTime("10:01 · 09 Eyl 26 · 369 Görüntülenme",now))
        assertEquals(120L,TweetAgeEvidence.parseDetailTime("10:00 · 09 Eyl 26 · 369 Görüntülenme",now))
        assertFalse(XTweetInspector.eligibleAge(TweetAgeEvidence.parseDetailTime("11:00 · 09 Eyl 26 · 369 Görüntülenme",now)))
        assertNull(TweetAgeEvidence.parseDetailTime("Metinde 10:00 · 09 Eyl 26 var",now))
    }
    @Test fun conflictingSameRowAgesUseYoungerEvidence() {
        val nodes=listOf(n("Source @source · 2 sa",y=200),n("Source @source · 1 sa",y=200),n("Haber metni",y=260))
        assertEquals(60L,FeedRowEvidence.rows(nodes).single().age)
        assertFalse(XTweetInspector.eligibleAge(FeedRowEvidence.rows(nodes).single().age))
    }
}
