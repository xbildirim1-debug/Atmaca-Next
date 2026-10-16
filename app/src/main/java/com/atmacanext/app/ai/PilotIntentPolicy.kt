package com.atmacanext.app.ai

import com.atmacanext.app.domain.model.TaskType
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

enum class PilotTool { GENERATE, TRANSFER, COPY, DELETE_LAST, UNDO_POOL, POOLS, SHOW_POOL, TASKS, REPORT, STATUS,
    START, STOP, PAUSE, RESUME, SKIP_TASK, SKIP_ACCOUNT, STOP_GENERATION }
data class PilotAction(val tool: PilotTool, val pool: ContentPool? = null, val destination: ContentPool? = null,
    val count: Int? = null, val concept: String = "", val types: List<TaskType> = emptyList(),
    val account: String? = null, val all: Boolean = false)
data class PilotToolPlan(val say: String = "", val actions: List<PilotAction> = emptyList())

/** Program operations stay local; Gemini only handles conversation, images and new content. */
object PilotIntentPolicy {
    fun normalized(raw: String): String = Normalizer.normalize(raw, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace('ı', 'i')
    private val tweet = Regex("tweet|twit|twet|toplu metin|gonderi met")
    private val reply = Regex("hazir yorum|yorum met|yorum hav|yanit met|yanit hav")
    fun pools(raw: String): List<ContentPool> {
        val s = normalized(raw)
        return listOfNotNull(tweet.find(s)?.range?.first?.let { it to ContentPool.TWEET },
            reply.find(s)?.range?.first?.let { it to ContentPool.REPLY }).sortedBy { it.first }.map { it.second }
    }
    private fun number(s: String): Int? = Regex("(?<![0-9])([0-9]+)\\s*(?:tane|adet)?\\s*(?:hazir\\s+)?(?:tweet|twit|twet|metin|metn|yorum|gonderi|yazi)")
        .find(s)?.groupValues?.get(1)?.toLongOrNull()?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
        ?: Regex("\\bson\\s+(?:ekledigin\\s+)?([0-9]+)\\b").find(s)?.groupValues?.get(1)?.toIntOrNull()
    private fun negative(s: String): Boolean = Regex("\\b(yapma|silme|tasima|kopyalama|uretme|ekleme|durdurma|baslatma|istemiyorum)\\b").containsMatchIn(s)
    fun local(raw: String): PilotToolPlan? {
        val s = normalized(raw).trim().trimEnd('.', '!', '?')
        if (Regex("\\b(geri al|undo)\\b").containsMatchIn(s)) return PilotToolPlan(actions = listOf(PilotAction(PilotTool.UNDO_POOL)))
        if (negative(s)) return null
        PilotProgramPolicy.examples(raw)?.let { return PilotToolPlan(actions = listOf(it)) }
        val selected = pools(s); val count = number(s)
        val transfer = Regex("\\b(tasi|aktar|transfer|move|kopyala)\\b").containsMatchIn(s) ||
            (Regex("\\b(sil|cikar)\\b").containsMatchIn(s) && Regex("\\bekle\\b").containsMatchIn(s))
        if (transfer && selected.size == 2 && count != null) return PilotToolPlan(actions = listOf(
            PilotAction(if ("kopyala" in s) PilotTool.COPY else PilotTool.TRANSFER, selected[0], selected[1], count)))
        if (selected.size == 1 && count != null && Regex("\\b(sil|cikar)\\b").containsMatchIn(s))
            return PilotToolPlan(actions = listOf(PilotAction(PilotTool.DELETE_LAST, selected.single(), count = count)))
        val creates = Regex("\\b(uret|olustur|hazirla|yaz|ekle)\\b").containsMatchIn(s)
        if (!transfer && selected.size <= 1 && creates && count != null &&
            Regex("\\b(sil|cikar|tasi|aktar)\\b").containsMatchIn(s).not()) {
            return PilotToolPlan(actions = listOf(PilotAction(PilotTool.GENERATE,
                selected.singleOrNull() ?: ContentPool.TWEET, count = count, concept = raw)))
        }
        if (("uretim" in s || "metin" in s) && Regex("\\b(durdur|iptal et)\\b").containsMatchIn(s))
            return PilotToolPlan(actions = listOf(PilotAction(PilotTool.STOP_GENERATION)))
        if (s.matches(Regex("(?:(?:tum )?(?:gorevleri|gorevi|programi|otomasyonu) )?(?:durdur|dur|stop)")))
            return PilotToolPlan(actions = listOf(PilotAction(PilotTool.STOP)))
        if (s.matches(Regex("(?:(?:gorevi|gorevleri|otomasyonu) )?duraklat"))) return PilotToolPlan(actions = listOf(PilotAction(PilotTool.PAUSE)))
        if (s.matches(Regex("(?:(?:goreve|gorevlere|kaldigin yerden|kaldigi yerden) )?(?:devam et|surdur|resume)")))
            return PilotToolPlan(actions = listOf(PilotAction(PilotTool.RESUME)))
        if (s.matches(Regex("(?:siradaki|sonraki) hesaba gec|hesabi atla"))) return PilotToolPlan(actions = listOf(PilotAction(PilotTool.SKIP_ACCOUNT)))
        if (s.matches(Regex("(?:siradaki|sonraki) goreve gec|gorevi atla"))) return PilotToolPlan(actions = listOf(PilotAction(PilotTool.SKIP_TASK)))
        if (Regex("kayit|log|hata|sikinti|takil|rapor").containsMatchIn(s) &&
            Regex("incele|analiz|en cok|en sik|neler|neden|rapor|duzelt").containsMatchIn(s))
            return PilotToolPlan(actions = listOf(PilotAction(PilotTool.REPORT)))
        if (Regex("gorev|kuyruk|otomasyon").containsMatchIn(s) && Regex("neden|takil|hata").containsMatchIn(s))
            return PilotToolPlan(actions = listOf(PilotAction(PilotTool.REPORT)))
        if (Regex("gorev(?:ler|leri)?\\b").containsMatchIn(s) && Regex("goster|listele|neler var|hangileri|kac").containsMatchIn(s))
            return PilotToolPlan(actions = listOf(PilotAction(PilotTool.TASKS)))
        if (Regex("havuz|metin").containsMatchIn(s) && Regex("kac|goster|sayisi|durum").containsMatchIn(s))
            return PilotToolPlan(actions = listOf(PilotAction(PilotTool.POOLS)))
        if (s.matches(Regex("durum|durum ne|ne durumda|ne yapiyorsun|hangi gorevdesin")))
            return PilotToolPlan(actions = listOf(PilotAction(PilotTool.STATUS)))
        if (Regex("\\b(baslat|calistir|basla)\\b").containsMatchIn(s)) {
            val types = listOf("hesaplar arasi" to TaskType.ACCOUNT_ENGAGEMENT, "onayli" to TaskType.VERIFIED_FOLLOW, "yorumcu" to TaskType.COMMENTER_FOLLOW,
                "retweetci" to TaskType.RETWEETER_FOLLOW, "takip etmeyen" to TaskType.UNFOLLOW_NON_FOLLOWERS,
                "takipten" to TaskType.UNFOLLOW, "tweet" to TaskType.TEXT_TWEET)
                .mapNotNull { (word, type) -> Regex("\\b" + Regex.escape(word) + "\\b").find(s)?.range?.first?.let { it to type } }.sortedBy { it.first }.map { it.second }.distinct()
            val all = Regex("hepsini|tum gorev").containsMatchIn(s)
            if (types.isNotEmpty() || all) return PilotToolPlan(actions = listOf(PilotAction(PilotTool.START,
                types = types, account = Regex("@([a-z0-9_]{1,15})").find(s)?.groupValues?.get(1), all = all)))
        }
        return null
    }

    fun parseModel(raw: String): PilotToolPlan? = runCatching {
        val obj = JSONObject(raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
        val array = obj.optJSONArray("actions")
        require((array?.length() ?: 0) <= 4) { "En çok dört işlem" }
        val actions = (0 until (array?.length() ?: 0)).map { i ->
            val a = array!!.getJSONObject(i)
            val tool = PilotTool.valueOf(a.getString("tool"))
            val pool = a.optString("pool").takeIf { it.isNotBlank() && it != "null" }?.let(ContentPool::valueOf)
            val destination = a.optString("destination").takeIf { it.isNotBlank() && it != "null" }?.let(ContentPool::valueOf)
            val count = if (a.has("count") && !a.isNull("count")) {
                val n = a.get("count"); require(n is Number && n.toString().matches(Regex("[0-9]+")))
                n.toString().toInt().also { require(it in 1..10_000) }
            } else null
            if (tool in setOf(PilotTool.GENERATE, PilotTool.DELETE_LAST, PilotTool.COPY, PilotTool.TRANSFER)) require(pool != null && count != null)
            if (tool in setOf(PilotTool.TRANSFER, PilotTool.COPY)) require(destination != null && destination != pool)
            if (tool == PilotTool.GENERATE) require(count!! <= TweetGenerationRequest.MAX_COUNT && a.optString("concept").isNotBlank())
            val types = a.optJSONArray("types")
            val taskTypes = (0 until (types?.length() ?: 0)).map { TaskType.valueOf(types!!.getString(it)) }.distinct()
            require(taskTypes.none { it == TaskType.SYNC })
            val account = a.optString("account").removePrefix("@").takeIf { it.isNotBlank() && it != "null" }
            require(account == null || account.matches(Regex("[A-Za-z0-9_]{1,15}")))
            PilotAction(tool, pool, destination, count, a.optString("concept").take(2_000), taskTypes, account, a.optBoolean("all", false))
        }.distinct()
        PilotToolPlan(obj.optString("say").take(5_000), actions)
    }.getOrNull()

    /** Model suggestions never receive authority from a screenshot or an older chat message. */
    fun authorized(raw: String, action: PilotAction): Boolean {
        val s = normalized(raw)
        if (action.tool == PilotTool.UNDO_POOL) return "geri al" in s || "undo" in s
        if (negative(s)) return false
        if (action.tool in setOf(PilotTool.GENERATE, PilotTool.TRANSFER, PilotTool.COPY, PilotTool.DELETE_LAST)) {
            val explicitCount = number(s)
            if (explicitCount != null && explicitCount != action.count) return false
            val mentioned = pools(s)
            if (mentioned.size == 1 && action.pool != mentioned.single()) return false
            if (mentioned.size == 2 && setOf(action.pool, action.destination) != mentioned.toSet()) return false
        }
        return when (action.tool) {
            PilotTool.SHOW_POOL -> PilotProgramPolicy.examples(raw)?.let { requested ->
                (requested.pool == null || requested.pool == action.pool) && requested.count == action.count
            } == true
            PilotTool.GENERATE -> Regex("\\b(uret|olustur|hazirla|yaz|ekle)\\b").containsMatchIn(s) &&
                !Regex("\\b(sil|tasi|aktar|cikar)\\b").containsMatchIn(s)
            PilotTool.TRANSFER, PilotTool.COPY -> Regex("\\b(tasi|aktar|kopyala|transfer|move)\\b").containsMatchIn(s) ||
                (Regex("\\b(sil|cikar)\\b").containsMatchIn(s) && Regex("\\bekle\\b").containsMatchIn(s))
            PilotTool.DELETE_LAST -> Regex("\\b(sil|cikar)\\b").containsMatchIn(s)
            PilotTool.UNDO_POOL -> "geri al" in s || "undo" in s
            PilotTool.START -> Regex("\\b(baslat|calistir|basla)\\b").containsMatchIn(s)
            PilotTool.STOP -> Regex("\\b(durdur|stop|dur)\\b").containsMatchIn(s)
            PilotTool.PAUSE -> "duraklat" in s
            PilotTool.RESUME -> Regex("\\b(devam et|surdur|resume)\\b").containsMatchIn(s) && !s.contains("neden")
            PilotTool.SKIP_TASK, PilotTool.SKIP_ACCOUNT -> Regex("\\b(atla|gec)\\b").containsMatchIn(s)
            PilotTool.STOP_GENERATION -> Regex("\\b(durdur|iptal et)\\b").containsMatchIn(s)
            else -> true
        }
    }
    fun honestChat(text: String): String = if (Regex("\\b(kaydettim|tasidim|sildim|baslattim|talimatlari guncelliyorum|kategorileri ayiriyorum)\\b")
        .containsMatchIn(normalized(text))) "Bu yanıtta bir değişiklik uygulanmadı. Yapılacak işlemi ve hedef havuzu netleştirelim."
        else text
}
