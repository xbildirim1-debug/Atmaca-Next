package com.atmacanext.app.ai

data class GeminiModelDescriptor(
    val name: String,
    val supportedGenerationMethods: Set<String>,
)

/** Chooses only text models that the current API key explicitly reports as usable. */
object GeminiModelResolver {
    fun orderedCandidates(
        models: List<GeminiModelDescriptor>,
        requestedModel: String?,
    ): List<String> {
        val requested = normalize(requestedModel).takeUnless { it == "auto" }
        return models.asSequence()
            .filter { descriptor -> descriptor.supportedGenerationMethods.any { it.equals("generateContent", ignoreCase = true) } }
            .map { normalize(it.name) }
            .filter(::isTextGenerationModel)
            .distinct()
            .sortedWith(
                compareByDescending<String> { it == requested }
                    .thenByDescending(::score)
                    .thenBy { it },
            )
            .toList()
    }

    fun normalize(value: String?): String = value.orEmpty()
        .trim()
        .removePrefix("models/")
        .lowercase()

    private fun isTextGenerationModel(model: String): Boolean {
        if (!model.startsWith("gemini-")) return false
        return listOf("embedding", "imagen", "image-generation", "tts", "audio", "live", "aqa").none(model::contains)
    }

    private fun score(model: String): Int {
        var score = 0
        if (model.contains("flash")) score += 120
        if (model.contains("pro")) score += 80
        if (model.startsWith("gemini-3")) score += 80
        else if (model.startsWith("gemini-2.5")) score += 60
        else if (model.startsWith("gemini-2.0")) score += 45
        else if (model.startsWith("gemini-1.5")) score += 20
        if (model.contains("latest")) score += 15
        if (model.contains("lite")) score -= 5
        if (model.contains("preview")) score -= 25
        if (model.contains("exp")) score -= 40
        return score
    }
}
