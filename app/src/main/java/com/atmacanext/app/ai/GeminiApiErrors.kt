package com.atmacanext.app.ai

import org.json.JSONObject
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

enum class GeminiLimitKind { SHORT_TERM, DAILY, UNAVAILABLE, UNKNOWN }

data class GeminiLimit(val kind: GeminiLimitKind, val retryAtMs: Long, val automaticRetry: Boolean) {
    fun exception(now: Long): GeminiApiException {
        val remaining = (retryAtMs - now).coerceAtLeast(0)
        val reason = when (kind) {
            GeminiLimitKind.SHORT_TERM -> "Gemini kısa süreli kullanım sınırına ulaştı (429). En az ${(remaining + 999) / 1000} saniye sonra yeniden denenebilir."
            GeminiLimitKind.DAILY -> "Gemini günlük kullanım sınırına ulaştı (429). Günlük sıfırlama: ${Instant.ofEpochMilli(retryAtMs).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))}."
            GeminiLimitKind.UNAVAILABLE -> "Gemini seçili model için kullanılabilir kota olmadığını bildirdi (429, limit 0). Ayarlar > Gemini kullanım sınırları bölümünden proje/model kotasını kontrol et."
            GeminiLimitKind.UNKNOWN -> "Gemini kullanım sınırı bildirdi (429). Sınırın süresi yanıtta belirtilmedi. Ayarlar > Gemini kullanım sınırları bölümünden proje/model kotasını kontrol et."
        }
        return GeminiApiException(429, "$reason Yerel görev denetimi ve havuz işlemleri çalışıyor.", remaining, automaticRetry, this)
    }
}

class GeminiApiException(val statusCode: Int, message: String, val retryAfterMs: Long = 20_000L,
    val automaticRetry: Boolean = statusCode == 503, val limit: GeminiLimit? = null) : IllegalStateException(message)

/** Parse the full structured error before formatting it. Never infer a daily quota from HTTP 429 alone. */
object GeminiApiErrors {
    fun parse(code: Int, body: String, prefix: String, apiKey: String, retryAfter: String? = null,
              now: Long = System.currentTimeMillis()): GeminiApiException {
        val error = runCatching { JSONObject(body).optJSONObject("error") }.getOrNull()
        val reason = error?.optString("message").orEmpty()
        val delays = mutableListOf<Long>()
        headerDelay(retryAfter, now)?.let(delays::add)
        val quotaIds = mutableListOf<String>()
        var zero = false
        val details = error?.optJSONArray("details")
        for (i in 0 until (details?.length() ?: 0)) {
            val detail = details!!.optJSONObject(i) ?: continue
            if (detail.optString("@type").endsWith("google.rpc.RetryInfo")) {
                duration(detail.optString("retryDelay"))?.let(delays::add)
            }
            if (!detail.optString("@type").endsWith("google.rpc.QuotaFailure")) continue
            val violations = detail.optJSONArray("violations") ?: continue
            for (j in 0 until violations.length()) {
                val violation = violations.optJSONObject(j) ?: continue
                quotaIds += violation.optString("quotaId") + " " + violation.optString("quotaMetric")
                if (violation.optString("quotaValue").toDoubleOrNull() == 0.0) zero = true
            }
        }
        val delay = delays.maxOrNull()
        if (code == 429) {
            val evidence = (quotaIds + reason).joinToString(" ").lowercase(java.util.Locale.ROOT)
            zero = zero || Regex("\\blimit\\s*:\\s*0(?:\\D|$)").containsMatchIn(evidence)
            val daily = Regex("perday|per_day|per day|daily").containsMatchIn(evidence)
            val kind = when {
                zero -> GeminiLimitKind.UNAVAILABLE
                daily -> GeminiLimitKind.DAILY
                delay != null -> GeminiLimitKind.SHORT_TERM
                else -> GeminiLimitKind.UNKNOWN
            }
            val wait = when (kind) {
                GeminiLimitKind.DAILY -> maxOf(nextDailyReset(now) - now, delay ?: 0)
                GeminiLimitKind.UNAVAILABLE -> maxOf(10 * 60_000L, delay ?: 0)
                else -> delay ?: 60_000L
            }
            return GeminiLimit(kind, now + wait, kind == GeminiLimitKind.SHORT_TERM).exception(now)
        }
        if (code == 503) return GeminiApiException(code, "Gemini sunucusu geçici olarak kullanılamıyor (503).", delay ?: 20_000L, true)
        val safeReason = if (apiKey.isBlank()) reason else reason.replace(apiKey, "[anahtar]")
        return GeminiApiException(code, "$prefix ($code): ${safeReason.ifBlank { "sunucu hatası" }.take(220)}")
    }

    private fun duration(value: String): Long? = seconds(value.removeSuffix("s").takeIf { value.endsWith("s") })
    private fun seconds(value: String?): Long? = value?.trim()?.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0 && it <= 7 * 24 * 3600 }
        ?.let { ceil(it * 1000).toLong().coerceAtLeast(1_000) }
    private fun headerDelay(value: String?, now: Long): Long? = seconds(value) ?: runCatching {
        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
            .minus(now).coerceIn(1_000, 7 * 24 * 3600_000L)
    }.getOrNull()
    internal fun nextDailyReset(now: Long): Long = Instant.ofEpochMilli(now)
        .atZone(ZoneId.of("America/Los_Angeles")).toLocalDate().plusDays(1)
        .atStartOfDay(ZoneId.of("America/Los_Angeles")).toInstant().toEpochMilli()

    fun localExplanation(raw: String, lastFailure: String?): String? = lastFailure?.takeIf {
        PilotIntentPolicy.normalized(raw).trim().trimEnd('?', '.', '!') in setOf(
            "neden", "neden olmuyor", "neden hata veriyor", "bu hata ne", "bu hata neden", "kota ne demek")
    }

    /** A retry must never run before the server's delay or turn a daily/unknown quota into a retry loop. */
    fun generationRetryDelay(error: GeminiApiException, attempt: Int): Long? {
        if (!error.automaticRetry || attempt !in 1..2 || error.retryAfterMs > 60_000) return null
        return maxOf(error.retryAfterMs, 1_000L shl attempt)
    }
}

/** Shared by chat, generation, connection tests and background advice in this service instance. */
class GeminiRateLimitGate {
    private val limits = mutableMapOf<String, GeminiLimit>()
    private fun key(credential: String, model: String): String = MessageDigest.getInstance("SHA-256")
        .digest(credential.toByteArray()).joinToString("") { "%02x".format(it) } + ":" + model

    @Synchronized fun check(credential: String, model: String, now: Long = System.currentTimeMillis()) {
        val key = key(credential, model)
        val limit = limits[key] ?: return
        if (now < limit.retryAtMs) throw limit.exception(now)
        limits.remove(key)
    }
    @Synchronized fun record(credential: String, model: String, error: GeminiApiException) {
        val limit = error.limit ?: return
        val key = key(credential, model)
        if ((limits[key]?.retryAtMs ?: 0) <= limit.retryAtMs) limits[key] = limit
    }
    @Synchronized fun clear() { limits.clear() }
}
