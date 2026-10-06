package com.atmacanext.app.domain.engine

import com.atmacanext.app.domain.model.*
import com.atmacanext.app.ui.screens.TASK_TYPES_BY_GROUP
import com.atmacanext.app.ui.screens.TaskGroup
import org.junit.Assert.*
import org.junit.Test

class QuoteTaskSetup26_61Test {
    private fun target(owner: String, handle: String, active: Boolean = true, kind: TargetKind = TargetKind.QUOTE) =
        TargetAccount("$owner:$handle", owner, handle, active, kind)
    private fun task(owner: String = "one", limit: Int = 1, repeat: Int = 1) = ScheduledTask(
        "task-$owner", owner, "@$owner", time = "group:batch:COMMENT_QUOTE_TARGETS",
        type = TaskType.COMMENT_QUOTE_TARGETS, limit = limit, repeatCount = repeat, contentText = "Yorum metni")

    @Test fun quoteIsAvailableInTheGeneralNewTaskCatalog() {
        assertEquals(listOf(TaskType.COMMENT_QUOTE_TARGETS), TASK_TYPES_BY_GROUP.getValue(TaskGroup.QUOTE_COMMENT))
        assertEquals(3, TASK_TYPES_BY_GROUP.size)
        assertFalse(TaskType.COMMENT_QUOTE_TARGETS.requiresLink)
        assertTrue(TaskType.COMMENT_QUOTE_TARGETS.supportsGemini)
    }
    @Test fun nonFollowerPositionIsPreserved() {
        val follow = TASK_TYPES_BY_GROUP.getValue(TaskGroup.FOLLOW)
        assertEquals(TaskType.UNFOLLOW_NON_FOLLOWERS, follow[follow.indexOf(TaskType.UNFOLLOW) + 1])
    }
    @Test fun ownersStandardAndInactiveTargetsDoNotLeakIntoQuoteWork() {
        val rows = listOf(target("one", "correct"), target("two", "wrong"),
            target("one", "inactive", false), target("one", "standard", kind = TargetKind.STANDARD))
        assertEquals(listOf("correct"), QuoteTaskSetupPolicy.activeHandles("one", rows))
    }
    @Test fun mixedCaseDuplicatesAndInvalidHandlesAreNormalizedBeforeCounting() {
        val rows = listOf(" @TARGET ", "target", "a-b", "", "abcdefghijklmnop", "second")
            .map { target("one", it) }
        assertEquals(listOf("target", "second"), QuoteTaskSetupPolicy.activeHandles("one", rows))
    }
    @Test fun atMostFiveDistinctQuoteTargetsAreBound() {
        assertEquals(5, QuoteTaskSetupPolicy.bindAccount(task(), (1..9).map { target("one", "target$it") }).quoteTargetHandles.size)
    }
    @Test fun eachSelectedAccountGetsItsOwnSnapshotInTheSameBatch() {
        val rows = listOf(target("one", "first"), target("two", "second"))
        val one = QuoteTaskSetupPolicy.bindAccount(task("one"), rows)
        val two = QuoteTaskSetupPolicy.bindAccount(task("two"), rows)
        assertEquals(listOf("first"), one.quoteTargetHandles)
        assertEquals(listOf("second"), two.quoteTargetHandles)
        assertEquals(one.time, two.time)
        assertEquals("Yorum metni", two.contentText)
    }
    @Test fun limitIsPerTargetAndRepeatsKeepTheFullTotal() {
        val bound = QuoteTaskSetupPolicy.bindAccount(task(limit = 3, repeat = 2),
            listOf(target("one", "first"), target("one", "second")))
        assertEquals(12, bound.totalLimit)
    }
    @Test(expected = IllegalArgumentException::class) fun missingQuoteTargetsCannotCreateARunnableTask() {
        QuoteTaskSetupPolicy.bindAccount(task(), listOf(target("two", "wrong")))
    }
    @Test(expected = IllegalArgumentException::class) fun unsupportedPerTargetLimitIsNotSilentlyClampedByTheMotor() {
        QuoteTaskSetupPolicy.bindAccount(task(limit = 21), listOf(target("one", "first")))
    }
    @Test fun editingKeepsProgressAcrossAllTargetsAndTheDurableReplyReservation() {
        val existing = task(limit = 3, repeat = 2).copy(progress = 9, quoteTargets = "first\nsecond",
            quotePostedKeys = "done", quotePendingKey = "pending")
        val saved = QuoteTaskSetupPolicy.bindAccount(existing.copy(progress = 6), emptyList(), existing)
        assertEquals(9, saved.progress)
        assertEquals("done", saved.quotePostedKeys)
        assertEquals("pending", saved.quotePendingKey)
        assertEquals(12, saved.totalLimit)
    }
    @Test fun addingAnotherAccountCannotCopyTheFirstAccountsPostedOrPendingKeys() {
        val existing = task().copy(quoteTargets = "old", quotePostedKeys = "done", quotePendingKey = "pending", progress = 1)
        val new = existing.copy(id = "new", accountId = "two", username = "@two")
        val saved = QuoteTaskSetupPolicy.bindAccount(new, listOf(target("two", "fresh")), existing)
        assertEquals(listOf("fresh"), saved.quoteTargetHandles)
        assertEquals(0, saved.progress)
        assertNull(saved.quotePostedKeys)
        assertNull(saved.quotePendingKey)
    }
    @Test fun aStartedTaskKeepsItsOriginalTargetSnapshotOnEdit() {
        val existing = task().copy(quoteTargets = "old", quotePostedKeys = "done")
        assertEquals(listOf("old"), QuoteTaskSetupPolicy.bindAccount(existing,
            listOf(target("one", "new")), existing).quoteTargetHandles)
    }
    @Test fun ordinaryTasksDoNotAcquireQuoteTargetRequirements() {
        val normal = task().copy(type = TaskType.UNFOLLOW, limit = 35)
        assertEquals(normal, QuoteTaskSetupPolicy.bindAccount(normal, emptyList()))
    }
}
