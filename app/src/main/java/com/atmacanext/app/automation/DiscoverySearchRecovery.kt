package com.atmacanext.app.automation

internal enum class SearchRecovery { WAIT, RETRY_RESULT, PAUSE }

/** A dispatched tap is not proof that X left the search results. */
internal object DiscoverySearchRecovery {
    fun decide(elapsedSinceTap: Long, attempts: Int, queryStillVisible: Boolean): SearchRecovery = when {
        elapsedSinceTap < 2_500L || !queryStillVisible -> SearchRecovery.WAIT
        attempts < 3 -> SearchRecovery.RETRY_RESULT
        else -> SearchRecovery.PAUSE
    }
}
