package com.atmacanext.app.automation

import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryScrollGesturePolicyTest {
    @Test fun discoverySwipeStaysInLeftGutterAwayFromMediaAndCompose() {
        assertTrue(DiscoveryScrollGesturePolicy.X_RATIO < 0.10f)
        assertTrue(DiscoveryScrollGesturePolicy.FORWARD_START_Y_RATIO > 0.75f)
        assertTrue(DiscoveryScrollGesturePolicy.FORWARD_END_Y_RATIO < 0.35f)
    }

    @Test fun backwardPathExactlyReversesForwardPath() {
        assertTrue(DiscoveryScrollGesturePolicy.BACKWARD_START_Y_RATIO == DiscoveryScrollGesturePolicy.FORWARD_END_Y_RATIO)
        assertTrue(DiscoveryScrollGesturePolicy.BACKWARD_END_Y_RATIO == DiscoveryScrollGesturePolicy.FORWARD_START_Y_RATIO)
    }
}
