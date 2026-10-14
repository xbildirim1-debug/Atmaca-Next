package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountRowAvatar26_85Test {
    @Test fun photoStaysInsideItsOwnRow() {
        val first = row("@ezeldestan", 8, imageTop = 20)
        val second = row("@bildirimhaber1", 96, imageTop = 108)
        val list = AccountRowAvatar.Item("list", 0, 0, 400, 300, children = listOf(first, second))
        val rows = AccountRowAvatar.lockedRows(list, 56, 130, 400).associateBy { it.handle }
        assertEquals(20, rows.getValue("ezeldestan").top)
        assertEquals(108, rows.getValue("bildirimhaber1").top)
    }

    private fun row(handle: String, top: Int, imageTop: Int) = AccountRowAvatar.Item(
        label = "",
        left = 0,
        top = top,
        width = 400,
        height = 72,
        children = listOf(
            AccountRowAvatar.Item("", 16, imageTop, 48, 48, className = "android.widget.ImageView"),
            AccountRowAvatar.Item(handle, 80, top + 20, 200, 28),
        ),
    )
}
