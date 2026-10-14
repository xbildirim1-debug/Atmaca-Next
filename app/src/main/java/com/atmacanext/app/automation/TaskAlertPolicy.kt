package com.atmacanext.app.automation

/** Which log lines become a plain-language notification. Noise stays in the log only. */
internal object TaskAlertPolicy {
    data class Alert(val title: String, val text: String, val dedupeKey: String)

    private val noisyWarn = setOf(
        "DISCOVERY_READ", "DETAIL_HEADER", "UI", "UI_FRESH", "NAV", "ACCOUNT_RETURN",
        "DISCOVERY_SCAN", "DISCOVERY_CLICK", "COMMENT_EVIDENCE", "DISCOVERY_STATE",
        "QUOTE_REPLY", "QUOTE_REPLY_WRITE", "PROFILE_UPSELL",
    )

    fun fromLog(level: String, category: String, message: String): Alert? {
        if (level == "INFO" || category == "INTERNET") return null
        if (level == "WARN" && (category in noisyWarn || !importantWarning(category, message))) return null
        if (level != "ERROR" && level != "WARN") return null
        val text = explain(category, message)
        return Alert(titleFor(category, level), text, "$category:${message.take(90)}")
    }

    fun internetLost(): Alert = Alert(
        "İnternet kesildi",
        "Bağlantı yok. Görev durdu ve Atmaca'ya dönüldü. İnternet her dakika kontrol edilecek, gelince kaldığı yerden devam edecek.",
        "internet:lost",
    )

    fun internetRestored(): Alert = Alert(
        "İnternet geldi",
        "Bağlantı geri geldi. Görev kaldığı hesaptan ve ilerlemeden devam ediyor.",
        "internet:restored",
    )

    fun internetResumeBlocked(reason: String): Alert = Alert(
        "İnternet geldi, görev bekliyor",
        "Bağlantı var ama görev sürdürülemedi: $reason",
        "internet:blocked:${reason.take(80)}",
    )

    private fun importantWarning(category: String, message: String): Boolean {
        if (category in setOf("STALL_RECOVERY", "TASK_RESTART", "QUEUE_TASK_FAILED", "QUEUE_PREPARE", "RUNTIME")) return true
        val text = message.lowercase()
        return listOf("yazılmadı", "eşleşmedi", "başarısız", "acılamadı", "açılamadı", "durduruldu", "doğrulanamadı").any(text::contains)
    }

    private fun titleFor(category: String, level: String): String = when (category) {
        "STALL_RECOVERY", "TASK_RESTART" -> "Görev takıldı"
        "QUEUE_TASK_FAILED", "QUEUE_PREPARE" -> "Görev durdu"
        else -> if (level == "ERROR") "Görev hatası" else "Görev uyarısı"
    }

    private fun explain(category: String, message: String): String {
        val clean = message.take(220)
        return when {
            category == "STALL_RECOVERY" ->
                "10 saniyedir gerçek ilerleme yok. Aynı görev ve sayaç korunup yeniden denenecek."
            category == "TASK_RESTART" ->
                "Görev takıldığı için yeniden başlatıldı. İlerleme silinmedi. $clean"
            clean.contains("eşleşmedi", true) ->
                "Açılan gönderi hedefle uyuşmadı, yorum yazılmadı."
            clean.contains("yazılmadı", true) ->
                "Yorum gönderilemedi. $clean"
            clean.contains("açılamadı", true) || clean.contains("acılamadı", true) ->
                "Bir ekran açılamadı, görev bu adımda kaldı. $clean"
            else -> clean
        }
    }
}
