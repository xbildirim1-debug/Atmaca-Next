package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

enum class PopupType {
    NONE, ACTION_CONFIRMATION, RATE_LIMIT, RETRYABLE_ERROR, GENERIC_ERROR,
    NOTIFICATION_PROMPT, CONTACT_SYNC, PROMOTION, PERMISSION_PROMPT, UNKNOWN_DIALOG,
}

/** Conservative X/Twitter popup classification. Unknown dialogs are never guessed through. */
object PopupClassifier {
    fun classify(root: AccessibilityNodeInfo?): PopupType = classify(AccessibilityTree.snapshots(root))

    internal fun classify(nodes: List<NodeSnapshot>): PopupType {
        if (nodes.isEmpty()) return PopupType.NONE
        val corpus = nodes.filter { it.visible }.joinToString(" ") {
            listOfNotNull(it.text, it.contentDescription, it.viewId).joinToString(" ")
        }.lowercase(Locale.ROOT).replace("\u0307", "")
        fun any(vararg tokens: String) = tokens.any(corpus::contains)

        if (any("takibi bırak", "takipten çık", "unfollow") && any("iptal", "cancel", "vazgeç")) {
            return PopupType.ACTION_CONFIRMATION
        }

        if (any(
                "daha sonra tekrar dene", "try again later",
                "şu anda daha fazla kişiyi takip edemezsin", "you are unable to follow more people at this time",
                "takip limitine ulaştın", "you've reached your follow limit", "you’ve reached your follow limit",
                "günlük limite ulaştın", "daily limit",
                "rate limit", "too many requests",
            )) return PopupType.RATE_LIMIT

        val retry = any("tekrar dene", "yeniden dene", "try again", "retry")
        val error = any(
            "bir sorun oluştu", "something went wrong", "bir hata oluştu", "an error occurred",
            "internet bağlantısı yok", "no internet connection", "gönderiler şu anda alınamıyor", "posts aren't loading right now",
        )
        if (error && retry) return PopupType.RETRYABLE_ERROR
        if (error) return PopupType.GENERIC_ERROR

        // Profile bios and posts can contain every prompt phrase below. Text alone
        // is not an interruption: require a dialog container or an actual dismiss
        // control. This also retains Compose sheets without a native Dialog class.
        val dialog = ScreenDetector.detect(nodes) == XScreen.DIALOG
        val dismissControl = nodes.any { node ->
            node.visible && node.enabled && (node.clickable || node.className.orEmpty().contains("button", true)) &&
                listOfNotNull(node.text, node.contentDescription).any { XUiVocabulary.normalize(it) in XUiVocabulary.safeDismissLabels }
        }
        val promptSurface = dialog || dismissControl
        if (promptSurface && any("bildirimleri aç", "bildirimleri etkinleştir", "turn on notifications", "enable notifications")) {
            return PopupType.NOTIFICATION_PROMPT
        }
        if (promptSurface && any("kişilerini senkronize et", "rehberi senkronize et", "sync contacts", "connect contacts")) {
            return PopupType.CONTACT_SYNC
        }
        if (promptSurface && any("x'in kameraya erişmesine izin ver", "x’in kameraya erişmesine izin ver", "allow x to access", "izin ver") &&
            any("şimdi değil", "not now", "iptal", "cancel")) {
            return PopupType.PERMISSION_PROMPT
        }
        if (promptSurface && any("premium'u dene", "try premium", "grok'u dene", "try grok", "yeni özelliği dene", "try the new")) {
            return PopupType.PROMOTION
        }
        return if (dialog) PopupType.UNKNOWN_DIALOG else PopupType.NONE
    }
}
