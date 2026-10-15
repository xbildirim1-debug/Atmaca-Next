package com.atmacanext.app.ai

import com.atmacanext.app.data.local.AutomationLogEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PilotLogReport {
    fun summarize(logs: List<AutomationLogEntity>): String {
        if (logs.isEmpty()) return "İşlem kaydı yok; en sık hata için henüz veri bulunmuyor."
        val issues = logs.filter { it.level.uppercase() in setOf("WARN", "ERROR") }
        val groups = issues.groupBy { row ->
            val stage = Regex("stage=([A-Z_]+)").find(row.details.orEmpty())?.groupValues?.get(1)
            row.category + (stage?.let { "/$it" } ?: "")
        }.entries.sortedByDescending { it.value.size }
        val fmt = DateTimeFormatter.ofPattern("dd.MM HH:mm:ss").withZone(ZoneId.systemDefault())
        val range = "${fmt.format(Instant.ofEpochMilli(logs.minOf { it.timestamp }))} – ${fmt.format(Instant.ofEpochMilli(logs.maxOf { it.timestamp }))}"
        val lines = groups.take(8).mapIndexed { i, entry ->
            val sample = entry.value.maxBy { it.timestamp }
            val errors = entry.value.count { it.level.equals("ERROR", true) }
            "${i + 1}. ${entry.key}: ${entry.value.size} kayıt ($errors hata). Son örnek: ${sample.message.take(170)}"
        }
        val advice = when {
            groups.any { it.key.contains("DISCOVERY") || it.key.contains("COMMENT") } -> "Öncelik: hedef gönderi/yorum listesinde kaydırma ve geri dönüş kanıtını kontrol etmek."
            groups.any { it.key.contains("SNAPSHOT") } -> "Öncelik: eski/eksik ekran ağacını yeniden okumak ve bekleyen işlemi tekrar basmamak."
            groups.any { it.key.contains("ACCOUNT") } -> "Öncelik: hesap seçimi tamamlanmadan sonraki adıma geçilmesini incelemek."
            groups.any { it.key.contains("STALL") || it.key.contains("RESTART") } -> "Öncelik: gerçek ilerleme ile ekran hareketini ayırıp tekrarlanan kurtarma döngüsünü incelemek."
            else -> "Öncelik: en sık kaydın görev/aşama ve son başarı kayıtlarını birlikte incelemek."
        }
        return "Son ${logs.size} gerçek işlem kaydı ($range). ${issues.size} uyarı/hata; uyarı sayısı tek başına başarısız işlem sayısı değildir.\n" +
            if (groups.isEmpty()) "Bu aralıkta uyarı veya hata yok." else lines.joinToString("\n") + "\n$advice\nBu rapor kodun değiştirildiği anlamına gelmez."
    }
}
