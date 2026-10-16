package com.atmacanext.app.ai

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class GeminiQuota27_68Test {
    private val now = Instant.parse("2026-10-15T08:40:00Z").toEpochMilli()
    private fun response(id: String? = null, value: String? = null, retry: String? = null,
                         message: String = "You exceeded your current quota, please check your plan and billing details."): String {
        val details = JSONArray()
        if (id != null) details.put(JSONObject().put("@type", "type.googleapis.com/google.rpc.QuotaFailure")
            .put("violations", JSONArray().put(JSONObject().put("quotaId", id).apply { if (value != null) put("quotaValue", value) })))
        if (retry != null) details.put(JSONObject().put("@type", "type.googleapis.com/google.rpc.RetryInfo").put("retryDelay", retry))
        return JSONObject().put("error", JSONObject().put("message", message).put("status", "RESOURCE_EXHAUSTED").put("details", details)).toString()
    }
    private fun parse(body: String, header: String? = null, code: Int = 429) =
        GeminiApiErrors.parse(code, body, "Gemini isteği başarısız", "private-api-key", header, now)

    @Test fun screenshotWithoutDetailsDoesNotInventADailyResetOrRetryLoop() {
        val e = parse(response())
        assertEquals(GeminiLimitKind.UNKNOWN, e.limit!!.kind)
        assertFalse(e.automaticRetry); assertEquals(60_000L, e.retryAfterMs)
        assertTrue(e.message!!.contains("süresi yanıtta belirtilmedi"))
        assertFalse(e.message!!.contains("billing")); assertFalse(e.message!!.contains("You exceeded"))
    }
    @Test fun fullDailyDetailAfterALongMessageIsReadBeforeDisplayTruncation() {
        val e = parse(response("GenerateRequestsPerDayPerProjectPerModel-FreeTier", "20", "12s", "x".repeat(500)))
        assertEquals(GeminiLimitKind.DAILY, e.limit!!.kind); assertFalse(e.automaticRetry)
        assertEquals(Instant.parse("2026-10-16T07:00:00Z").toEpochMilli(), e.limit!!.retryAtMs)
        assertNull(GeminiApiErrors.generationRetryDelay(e, 1))
    }
    @Test fun zeroDailyQuotaIsUnavailableAndNotAFakeTomorrowPromise() {
        val e = parse(response("GenerateRequestsPerDayPerProjectPerModel-FreeTier", "0", "30s"))
        assertEquals(GeminiLimitKind.UNAVAILABLE, e.limit!!.kind); assertFalse(e.automaticRetry)
        assertTrue(e.message!!.contains("limit 0")); assertFalse(e.message!!.contains("sıfırlama"))
    }
    @Test fun dailyResetUsesPacificWinterOffset() {
        val winter = Instant.parse("2026-12-15T08:40:00Z").toEpochMilli()
        assertEquals(Instant.parse("2026-12-16T08:00:00Z").toEpochMilli(), GeminiApiErrors.nextDailyReset(winter))
    }
    @Test fun fractionalRetryInfoIsRoundedUpNotShortened() {
        val e = parse(response("GenerateRequestsPerMinutePerProjectPerModel-FreeTier", "20", "45.06576809s"))
        assertEquals(GeminiLimitKind.SHORT_TERM, e.limit!!.kind); assertTrue(e.automaticRetry)
        assertEquals(45_066L, e.retryAfterMs)
        assertEquals(45_066L, GeminiApiErrors.generationRetryDelay(e, 1))
    }
    @Test fun longestServerDelayWinsAcrossHeaderAndBody() {
        val e = parse(response(retry = "3s"), "45")
        assertEquals(45_000L, e.retryAfterMs)
    }
    @Test fun retryAfterHttpDateIsRespected() {
        assertEquals(30_000L, parse(response(), "Thu, 15 Oct 2026 08:40:30 GMT").retryAfterMs)
    }
    @Test fun aLongServerDelayIsNotClampedToAnEarlyOneMinuteRetry() {
        val e = parse(response(retry = "120s"))
        assertEquals(120_000L, e.retryAfterMs)
        assertNull(GeminiApiErrors.generationRetryDelay(e, 1))
    }
    @Test fun malformedResponseStillGivesSafeTurkishQuotaNotice() {
        val e = parse("not JSON private-api-key")
        assertEquals(GeminiLimitKind.UNKNOWN, e.limit!!.kind)
        assertFalse(e.message!!.contains("private-api-key"))
    }
    @Test fun nonQuotaErrorsKeepStatusAndRedactTheCredential() {
        val e = parse(response(message = "Invalid private-api-key"), code = 403)
        assertEquals(403, e.statusCode); assertNull(e.limit); assertFalse(e.automaticRetry)
        assertTrue(e.message!!.contains("[anahtar]")); assertFalse(e.message!!.contains("private-api-key"))
    }
    @Test fun temporaryUnavailableUsesBoundedExponentialRetry() {
        val e = parse(response(), "1", 503)
        assertEquals(2_000L, GeminiApiErrors.generationRetryDelay(e, 1))
        assertEquals(4_000L, GeminiApiErrors.generationRetryDelay(e, 2))
        assertNull(GeminiApiErrors.generationRetryDelay(e, 3))
    }
    @Test fun negativeAndOverflowingRetryHintsNeverStartAQuickLoop() {
        for (value in listOf("-3s", "Infinitys", "9999999999999999999999s", "abc")) {
            assertFalse(parse(response(retry = value)).automaticRetry)
        }
    }
    @Test fun gateBlocksOtherCallersUntilTheSameModelsServerDelay() {
        val gate = GeminiRateLimitGate()
        gate.record("key", "model", parse(response(retry = "45s")))
        try { gate.check("key", "model", now + 1_000); fail("Quota gate allowed an early request") }
        catch (e: GeminiApiException) { assertEquals(44_000L, e.retryAfterMs) }
        gate.check("key", "model", now + 45_000)
    }
    @Test fun unrelatedModelAndChangedCredentialAreNotBlockedByStaleState() {
        val gate = GeminiRateLimitGate()
        gate.record("key", "model", parse(response()))
        gate.check("key", "other", now); gate.check("new-key", "model", now)
        gate.clear(); gate.check("key", "model", now)
    }
    @Test fun shorterLateResponseCannotOverwriteALongerQuotaBlock() {
        val gate = GeminiRateLimitGate()
        gate.record("key", "model", parse(response("RequestsPerDay", "20")))
        gate.record("key", "model", parse(response(retry = "1s")))
        try { gate.check("key", "model", now + 60_000); fail("Daily quota block lost") }
        catch (e: GeminiApiException) { assertEquals(GeminiLimitKind.DAILY, e.limit!!.kind) }
    }
    @Test fun whyAfterAQuotaErrorIsExplainedLocally() {
        val reason = parse(response()).message!!
        assertEquals(reason, GeminiApiErrors.localExplanation("neden?", reason))
        assertNull(GeminiApiErrors.localExplanation("neden", null))
        assertNull(GeminiApiErrors.localExplanation("100 yorum hazırla", reason))
    }
    @Test fun screenshotCommandChoosesOneHundredRepliesWithoutAPlannerRequest() {
        val raw = "100 tane hazır yorum metni hazırla ekle. şaşırma ve merak içerikli kısa"
        val a = PilotIntentPolicy.local(raw)!!.actions.single()
        assertEquals(PilotTool.GENERATE, a.tool); assertEquals(100, a.count); assertEquals(ContentPool.REPLY, a.pool)
        assertTrue(PilotIntentPolicy.authorized(raw, a)); assertFalse(PilotIntentPolicy.authorized(raw, a.copy(count = 6)))
    }
    @Test fun preparedReplyCountAlsoSupportsAdetAndNoUnit() {
        for (raw in listOf("100 adet hazır yorum oluştur", "100 hazır yorum metni yaz")) {
            val a = PilotIntentPolicy.local(raw)!!.actions.single()
            assertEquals(100, a.count); assertEquals(ContentPool.REPLY, a.pool)
        }
    }
    @Test fun preparedReplyNegationDoesNotCreateAMutation() {
        assertNull(PilotIntentPolicy.local("100 tane hazır yorum üretme"))
    }
    @Test fun existingDirectControlsRemainLocalAfterQuotaFailure() {
        for ((raw, tool) in listOf("durdur" to PilotTool.STOP, "görevi duraklat" to PilotTool.PAUSE,
            "devam et" to PilotTool.RESUME, "havuz kaç metin" to PilotTool.POOLS)) {
            assertEquals(tool, PilotIntentPolicy.local(raw)!!.actions.single().tool)
        }
    }
}
