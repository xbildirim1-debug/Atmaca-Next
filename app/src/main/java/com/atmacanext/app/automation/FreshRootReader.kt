package com.atmacanext.app.automation

/** Never carry a root obtained before invalidating Android's node cache. */
internal object FreshRootReader {
    fun <T : Any> read(invalidate: () -> Unit, acquire: () -> T?, refresh: (T) -> Boolean): T? {
        invalidate()
        val root = acquire() ?: return null
        return root.takeIf(refresh)
    }
}
