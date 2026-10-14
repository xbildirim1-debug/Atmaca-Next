package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InternetAndAlert26_77Test {
    @Test fun offlineTaskReturnsHomeAndWaits() {
        assertEquals(InternetWatchPolicy.Event.LOST, InternetWatchPolicy.decide(true, false, true, false))
        assertEquals(InternetWatchPolicy.Event.LOST, InternetWatchPolicy.decide(false, false, true, false))
        assertNull(InternetWatchPolicy.decide(true, true, true, false))
        assertNull(InternetWatchPolicy.decide(false, false, false, false))
    }

    @Test fun internetReturnResumesOnlyTheWaitingTask() {
        assertEquals(InternetWatchPolicy.Event.RESTORED, InternetWatchPolicy.decide(false, true, false, true))
        assertNull(InternetWatchPolicy.decide(false, true, true, false))
        assertEquals(60_000L, InternetWatchPolicy.CHECK_MS)
    }

    @Test fun errorsBecomePlainNotificationsAndNoiseDoesNot() {
        val mismatch = TaskAlertPolicy.fromLog("WARN", "RUNTIME", "Açılan gönderinin yazarı ve metni hedefle eşleşmedi")
        assertEquals("Açılan gönderi hedefle uyuşmadı, yorum yazılmadı.", mismatch?.text)
        assertNull(TaskAlertPolicy.fromLog("INFO", "NAV", "X acildi: home"))
        assertNull(TaskAlertPolicy.fromLog("WARN", "DISCOVERY_SCAN", "rows=1"))
        assertTrue(TaskAlertPolicy.fromLog("ERROR", "QUOTE_REPLY", "metin yazılamadı")?.text?.contains("yazılamadı") == true)
        assertTrue(TaskAlertPolicy.internetLost().text.contains("her dakika"))
        assertTrue(TaskAlertPolicy.internetResumeBlocked("Ekran kapalı").text.contains("Ekran kapalı"))
    }
}
