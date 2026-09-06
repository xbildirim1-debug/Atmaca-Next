package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class XAccountSyncEvidenceCp15Test {
    private fun n(text: String? = null, id: String? = null) = NodeSnapshot(
        text = text,
        contentDescription = null,
        viewId = id,
        className = "TextView",
        clickable = false,
        enabled = true,
        bounds = Rect(0, 0, 100, 40),
    )

    @Test
    fun discovery_is_unique_and_capped_at_ten() {
        val nodes = (1..12).map { n("@acc$it") } + n("@acc1")
        val handles = XAccountSyncEvidence.accountHandles(nodes)
        assertEquals(10, handles.size)
        assertEquals("@acc1", handles.first())
        assertFalse(handles.contains("@acc11"))
    }

    @Test
    fun counters_require_expected_handle() {
        val nodes = listOf(
            n("@right"),
            n("1.2K Followers"),
            n("345 Following"),
        )
        assertNull(XAccountSyncEvidence.profileCounters(nodes, "@wrong"))
        val result = XAccountSyncEvidence.profileCounters(nodes, "@right")
        assertNotNull(result)
        assertEquals("1.2k", result!!.followers?.lowercase())
        assertEquals("345", result.following)
    }

    @Test
    fun turkish_profile_counters_are_read() {
        val nodes = listOf(
            n("@hesap"),
            n("2.345 Takipçiler"),
            n("876 Takip edilen"),
        )
        val result = XAccountSyncEvidence.profileCounters(nodes, "@hesap")
        assertEquals("2.345", result?.followers)
        assertEquals("876", result?.following)
    }
}

class XAccountSwitchGuardCp15Test {
    @Test
    fun mutation_is_blocked_until_exact_profile_handle_is_verified() {
        assertFalse(
            XAccountSwitchGuard.mayPerformMutation(
                XAccountSwitchGuard.evaluate("@a", "@b", XScreen.PROFILE, true)
            )
        )
        assertTrue(
            XAccountSwitchGuard.mayPerformMutation(
                XAccountSwitchGuard.evaluate("@a", "@a", XScreen.PROFILE, true)
            )
        )
    }
}
