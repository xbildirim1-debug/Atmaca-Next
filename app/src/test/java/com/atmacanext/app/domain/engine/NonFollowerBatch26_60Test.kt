package com.atmacanext.app.domain.engine

import com.atmacanext.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class NonFollowerBatch26_60Test {
    private fun account(id:String)=Account(id,"@account$id","Account $id","0","200","0",100,AccountAccent.BLUE)
    private fun task(id:String,account:Account,type:TaskType)=ScheduledTask(id,account.id,account.username,type=type,limit=20,repeatCount=2)
    @Test fun newTaskIsRunnableAcrossAllSelectedAccounts() {
        val accounts=(1..4).map {account(it.toString())}
        val tasks=accounts.map {task("nf${it.id}",it,TaskType.UNFOLLOW_NON_FOLLOWERS)}
        assertEquals(listOf("nf1","nf2","nf3","nf4"),TaskQueuePlanner.build(accounts.reversed(),tasks.reversed()).map {it.taskId})
        assertEquals(40,tasks.first().totalLimit)
    }
    @Test fun mixedTasksStayAccountMajorWithTheNewOptionAfterUnfollow() {
        val accounts=listOf(account("1"),account("2"))
        val tasks=accounts.flatMap {a->listOf(task("c${a.id}",a,TaskType.COMMENTER_FOLLOW),
            task("nf${a.id}",a,TaskType.UNFOLLOW_NON_FOLLOWERS),task("u${a.id}",a,TaskType.UNFOLLOW))}
        assertEquals(listOf("u1","nf1","c1","u2","nf2","c2"),TaskQueuePlanner.build(accounts,tasks).map {it.taskId})
    }
    @Test fun completedAccountsDoNotReturnToTheQueueAndPartialProgressDoes() {
        val a1=account("1");val a2=account("2")
        val completed=task("nf1",a1,TaskType.UNFOLLOW_NON_FOLLOWERS).copy(status=TaskStatus.COMPLETED,progress=40)
        val partial=task("nf2",a2,TaskType.UNFOLLOW_NON_FOLLOWERS).copy(status=TaskStatus.PAUSED,progress=1)
        assertEquals(listOf("nf2"),TaskQueuePlanner.build(listOf(a1,a2),listOf(completed,partial)).map {it.taskId})
    }
    @Test fun unfollowVariantsDoNotRequireAnExternalTargetOrContent() {
        assertTrue(TaskType.UNFOLLOW.isUnfollowAction);assertTrue(TaskType.UNFOLLOW_NON_FOLLOWERS.isUnfollowAction)
        assertFalse(TaskType.UNFOLLOW_NON_FOLLOWERS.requiresLink);assertFalse(TaskType.UNFOLLOW_NON_FOLLOWERS.supportsGemini)
        assertFalse(TaskType.UNFOLLOW_NON_FOLLOWERS.isDiscoveryFollow)
    }
}
