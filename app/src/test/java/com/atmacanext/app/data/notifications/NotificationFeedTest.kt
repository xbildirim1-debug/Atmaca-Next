package com.atmacanext.app.data.notifications

import org.junit.Assert.*
import org.junit.Test

class NotificationFeedTest {
    private fun event(id: String) = AccountNotification(id, "@account", "Üç ardışık geri dönüş", 100)
    @Test fun duplicateRuntimeEventsCreateOneNotification() {
        val first = NotificationFeed.add(emptyList(), event("queue1:account1"))
        assertEquals(first, NotificationFeed.add(first, event("queue1:account1")))
    }
    @Test fun differentAccountsAndLaterRunsCreateSeparateNotifications() {
        val keys = listOf("queue1:account1", "queue1:account2", "queue2:account1")
        val result = keys.fold(emptyList<AccountNotification>()) { rows, id -> NotificationFeed.add(rows, event(id)) }
        assertEquals(keys.reversed(), result.map { it.id })
        assertEquals(3, result.count { !it.read })
    }
    @Test fun openingInboxMarksExistingEventsReadWithoutReadingFutureOnes() {
        val read = NotificationFeed.readAll(listOf(event("old")))
        val result = NotificationFeed.add(read, event("new"))
        assertEquals(listOf("new"), result.filterNot { it.read }.map { it.id })
    }
    @Test fun inboxKeepsNewestTwoHundredEvents() {
        val rows = (0..200).fold(emptyList<AccountNotification>()) { rows, id -> NotificationFeed.add(rows, event("$id")) }
        assertEquals(200, rows.size)
        assertEquals("200", rows.first().id)
        assertFalse(rows.any { it.id == "0" })
    }
}
