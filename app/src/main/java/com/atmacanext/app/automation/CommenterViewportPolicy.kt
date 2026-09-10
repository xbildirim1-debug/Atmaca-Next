package com.atmacanext.app.automation

/**
 * Drain every still-visible, unprocessed commenter before advancing the thread.
 * Scrolling is a pagination step, never a side effect of returning from one profile.
 */
internal object CommenterViewportPolicy {
    fun nextVisible(visibleAuthors: List<String>, excludedHandles: Set<String>): String? =
        visibleAuthors.firstOrNull { it !in excludedHandles }

    fun shouldScroll(visibleAuthors: List<String>, excludedHandles: Set<String>): Boolean =
        nextVisible(visibleAuthors, excludedHandles) == null
}
