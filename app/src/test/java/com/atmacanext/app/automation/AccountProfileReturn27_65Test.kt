package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class AccountProfileReturn27_65Test {
    private fun node(text: String? = null, desc: String? = null, id: String? = null,
                     l: Int = 0, t: Int = 0, r: Int = 1080, b: Int = 2400, visible: Boolean = true) =
        NodeSnapshot(text, desc, id, "android.view.View", true, true,
            Rect().apply { left = l; top = t; right = r; bottom = b }, visible = visible)

    private fun profile(extra: List<NodeSnapshot> = emptyList()) = listOf(
        node(), node("Profili düzenle", l = 700, t = 330, r = 1040, b = 400),
        node("@xhesaplar1", l = 30, t = 500, r = 450, b = 545),
        node("1.455 Takip ediliyor", l = 30, t = 600, r = 350, b = 650),
        node("1.551 Takipçiler", l = 380, t = 600, r = 730, b = 650),
    ) + extra

    @Test fun profileMisclassifiedAsHomeStillNeedsBack() {
        val nodes = profile()
        assertTrue(AccountProfileReturnPolicy.ownProfile(nodes, "xhesaplar1"))
        assertEquals(AccountProfileReturnPolicy.Action.POP_PROFILE,
            AccountProfileReturnPolicy.decide(XScreen.HOME, AccountProfileReturnPolicy.profilePresent(nodes), true))
    }

    @Test fun acceptedBackWithoutAnActualScreenChangeCannotAdvanceToNextAccount() {
        repeat(3) {
            assertEquals(AccountProfileReturnPolicy.Action.POP_PROFILE,
                AccountProfileReturnPolicy.decide(XScreen.PROFILE, true, false))
        }
        assertEquals(AccountProfileReturnPolicy.Action.READY,
            AccountProfileReturnPolicy.decide(XScreen.HOME, false, true))
    }

    @Test fun aHomeClassificationWithoutTimelineEvidenceIsNotAConfirmedReturn() {
        assertEquals(AccountProfileReturnPolicy.Action.WAIT,
            AccountProfileReturnPolicy.decide(XScreen.HOME, false, false))
    }

    @Test fun loadingOrUnexpectedDetailDoesNotSwitchAccounts() {
        assertEquals(AccountProfileReturnPolicy.Action.WAIT,
            AccountProfileReturnPolicy.decide(XScreen.UNKNOWN, false, false))
        assertEquals(AccountProfileReturnPolicy.Action.LEAVE_DETAIL,
            AccountProfileReturnPolicy.decide(XScreen.TWEET_DETAIL, false, false))
    }

    @Test fun wrongAccountProfileCannotAuthorizeAGlobalBack() {
        assertFalse(AccountProfileReturnPolicy.ownProfile(profile(), "atmaca2025"))
        assertNull(AccountProfileReturnPolicy.backIndex(profile(), "atmaca2025"))
    }

    @Test fun tweetContainingGeriIsNotTheBackArrow() {
        val nodes = profile(listOf(node("Geri gel artık", l = 30, t = 100, r = 120, b = 180)))
        assertNull(AccountProfileReturnPolicy.backIndex(nodes, "xhesaplar1"))
    }

    @Test fun unlabelledCornerAndScreenWideBackContainersAreIgnored() {
        assertNull(AccountProfileReturnPolicy.backIndex(profile(listOf(
            node(l = 30, t = 100, r = 120, b = 180),
            node(desc = "Geri", l = 0, t = 0, r = 1080, b = 2400),
        )), "xhesaplar1"))
    }

    @Test fun identifiedSmallArrowAboveProfileHeaderIsAccepted() {
        val nodes = profile(listOf(node(desc = "Geri, düğme", l = 30, t = 100, r = 120, b = 180)))
        assertEquals(nodes.lastIndex, AccountProfileReturnPolicy.backIndex(nodes, "xhesaplar1"))
    }

    @Test fun backTextInThePostBodyAndInvisibleArrowsAreRejected() {
        val nodes = profile(listOf(
            node(desc = "Geri", l = 30, t = 900, r = 120, b = 980),
            node(desc = "Geri", l = 30, t = 100, r = 120, b = 180, visible = false),
        ))
        assertNull(AccountProfileReturnPolicy.backIndex(nodes, "xhesaplar1"))
    }

    @Test fun aDrawerIsAValidPlaceToContinueAccountSelection() {
        assertEquals(AccountProfileReturnPolicy.Action.READY,
            AccountProfileReturnPolicy.decide(XScreen.ACCOUNT_DRAWER, false, false))
    }

    @Test fun profileCountersBehindAnOpenDrawerCannotHideTheConfirmedMenu() {
        assertEquals(AccountProfileReturnPolicy.Action.READY,
            AccountProfileReturnPolicy.decide(XScreen.ACCOUNT_DRAWER, true, false))
    }
}
