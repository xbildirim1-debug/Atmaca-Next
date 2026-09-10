package com.atmacanext.app.automation

/**
 * Session-local identity hint for a target profile that was already proven by
 * DiscoveryProfileEvidence. It is used only to expand any X feed header that is
 * ellipsized (for example @longuse...) back to that same verified target; it never
 * proves a new profile or a follow target by itself.
 */
internal object DiscoveryTargetIdentityCache {
    @Volatile private var verifiedTarget: String? = null

    fun remember(expected: String) {
        val target = XIdentityDetector.normalizeUsername(expected)
        if (target.isNotBlank()) verifiedTarget = target
    }

    fun resolveTruncated(prefix: String): String? {
        val normalizedPrefix = XIdentityDetector.normalizeUsername(prefix)
        val target = verifiedTarget ?: return null
        return target.takeIf { normalizedPrefix.length >= 4 && it.startsWith(normalizedPrefix) }
    }
}
