package com.atmacanext.app.ai

data class PilotGenerationContext(val pool: ContentPool, val concept: String)

/** Resolves references locally. An older message supplies a topic, never permission for program tools. */
object PilotGenerationFollowup {
    enum class Mode { ADD, TOTAL }
    data class Request(val context: PilotGenerationContext, val quantity: Int, val mode: Mode)
    data class Pending(val context: PilotGenerationContext, val quantity: Int)
    data class Turn(val mine: Boolean, val text: String, val hasImage: Boolean = false)
    sealed class Decision {
        data object None : Decision()
        data class Ask(val message: String, val pending: Pending? = null) : Decision()
        data class Ready(val request: Request) : Decision()
    }

    private val references = Regex("\\b(onlari|bunlari|onlardan|bunlardan|onlar|bunlar|ayni|onceki)\\b")
    private val verbs = Regex("\\b(yap|uret|olustur|hazirla|yaz|ekle|tamamla|cikar)\\b")
    private val blocked = Regex("\\b(gorev\\w*|kuyruk\\w*|otomasyon\\w*|hesap\\w*|takip\\w*|baslat|calistir|durdur|duraklat|sil|tasi|aktar|kopyala|geri al)\\b")
    private val negatives = Regex("\\b(yapma|uretme|olusturma|hazirlama|yazma|ekleme|tamamlama|cikarma|istemiyorum|iptal|vazgec|degil)\\b")
    private val digits = Regex("(?<![a-z0-9])(-?[0-9]+)(?![a-z0-9])")

    fun mentionedPool(raw: String): ContentPool? {
        val pools = PilotIntentPolicy.pools(raw)
        if (pools.size > 1) return null
        return pools.singleOrNull() ?: if (Regex("\\byorum\\w*\\b").containsMatchIn(PilotIntentPolicy.normalized(raw))) ContentPool.REPLY else null
    }

    fun resolve(raw: String, context: PilotGenerationContext?, pending: Pending? = null): Decision {
        val s = PilotIntentPolicy.normalized(raw).trim().trimEnd('.', '!', '?')
        if (blocked.containsMatchIn(s)) return Decision.None
        val shortAnswer = pending != null && s.matches(Regex("(?:[0-9]+\\s*(?:tane|adet)?\\s*)?(?:toplam(?:a)?(?:\\s+(?:tamamla|cikar|yap))?|(?:yeni|daha)(?:\\s+(?:uret|ekle|yap|hazirla))?)(?:\\s+(?:onlari|bunlari))?"))
        val total = Regex("\\b(toplam|tamamla|sayisini)\\b").containsMatchIn(s) ||
            Regex("[0-9]+\\s*['’]?\\s*(?:e|a)\\s+cikar\\b").containsMatchIn(s)
        val reference = references.containsMatchIn(s) || Regex("\\bdaha\\b").containsMatchIn(s)
        val candidate = shortAnswer || (digits.containsMatchIn(s) && verbs.containsMatchIn(s) &&
            (reference || total))
        if (!candidate) return Decision.None
        if (negatives.containsMatchIn(s) || '?' in raw || Regex("\\b(mi|mu|miyim|miyiz|neden|nasil|kac)\\b").containsMatchIn(s))
            return Decision.Ask("Üretim başlatılmadı. İstediğin adedi ve yeni ekleme mi, toplamı tamamlama mı olduğunu açıkça belirt.")
        val numbers = digits.findAll(s).toList()
        if (numbers.size > 1) return Decision.Ask("Tek bir adet belirt: kaç yeni metin ekleyeyim veya toplamı kaça tamamlayayım?")
        val quantity = numbers.singleOrNull()?.value?.toIntOrNull() ?: if (numbers.isEmpty() && shortAnswer) pending?.quantity else null
        if (quantity == null || quantity !in 1..TweetPoolPolicy.MAX_LINES)
            return Decision.Ask("Adedi 1–10000 arasında belirt. Tek üretimde en fazla 1000 yeni metin eklenebilir.")
        val selected = if (shortAnswer) pending?.context else context
        val explicit = mentionedPool(raw)
        if (PilotIntentPolicy.pools(raw).size > 1 || selected == null || selected.concept.isBlank() ||
            (explicit != null && explicit != selected.pool))
            return Decision.Ask("Hangi havuza, hangi konuda metin üreteyim? Örneğin: 1000 hazır yorum üret, kısa şaşkınlık ve merak içerikli olsun.")
        val mode = when {
            total -> Mode.TOTAL
            Regex("\\b(yeni|daha|uret|olustur|hazirla|yaz|ekle)\\b").containsMatchIn(s) -> Mode.ADD
            else -> null
        }
        if (mode == null) return Decision.Ask(
            "${selected.pool.title} için önceki konuyu kullanacağım. Toplamı $quantity metne mi tamamlayayım, $quantity yeni metin mi ekleyeyim? ‘Toplam’ veya ‘yeni ekle’ yazabilirsin.",
            Pending(selected, quantity))
        if (mode == Mode.ADD && quantity > TweetGenerationRequest.MAX_COUNT)
            return Decision.Ask("Tek istekte en fazla 1000 yeni metin üretebilirim. Daha küçük bir adet belirt.")
        return Decision.Ready(Request(selected, quantity, mode))
    }

    /** Read the real count immediately before starting. Reaching a target never deletes saved text. */
    fun action(request: Request, currentCount: Int): PilotAction? {
        require(currentCount in 0..TweetPoolPolicy.MAX_LINES)
        require(request.context.concept.isNotBlank())
        require(request.quantity in 1..TweetPoolPolicy.MAX_LINES)
        val count = if (request.mode == Mode.TOTAL) (request.quantity - currentCount).coerceAtLeast(0) else request.quantity
        if (count == 0) return null
        require(count <= TweetGenerationRequest.MAX_COUNT) { "Hedefe ulaşmak için $count yeni metin gerekiyor. Tek istekte en fazla 1000 yeni metin üretilebilir." }
        require(currentCount + count <= TweetPoolPolicy.MAX_LINES) { "Havuzda yeterli yer yok; kayıtlı metinler korundu." }
        return PilotAction(PilotTool.GENERATE, request.context.pool, count = count, concept = request.context.concept)
    }

    /** Upgrade recovery needs a user request followed by a saved/partially saved receipt, not a preview or a promise. */
    fun recover(pool: ContentPool, turns: List<Turn>): PilotGenerationContext? {
        var candidate: PilotGenerationContext? = null
        var recovered: PilotGenerationContext? = null
        for (turn in turns) {
            if (turn.mine) {
                if (turn.hasImage) { candidate = null; continue }
                val a = PilotIntentPolicy.local(turn.text)?.actions?.singleOrNull()
                if (a?.tool == PilotTool.GENERATE && a.pool == pool &&
                    a.count?.let { it in 1..TweetGenerationRequest.MAX_COUNT } == true && a.concept.isNotBlank())
                    candidate = PilotGenerationContext(pool, a.concept)
                else if (a?.tool == PilotTool.GENERATE) candidate = null
            } else if (candidate != null) {
                val s = PilotIntentPolicy.normalized(turn.text)
                val receipt = Regex("^(?:gemini uretimi (?:tamamlandi:|tamamlanamadi;)\\s*)?([0-9]+)\\s*/\\s*([0-9]+)\\s+(?:farkli\\s+)?metin\\s+")
                    .find(s)
                if (receipt != null && receipt.groupValues[1].toIntOrNull()?.let { it > 0 } == true &&
                    s.contains(PilotIntentPolicy.normalized(pool.title)) && Regex("kaydedildi|havuzunda korundu").containsMatchIn(s)) {
                    recovered = candidate; candidate = null
                }
            }
        }
        return recovered
    }
}
