package com.atmacanext.app.automation

import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryScrollGesturePolicyTest {
    @Test fun discoverySwipeStaysInLeftGutterAwayFromMediaAndCompose() {
        assertTrue(DiscoveryScrollGesturePolicy.xRatio(0) < 0.10f)
        assertTrue(DiscoveryScrollGesturePolicy.xRatio(1) < 0.15f)
        assertTrue(DiscoveryScrollGesturePolicy.FORWARD_START_Y_RATIO > 0.85f)
        assertTrue(DiscoveryScrollGesturePolicy.FORWARD_END_Y_RATIO < 0.25f)
    }

    @Test fun swallowedSwipeAlternatesBetweenTwoSafeGutters() {
        assertTrue(DiscoveryScrollGesturePolicy.xRatio(0) != DiscoveryScrollGesturePolicy.xRatio(1))
        assertTrue(DiscoveryScrollGesturePolicy.xRatio(0) == DiscoveryScrollGesturePolicy.xRatio(2))
    }

    @Test fun backwardPathExactlyReversesForwardPath() {
        assertTrue(DiscoveryScrollGesturePolicy.BACKWARD_START_Y_RATIO == DiscoveryScrollGesturePolicy.FORWARD_END_Y_RATIO)
        assertTrue(DiscoveryScrollGesturePolicy.BACKWARD_END_Y_RATIO == DiscoveryScrollGesturePolicy.FORWARD_START_Y_RATIO)
    }
}
