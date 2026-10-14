package com.atmacanext.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PilotChatStoreTest {
    @Test fun transcriptRoundTripsAndDropsBlanks() {
        val raw = PilotChatStore.encode(
            listOf(
                PilotChatStore.Line(true, "onaylı takibi başlat"),
                PilotChatStore.Line(false, "  "),
                PilotChatStore.Line(false, "Aldım"),
            ),
        )
        val back = PilotChatStore.decode(raw)
        assertEquals(listOf(true, false), back.map { it.mine })
        assertEquals(listOf("onaylı takibi başlat", "Aldım"), back.map { it.text })
    }

    @Test fun dutyLeavesAPersonPauseAlone() {
        assertFalse(PilotDuty.shouldAutoResume("Kullanıcı duraklattı"))
        assertFalse(PilotDuty.shouldAutoResume("Ekran kapandı; güvenli devam için görev duraklatıldı"))
        assertFalse(PilotDuty.shouldAutoResume("Önceki yorumun gönderim sonucu belirsiz"))
        assertTrue(PilotDuty.shouldAutoResume("Liste başına kaydırma tamamlanamadı"))
    }
}
