package com.atmacanext.app.automation

import org.junit.Assert.*
import org.junit.Test

class AccountScanPolicyTest {
    private val handles = listOf("atmaca2025", "xhesaplar1", "ezeldestan", "bildirimhaber1")

    @Test fun allFourAccountsAreVisitedEvenWhenFirstCannotBeRead() {
        val saved = linkedSetOf<String>()
        val skipped = linkedSetOf("atmaca2025")
        while (true) {
            val next = AccountScanPolicy.next(handles, saved, skipped) ?: break
            assertTrue(saved.add(next))
        }
        assertEquals(handles.drop(1), saved.toList())
        assertNull(AccountScanPolicy.next(handles, saved, skipped))
    }

    @Test fun successfulAccountsAreVisitedOnceAndInOrder() {
        val saved = linkedSetOf<String>()
        repeat(4) { saved += requireNotNull(AccountScanPolicy.next(handles, saved, emptySet())) }
        assertEquals(handles, saved.toList())
        assertNull(AccountScanPolicy.next(handles, saved, emptySet()))
    }

    @Test fun wrongAccountOrMissingCounterCannotBeSaved() {
        assertFalse(AccountScanPolicy.canRecord("atmaca2025", "xhesaplar1", "238", "366"))
        assertFalse(AccountScanPolicy.canRecord("atmaca2025", "atmaca2025", null, "366"))
        assertFalse(AccountScanPolicy.canRecord(null, null, "238", "366"))
        assertTrue(AccountScanPolicy.canRecord("xhesaplar1", "xhesaplar1", "238", "366"))
        assertTrue(AccountScanPolicy.canRecord("xhesaplar1", "xhesaplar1", "0", "0"))
    }

    @Test fun noisyAccessibilityEventsDoNotPostponePendingTick() {
        var due: Long? = 1_000L
        (1_001L..2_000L).forEach { requested ->
            if (AccountScanPolicy.replaceTick(due, requested)) due = requested
        }
        assertEquals(1_000L, due)
        assertTrue(AccountScanPolicy.replaceTick(due, 900L))
        assertTrue(AccountScanPolicy.replaceTick(null, 2_000L))
    }

    @Test fun discoveryIsCappedAtTenDistinctAccounts() {
        val many = (1..12).map { "account$it" }
        assertNull(AccountScanPolicy.next(many, many.take(10).toSet(), emptySet()))
        assertEquals("account2", AccountScanPolicy.next(listOf("account1", "account1", "account2"), setOf("account1"), emptySet()))
    }
}
