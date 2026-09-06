package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationPlannerTest {
    @Test fun homeOpensXDrawer() {
        val d = NavigationPlanner.decide(XScreen.HOME, AutomationAction.UNFOLLOW, "@hedef", OwnProfileIdentity(false, false), false, false)
        assertEquals(NavigationCommand.OPEN_ACCOUNT_DRAWER, d.command)
    }

    @Test fun drawerOpensProfile() {
        val d = NavigationPlanner.decide(XScreen.ACCOUNT_DRAWER, AutomationAction.UNFOLLOW, "@hedef", OwnProfileIdentity(false, false), false, false)
        assertEquals(NavigationCommand.OPEN_PROFILE_FROM_DRAWER, d.command)
    }

    @Test fun accountSwitcherSelectsExactTarget() {
        val d = NavigationPlanner.decide(XScreen.ACCOUNT_SWITCHER, AutomationAction.UNFOLLOW, "@hedef", OwnProfileIdentity(false, false), false, false)
        assertEquals(NavigationCommand.SELECT_TARGET_ACCOUNT, d.command)
    }

    @Test fun verifiedOwnProfileOpensCorrectInitialList() {
        val identity = OwnProfileIdentity(true, true, "@hedef")
        val follow = NavigationPlanner.decide(XScreen.PROFILE, AutomationAction.FOLLOW_VERIFIED, "@hedef", identity, true, false)
        val unfollow = NavigationPlanner.decide(XScreen.PROFILE, AutomationAction.UNFOLLOW, "@hedef", identity, true, false)
        assertEquals(NavigationCommand.OPEN_FOLLOWERS, follow.command)
        assertEquals(NavigationCommand.OPEN_FOLLOWING, unfollow.command)
    }

    @Test fun verifiedFollowersListIsReadyOnlyForVerifiedFollow() {
        val d = NavigationPlanner.decide(XScreen.VERIFIED_FOLLOWERS_LIST, AutomationAction.FOLLOW_VERIFIED, "@hedef", OwnProfileIdentity(false, false), true, true)
        assertEquals(NavigationCommand.READY, d.command)
    }
}
