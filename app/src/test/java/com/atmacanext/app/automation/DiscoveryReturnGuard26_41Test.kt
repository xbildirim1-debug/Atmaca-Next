package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscoveryReturnGuard26_41Test {
    private fun backNode() = NodeSnapshot(
        text = null,
        contentDescription = "Geri",
        viewId = "com.twitter.android:id/back",
        className = "android.widget.ImageButton",
        clickable = true,
        enabled = true,
        // Android local-unit-test stubs can return the default object for the
        // four-argument Rect constructor when returnDefaultValues=true. Set the
        // fields explicitly so the selector sees a real non-empty control.
        bounds = Rect().apply { left = 20; top = 30; right = 100; bottom = 110 },
        visible = true,
    )

    @Test fun returnToDiscoveryTargetNeverBacksOutOfProfile() {
        val nodes = listOf(backNode())
        assertNull(NavigationSurfaceEvidence.backIndex(nodes, XFlowStage.RETURN_DISCOVERY_TARGET, XScreen.PROFILE))
    }

    @Test fun returnRouteStillAllowsBackFromTweetDetail() {
        val nodes = listOf(backNode())
        assertEquals(0, NavigationSurfaceEvidence.backIndex(nodes, XFlowStage.RETURN_DISCOVERY_TARGET, XScreen.TWEET_DETAIL))
    }

    @Test fun unrelatedProfileFlowsKeepNormalBackBehavior() {
        val nodes = listOf(backNode())
        assertEquals(0, NavigationSurfaceEvidence.backIndex(nodes, XFlowStage.OPEN_SOURCE_PROFILE, XScreen.PROFILE))
    }
}
