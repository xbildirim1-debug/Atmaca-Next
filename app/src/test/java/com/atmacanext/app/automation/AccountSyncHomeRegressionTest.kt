package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountSyncHomeRegressionTest {
    private fun n(text: String? = null, selected: Boolean = false, clickable: Boolean = false,
                  visible: Boolean = true, id: String? = null, scrollable: Boolean = false) = NodeSnapshot(
        text, null, id, "TextView", clickable, true, Rect(), selected = selected,
        visible = visible, scrollable = scrollable)

    private fun feed(english: Boolean = false, followingSelected: Boolean = false) = listOf(
        n(if (english) "For you" else "Sana özel", selected = !followingSelected),
        n(if (english) "Following" else "Takip edilenler", selected = followingSelected),
        n("X Premium"), n("@author_one"), n("@author_two"), n(scrollable = true))

    @Test fun videoFeedRemainsHomeWhenTweetAuthorsLoad() {
        val loading = feed().filterNot { it.text.orEmpty().startsWith("@") }
        assertEquals(XScreen.HOME, ScreenDetector.detect(loading))
        assertEquals(XScreen.HOME, ScreenDetector.detect(feed()))
    }

    @Test fun selectedFollowingFeedIsHomeInBothLanguages() {
        for (english in listOf(false, true)) {
            assertEquals(XScreen.HOME, ScreenDetector.detect(feed(english, true)))
        }
    }

    @Test fun rawSelectedFollowingTabCannotOverrideHomePager() {
        assertEquals(XScreen.HOME, ScreenDetector.detect(feed(true, true), RelationshipTabInspector.FOLLOWING))
    }

    @Test fun decoratedHomeTabsAreRecognized() {
        assertEquals(XScreen.HOME, ScreenDetector.detect(listOf(
            n("Sana özel, sekme 1/3, seçili"), n("Takip edilenler, sekme 2/3"),
            n("@one"), n("@two"))))
    }

    @Test fun drawerAboveFeedWinsOverHomeAndRawFollowingTab() {
        val nodes = feed(true, true) + listOf(n("Profile", clickable = true),
            n("Bookmarks", clickable = true), n("Settings and privacy", clickable = true))
        assertEquals(XScreen.ACCOUNT_DRAWER, ScreenDetector.detect(nodes, RelationshipTabInspector.FOLLOWING))
    }

    @Test fun accountSwitcherAboveFeedStillAllowsImport() {
        assertEquals(XScreen.ACCOUNT_SWITCHER,
            ScreenDetector.detect(feed() + n("Add an existing account"), RelationshipTabInspector.FOLLOWING))
    }

    @Test fun relationshipListWithBottomNavigationRemainsFollowingList() {
        val nodes = listOf(n("Followers"), n("Following", selected = true), n("@one"), n("@two"),
            n("Home"), n("Search"), n("Notifications"))
        assertEquals(XScreen.FOLLOWING_LIST, ScreenDetector.detect(nodes))
        assertEquals(XScreen.FOLLOWING_LIST, ScreenDetector.detect(nodes, RelationshipTabInspector.FOLLOWING))
    }

    @Test fun hiddenHomePagerDoesNotOverrideRelationshipList() {
        val nodes = listOf(n("For you", visible = false), n("Following", selected = true), n("@one"), n("@two"))
        assertEquals(XScreen.FOLLOWING_LIST, ScreenDetector.detect(nodes))
    }

    @Test fun profileWithBottomNavigationRemainsProfile() {
        val nodes = listOf(n("Edit profile"), n("@owner"), n("10 Following"), n("20 Followers"),
            n("Home"), n("Search"), n("Notifications"))
        assertEquals(XScreen.PROFILE, ScreenDetector.detect(nodes))
    }
}
