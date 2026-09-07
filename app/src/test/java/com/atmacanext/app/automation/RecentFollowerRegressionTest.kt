package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class RecentFollowerRegressionTest {
    private fun n(text: String, y: Int = 100, selected: Boolean = false, visible: Boolean = true) =
        NodeSnapshot(text, null, null, "TextView", false, true,
            Rect().apply { left = 0; top = y; right = 100; bottom = y + 20 }, selected = selected, visible = visible)
    @Test fun knownFollowersIsNotPlainFollowers() {
        for (label in listOf("Followers you know", "Followers you know, selected", "Tanıdığın takipçiler"))
            assertEquals(RelationshipTabInspector.OTHER, RelationshipTabInspector.classifySelectedLabels(listOf(label)))
        assertEquals(RelationshipTabInspector.FOLLOWERS, RelationshipTabInspector.classifySelectedLabels(listOf("Followers")))
    }
    @Test fun videoKnownFollowersSelectionIsNotAFollowersList() {
        val nodes = listOf(n("Verified followers"), n("Followers you know", selected = true), n("Followers"), n("@one"), n("@two"))
        assertEquals(XScreen.UNKNOWN, ScreenDetector.detect(nodes))
    }
    @Test fun rawOtherTabCannotFallThroughToFollowersHeuristic() {
        assertEquals(XScreen.UNKNOWN, ScreenDetector.detect(listOf(n("Followers"), n("@one"), n("@two")), RelationshipTabInspector.OTHER))
    }
    @Test fun switchingToActualFollowersAllowsSourceSelection() {
        assertEquals(XScreen.FOLLOWERS_LIST, ScreenDetector.detect(listOf(n("Followers you know"), n("Followers", selected = true), n("@one"))))
    }
    @Test fun topSourceDoesNotNeedFollowButton() {
        val rows = listOf(n("@second", 200), n("Takip ediliyor", 105), n("@first", 100), n("Geri takip et", 205))
        assertEquals(listOf("first", "second"), RecentFollowerSelector.orderedHandles(rows, emptySet()))
    }
    @Test fun changedTopUserIsReadAgainRatherThanRemembered() {
        assertEquals("first", RecentFollowerSelector.orderedHandles(listOf(n("@first")), emptySet()).first())
        assertEquals("new_top", RecentFollowerSelector.orderedHandles(listOf(n("@new_top")), emptySet()).first())
    }
    @Test fun hiddenAndBioMentionsCannotBecomeSource() {
        val rows = listOf(n("@hidden", 0, visible = false), n("Ask @advertiser for help", 10), n("@real", 100))
        assertEquals(listOf("real"), RecentFollowerSelector.orderedHandles(rows, emptySet()))
    }
    @Test fun ownAndVisitedHandlesAreSkippedWithoutHardcodedNames() {
        val rows = listOf(n("@own", 0), n("@visited", 100), n("@next", 200), n("@next", 200))
        assertEquals(listOf("next"), RecentFollowerSelector.orderedHandles(rows, setOf("own", "visited")))
    }
    @Test fun selectedFollowersWithoutRowsIsIdentifiedButNoSourceIsInvented() {
        val nodes = listOf(n("Followers", selected = true))
        assertEquals(XScreen.FOLLOWERS_LIST, ScreenDetector.detect(nodes))
        assertTrue(RecentFollowerSelector.orderedHandles(nodes, emptySet()).isEmpty())
    }
    @Test fun selectedVerifiedWithoutRowsIsIdentifiedWithoutInventingTargets() {
        assertEquals(XScreen.VERIFIED_FOLLOWERS_LIST, ScreenDetector.detect(listOf(n("Verified followers", selected = true))))
    }
    @Test fun rawAndSnapshotTabEvidenceAgreeForSingleSource() {
        for ((title, tab, expected) in listOf(
            Triple("Followers", RelationshipTabInspector.FOLLOWERS, XScreen.FOLLOWERS_LIST),
            Triple("Following", RelationshipTabInspector.FOLLOWING, XScreen.FOLLOWING_LIST),
            Triple("Verified followers", RelationshipTabInspector.VERIFIED, XScreen.VERIFIED_FOLLOWERS_LIST),
            Triple("Followers you know", RelationshipTabInspector.OTHER, XScreen.UNKNOWN),
        )) {
            val nodes = listOf(n(title, selected = true), n("@dynamic_source"))
            assertEquals(expected, ScreenDetector.detect(nodes))
            assertEquals(expected, ScreenDetector.detect(nodes, tab))
        }
    }
    @Test fun sameUsersStillMovingCannotProveTopOfList() {
        val before = listOf(n("@first", 110), n("@second", 300))
        val moving = listOf(n("@first", 140), n("@second", 330))
        assertNotEquals(RecentFollowerSelector.viewportSignature(before), RecentFollowerSelector.viewportSignature(moving))
        assertEquals(RecentFollowerSelector.viewportSignature(before), RecentFollowerSelector.viewportSignature(before.reversed()))
    }
    @Test fun unselectedVerifiedTitleDoesNotOverrideActualFollowers() {
        assertEquals(XScreen.FOLLOWERS_LIST, ScreenDetector.detect(listOf(n("Verified followers"),
            n("Followers you know"), n("Followers", selected = true), n("@changed_user"))))
    }
}
