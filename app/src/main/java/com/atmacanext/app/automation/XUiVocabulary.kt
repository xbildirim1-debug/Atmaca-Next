package com.atmacanext.app.automation

import java.util.Locale

object XUiVocabulary {
    val followersHeaders = setOf("takipçiler", "takipçi", "followers", "follower")
    val followingHeaders = setOf("takip edilen", "following")
    val verifiedFollowersHeaders = setOf(
        "onaylı takipçiler", "doğrulanmış takipçiler", "verified followers",
        "onaylı", "doğrulanmış", "verified",
    )

    val followActions = setOf("takip et", "geri takip et", "sen de takip et", "sende takip et", "follow", "follow back")
    val followingActions = setOf("takip ediliyor", "takip ediyor", "following")
    val unfollowConfirmationActions = setOf("takipten çık", "takibi bırak", "unfollow")

    val likeActions = setOf("beğen", "like")
    val unlikeActions = setOf("beğeniyi geri al", "beğenmekten vazgeç", "unlike")
    val repostActions = setOf("retweet", "yeniden gönder", "repost")
    val undoRepostActions = setOf("retweeti geri al", "yeniden göndermeyi geri al", "undo repost", "undo retweet")
    val bookmarkActions = setOf("yer işaretlerine ekle", "kaydet", "bookmark")
    val removeBookmarkActions = setOf("yer işaretlerinden kaldır", "kayıttan kaldır", "remove bookmark")
    val replyActions = setOf("yanıtla", "cevapla", "reply")
    val quoteActions = setOf("alıntıla", "alıntı", "quote")
    val postActions = setOf("gönder", "yayınla", "post", "tweetle", "yanıtla", "reply")
    val composerSignals = setOf("neler oluyor", "what is happening", "yanıtını gönder", "post your reply", "gönderi metni", "post text")
    val tweetDetailSignals = setOf("gönderi", "post", "tweet", "görüntülenme", "views", "yanıtlar", "replies")
    val backSignals = setOf("geri", "back", "geri git", "navigate up", "yukarı git")
    val engagementSignals = setOf("retweetler", "reposts", "alıntılar", "quotes", "beğenenler", "likes")

    val profileEntryLabels = setOf("profil", "profile")
    val ownProfileSignals = setOf("profili düzenle", "edit profile")
    val joinedSignals = setOf("katıldı", "joined")

    /** V18 cihaz ağacında görülen gerçek TR etiket dahil. */
    val accountMenuLabels = setOf(
        "gezinti çekmecesini göster", "gezinti çekmecesi", "çekmece", "gezinti",
        "gezinme menüsünü aç", "gezinme menusu",
        "open navigation menu", "show navigation drawer", "show navigation menu",
        "navigation drawer", "navigation menu", "drawer",
        "hesap menüsü", "account menu", "profil fotoğrafı", "profile photo", "profile picture",
    )

    val accountSwitcherLabels = setOf("hesaplar", "accounts", "hesap değiştir", "switch account", "hesapları değiştir", "switch accounts")
    val accountSwitcherSignals = setOf(
        "hesap ekle", "mevcut bir hesap ekle", "add an existing account",
        "yeni hesap oluştur", "create a new account", "hesapları yönet", "manage accounts",
    )

    val drawerSignals = setOf(
        "profil", "profile", "yer işaretleri", "bookmarks", "listeler", "lists", "premium",
        "topluluklar", "communities", "ayarlar ve gizlilik", "settings and privacy",
    )

    val homeSignals = setOf(
        "anasayfa", "home", "ara", "search", "bildirimler", "notifications", "mesajlar", "messages",
        "sana özel", "for you", "topluluklar", "communities", "grok", "keşfet", "explore",
    )

    val emptyListSignals = setOf(
        "henüz takipçi yok", "no followers yet", "henüz kimseyi takip etmiyor", "not following anyone", "nothing to see here",
    )

    val safeDismissLabels = setOf(
        "şimdi değil", "not now", "daha sonra", "later", "atla", "skip", "iptal", "cancel",
        "vazgeç", "dismiss", "kapat", "close", "hayır, teşekkürler", "no thanks",
    )

    val forbiddenProfilePhrases = setOf(
        "daha fazlasını göster", "daha fazla", "show more", "see more", "doğum tarihi", "doğum günü",
        "birthday", "bio", "çevirisine bak", "see translation",
    )

    val structuralLabels: Set<String> = buildSet {
        addAll(followersHeaders); addAll(followingHeaders); addAll(verifiedFollowersHeaders)
        addAll(followActions); addAll(followingActions); addAll(unfollowConfirmationActions)
        addAll(likeActions); addAll(unlikeActions); addAll(repostActions); addAll(undoRepostActions)
        addAll(bookmarkActions); addAll(removeBookmarkActions); addAll(replyActions); addAll(quoteActions)
        addAll(postActions); addAll(composerSignals); addAll(tweetDetailSignals); addAll(engagementSignals)
        addAll(backSignals)
        addAll(profileEntryLabels); addAll(ownProfileSignals); addAll(joinedSignals)
        addAll(accountMenuLabels); addAll(accountSwitcherLabels); addAll(accountSwitcherSignals)
        addAll(drawerSignals); addAll(homeSignals); addAll(emptyListSignals); addAll(safeDismissLabels); addAll(forbiddenProfilePhrases)
    }

    fun normalize(raw: String?): String = raw.orEmpty().lowercase(Locale.ROOT).trim()
    fun containsExact(labels: Collection<String>, vocabulary: Set<String>): Boolean = labels.any { normalize(it) in vocabulary }
}
