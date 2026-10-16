package com.atmacanext.app.domain.engine

import com.atmacanext.app.domain.model.*
import com.atmacanext.app.ai.*
import org.junit.Assert.*
import org.junit.Test

class AccountEngagement27_71Test {
    private fun accounts(n: Int) = (1..n).map { Account("$it", "@actor$it", "Actor $it", "0", "0", "", 100, AccountAccent.BLUE) }
    private fun tasks(n: Int = 3, repeats: Int = 1) = AccountEngagementPolicy.create(accounts(n), repeats, 5, "group:test:ACCOUNT_ENGAGEMENT")
    @Test fun twoAccountsEachEngageTheOther() {
        val rows = tasks(2); assertEquals(listOf("actor2"), AccountEngagementPolicy.peers(rows[0]))
        assertEquals(listOf("actor1"), AccountEngagementPolicy.peers(rows[1])); assertEquals(2, rows.sumOf { it.totalLimit })
    }
    @Test fun fiveAccountsHave20BundlesAnd60ActionChecksPerRound() {
        val rows = tasks(5); assertEquals(20, rows.sumOf { it.totalLimit }); assertEquals(60, rows.sumOf { it.totalLimit } * 3)
    }
    @Test fun selfIsExcludedForEveryActor() {
        tasks(10).forEach { assertFalse(AccountEngagementPolicy.peers(it).contains(AccountEngagementPolicy.handle(it.username))) }
    }
    @Test fun everyOrderedPairOccursExactlyOncePerRound() {
        val rows = tasks(5); val pairs = rows.flatMap { t -> (0 until t.totalLimit).map { t.username to AccountEngagementPolicy.peer(t, it) } }
        assertEquals(20, pairs.size); assertEquals(pairs.size, pairs.distinct().size)
    }
    @Test fun repeatRoundsPreservePeerOrder() {
        val row = tasks(3, 2).first()
        assertEquals(listOf("actor2", "actor3", "actor2", "actor3"), (0 until row.totalLimit).map { AccountEngagementPolicy.peer(row, it) })
    }
    @Test fun accountEngagementIsLocalAndDoesNotNeedAnExternalLinkOrGemini() {
        assertFalse(TaskType.ACCOUNT_ENGAGEMENT.requiresLink); assertFalse(TaskType.ACCOUNT_ENGAGEMENT.supportsGemini)
        assertTrue(tasks().all(AccountEngagementPolicy::valid))
    }
    @Test fun aSingleAccountIsRejected() { try { tasks(1); fail() } catch (_: IllegalArgumentException) {} }
    @Test fun moreThan10AccountsIsRejectedWithoutSilentDropping() { try { tasks(11); fail() } catch (_: IllegalArgumentException) {} }
    @Test fun inactiveAccountsAreRejected() {
        try { AccountEngagementPolicy.create(accounts(2).map { it.copy(active = false) }, 1, 5, "g"); fail() } catch (_: IllegalArgumentException) {}
    }
    @Test fun duplicateHandlesCannotCreateSelfEngagementThroughAnotherId() {
        try { AccountEngagementPolicy.create(listOf(accounts(2)[0], accounts(2)[1].copy(username = "ACTOR1")), 1, 5, "g"); fail() } catch (_: IllegalArgumentException) {}
    }
    @Test fun duplicateIdsAreRejected() {
        try { AccountEngagementPolicy.create(accounts(2).map { it.copy(id = "same") }, 1, 5, "g"); fail() } catch (_: IllegalArgumentException) {}
    }
    @Test fun malformedHandlesAreRejected() {
        try { AccountEngagementPolicy.create(accounts(2).map { it.copy(username = "https://x.com/other") }, 1, 5, "g"); fail() } catch (_: IllegalArgumentException) {}
    }
    @Test fun repeatsAndIntervalAreBounded() {
        for ((r, i) in listOf(0 to 5, 101 to 5, 1 to 0, 1 to 1441))
            try { AccountEngagementPolicy.create(accounts(2), r, i, "g"); fail() } catch (_: IllegalArgumentException) {}
    }
    @Test fun explicitAccountSelectionCannotGrowToUnselectedAccounts() {
        val chosen = listOf(accounts(5)[0], accounts(5)[3]); val rows = AccountEngagementPolicy.create(chosen, 1, 5, "g")
        assertEquals(setOf("1", "4"), rows.map { it.accountId }.toSet()); assertTrue(rows.all { AccountEngagementPolicy.peers(it).all { p -> p in setOf("actor1", "actor4") } })
    }
    @Test fun storedSelfTargetIsRejected() { assertFalse(AccountEngagementPolicy.valid(tasks().first().copy(contentPrompt = "actor1"))) }
    @Test fun duplicateOrInvalidStoredPeersAreRejected() {
        for (config in listOf("actor2\nactor2", "actor2\nhttps://x.com/other", "", "actor2\n*"))
            assertFalse(AccountEngagementPolicy.valid(tasks().first().copy(contentPrompt = config)))
    }
    @Test fun storedLimitMustEqualThePeerCount() { assertFalse(AccountEngagementPolicy.valid(tasks().first().copy(limit = 99))) }
    @Test fun progressRestoresTheNextPeerWithoutStartingFromPeerOne() { assertEquals("actor3", AccountEngagementPolicy.peer(tasks().first(), 1)) }
    @Test fun editingTheSameSelectionPreservesRealProgress() {
        val old = tasks().map { it.copy(progress = 1) }
        assertTrue(AccountEngagementPolicy.create(accounts(3), 2, 5, "g", old).all { it.progress == 1 })
    }
    @Test fun changedPeerSelectionResetsProgressAndDoesNotClaimOldWork() {
        val old = tasks().map { it.copy(progress = 2) }
        assertTrue(AccountEngagementPolicy.create(accounts(4), 1, 5, "g", old).all { it.progress == 0 })
    }
    @Test fun everySelectedAccountPostsBeforeAnyCrossAccountEngagement() {
        val a = accounts(3); val engage = tasks()
        val posts = a.map { ScheduledTask("post${it.id}", it.id, it.username, type = TaskType.TEXT_TWEET) }
        val q = TaskQueuePlanner.build(a, engage + posts)
        assertEquals(listOf(TaskType.TEXT_TWEET, TaskType.TEXT_TWEET, TaskType.TEXT_TWEET), q.take(3).map { it.taskType })
        assertTrue(q.drop(3).all { it.taskType == TaskType.ACCOUNT_ENGAGEMENT })
    }
    @Test fun actorsRemainSerialAndInTheExistingDeterministicAccountOrder() {
        assertEquals(listOf("1", "2", "3"), TaskQueuePlanner.build(accounts(3).reversed(), tasks().reversed()).map { it.accountId })
    }
    @Test fun completedActorsAreNotRestartedByTheQueue() {
        val rows = tasks().map { it.copy(progress = it.totalLimit, status = TaskStatus.COMPLETED) }
        assertTrue(TaskQueuePlanner.build(accounts(3), rows).isEmpty())
    }
    @Test fun pilotStartsOnlySavedCrossAccountTasksThroughTheLocalPath() {
        val a = PilotIntentPolicy.local("hesaplar arası etkileşim görevini başlat")!!.actions.single()
        assertEquals(listOf(TaskType.ACCOUNT_ENGAGEMENT), a.types); assertTrue(PilotIntentPolicy.authorized("hesaplar arası etkileşim görevini başlat", a))
        assertFalse(PilotProgramPolicy.acceptsNetworkPlan(PilotToolPlan(actions = listOf(a))))
    }
}
