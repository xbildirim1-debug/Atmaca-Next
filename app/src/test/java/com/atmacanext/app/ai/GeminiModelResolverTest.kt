package com.atmacanext.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GeminiModelResolverTest {
    @Test
    fun requestedUnavailableModelFallsBackToAvailableGenerateContentModel() {
        val models = listOf(
            GeminiModelDescriptor("models/text-embedding-004", setOf("embedContent")),
            GeminiModelDescriptor("models/gemini-2.0-flash", setOf("generateContent")),
            GeminiModelDescriptor("models/gemini-3-flash-preview", setOf("generateContent")),
        )

        val result = GeminiModelResolver.orderedCandidates(models, "gemini-2.5-flash")

        assertFalse(result.contains("gemini-2.5-flash"))
        assertEquals("gemini-3-flash-preview", result.first())
    }

    @Test
    fun explicitlyAvailableRequestedModelIsFirst() {
        val models = listOf(
            GeminiModelDescriptor("models/gemini-3-flash", setOf("generateContent")),
            GeminiModelDescriptor("models/gemini-2.0-flash", setOf("generateContent")),
        )

        assertEquals(
            "gemini-2.0-flash",
            GeminiModelResolver.orderedCandidates(models, "models/gemini-2.0-flash").first(),
        )
    }
}
