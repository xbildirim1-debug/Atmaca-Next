package com.atmacanext.app.ai

import com.atmacanext.app.data.local.AutomationLogEntity
import org.junit.Assert.*
import org.junit.Test

class PilotReport27_67Test {
    private fun log(n: Int, level: String, category: String, message: String) = AutomationLogEntity(n.toLong(), level, category, null, null, message, "stage=PROCESS_ENGAGEMENT")
    @Test fun reportReadsRealRecordsAndRanksFrequentIssues() {
        val report = PilotLogReport.summarize(listOf(log(1,"WARN","DISCOVERY","Örnek bir"), log(2,"ERROR","DISCOVERY","Örnek iki"),log(3,"WARN","ACCOUNT","Örnek üç"),log(4,"INFO","TASK","Tamamlandı")))
        assertTrue(report.contains("4 gerçek işlem kaydı")); assertTrue(report.contains("1. DISCOVERY/PROCESS_ENGAGEMENT: 2 kayıt (1 hata)"))
        assertTrue(report.contains("Örnek iki")); assertFalse(report.contains("Tamamlandı"))
    }
    @Test fun emptyAndHealthyRecordsDoNotInventErrors() {
        assertTrue(PilotLogReport.summarize(emptyList()).contains("kaydı yok"))
        assertTrue(PilotLogReport.summarize(listOf(log(1,"INFO","TASK","OK"))).contains("uyarı veya hata yok"))
    }
}
