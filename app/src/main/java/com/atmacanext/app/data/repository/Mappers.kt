package com.atmacanext.app.data.repository

import com.atmacanext.app.automation.AutomationRuntimeState
import com.atmacanext.app.automation.RuntimeStatus
import com.atmacanext.app.data.local.AccountEntity
import com.atmacanext.app.data.local.RuntimeCheckpointEntity
import com.atmacanext.app.data.local.TaskEntity
import com.atmacanext.app.data.local.TargetAccountEntity
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.AccountAccent
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskStatus
import com.atmacanext.app.domain.model.TargetAccount
import com.atmacanext.app.domain.model.TaskType

internal fun Account.toEntity(now: Long = System.currentTimeMillis()) = AccountEntity(
    id = id,
    username = username,
    displayName = displayName,
    followers = followers,
    following = following,
    engagement = engagement,
    health = health,
    accent = accent.name,
    active = active,
    isCurrent = isCurrent,
    inactiveReason = inactiveReason,
    unfollowRevertCount = unfollowRevertCount,
    aiPersona = aiPersona,
    aiLanguage = aiLanguage,
    aiTone = aiTone,
    updatedAt = now,
)

internal fun AccountEntity.toDomain() = Account(
    id = id,
    username = username,
    displayName = displayName,
    followers = followers,
    following = following,
    engagement = engagement,
    health = health,
    accent = runCatching { AccountAccent.valueOf(accent) }.getOrDefault(AccountAccent.BLUE),
    active = active,
    isCurrent = isCurrent,
    inactiveReason = inactiveReason,
    unfollowRevertCount = unfollowRevertCount,
    aiPersona = aiPersona,
    aiLanguage = aiLanguage,
    aiTone = aiTone,
)

internal fun ScheduledTask.toEntity(now: Long = System.currentTimeMillis()) = TaskEntity(
    id = id,
    accountId = accountId,
    username = username,
    time = time,
    type = type.name,
    status = status.name,
    progress = progress,
    taskLimit = limit,
    repeatCount = repeatCount,
    intervalMinutes = intervalMinutes,
    targetUrl = targetUrl,
    contentPrompt = contentPrompt,
    contentText = contentText,
    mediaUri = mediaUri,
    useGemini = useGemini,
    updatedAt = now,
)

internal fun TaskEntity.toDomain() = ScheduledTask(
    id = id,
    accountId = accountId,
    username = username,
    time = time,
    type = runCatching { TaskType.valueOf(type) }.getOrDefault(TaskType.SYNC),
    status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.QUEUED),
    progress = progress,
    limit = taskLimit,
    repeatCount = repeatCount,
    intervalMinutes = intervalMinutes,
    targetUrl = targetUrl,
    contentPrompt = contentPrompt,
    contentText = contentText,
    mediaUri = mediaUri,
    useGemini = useGemini,
)

internal fun AutomationRuntimeState.toCheckpoint(now: Long = System.currentTimeMillis()) = RuntimeCheckpointEntity(
    taskId = taskId,
    username = username,
    taskType = taskType?.name,
    action = action?.name,
    status = status.name,
    verifiedCount = verifiedCount,
    taskLimit = limit,
    message = message,
    lastTarget = lastTarget,
    lastActionAt = lastActionAt,
    detectedAccount = detectedAccount,
    accountVerified = accountVerified,
    cooldownUntil = cooldownUntil,
    rateLimitRetries = rateLimitRetries,
    repeatCount = repeatCount,
    perCycleLimit = perCycleLimit,
    intervalMinutes = intervalMinutes,
    cycleIndex = cycleIndex,
    cycleWaitUntil = cycleWaitUntil,
    unfollowRevertCount = unfollowRevertCount,
    savedAt = now,
    safeResumeRequired = status !in setOf(RuntimeStatus.IDLE, RuntimeStatus.COMPLETED, RuntimeStatus.FAILED),
)

internal fun RuntimeStatus.toTaskStatus(): TaskStatus = when (this) {
    RuntimeStatus.COMPLETED -> TaskStatus.COMPLETED
    RuntimeStatus.FAILED -> TaskStatus.FAILED
    RuntimeStatus.PAUSED, RuntimeStatus.COOLDOWN -> TaskStatus.PAUSED
    RuntimeStatus.IDLE -> TaskStatus.QUEUED
    else -> TaskStatus.RUNNING
}


internal fun TargetAccount.toEntity(now: Long = System.currentTimeMillis()) = TargetAccountEntity(
    id = id,
    ownerAccountId = ownerAccountId,
    handle = handle,
    active = active,
    updatedAt = now,
)

internal fun TargetAccountEntity.toDomain() = TargetAccount(
    id = id,
    ownerAccountId = ownerAccountId,
    handle = handle,
    active = active,
)
