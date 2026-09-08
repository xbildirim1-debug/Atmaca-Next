package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoveryTargetLaunchPolicyTest {
    @Test fun ownProfileTransitionWaitsBeforeFirstTargetLaunch() {
        assertEquals(
            DiscoveryTargetLaunchDecision.WAIT,
            DiscoveryTargetLaunchPolicy.decide(false, 0, DiscoveryTargetLaunchPolicy.INITIAL_DELAY_MS - 1),
        )
        assertEquals(
            DiscoveryTargetLaunchDecision.LAUNCH,
            DiscoveryTargetLaunchPolicy.decide(false, 0, DiscoveryTargetLaunchPolicy.INITIAL_DELAY_MS),
        )
    }

    @Test fun swallowedTargetDeepLinkIsRetriedButBounded() {
        assertEquals(DiscoveryTargetLaunchDecision.LAUNCH, DiscoveryTargetLaunchPolicy.decide(false, 1, 2_000L))
        assertEquals(DiscoveryTargetLaunchDecision.LAUNCH, DiscoveryTargetLaunchPolicy.decide(false, 2, 4_000L))
        assertEquals(DiscoveryTargetLaunchDecision.GIVE_UP, DiscoveryTargetLaunchPolicy.decide(false, 3, 6_000L))
    }

    @Test fun exactTargetNeverRequestsAnotherLaunch() {
        assertEquals(DiscoveryTargetLaunchDecision.WAIT, DiscoveryTargetLaunchPolicy.decide(true, 0, 10_000L))
    }

    @Test fun retriesUseActuallyDifferentXRoutes() {
        assertEquals("twitter://user?screen_name=pusholder", DiscoveryProfileRoutePolicy.route("pusholder", 1).uri)
        assertEquals("https://twitter.com/pusholder", DiscoveryProfileRoutePolicy.route("pusholder", 2).uri)
        assertEquals("https://x.com/pusholder", DiscoveryProfileRoutePolicy.route("pusholder", 3).uri)
        assertEquals(3, (1..3).map { DiscoveryProfileRoutePolicy.route("pusholder", it).uri }.distinct().size)
    }
}
