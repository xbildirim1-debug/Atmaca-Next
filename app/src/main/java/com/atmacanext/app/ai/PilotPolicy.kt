package com.atmacanext.app.ai

import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskType
import java.time.LocalDate

/** On-device director. It does not call a network model. */
internal object PilotPolicy {
    enum class Move { REFRESH, BACK, HOME, CONTINUE }
    enum class Command { NONE, STOP, PAUSE, RESUME, SKIP_TASK, SKIP_ACCOUNT, START, TWEETS, STATUS, CLEAR_MEMORY, REPORT }

    fun stallMove(
        attempt: Int,
        pendingAction: Boolean,
        knownStreak: Int = 0,
        screen: String = "",
        failedRecoveries: Int = 0,
        lastFailed: String? = null,
    ): Move {
        if (pendingAction) return Move.CONTINUE
        val unsafeBack = screen in setOf("HOME", "ACCOUNT_DRAWER", "ACCOUNT_SWITCHER", "UNKNOWN", "")
        val escalate = knownStreak >= 3 || failedRecoveries >= 2 || attempt >= 3 || lastFailed == "HOME"
        if (escalate) return Move.HOME
        if (lastFailed == "REFRESH" && !unsafeBack) return Move.BACK
        val wantBack = !unsafeBack && (knownStreak >= 2 || attempt == 2) && lastFailed != "BACK"
        if (wantBack) return Move.BACK
        if (attempt <= 1 && lastFailed != "REFRESH") return Move.REFRESH
        return Move.HOME
    }

    fun tweets(topic: String, count: Int, salt: Int): List<String> {
        val subject = topic.trim().ifBlank { "bugünün notu" }.take(80)
        val wanted = count.coerceIn(1, 12)
        val openers = listOf(
            "Bugün net olan şu:",
            "Kısa not:",
            "Şunu bir kenara yazıyorum:",
            "Üzerine düşününce:",
            "Günün içinden:",
            "Bunu sade söyleyeyim:",
        )
        val bridges = listOf(
            "asıl mesele hız değil, aynı işi bozmadan bitirmek.",
            "kalabalıkta bağıran değil, işini bitiren kazanıyor.",
            "acele karar, sonra geri alınan işten pahalıya geliyor.",
            "düzen bozulunca emek de boşa gidiyor.",
            "az ama temiz iş, dağınık yüz işten iyi.",
            "aynı hatayı ikinci kez yapmak artık şans değil.",
        )
        val ends = listOf(
            "Ben böyle görüyorum.",
            "Gerisi sende.",
            "Bunu burada bırakıyorum.",
            "Not düşüldü.",
            "Fazlası gürültü.",
            "Devamını sen yaz.",
        )
        return List(wanted) { index ->
            val n = salt + index * 17
            val line = "${openers[n % openers.size]} $subject, ${bridges[(n / 3) % bridges.size]} ${ends[(n / 5) % ends.size]}"
            line.replace(Regex("\\s+"), " ").trim().take(280)
        }.distinct()
    }

    fun plan(text: String, status: String, screen: String, stage: String, account: String?, message: String, pool: Int, lastMove: String, memory: String = ""): PilotPlan {
        val raw = text.trim()
        val lower = raw.lowercase()
        if (raw.isBlank()) return PilotPlan("Ne yapacağımı yaz. Örnek: onaylı takibi başlat, hepsini çalıştır, durdur, 6 tweet üret.")
        val named = Regex("@([A-Za-z0-9_]{1,15})").find(raw)?.groupValues?.get(1)
        if (listOf("hafızayı sil", "hafizayi sil", "belleği sil", "belleği temizle", "hataları sil").any { it in lower }) {
            return PilotPlan("Hafıza ve hata kaydını siliyorum.", Command.CLEAR_MEMORY)
        }
        if (listOf("naber", "selam", "merhaba", "hey", "nasılsın", "nasilsin", "iyi misin").any { it in lower }) {
            return PilotPlan("İyiyim. Görev başlatabilir, durdurabilir veya 'sık hataları söyle' diyebilirsin.")
        }
        if (listOf("sık hata", "ardışık", "rapor", "en çok", "hangi ekran", "nerede takıl").any { it in lower }) {
            return PilotPlan("Rapor hazırlanıyor.", Command.REPORT)
        }
        if ("durdur" in lower) return PilotPlan("Görevi durduruyorum.", Command.STOP)
        if ("devam" in lower || "sürdür" in lower) return PilotPlan("Kaldığı yerden devam ediyorum.", Command.RESUME)
        if ("duraklat" in lower) return PilotPlan("Duraklatıyorum.", Command.PAUSE)
        if ("hesap atla" in lower || "hesabı atla" in lower || "hesabi atla" in lower) return PilotPlan("Bu hesabı atlıyorum.", Command.SKIP_ACCOUNT)
        if ("atla" in lower) return PilotPlan("Bu görevi atlıyorum.", Command.SKIP_TASK)
        if (listOf("durum", "ne durumda", "nerede", "kaçıncı").any { it in lower }) {
            return PilotPlan(statusLine(status, screen, stage, account, message, pool, lastMove, memory), Command.STATUS)
        }
        val sequence = sequenceOf(lower)
        val starting = listOf("başlat", "baslat", "çalıştır", "calistir", "başla", "basla", "görevini", "görevi", "önce").any { it in lower }
        val type = taskType(lower)
        val all = "hepsini" in lower || "tüm görev" in lower || "tum gorev" in lower
        if (starting && (type != null || all || sequence != null)) {
            if (sequence != null) {
                val first = type ?: sequence.first()
                return PilotPlan(
                    "İlk adımı alıyorum: ${first.title}. Sıranın kalanı not; kuyruk bitince söyle, devamını kendim açmam.",
                    Command.START, first, named, false,
                )
            }
            val what = when {
                all && type == null -> "kayıtlı görevlerin hepsini"
                named != null -> "@$named ${type?.title ?: "görev"}"
                else -> type?.title ?: "görev"
            }
            return PilotPlan("$what ben alıyorum.", Command.START, type, named, all && type == null)
        }
        if (!starting && listOf("tweet", "havuz", "metin", "üret", "yaz").any { it in lower }) {
            val topic = raw.replace(Regex("(?i)tweet|havuz|metin|üret|yaz|ekle|tane|adet"), " ").replace(Regex("\\d+"), " ").trim()
            val count = Regex("\\d+").find(raw)?.value?.toIntOrNull()?.coerceIn(1, 12) ?: 6
            return PilotPlan("Taslakları havuza koyuyorum. Yayına ben almam.", Command.TWEETS, tweets = tweets(topic, count, raw.length + pool))
        }
        if (listOf("takıl", "neden", "hata", "dondu", "takildi").any { it in lower }) {
            val why = lastMove.ifBlank { message.ifBlank { "Kayıtlı bir takılma yok." } }
            return PilotPlan("Son müdahale: $why. Ekran $screen, aşama $stage. Hafıza: ${memory.take(80)}")
        }
        val who = account?.let { "@$it" } ?: "hesap yok"
        val mem = memory.take(60).takeIf { it.isNotBlank() }?.let { " Hafıza: $it." }.orEmpty()
        return PilotPlan("$who. Durum $status, ekran $screen, aşama $stage. Havuz $pool.$mem ${message.take(140)}")
    }

    fun formatHotspots(locations: List<Triple<String, Int, Int>>, longest: Pair<String, Int>?): String {
        if (locations.isEmpty()) return "Kayıtlı ardışık hata yok. Görev çalışıp takılınca buraya düşer."
        val lines = locations.take(8).mapIndexed { i, (where, total, failed) ->
            val fail = if (failed > 0) ", $failed kurtarma tutmadı" else ""
            "${i + 1}. $where — $total kez$fail"
        }
        val streak = longest?.let { "\nEn uzun ardışık seri: ${it.first} (${it.second})" }.orEmpty()
        val lesson = locations.firstOrNull { it.third > 0 }?.let {
            "\nSonraki sefer ${it.first} için aynı kurtarmayı tekrar etmem."
        }.orEmpty()
        return "En sık takıldığım yerler:\n${lines.joinToString("\n")}$streak$lesson"
    }

    private fun statusLine(status: String, screen: String, stage: String, account: String?, message: String, pool: Int, lastMove: String, memory: String): String {
        val who = account?.let { "@$it" } ?: "hesap yok"
        val last = lastMove.ifBlank { "yok" }
        return "$who. $status / $screen / $stage. Havuz $pool. Son müdahale: $last. ${message.take(100)} Hafıza: ${memory.take(60)}"
    }

    /** Detect "önce X sonra Y" style chains. Returns ordered task types or null. */
    private fun sequenceOf(lower: String): List<TaskType>? {
        if ("önce" !in lower && "sonra" !in lower && "ardından" !in lower) return null
        val parts = lower.split(Regex("sonra|ardından|ve sonra")).map { it.trim() }.filter { it.isNotBlank() }
        val types = parts.mapNotNull { taskType(it) }.distinct()
        return types.takeIf { it.size >= 2 }
    }

    fun select(tasks: List<ScheduledTask>, type: TaskType?, account: String?, all: Boolean): List<ScheduledTask> {
        val named = account?.removePrefix("@")?.trim()?.lowercase()
        val mine = tasks.filter { named == null || it.username.removePrefix("@").equals(named, true) }
        if (all || type == null) return mine.filter { it.type != TaskType.SYNC }
        return mine.filter { it.type == type }
    }

    private fun taskType(lower: String): TaskType? = when {
        "onaylı" in lower || "onayli" in lower -> TaskType.VERIFIED_FOLLOW
        "yorumcu" in lower -> TaskType.COMMENTER_FOLLOW
        "retweetçi" in lower || "retweetci" in lower -> TaskType.RETWEETER_FOLLOW
        "alıntıcı" in lower || "alintici" in lower -> TaskType.QUOTER_FOLLOW
        "takip etmeyen" in lower -> TaskType.UNFOLLOW_NON_FOLLOWERS
        "takipten" in lower || "unfollow" in lower -> TaskType.UNFOLLOW
        "beğeni" in lower || "begeni" in lower -> TaskType.LIKE
        "retweet" in lower -> TaskType.RETWEET
        "kaydet" in lower -> TaskType.BOOKMARK
        "alıntı" in lower || "alinti" in lower -> TaskType.QUOTE
        "yorum" in lower -> TaskType.COMMENT
        "tweet" in lower -> TaskType.TEXT_TWEET
        "takip" in lower -> TaskType.FOLLOW
        else -> null
    }

    fun parseModel(raw: String): PilotPlan? {
        val body = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = body.indexOf('{')
        val end = body.lastIndexOf('}')
        val clean = if (start >= 0 && end > start) body.substring(start, end + 1) else body
        if (!clean.startsWith("{")) return null
        fun text(name: String) = Regex(""""$name"\s*:\s*"([^"]*)"""").find(clean)?.groupValues?.get(1)?.trim()
        val say = text("say").orEmpty()
        if (say.isBlank()) return null
        if (listOf("kota", "quota", "resource_exhausted", "rate limit").any { say.contains(it, true) }) return null
        val command = runCatching { Command.valueOf(text("command")?.ifBlank { "NONE" } ?: "NONE") }.getOrDefault(Command.NONE)
        val type = text("taskType")?.takeIf { it.isNotBlank() && it != "null" }?.let { runCatching { TaskType.valueOf(it) }.getOrNull() }
        val tweets = Regex(""""tweets"\s*:\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL).find(clean)?.groupValues?.get(1)
            ?.let { body -> Regex(""""([^"]*)"""").findAll(body).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.map { it.take(280) }.toList() }
            .orEmpty()
        return PilotPlan(
            say = say.take(500),
            command = command,
            taskType = type,
            account = text("account")?.removePrefix("@")?.trim()?.takeIf { it.isNotBlank() && it != "null" },
            all = Regex(""""all"\s*:\s*true""").containsMatchIn(clean),
            tweets = tweets,
        )
    }
}

internal data class PilotPlan(
    val say: String,
    val command: PilotPolicy.Command = PilotPolicy.Command.NONE,
    val taskType: TaskType? = null,
    val account: String? = null,
    val all: Boolean = false,
    val tweets: List<String> = emptyList(),
)

internal object PilotBoard {
    @Volatile var lastMove: String = ""
}

/** After a free-tier limit, stay local until the next day and never surface the limit. */
internal object PilotQuota {
    @Volatile private var blockedDay: String? = null

    fun blocked(today: String = LocalDate.now().toString()): Boolean = blockedDay == today

    fun block(today: String = LocalDate.now().toString()) { blockedDay = today }

    fun clear() { blockedDay = null }
}
