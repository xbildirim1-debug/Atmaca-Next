package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class FeedRowEvidenceTest {
    private fun n(t: String?, y: Int, x: Int = 20, desc: String? = null, id: String? = null) = NodeSnapshot(
        t, desc, id, "TextView", true, true, Rect().apply { left=x; top=y; right=x+200; bottom=y+30 })
    @Test fun splitAuthorAndTimeWithoutActionsAreReadable() {
        val nodes = listOf(n("@pusholder",100), n("4 sa",100,240), n("Vanlı Kara Haydar olarak bilinen kişi",140))
        val row = FeedRowEvidence.rows(nodes).single()
        assertEquals("pusholder", row.author); assertEquals(240L,row.age); assertEquals(2,row.bodyIndex)
    }
    @Test fun combinedHeaderWithoutContainerIsReadable() {
        val row = FeedRowEvidence.rows(listOf(n("Pusholder @pusholder · 2 sa",100),n("Haberin açıklama metni",140))).single()
        assertEquals(120L,row.age); assertEquals(1,row.bodyIndex)
    }
    @Test fun descriptionOnlyBodySupported() {
        val row = FeedRowEvidence.rows(listOf(n("@someone · 3 h",100),n(null,140,desc="Actual reply body"))).single()
        assertEquals(1,row.bodyIndex)
    }
    @Test fun bodyMentionIsNotReplyAuthor() {
        assertTrue(FeedRowEvidence.rows(listOf(n("Merhaba @someone",100),n("2 sa",100,240),n("Merhaba dünya",140))).isEmpty())
    }
    @Test fun timeFromAnotherRowCannotBeBorrowed() {
        assertTrue(FeedRowEvidence.rows(listOf(n("@someone",100),n("2 sa",160,240))).isEmpty())
    }
    @Test fun nextPostTextIsNeverBorrowed() {
        val rows = FeedRowEvidence.rows(listOf(n("@first · 2 sa",100),n("@second · 3 sa",150),n("İkinci gönderi açıklaması",190)))
        assertNull(rows[0].bodyIndex); assertEquals(2,rows[1].bodyIndex)
    }
    @Test fun recommendationTextIsNeverPostBody() {
        val rows = FeedRowEvidence.rows(listOf(n("@first · 2 sa",100),n("Kimi takip etmeli",140),n("BaBaLa TV",180),n("Takip et",220)))
        assertNull(rows.single().bodyIndex)
    }
    @Test fun mediaDoesNotBecomeTextOrPermitFollowingText() {
        val row = FeedRowEvidence.rows(listOf(n("@first · 2 sa",100),n("Video oynat",140,id="video_player"),n("Dışarıdaki açıklama",400))).single()
        assertNull(row.bodyIndex)
    }
    @Test fun identityIgnoresAgeAndActionCounts() {
        fun key(age: String,count: String) = FeedRowEvidence.rows(listOf(n("@first · $age",100),n("Aynı gönderinin metni",140),n(count,200))).single().key
        assertEquals(key("2 sa","1 beğeni"),key("3 sa","100 beğeni"))
    }
    @Test fun hiddenHeaderCannotProduceRow() {
        assertTrue(FeedRowEvidence.rows(listOf(n("@first · 2 sa",100).copy(visible=false),n("Haber metni",140))).isEmpty())
    }
    @Test fun profileRecommendationsDoNotBecomeFollowingList() {
        val nodes = listOf(n("Gönderiler",50),n("87.996 gönderileri",10),n("Kimi takip etmeli",200),n("@babala",250),n("Takip ediliyor",300))
        assertEquals(XScreen.PROFILE,ScreenDetector.detect(nodes))
    }
    @Test fun repliesWithFollowingLabelsRemainTweetDetail() {
        val nodes = listOf(n("Yanıtını gönder",800),n("Alıntıları görüntüle",100),n("@one",200),n("@two",300),n("Takip ediliyor",250))
        assertEquals(XScreen.TWEET_DETAIL,ScreenDetector.detect(nodes))
    }
    @Test fun pinnedCardSkipped() {
        assertTrue(FeedRowEvidence.rows(listOf(n("Sabitlendi",70),n("@first · 4 sa",100),n("Haber açıklama metni",140))).isEmpty())
    }
    @Test fun scaledLayoutHasSameResult() {
        for (scale in listOf(1,2,3)) {
            val nodes = listOf(n("@first",100*scale),n("2 sa",100*scale,240),n("Haber açıklama metni",140*scale))
            assertEquals(2,FeedRowEvidence.rows(nodes).single().bodyIndex)
        }
    }
}
