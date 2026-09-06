package com.atmacanext.app.automation

import java.security.MessageDigest

/** Privacy-preserving sanitizer for real-device X accessibility diagnostics. */
object XDiagnosticSanitizer {
    private val countPattern = Regex("\\d[\\d.,\\s]*")

    fun sanitizeLabel(raw: String?): String? {
        val normalized = XUiVocabulary.normalize(raw)
        if (normalized.isBlank()) return null

        val explicitHandle = XIdentityDetector.extractHandle(raw)
        if (explicitHandle != null) return "@<handle:${shortHash(explicitHandle)}>"

        if (normalized in XUiVocabulary.structuralLabels) return normalized.take(100)

        // Preserve only pure count+header labels (for example "123 Followers"). Arbitrary user
        // content that merely mentions followers/following remains redacted.
        val countNormalized = countPattern.replace(normalized, "#").replace(Regex("\\s+"), " ").trim()
        val compact = countNormalized.replace(" ", "")
        val headerCompacts = (XUiVocabulary.followersHeaders + XUiVocabulary.followingHeaders)
            .map { it.replace(" ", "") }
            .toSet()
        if (headerCompacts.any { compact == "#$it" || compact == "$it#" }) return countNormalized.take(100)

        // Do not write posts, bios, DMs, names or arbitrary user-generated text to diagnostics.
        return "<redacted>"
    }

    fun sanitizeViewId(raw: String?): String? = raw
        ?.takeIf { it.isNotBlank() }
        ?.substringAfterLast('/')
        ?.take(120)

    private fun shortHash(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.take(4).joinToString("") { "%02x".format(it) }
    }
}
