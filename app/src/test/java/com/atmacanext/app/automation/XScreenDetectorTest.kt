package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class XScreenDetectorTest {
    private fun n(
        text: String? = null,
        clickable: Boolean = false,
        scrollable: Boolean = false,
        id: String? = null,
        description: String? = null,
        editable: Boolean = false,
        selected: Boolean = false,
    ) = NodeSnapshot(
        text = text, contentDescription = description, viewId = id, className = if (editable) "EditText" else if (clickable) "Button" else "TextView",
        clickable = clickable, enabled = true, bounds = Rect(0, 0, 100, 100), scrollable = scrollable,
        selected = selected, editable = editable,
    )

    @Test fun detectsVerifiedFollowersBeforePlainFollowers() {
        val nodes = listOf(n("Verified Followers"), n("@source"), n("Follow", true), n(scrollable = true))
        assertEquals(XScreen.VERIFIED_FOLLOWERS_LIST, ScreenDetector.detect(nodes))
    }

    @Test fun detectsTurkishVerifiedFollowersVariantFromWorkingXBuild() {
        val nodes = listOf(n("Doğrulanmış Takipçiler"), n("@source"), n("Sen de takip et", true), n(scrollable = true))
        assertEquals(XScreen.VERIFIED_FOLLOWERS_LIST, ScreenDetector.detect(nodes))
    }

    @Test fun detectsFollowingList() {
        val nodes = listOf(n("Following"), n("@abc"), n("Following", true), n(scrollable = true))
        assertEquals(XScreen.FOLLOWING_LIST, ScreenDetector.detect(nodes))
    }

    @Test fun followingTabsAndClickableRowsAreNotMistakenForProfile() {
        val nodes = listOf(
            n("Followers"), n("Following", selected = true), n("@one"), n("Following", true),
            n("@two"), n("Following", true), n("@three"), n("Following", true),
            n(scrollable = true),
        )
        assertEquals(XScreen.FOLLOWING_LIST, ScreenDetector.detect(nodes))
    }

    @Test fun staleProfileLabelsDoNotOverrideSelectedFollowingTab() {
        val nodes = listOf(
            n("Edit profile"), n("Joined August 2026"), n("Followers"),
            n("Following", selected = true), n("Subscribers"), n("Subscriptions"),
            n("@one"), n("Following", true), n("@two"), n("Following", true),
            n(scrollable = true),
        )
        assertEquals(XScreen.FOLLOWING_LIST, ScreenDetector.detect(nodes))
    }

    @Test fun detectsOwnProfileBeforeFollowingListHeuristic() {
        val nodes = listOf(n("@atmaca"), n("Edit profile", true), n("123 Following"), n("456 Followers"), n(scrollable = true))
        assertEquals(XScreen.PROFILE, ScreenDetector.detect(nodes))
    }

    @Test fun uiWordsDoNotCreateFakeHandles() {
        val nodes = listOf(n("Following"), n("Followers"), n("Profile"), n(scrollable = true))
        assertEquals(XScreen.UNKNOWN, ScreenDetector.detect(nodes))
    }

    @Test fun accountSwitcherNeedsRealHandleEvidence() {
        val nodes = listOf(n("Accounts"), n("@one", true), n("@two", true))
        assertEquals(XScreen.ACCOUNT_SWITCHER, ScreenDetector.detect(nodes))
    }

    @Test fun homeTimelineWithManyTweetActionsIsNotTweetDetail() {
        val nodes = listOf(
            n("Home"), n("Search"), n("Notifications"), n("Messages"), n(scrollable = true),
            n("Reply", true, id = "toolbar_reply"), n("Like", true, id = "toolbar_like"),
            n("Repost", true, id = "toolbar_retweet"), n("Bookmark", true, id = "bookmark"),
            n("Reply", true, id = "toolbar_reply_2"), n("Like", true, id = "toolbar_like_2"),
        )
        assertEquals(XScreen.HOME, ScreenDetector.detect(nodes))
    }

    @Test fun composerRequiresEditableEvidenceInsteadOfFloatingPostButton() {
        val home = listOf(n("Home"), n("Search"), n("Notifications"), n("Post", true, id = "post_button"))
        assertEquals(XScreen.HOME, ScreenDetector.detect(home))

        val composer = listOf(n("What is happening"), n(id = "tweet_box", editable = true), n("Post", true, id = "post_button"))
        assertEquals(XScreen.COMPOSER, ScreenDetector.detect(composer))
    }

    @Test fun explicitTweetDetailEvidenceWinsWithoutGuessingFromActionCount() {
        val nodes = listOf(
            n("Back", true), n("Post"), n(id = "tweet_detail_header"),
            n("Reply", true, id = "toolbar_reply"), n("Like", true, id = "toolbar_like"),
        )
        assertEquals(XScreen.TWEET_DETAIL, ScreenDetector.detect(nodes))
    }
}
