package com.atmacanext.app.ai

import com.atmacanext.app.automation.ReplyTextPoolPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TweetBatch27_66Test {
    @Test fun requestedFiveHundredIsNotClampedToTwelve() {
        assertEquals(500, TweetGenerationRequest.parse("Tweet havuzuna futbol konseptinde 500 twit metni ekle")?.count)
        assertEquals(500, TweetGenerationRequest.parse("şu konseptte 500 twet metni ekle")?.count)
    }
    @Test fun topicYearDoesNotBecomeTheRequestedCount() {
        assertEquals(500, TweetGenerationRequest.parse("2026 futbol konseptinde 500 tweet üret")?.count)
    }
    @Test fun plainChatAndStopAreNotGenerationRequests() {
        assertNull(TweetGenerationRequest.parse("merhaba nasılsın")); assertNull(TweetGenerationRequest.parse("görevi durdur"))
    }
    @Test fun invalidCountsAreVisibleInsteadOfSilentlyClamped() {
        assertEquals(1500, TweetGenerationRequest.parse("1500 tweet üret")?.count)
        assertEquals(0, TweetGenerationRequest.parse("0 tweet üret")?.count)
    }
    @Test fun fiveHundredAreSavedInTenSequentialBatches() = runBlocking {
        val saved = mutableListOf<String>(); val sizes = mutableListOf<Int>(); val progress = mutableListOf<Int>()
        val engine = TweetBatchGeneration({ _, count, batch, _ -> sizes += count; List(count) { "Parti $batch metin $it" } },
            { it.also(saved::addAll) })
        val result = engine.generate(TweetGenerationRequest("futbol", 500), emptyList()) { count, _ -> progress += count }
        assertEquals(500, result.saved); assertEquals(List(10) { 50 }, sizes)
        assertEquals(500, saved.toSet().size); assertEquals((0..500 step 50).toList(), progress)
    }
    @Test fun finalBatchUsesOnlyTheRemainingCount() = runBlocking {
        val sizes = mutableListOf<Int>()
        TweetBatchGeneration({ _, count, batch, _ -> sizes += count; List(count) { "$batch/$it" } }, { it })
            .generate(TweetGenerationRequest("konsept", 117), emptyList()) { _, _ -> }
        assertEquals(listOf(50, 50, 17), sizes)
    }
    @Test fun previousPoolAndEarlierBatchDuplicatesDoNotInflateProgress() = runBlocking {
        val stored = mutableListOf<String>()
        val engine = TweetBatchGeneration({ _, _, batch, _ -> listOf("Eski", "eski", "yeni $batch", " YENİ $batch ") },
            { it.also(stored::addAll) })
        assertEquals(3, engine.generate(TweetGenerationRequest("konsept", 3), listOf("eski")) { _, _ -> }.saved)
        assertEquals(listOf("yeni 1", "yeni 2", "yeni 3"), stored)
    }
    @Test fun networkFailureKeepsTheCompletedBatchAndItsRealCount() = runBlocking {
        val stored = mutableListOf<String>(); var progress = 0
        val engine = TweetBatchGeneration({ _, count, batch, _ ->
            if (batch == 2) error("API unavailable") else List(count) { "metin $it" } }, { it.also(stored::addAll) })
        try { engine.generate(TweetGenerationRequest("konsept", 100), emptyList()) { n, _ -> progress = n }; fail() }
        catch (e: IllegalStateException) { assertEquals("API unavailable", e.message) }
        assertEquals(50, stored.size); assertEquals(50, progress)
    }
    @Test fun cancellationBeforeTheNextBatchDoesNotUndoSavedTexts() = runBlocking {
        val stored = mutableListOf<String>(); var progress = 0
        val engine = TweetBatchGeneration({ _, count, batch, _ ->
            if (batch == 2) throw CancellationException("stop") else List(count) { "metin $it" } }, { it.also(stored::addAll) })
        try { engine.generate(TweetGenerationRequest("konsept", 100), emptyList()) { n, _ -> progress = n }; fail() }
        catch (_: CancellationException) { }
        assertEquals(50, stored.size); assertEquals(50, progress)
    }
    @Test fun zeroUsableResponsesStopAfterThreeWithoutClaimingCompletion() = runBlocking {
        var calls = 0
        try { TweetBatchGeneration({ _, _, _, _ -> calls++; listOf(" ", "x".repeat(281)) }, { it })
            .generate(TweetGenerationRequest("konsept", 5), emptyList()) { n, _ -> assertEquals(0, n) }; fail() }
        catch (_: IllegalStateException) { }
        assertEquals(3, calls)
    }
    @Test fun storageAdmitsOnlyActualUniqueOfferedTexts() = runBlocking {
        val engine = TweetBatchGeneration({ _, _, batch, _ -> listOf("metin $batch") }, { listOf(it.first(), it.first(), "unrelated") })
        assertEquals(2, engine.generate(TweetGenerationRequest("konsept", 2), emptyList()) { _, _ -> }.saved)
    }
    @Test fun insufficientCapacityDoesNotCallTheNetwork() = runBlocking {
        var calls = 0
        try { TweetBatchGeneration({ _, _, _, _ -> calls++; emptyList() }, { it })
            .generate(TweetGenerationRequest("konsept", 500), List(9501) { "$it" }) { _, _ -> }; fail() }
        catch (_: IllegalArgumentException) { }
        assertEquals(0, calls)
    }
    @Test fun mergePreservesOrderAndReportsOnlyNewlyStoredLines() {
        val merged = TweetPoolPolicy.merge(listOf("ilk", "ikinci"), listOf(" İLK ", "üçüncü", "ÜÇÜNCÜ", "dördüncü"), 3)
        assertEquals(listOf("ilk", "ikinci", "üçüncü"), merged.lines); assertEquals(listOf("üçüncü"), merged.added)
    }
    @Test fun tweetPoolCanReadMoreThanTheOldFiveHundredLimit() {
        val raw = (1..700).joinToString("\n") { "metin $it" }
        assertEquals(700, ReplyTextPoolPolicy.lines(raw, 280, TweetPoolPolicy.MAX_LINES).size)
        assertEquals(500, ReplyTextPoolPolicy.lines(raw, 280).size)
    }
    @Test fun emojiLimitUsesUnicodeCodePointsAndDoesNotSplitSurrogates() {
        assertTrue(TweetPoolPolicy.valid("😀".repeat(280))); assertFalse(TweetPoolPolicy.valid("😀".repeat(281)))
        val line = ReplyTextPoolPolicy.lines("😀".repeat(281), 280).single()
        assertEquals("😀".repeat(280), line)
    }
    @Test fun batchTokenBudgetReachesTheActualApiPayload() {
        assertEquals(8192, GeminiJson.payload("batch", 8192).getJSONObject("generationConfig").getInt("maxOutputTokens"))
        assertEquals(256, GeminiJson.payload("pilot", 256).getJSONObject("generationConfig").getInt("maxOutputTokens"))
    }
    @Test fun jsonEscapesAndFencedDraftsStayIntact() {
        assertEquals(listOf("Bir \"alıntı\" 😀", "ikinci"), GeminiJson.drafts("```json\n[\"Bir \\\"alıntı\\\" 😀\",\"ikinci\"]\n```"))
        assertEquals(listOf("metin"), GeminiJson.drafts("{\"drafts\":[\"metin\",123,null,{}]}"))
    }
    @Test fun incompleteJsonIsNotSilentlyStoredAsTweetText() {
        assertTrue(GeminiJson.drafts("[\"yarım").isEmpty())
        assertTrue(GeminiJson.drafts("normal chatbot yanıtı").isEmpty())
    }
    @Test fun chatbotJsonQuotedTextIsParsedWithoutARegexTruncation() {
        val reply = PilotPolicy.parseModel("""{"say":"Şöyle yaz: \"merhaba\" 😀","command":"NONE"}""")
        assertEquals("Şöyle yaz: \"merhaba\" 😀", reply?.say)
    }
}
