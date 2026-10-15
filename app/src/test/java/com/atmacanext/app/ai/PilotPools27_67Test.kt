package com.atmacanext.app.ai

import org.junit.Assert.*
import org.junit.Test

class PilotPools27_67Test {
    @Test fun lastFiftySixMoveOutOfOneHundredFiftySixInTheirOriginalOrder() {
        val from = (1..156).map { "metin $it" }
        val to = listOf("hazır yorum")
        val edit = PilotPoolPolicy.transfer(from, to, 56)
        assertEquals((1..100).map { "metin $it" }, edit.source)
        assertEquals(to + (101..156).map { "metin $it" }, edit.destination)
        assertEquals(56, edit.count); assertEquals(56, edit.added)
    }
    @Test fun alreadyPresentTextsAreNotLostOrDoubleCounted() {
        val edit = PilotPoolPolicy.transfer(listOf("ilk", "İKİNCİ", "üçüncü"), listOf("ikinci"), 2)
        assertEquals(listOf("ilk"), edit.source); assertEquals(listOf("ikinci", "üçüncü"), edit.destination)
        assertEquals(2, edit.count); assertEquals(1, edit.added)
    }
    @Test fun copyPreservesEverySourceText() {
        val source = listOf("a", "b", "c")
        assertEquals(source, PilotPoolPolicy.transfer(source, emptyList(), 2, copy = true).source)
    }
    @Test fun insufficientDestinationCapacityRejectsBeforeSourceRemoval() {
        val source = listOf("a", "b", "c"); val destination = listOf("x", "y")
        try { PilotPoolPolicy.transfer(source, destination, 2, capacity = 3); fail() } catch (_: IllegalArgumentException) { }
        assertEquals(listOf("a", "b", "c"), source); assertEquals(listOf("x", "y"), destination)
    }
    @Test fun unavailableTailCountIsNotSilentlyReduced() {
        try { PilotPoolPolicy.transfer(listOf("a"), emptyList(), 56); fail() } catch (_: IllegalArgumentException) { }
        try { PilotPoolPolicy.deleteLast(listOf("a"), 0); fail() } catch (_: IllegalArgumentException) { }
    }
    @Test fun deletionRemovesOnlyTheRequestedTail() {
        assertEquals(listOf("a", "b"), PilotPoolPolicy.deleteLast(listOf("a", "b", "c", "d"), 2))
    }
    @Test fun existingCursorIsKeptOrResetOnlyWhenRemoved() {
        assertEquals(5, PilotPoolPolicy.cursor(5, 100)); assertEquals(0, PilotPoolPolicy.cursor(120, 100)); assertEquals(0, PilotPoolPolicy.cursor(0, 0))
    }
    @Test fun fullTweetUnicodeTextIsNotTruncatedByTransfer() {
        val text = "😀".repeat(280)
        assertEquals(text, PilotPoolPolicy.transfer(listOf(text), emptyList(), 1).destination.single())
    }
    @Test fun imageAndLiteralNewlinesSurviveTheNewChatStore() {
        val text = "satır 1\nsatır 2 \\n 😀"
        val line = PilotChatStore.Line(true, text, "/app/files/pilot_images/a.jpg")
        val back = PilotChatStore.decode(PilotChatStore.encode(listOf(line))).single()
        assertEquals(line, back)
    }
    @Test fun oldTranscriptIsStillReadable() {
        assertEquals(listOf("Eski kullanıcı", "Eski cevap"), PilotChatStore.decode("1\u001fEski kullanıcı\n0\u001fEski cevap").map { it.text })
    }
    @Test fun imagesAreSentAsActualInlineImageData() {
        val request = GeminiJson.payload("Bu resmi incele", 4096, "aW1hZ2U=", PilotIntentSchema.value())
        val parts = request.getJSONArray("contents").getJSONObject(0).getJSONArray("parts")
        assertEquals("Bu resmi incele", parts.getJSONObject(0).getString("text"))
        assertEquals("image/jpeg", parts.getJSONObject(1).getJSONObject("inline_data").getString("mime_type"))
        assertEquals("aW1hZ2U=", parts.getJSONObject(1).getJSONObject("inline_data").getString("data"))
        assertEquals(4, request.getJSONObject("generationConfig").getJSONObject("responseJsonSchema").getJSONObject("properties").getJSONObject("actions").getInt("maxItems"))
    }
}
