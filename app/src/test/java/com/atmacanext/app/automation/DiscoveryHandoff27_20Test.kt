package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscoveryHandoff27_20Test {
    private fun backNode() = NodeSnapshot(
        text = null,
        contentDescription = "Geri",
        viewId = "com.twitter.android:id/back",
        className = "android.widget.ImageButton",
        clickable = false,
        enabled = true,
        bounds = Rect().apply { left = 55; top = 148; right = 121; bottom = 214 },
        visible = true,
    )

    @Test fun tweetWithoutBottomHomeUsesTheInAppArrow() {
        assertEquals(
            DiscoveryHandoffPolicy.Action.TAP_BACK,
            DiscoveryHandoffPolicy.action(XScreen.TWEET_DETAIL, bottomHomeVisible = false, backVisible = true, unknownSettled = false),
        )
    }

    @Test fun profilePrefersTheBottomHomeTabOverAnotherBack() {
        assertEquals(
            DiscoveryHandoffPolicy.Action.TAP_BOTTOM_HOME,
            DiscoveryHandoffPolicy.action(XScreen.PROFILE, bottomHomeVisible = true, backVisible = true, unknownSettled = false),
        )
    }

    @Test fun homeAndAccountDrawerAreAlreadyUsable() {
        assertEquals(DiscoveryHandoffPolicy.Action.DONE, DiscoveryHandoffPolicy.action(XScreen.HOME, false, true, false))
        assertEquals(DiscoveryHandoffPolicy.Action.DONE, DiscoveryHandoffPolicy.action(XScreen.ACCOUNT_DRAWER, false, true, false))
        assertEquals(DiscoveryHandoffPolicy.Action.DONE, DiscoveryHandoffPolicy.action(XScreen.ACCOUNT_SWITCHER, false, false, false))
    }

    @Test fun unknownWaitsUntilSettledAndNeverCountsAsLeft() {
        assertEquals(DiscoveryHandoffPolicy.Action.WAIT, DiscoveryHandoffPolicy.action(XScreen.UNKNOWN, false, true, false))
        assertEquals(DiscoveryHandoffPolicy.Action.TAP_BACK, DiscoveryHandoffPolicy.action(XScreen.UNKNOWN, false, true, true))
        assertEquals(DiscoveryHandoffPolicy.Action.WAIT, DiscoveryHandoffPolicy.action(XScreen.UNKNOWN, false, false, true))
    }

    @Test fun childWithoutArrowIsNotAHomeIntent() {
        for (screen in listOf(XScreen.TWEET_DETAIL, XScreen.PROFILE, XScreen.ENGAGEMENT_LIST, XScreen.FOLLOWERS_LIST)) {
            assertEquals(
                DiscoveryHandoffPolicy.Action.FAILED,
                DiscoveryHandoffPolicy.action(screen, bottomHomeVisible = false, backVisible = false, unknownSettled = true),
            )
        }
    }

    @Test fun handoffStillSeesTheArrowTheDiscoveryReturnSuppresses() {
        val nodes = listOf(backNode())
        assertNull(NavigationSurfaceEvidence.backIndex(nodes, XFlowStage.RETURN_DISCOVERY_TARGET, XScreen.PROFILE))
        assertEquals(0, NavigationSurfaceEvidence.backIndex(nodes, XFlowStage.VERIFY_ACCOUNT, XScreen.PROFILE))
        assertEquals(0, NavigationSurfaceEvidence.backIndex(nodes, XFlowStage.VERIFY_ACCOUNT, XScreen.TWEET_DETAIL))
    }
}
