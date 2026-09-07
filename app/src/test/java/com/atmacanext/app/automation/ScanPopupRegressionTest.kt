package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test

class ScanPopupRegressionTest {
    private fun n(text: String, button: Boolean = false, visible: Boolean = true) = NodeSnapshot(
        text, null, null, if (button) "android.widget.Button" else "TextView", button, true, Rect(), visible = visible)

    @Test fun timelineWordsDoNotMakeUnknownDialog() {
        val nodes = listOf(n("Home"), n("Search"), n("Notifications"), n("çok güzel tamam"), n("Follow", true))
        assertEquals(PopupType.NONE, PopupClassifier.classify(nodes))
        assertEquals(XScreen.HOME, ScreenDetector.detect(nodes))
    }
    @Test fun realAcknowledgementButtonIsStillDetected() {
        assertEquals(PopupType.UNKNOWN_DIALOG, PopupClassifier.classify(listOf(n("Yeni bir uyarı"), n("Tamam", true))))
    }
    @Test fun hiddenDismissButtonDoesNotStopScanning() {
        assertEquals(PopupType.NONE, PopupClassifier.classify(listOf(n("Home"), n("Tamam", true, false))))
    }
    @Test fun unknownAndConfirmationDialogsAreNeverAccepted() {
        assertFalse(ScanPopupRecovery.mayDismissLabel("Tamam", PopupType.UNKNOWN_DIALOG))
        assertFalse(ScanPopupRecovery.mayDismissLabel("Takipten çık", PopupType.ACTION_CONFIRMATION))
        assertFalse(ScanPopupRecovery.mayDismissLabel("İzin ver", PopupType.PERMISSION_PROMPT))
    }
    @Test fun onlyExactNegativeActionsDismissOptionalPrompts() {
        assertTrue(ScanPopupRecovery.mayDismissLabel("Şimdi değil", PopupType.NOTIFICATION_PROMPT))
        assertFalse(ScanPopupRecovery.mayDismissLabel("Şimdi değil diyen bir gönderi", PopupType.NOTIFICATION_PROMPT))
    }
}
