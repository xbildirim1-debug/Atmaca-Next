package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class RelationshipListRegressionTest {
    private fun n(text: String, selected: Boolean = false) = NodeSnapshot(
        text, null, null, "TextView", false, true, Rect(), selected = selected)

    @Test fun selectedFollowingWinsOverUnselectedVerifiedFollowers() {
        val nodes = listOf(n("Verified Followers"), n("Followers"), n("Following", true), n("@alice"), n("@bob"))
        assertEquals(XScreen.FOLLOWING_LIST, ScreenDetector.detect(nodes))
    }
    @Test fun decoratedTurkishTabDoesNotBlockLoadedList() {
        val nodes = listOf(n("Takip ediliyor, sekme 3/4, seçili", true), n("@alice"), n("@bob"))
        assertTrue(ListLoadVerifier.isLoaded(nodes, XScreen.FOLLOWING_LIST))
        assertEquals(XScreen.FOLLOWING_LIST, ScreenDetector.detect(nodes))
    }
    @Test fun selectedContainerCanUseChildTabLabel() {
        assertEquals(RelationshipTabInspector.FOLLOWING, RelationshipTabInspector.classifySelectedLabels(listOf("Seçili", "Takip ediliyor")))
    }
    @Test fun wholePagerAndSelectedAccountRowsAreNotTabProof() {
        assertEquals(RelationshipTabInspector.NONE, RelationshipTabInspector.classifySelectedLabels(listOf("Following", "Followers")))
        assertEquals(RelationshipTabInspector.NONE, RelationshipTabInspector.classifySelectedLabels(listOf("@alice", "Following")))
    }
    @Test fun profileCountersAreNotLoadedRelationshipLists() {
        val nodes = listOf(n("@alice"), n("Edit profile"), n("100 Following"), n("200 Followers"))
        assertFalse(ListLoadVerifier.isLoaded(nodes, XScreen.FOLLOWING_LIST))
    }
    @Test fun headersWithoutRowsRemainLoading() {
        assertFalse(ListLoadVerifier.isLoaded(listOf(n("Takip ediliyor, sekme 3/4, seçili")), XScreen.FOLLOWING_LIST))
    }
}
