package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class VerifiedTurkishTabTest {
    private fun node(label: String, selected: Boolean = false) = NodeSnapshot(
        label, null, null, "TextView", false, true,
        Rect().apply { left = 0; top = 100; right = 240; bottom = 130 }, selected = selected,
    )

    @Test fun deviceTurkishTitleIsSharedByDetectionAndTabClick() {
        val title = "Onaylanmış takipçiler"
        assertTrue(XUiVocabulary.normalize(title) in XUiVocabulary.fullVerifiedFollowersHeaders)
        assertEquals(RelationshipTabInspector.VERIFIED,
            RelationshipTabInspector.classifySelectedLabels(listOf(title, "$title, seçili")))
        assertEquals(XScreen.VERIFIED_FOLLOWERS_LIST, ScreenDetector.detect(listOf(
            node(title, true), node("Tanıdığın takipçiler"), node("@candidate"), node("Takip et"),
        )))
    }

    @Test fun selectedTurkishTabWorksBeforeRowsLoad() {
        assertEquals(XScreen.VERIFIED_FOLLOWERS_LIST,
            ScreenDetector.detect(listOf(node("Onaylanmış takipçiler", true))))
    }

    @Test fun neighboringTurkishTitleDoesNotAuthorizeFollowing() {
        assertEquals(XScreen.UNKNOWN, ScreenDetector.detect(listOf(
            node("Onaylanmış takipçiler"), node("Tanıdığın takipçiler", true),
            node("@candidate"), node("Takip et"),
        )))
    }

    @Test fun verifiedRowsStillExcludeFollowBackAndFollowing() {
        assertTrue(VerifiedFollowPolicy.isPlainFollow(listOf("Takip et")))
        for (label in listOf("Geri Takip Et", "Takip ediliyor", "Follow back", "Following"))
            assertFalse(VerifiedFollowPolicy.isPlainFollow(listOf(label)))
    }

    @Test fun nextSourceIsRandomOnlyWithinLatestListAndNeverVisitedOrOwn() {
        val latest = listOf("own", "visited", "new_one", "new_two")
        val picked = (0..30).map { VerifiedFollowPolicy.nextSource(latest, "own", setOf("visited"), Random(it)) }.toSet()
        assertEquals(setOf("new_one", "new_two"), picked)
        assertNull(VerifiedFollowPolicy.nextSource(listOf("own", "visited"), "own", setOf("visited")))
    }
}
