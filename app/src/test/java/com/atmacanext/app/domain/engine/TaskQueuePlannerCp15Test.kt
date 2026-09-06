package com.atmacanext.app.domain.engine

import com.atmacanext.app.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskQueuePlannerCp15Test {
    private fun account(i: Int) = Account(
        id = i.toString(),
        username = "@acc$i",
        displayName = "Account $i",
        followers = "0",
        following = "0",
        engagement = "0",
        health = 100,
        accent = AccountAccent.BLUE,
        active = true,
    )

    private fun task(i: Int) = ScheduledTask(
        id = "t$i",
        accountId = i.toString(),
        username = "@acc$i",
        time = "10:00",
        type = TaskType.UNFOLLOW,
        status = TaskStatus.QUEUED,
        progress = 0,
        limit = 35,
    )

    @Test
    fun queue_is_capped_to_ten_accounts() {
        val queue = TaskQueuePlanner.build((1..12).map(::account), (1..12).map(::task))
        assertEquals(10, queue.map { it.accountId }.distinct().size)
        assertTrue(queue.none { it.accountId == "11" || it.accountId == "12" })
    }

    @Test
    fun duplicate_handles_do_not_create_duplicate_account_slots() {
        val accounts = listOf(
            account(1),
            account(2).copy(username = "@acc1"),
            account(3),
        )
        val tasks = listOf(task(1), task(2), task(3))
        val queue = TaskQueuePlanner.build(accounts, tasks)
        assertEquals(listOf("1", "3"), queue.map { it.accountId })
    }
}
