package com.atmacanext.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PilotPolicyTest {
    @Test fun firstStallRefreshesInsteadOfRestarting() {
        assertEquals(PilotPolicy.Move.REFRESH, PilotPolicy.stallMove(1, pendingAction = false, screen = "VERIFIED_FOLLOWERS_LIST"))
        assertEquals(PilotPolicy.Move.BACK, PilotPolicy.stallMove(2, pendingAction = false, screen = "VERIFIED_FOLLOWERS_LIST"))
        assertEquals(PilotPolicy.Move.HOME, PilotPolicy.stallMove(2, pendingAction = false, screen = "HOME"))
        assertEquals(PilotPolicy.Move.CONTINUE, PilotPolicy.stallMove(1, pendingAction = true))
        assertEquals(PilotPolicy.Move.HOME, PilotPolicy.stallMove(1, pendingAction = false, knownStreak = 3, screen = "TWEET_DETAIL"))
        assertEquals(PilotPolicy.Move.BACK, PilotPolicy.stallMove(1, pendingAction = false, screen = "TWEET_DETAIL", lastFailed = "REFRESH"))
    }

    @Test fun tweetsStayDistinctAndInsideLimit() {
        val lines = PilotPolicy.tweets("sabah notu", 6, 3)
        assertEquals(6, lines.size)
        assertEquals(lines.size, lines.toSet().size)
        assertTrue(lines.all { it.length <= 280 && "sabah notu" in it })
    }

    @Test fun chatAddsDraftsAndCanStop() {
        val added = PilotPolicy.plan("5 tweet üret gündem", "RUNNING", "HOME", "OPEN_COMPOSER", "a", "", 0, "")
        assertEquals(PilotPolicy.Command.TWEETS, added.command)
        assertEquals(5, added.tweets.size)
        val stop = PilotPolicy.plan("görevi durdur", "RUNNING", "HOME", "VERIFY_ACCOUNT", "a", "", 0, "")
        assertEquals(PilotPolicy.Command.STOP, stop.command)
    }

    @Test fun instructionPicksSavedVerifiedTasks() {
        val plan = PilotPolicy.plan("@ezeldestan onaylı takibi başlat", "IDLE", "UNKNOWN", "VERIFY_ACCOUNT", null, "", 0, "")
        assertEquals(PilotPolicy.Command.START, plan.command)
        assertEquals(com.atmacanext.app.domain.model.TaskType.VERIFIED_FOLLOW, plan.taskType)
        val tasks = listOf(
            task("ezeldestan", com.atmacanext.app.domain.model.TaskType.VERIFIED_FOLLOW),
            task("diger", com.atmacanext.app.domain.model.TaskType.VERIFIED_FOLLOW),
            task("ezeldestan", com.atmacanext.app.domain.model.TaskType.TEXT_TWEET),
        )
        val picked = PilotPolicy.select(tasks, plan.taskType, plan.account, plan.all)
        assertEquals(listOf("ezeldestan"), picked.map { it.username })
    }

    @Test fun quotaWordingIsDiscardedAndLocalDayBlocks() {
        assertNull(PilotPolicy.parseModel("""{"say":"Günlük kota doldu","command":"NONE"}"""))
        val ok = PilotPolicy.parseModel("""{"say":"Onaylı takibi alıyorum","command":"START","taskType":"VERIFIED_FOLLOW","account":"ezeldestan"}""")
        assertEquals(PilotPolicy.Command.START, ok?.command)
        assertEquals("ezeldestan", ok?.account)
        PilotQuota.clear()
        assertTrue(!PilotQuota.blocked("2026-10-11"))
        PilotQuota.block("2026-10-11")
        assertTrue(PilotQuota.blocked("2026-10-11"))
        assertTrue(!PilotQuota.blocked("2026-10-12"))
        PilotQuota.clear()
    }

    private fun task(name: String, type: com.atmacanext.app.domain.model.TaskType) =
        com.atmacanext.app.domain.model.ScheduledTask(id = name + type.name, accountId = name, username = name, type = type)
}
