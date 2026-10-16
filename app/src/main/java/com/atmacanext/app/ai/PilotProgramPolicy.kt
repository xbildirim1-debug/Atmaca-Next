package com.atmacanext.app.ai

/** Program commands never need a conversational API request, even after a Gemini quota error. */
object PilotProgramPolicy {
    fun examples(raw: String): PilotAction? {
        val s = PilotIntentPolicy.normalized(raw).trim().trimEnd('.', '!', '?')
        if (Regex("\\b(uret|olustur|hazirla|ekle|sil|tasi|aktar|kopyala|baslat|gosterme|listeleme|okuma)\\b").containsMatchIn(s)) return null
        if (Regex("\\byaz\\b").containsMatchIn(s) && !Regex("kayitli|havuzdaki|onceden|son ekledigin|son uretilen").containsMatchIn(s)) return null
        val pools = PilotIntentPolicy.pools(s)
        if (pools.size > 1) return null
        val explicit = pools.singleOrNull() ?: if (Regex("\\byorum(?:u|lar|lari|lardan|lardan)?\\b").containsMatchIn(s)) ContentPool.REPLY else null
        val brief = s.matches(Regex("(?:bir ?kac |[0-9]+ (?:tane )?)?ornek(?:ler)?(?: neler var| ne var| goster|leri goster| ver|leri ver)?")) ||
            s in setOf("neler kaydettin", "kaydettiklerini goster", "son eklenenleri goster", "son urettiklerini goster", "icinde neler var", "nasil yorumlar var")
        val read = Regex("ornek|goster|listele|\\boku\\b|neler var|ne var|icindekiler|kayitli.*yaz|havuzdaki.*yaz").containsMatchIn(s)
        val stored = Regex("havuz|kayitli metin|\\bmetin(?:ler|leri)?\\b|\\bmetni\\b").containsMatchIn(s)
        if (!brief && !((explicit != null || stored) && read)) return null
        val count = Regex("(?:\\bson\\s+)?(-?[0-9]+)\\s*(?:tane|adet)?\\s*(?:ornek|orneg|hazir yorum|yorum|tweet|twit|metin|metn)")
            .find(s)?.groupValues?.get(1)?.toLongOrNull()?.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())?.toInt() ?: 5
        return PilotAction(PilotTool.SHOW_POOL, pool = explicit, count = count)
    }

    fun recentPool(receipt: String?): ContentPool? = ContentPool.entries
        .filter { receipt?.contains(it.title) == true }.singleOrNull()

    fun selectPool(explicit: ContentPool?, remembered: ContentPool?, tweetCount: Int, replyCount: Int): ContentPool? =
        explicit ?: remembered ?: when {
            tweetCount > 0 && replyCount == 0 -> ContentPool.TWEET
            replyCount > 0 && tweetCount == 0 -> ContentPool.REPLY
            else -> null
        }

    /** Only actual saved values are shown. Reading does not advance the publishing cursor or edit a pool. */
    fun show(pool: ContentPool, values: List<String>, requested: Int = 5): String {
        require(requested in 1..10_000) { "Gösterilecek metin adedini 1–10000 arasında belirt." }
        if (values.isEmpty()) return "${pool.title} havuzu boş; kayıtlı metin yok."
        val selected = values.takeLast(requested.coerceAtMost(20))
        return buildString {
            append("${pool.title}: toplam ${values.size} kayıt. Kayıtlı son ${selected.size} metin:\n\n")
            selected.forEachIndexed { i, value -> append("${values.size - selected.size + i + 1}. $value\n\n") }
            if (requested > 20) append("Bir yanıtta en fazla 20 kayıt gösteriliyor.")
        }.trim()
    }

    fun clarification(raw: String): String? {
        val s = PilotIntentPolicy.normalized(raw)
        if (Regex("\\b(gorev\\w*|otomasyon\\w*|kuyruk\\w*)\\b").containsMatchIn(s) &&
            Regex("baslat|calistir|basla").containsMatchIn(s)) return "Hangi kayıtlı program görevini başlatayım? Görev adını ve varsa hesabı belirt."
        if (Regex("havuz|hazir yorum|kayitli metin").containsMatchIn(s) &&
            Regex("ornek|goster|listele|tasi|sil|kopyala").containsMatchIn(s)) return "Hangi havuzu kullanayım: tweet metinleri mi, hazır yorum metinleri mi? İstediğin işlemi ve varsa adedi belirt."
        if (Regex("\\b(gorev\\w*|otomasyon\\w*|kuyruk\\w*)\\b").containsMatchIn(s) &&
            Regex("durdur|duraklat|devam|atla|goster|listele|sayisi").containsMatchIn(s)) return "Program görevi için durdur, duraklat, devam et, görevi atla veya görevleri göster komutunu kullanabilirsin. Hiçbir görev değiştirilmedi."
        if (Regex("onayli|yorumcu|retweetci|takip|hesap|begeni|yer imi|alinti").containsMatchIn(s) &&
            Regex("\\b(baslat|calistir|durdur|duraklat|goster|listele|atla)\\b").containsMatchIn(s))
            return "Hangi kayıtlı program görevinde işlem yapacağımı görev adıyla belirt. Kayıtlı görevleri görmek için görevleri göster yazabilirsin."
        return null
    }

    /** The conversation model can propose content generation; it cannot execute program tools. */
    fun acceptsNetworkPlan(plan: PilotToolPlan): Boolean = plan.actions.all { it.tool == PilotTool.GENERATE }
}
