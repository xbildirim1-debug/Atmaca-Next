package com.atmacanext.app.automation

internal enum class DiscoveryTargetLaunchDecision { WAIT, LAUNCH, GIVE_UP }

/**
 * X sometimes swallows a profile deep-link while its own-profile transition is
 * still settling after account verification. Keep navigation separate from the
 * verification callback and retry only until the exact target @handle is seen.
 */
internal object DiscoveryTargetLaunchPolicy {
    const val INITIAL_DELAY_MS = 450L
    const val SETTLE_MS = 2_500L
    const val MAX_ATTEMPTS = 3

    fun decide(exactTargetVisible: Boolean, launchAttempts: Int, elapsedMs: Long): DiscoveryTargetLaunchDecision {
        if (exactTargetVisible) return DiscoveryTargetLaunchDecision.WAIT
        if (elapsedMs < INITIAL_DELAY_MS) return DiscoveryTargetLaunchDecision.WAIT
        if (launchAttempts >= MAX_ATTEMPTS) return DiscoveryTargetLaunchDecision.GIVE_UP
        return DiscoveryTargetLaunchDecision.LAUNCH
    }
}
