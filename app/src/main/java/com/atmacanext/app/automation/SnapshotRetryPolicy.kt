package com.atmacanext.app.automation

/** A transient read failure re-reads the same operation; it never resubmits it. */
internal class SnapshotRetryPolicy {
    private var session: String? = null
    private var failures = 0

    fun retry(key: String): Boolean {
        if (session != key) { session = key; failures = 0 }
        failures++
        return failures <= 3
    }

    fun recovered() { failures = 0 }
}
