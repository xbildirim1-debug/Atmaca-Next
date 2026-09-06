package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo

/** Result of one conservative popup-recovery attempt. */
enum class PopupResolutionType {
    NONE,
    DISMISSED,
    RETRIED,
    COOLDOWN_REQUIRED,
    UNSAFE,
}

data class PopupResolution(
    val type: PopupResolutionType,
    val message: String,
)

/**
 * Handles only exact, low-risk popup buttons.  There is no fuzzy text matching and no
 * coordinate fallback.  Unknown dialogs without a known dismiss button are reported UNSAFE.
 */
object PopupHandler {
    private val dismissLabels = XUiVocabulary.safeDismissLabels
    private val acknowledgeLabels = setOf("tamam", "ok", "anladım", "got it")
    private val retryLabels = setOf("tekrar dene", "yeniden dene", "try again", "retry")

    fun handle(root: AccessibilityNodeInfo?, popup: PopupType): PopupResolution = when (popup) {
        PopupType.NONE -> PopupResolution(PopupResolutionType.NONE, "Popup yok")
        PopupType.ACTION_CONFIRMATION -> PopupResolution(
            PopupResolutionType.UNSAFE,
            "İşleme ait onay penceresi genel popup yöneticisi tarafından kapatılmadı",
        )
        PopupType.RATE_LIMIT -> {
            val dismissed = clickExact(root, acknowledgeLabels + dismissLabels)
            PopupResolution(
                if (dismissed) PopupResolutionType.COOLDOWN_REQUIRED else PopupResolutionType.UNSAFE,
                if (dismissed) "İşlem sınırı penceresi kapatıldı; cooldown gerekli" else "İşlem sınırı penceresinde güvenli kapatma düğmesi bulunamadı",
            )
        }
        PopupType.RETRYABLE_ERROR -> {
            val retried = clickExact(root, retryLabels)
            PopupResolution(
                if (retried) PopupResolutionType.RETRIED else PopupResolutionType.UNSAFE,
                if (retried) "Geçici hata için tam eşleşen Tekrar Dene düğmesine basıldı" else "Geçici hata ekranında güvenli Tekrar Dene düğmesi bulunamadı",
            )
        }
        PopupType.GENERIC_ERROR -> {
            val dismissed = clickExact(root, acknowledgeLabels + dismissLabels)
            PopupResolution(
                if (dismissed) PopupResolutionType.DISMISSED else PopupResolutionType.UNSAFE,
                if (dismissed) "Genel hata penceresi güvenli düğmeyle kapatıldı" else "Genel hata penceresi güvenle kapatılamadı",
            )
        }
        PopupType.NOTIFICATION_PROMPT,
        PopupType.CONTACT_SYNC,
        PopupType.PROMOTION,
        PopupType.PERMISSION_PROMPT -> {
            val dismissed = clickExact(root, dismissLabels)
            PopupResolution(
                if (dismissed) PopupResolutionType.DISMISSED else PopupResolutionType.UNSAFE,
                if (dismissed) "İsteğe bağlı X penceresi güvenli şekilde kapatıldı" else "Pencerede tam eşleşen güvenli kapatma düğmesi bulunamadı",
            )
        }
        PopupType.UNKNOWN_DIALOG -> {
            val dismissed = clickExact(root, dismissLabels)
            PopupResolution(
                if (dismissed) PopupResolutionType.DISMISSED else PopupResolutionType.UNSAFE,
                if (dismissed) "Bilinmeyen dialog yalnızca açık bir Vazgeç/Şimdi Değil/Kapat düğmesiyle kapatıldı" else "Bilinmeyen dialog için güvenli otomatik karar verilemedi",
            )
        }
    }

    private fun clickExact(root: AccessibilityNodeInfo?, labels: Set<String>): Boolean {
        val match = NodeSelector.best(
            root,
            NodeSelector.Query(
                exactTexts = labels,
                contentDescriptions = labels,
                requireClickable = true,
            ),
        )?.node ?: return false
        return TargetVerifier.matchesExactLabel(match, labels) &&
            match.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
}
