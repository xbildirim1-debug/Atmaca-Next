package com.atmacanext.app.ai

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.Locale

data class TweetGenerationRequest(val concept: String, val count: Int) {
    companion object {
        const val MAX_COUNT = 1_000
        fun parse(raw: String): TweetGenerationRequest? {
            val lower = raw.lowercase(Locale.forLanguageTag("tr"))
            val noun = "(?:tweet|twit|twet|metin|gönderi|yazı)"
            val wantsText = Regex("$noun|havuz").containsMatchIn(lower)
            val creates = Regex("üret|uret|oluştur|olustur|ekle|hazırla|hazirla|yaz").containsMatchIn(lower)
            if (!wantsText || !creates) return null
            val count = Regex("(\\d{1,5})\\s*(?:tane|adet)?\\s*$noun").find(lower)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("$noun\\s*(?:üret|uret|ekle|yaz)?\\s*(\\d{1,5})").find(lower)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("(\\d{1,5})\\s*(?:tane|adet)").find(lower)?.groupValues?.get(1)?.toIntOrNull()
                ?: 6
            return TweetGenerationRequest(raw.trim().take(2_000), count)
        }
    }
}

object TweetPoolPolicy {
    const val MAX_LINES = 10_000
    data class Merge(val lines: List<String>, val added: List<String>)
    fun normalized(value: String): String = value.replace(Regex("[\\r\\n\\t]+"), " ")
        .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
        .replace(Regex("\\s+"), " ").trim()
    fun valid(value: String): Boolean = value.isNotBlank() && value.codePointCount(0, value.length) <= 280
    fun key(value: String): String = normalized(value).lowercase(Locale.ROOT)
    fun merge(existing: List<String>, incoming: List<String>, capacity: Int = MAX_LINES): Merge {
        val kept = existing.take(capacity.coerceAtLeast(0))
        val keys = kept.map(::key).toMutableSet()
        val added = incoming.asSequence().map(::normalized).filter(::valid)
            .filter { keys.add(key(it)) }.take((capacity - kept.size).coerceAtLeast(0)).toList()
        return Merge(kept + added, added)
    }
}

/** Each completed batch is saved before requesting the next; cancellation never fabricates a count. */
class TweetBatchGeneration(
    private val produce: suspend (concept: String, count: Int, batch: Int, avoid: List<String>) -> List<String>,
    private val save: suspend (List<String>) -> List<String>,
) {
    data class Result(val requested: Int, val saved: Int)
    suspend fun generate(request: TweetGenerationRequest, existing: List<String>, progress: (Int, Int) -> Unit): Result {
        require(request.count in 1..TweetGenerationRequest.MAX_COUNT) { "Tek istekte 1–1000 metin üretebilirsin" }
        require(request.concept.isNotBlank()) { "Tweet konsepti boş" }
        require(existing.size + request.count <= TweetPoolPolicy.MAX_LINES) { "Tweet havuzunda bu istek için yeterli yer yok" }
        val seen = existing.map(TweetPoolPolicy::key).toMutableSet()
        val recent = existing.takeLast(12).toMutableList()
        var saved = 0
        var batch = 0
        var emptyBatches = 0
        progress(0, request.count)
        while (saved < request.count) {
            currentCoroutineContext().ensureActive()
            val wanted = minOf(BATCH_SIZE, request.count - saved)
            val raw = produce(request.concept, wanted, ++batch, recent.takeLast(12))
            currentCoroutineContext().ensureActive()
            val batchKeys = HashSet<String>()
            val unique = raw.map(TweetPoolPolicy::normalized).filter(TweetPoolPolicy::valid)
                .filter { TweetPoolPolicy.key(it) !in seen && batchKeys.add(TweetPoolPolicy.key(it)) }.take(wanted)
            // The storage port reports the actual admitted texts, including a concurrent pool update.
            val admittedKeys = HashSet<String>()
            val offeredKeys = unique.map(TweetPoolPolicy::key).toSet()
            val stored = save(unique).filter { TweetPoolPolicy.key(it) in offeredKeys &&
                TweetPoolPolicy.key(it) !in seen && admittedKeys.add(TweetPoolPolicy.key(it)) }.take(wanted)
            stored.forEach { seen += TweetPoolPolicy.key(it) }
            saved += stored.size
            recent += stored
            progress(saved, request.count)
            if (stored.isEmpty()) {
                if (++emptyBatches >= 3) error("Yeni ve geçerli metin alınamadı; $saved/${request.count} metin kaydedildi")
            } else emptyBatches = 0
        }
        return Result(request.count, saved)
    }
    companion object { const val BATCH_SIZE = 50 }
}
