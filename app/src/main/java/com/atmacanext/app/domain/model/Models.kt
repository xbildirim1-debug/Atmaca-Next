package com.atmacanext.app.domain.model

data class Account(
    val id: String,
    val username: String,
    val displayName: String,
    val followers: String,
    val following: String,
    val engagement: String,
    val health: Int,
    val accent: AccountAccent,
    val active: Boolean = true,
    val isCurrent: Boolean = false,
    val inactiveReason: String? = null,
    val unfollowRevertCount: Int = 0,
    val aiPersona: String = "",
    val aiLanguage: String = "tr",
    val aiTone: String = "doğal",
)

enum class AccountAccent { BLUE, TEAL, PURPLE, ORANGE, NAVY }

enum class TaskType(val title: String) {
    TEXT_TWEET("Tweet"),
    IMAGE_TWEET("Resimli tweet"),
    FOLLOW("Takip"),
    LIKE("Beğeni"),
    RETWEET("Retweet"),
    BOOKMARK("Kaydet"),
    COMMENT("Yorum"),
    QUOTE("Alıntı"),
    UNFOLLOW("Takipten çık"),
    VERIFIED_FOLLOW("Onaylı kullanıcı takibi"),
    COMMENTER_FOLLOW("Yorumcu takip etme"),
    RETWEETER_FOLLOW("Retweetçi takip etme"),
    QUOTER_FOLLOW("Alıntıcı takip etme"),

    // V23 ve daha eski veritabanlarındaki görev adlarını okuyabilmek için tutulur.
    // Yeni görev ekranında gösterilmezler.
    PUBLISH("Eski içerik paylaşımı"),
    TREND("Trend paylaşımı"),
    COMMUNITY("Topluluk etkileşimi"),
    SYNC("Hesap senkronizasyonu");

    val requiresLink: Boolean
        get() = this in setOf(FOLLOW, LIKE, RETWEET, BOOKMARK, COMMENT, QUOTE)

    val supportsGemini: Boolean
        get() = this in setOf(TEXT_TWEET, IMAGE_TWEET, COMMENT, QUOTE)

    val isDiscoveryFollow: Boolean
        get() = this in setOf(COMMENTER_FOLLOW, RETWEETER_FOLLOW, QUOTER_FOLLOW)

    val isFollowLikeAction: Boolean
        get() = this in setOf(FOLLOW, VERIFIED_FOLLOW, COMMENTER_FOLLOW, RETWEETER_FOLLOW, QUOTER_FOLLOW)
}

enum class TaskStatus { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED }

data class ScheduledTask(
    val id: String,
    val accountId: String,
    val username: String,
    /** Kept only so a V23 database can be migrated without losing rows. */
    val time: String = "",
    val type: TaskType,
    val status: TaskStatus = TaskStatus.QUEUED,
    val progress: Int = 0,
    val limit: Int = 35,
    val repeatCount: Int = 1,
    val intervalMinutes: Int = 1,
    val targetUrl: String? = null,
    val contentPrompt: String? = null,
    val contentText: String? = null,
    val mediaUri: String? = null,
    val useGemini: Boolean = false,
) {
    val totalLimit: Int
        get() = limit.coerceAtLeast(1) * repeatCount.coerceAtLeast(1)
}

data class TargetAccount(
    val id: String,
    val ownerAccountId: String,
    val handle: String,
    val active: Boolean = true,
)

data class DashboardMetric(
    val label: String,
    val value: String,
    val delta: String,
)
