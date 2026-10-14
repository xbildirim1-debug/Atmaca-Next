package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileUpsell26_74Test {
    private fun n(text: String, top: Int, left: Int = 40, width: Int = 700, height: Int = 48, clickable: Boolean = false) =
        NodeSnapshot(text, null, null, "android.widget.TextView", clickable, true,
            Rect(left, top, left + width, top + height))

    @Test fun verificationCardIsNotADialogWhilePostsStayVisible() {
        val nodes = listOf(
            n("Ezel", 200),
            n("@ezeldestan", 260),
            n("Henüz hesabın onaylı değil", 700, width = 640),
            n("Onaylı onay işareti almak için abone ol.", 760, width = 640),
            n("Doğrulan", 840, width = 220, clickable = true),
            n("Kapat", 700, left = 900, width = 48, height = 48, clickable = true),
            n("Gönderiler", 980, width = 200),
            n("Yanıtlar", 980, left = 280, width = 200),
        )
        assertTrue(ProfileUpsellEvidence.isInlineCard(nodes))
        assertEquals(5, ProfileUpsellEvidence.closeIndex(nodes))
        assertEquals(XScreen.PROFILE, ScreenDetector.detect(nodes))
    }

    @Test fun aRealDismissDialogStaysADialog() {
        val nodes = listOf(n("Bir sorun oluştu", 400), n("Kapat", 520, clickable = true))
        assertFalse(ProfileUpsellEvidence.isInlineCard(nodes))
        assertEquals(XScreen.DIALOG, ScreenDetector.detect(nodes))
    }

    @Test fun showMoreIsAnInlineExpansion() {
        assertTrue(DiscoveryTweetOpenRecovery.inlineExpansion("Uzun metin. Daha fazlasını göster"))
        assertEquals(0.28f to 0.16f, DiscoveryTweetOpenRecovery.expandTap(0))
        assertFalse(DiscoveryTweetOpenRecovery.inlineExpansion("Kısa gönderi"))
    }
}
