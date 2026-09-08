package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class DiscoverySearchSelectorTest {
    private fun n(text: String, top: Int, bottom: Int = top + 40, editable: Boolean = false) = NodeSnapshot(
        text, null, null, "TextView", true, true,
        Rect().apply { left = 10; right = 250; this.top = top; this.bottom = bottom }, editable = editable)
    @Test fun ownProfileMagnifierIsNotGlobalSearch() {
        assertNull(DiscoverySearchSelector.searchTab(listOf(n("Ara", 40), n("@own", 300), n("Post", 900))))
    }
    @Test fun bottomSearchLabelsWorkInBothLanguagesAndScaledScreens() {
        for (label in listOf("Search and Explore", "Ara ve Keşfet", "Ara, sekme 2 / 5", "Search")) {
            for (scale in listOf(1, 2, 3)) {
                val nodes = listOf(n("Home", 10 * scale), n(label, 900 * scale, 940 * scale))
                assertEquals(1, DiscoverySearchSelector.searchTab(nodes))
            }
        }
    }
    @Test fun queryIsNeverClickedAsResult() {
        val nodes = listOf(n("@pusholder", 50, editable = true))
        assertEquals(0, DiscoverySearchSelector.searchField(nodes))
        assertNull(DiscoverySearchSelector.result(nodes, "pusholder", 0))
    }
    @Test fun exactSuggestionBelowQueryIsSelectedCaseInsensitively() {
        val nodes = listOf(n("@pusholder", 50, editable = true), n("@Pusholder", 200), n("Follow", 210))
        assertEquals(1, DiscoverySearchSelector.result(nodes, "pusholder", 0))
    }
    @Test fun otherHandlesMentionsAndFollowRowsAreRejected() {
        val field = n("@pusholder", 50, editable = true)
        for (label in listOf("@pusholder1", "News by @pusholder", "Follow @pusholder", "@pusholder, Follow")) {
            assertNull(DiscoverySearchSelector.result(listOf(field, n(label, 200)), "pusholder", 0))
        }
    }
    @Test fun hiddenDisabledAndAboveQueryMatchesAreRejected() {
        val nodes = listOf(n("@pusholder", 100, editable = true), n("@pusholder", 20),
            n("@pusholder", 200).copy(visible = false), n("@pusholder", 300).copy(enabled = false))
        assertNull(DiscoverySearchSelector.result(nodes, "pusholder", 0))
    }
    @Test fun multipleUnidentifiedEditorsAreNotGuessed() {
        assertNull(DiscoverySearchSelector.searchField(listOf(n("one", 50, editable = true), n("two", 150, editable = true))))
    }
}
