package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo

object ScanPopupRecovery {
    internal fun mayDismissLabel(label: String?, popup: PopupType): Boolean =
        popup in setOf(PopupType.NOTIFICATION_PROMPT, PopupType.CONTACT_SYNC, PopupType.PROMOTION, PopupType.PERMISSION_PROMPT) &&
            XUiVocabulary.normalize(label) in setOf("şimdi değil", "not now", "hayır teşekkürler", "no thanks", "atla", "skip", "kapat", "close", "iptal", "cancel")

    fun dismiss(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, popup: PopupType): Boolean {
        val button = AccessibilityTree.nodes(root, 400).firstOrNull { node ->
            node.isVisibleToUser && TargetVerifier.isSafeClickable(node) &&
                listOf(node.text?.toString(), node.contentDescription?.toString()).any { mayDismissLabel(it, popup) }
        }
        return button?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true || service.pressBack()
    }
}
