package com.buse.app.automation

/** Brief overlays/root changes are not evidence that the selected X account changed. */
internal object ForegroundReadPolicy {
    const val GRACE_MS = 2_500L
    fun wait(packageName: String?, elapsedMs: Long): Boolean =
        elapsedMs < GRACE_MS && (packageName == null ||
            packageName == "com.android.systemui" ||
            packageName.contains("inputmethod", true) || packageName.contains("keyboard", true))
}
