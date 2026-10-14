package com.atmacanext.app.ai

/**
 * Decides which pauses Pilot may lift on its own.
 * A person, a locked screen, a permission dialog or an unconfirmed post stays paused.
 */
internal object PilotDuty {
    fun shouldAutoResume(message: String): Boolean {
        val lower = message.lowercase()
        val held = listOf(
            "kullanıcı",
            "kullanici",
            "bildirimden",
            "ekran kapandı",
            "ekran kapandi",
            "kilit",
            "izin",
            "belirsiz",
            "tekrar gönderil",
            "tekrar gonderil",
            "kontrol et",
            "durduruldu",
        )
        return held.none { it in lower }
    }
}
