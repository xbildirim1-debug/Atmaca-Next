package com.buse.app.domain.policy

import com.buse.app.domain.model.ScheduledTask
import com.buse.app.domain.model.TaskType
import com.buse.app.domain.model.TaskStatus

enum class BuseTaskMode(val title: String, val description: String) {
    UNFOLLOW("Takipten Çıkma", "Takip edilen kullanıcıları seçtiğin limite kadar çıkarır."),
    NON_FOLLOWERS("Takip Etmeyenleri Çıkma", "İlk 100 kişi kalır. 101. kişiden aşağıya doğru, seni takip etmeyenler çıkarılır."),
}

object BuseTaskPolicy {
    const val NON_FOLLOWER_MARKER = "buse:non-followers:v1"
    fun mode(task: ScheduledTask): BuseTaskMode =
        if (task.contentPrompt == NON_FOLLOWER_MARKER) BuseTaskMode.NON_FOLLOWERS else BuseTaskMode.UNFOLLOW

    fun accepts(task: ScheduledTask): Boolean = task.type == TaskType.UNFOLLOW &&
        (task.contentPrompt == null || task.contentPrompt == NON_FOLLOWER_MARKER) &&
        !task.useGemini && task.targetUrl == null && task.mediaUri == null && task.contentText == null &&
        task.limit in 1..35 && task.repeatCount in 1..100 && task.intervalMinutes in 1..1440

    fun isNonFollower(task: ScheduledTask?): Boolean = task?.type == TaskType.UNFOLLOW &&
        task.contentPrompt == NON_FOLLOWER_MARKER

    /** Older Buse releases could persist COMPLETED after finding only part of the requested limit. */
    fun completionNeedsRecovery(task: ScheduledTask): Boolean = accepts(task) &&
        task.status == TaskStatus.COMPLETED && task.progress < task.totalLimit
}
