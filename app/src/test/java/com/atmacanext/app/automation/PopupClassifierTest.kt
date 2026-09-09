package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class PopupClassifierTest {
    @Test fun bosunatiklamaBioIsNotNotificationDialog() {
        assertEquals(PopupType.NONE, PopupClassifier.classify(listOf(
            node("Boşuna Tıklama"), node("@bosunatiklama"),
            node("Dijital Medya ve Haber | Gündemi bir tıkla takip etmek için bildirimleri açın. info@bosunatiklama.com"),
            node("Takip et", true), node("Mesaj gönder", true), node("Gönderiler", true),
        )))
    }

    @Test fun postAndBioPromptPhrasesWithoutDialogAreIgnored() {
        for (phrase in listOf("Turn on notifications", "Bildirimleri etkinleştir", "Sync contacts", "Try premium", "Try Grok")) {
            assertEquals(phrase, PopupType.NONE, PopupClassifier.classify(listOf(node(phrase), node("Takip et", true))))
        }
    }

    @Test fun realNotificationSheetWithDismissControlIsStillDetected() {
        assertEquals(PopupType.NOTIFICATION_PROMPT, PopupClassifier.classify(listOf(
            node("Bildirimleri aç"), node("Şimdi değil", true),
        )))
    }

    @Test fun nativeNotificationDialogWithoutDismissRemainsBlocking() {
        assertEquals(PopupType.NOTIFICATION_PROMPT, PopupClassifier.classify(listOf(
            node("Turn on notifications").copy(className = "android.app.Dialog"),
        )))
    }

    @Test fun hiddenOrPlainTextDismissLabelDoesNotTurnBioIntoPrompt() {
        for (dismiss in listOf(node("Şimdi değil"), node("Şimdi değil", true).copy(visible = false))) {
            assertEquals(PopupType.NONE, PopupClassifier.classify(listOf(node("Bildirimleri açın"), dismiss)))
        }
    }

    private fun node(text: String, clickable: Boolean = false) = NodeSnapshot(
        text = text,
        contentDescription = null,
        viewId = null,
        className = if (clickable) "android.widget.Button" else "android.widget.TextView",
        clickable = clickable,
        enabled = true,
        bounds = Rect(),
    )

    @Test
    fun rateLimitBeatsGenericDialog() {
        assertEquals(
            PopupType.RATE_LIMIT,
            PopupClassifier.classify(listOf(node("Daha sonra tekrar dene"), node("Tamam", true))),
        )
    }

    @Test
    fun unfollowConfirmationIsNotTreatedAsGenericPopup() {
        assertEquals(
            PopupType.ACTION_CONFIRMATION,
            PopupClassifier.classify(listOf(node("Takipten çık", true), node("İptal", true))),
        )
    }

    @Test
    fun retryableErrorIsSeparatedFromPermanentError() {
        assertEquals(
            PopupType.RETRYABLE_ERROR,
            PopupClassifier.classify(listOf(node("Bir sorun oluştu"), node("Tekrar dene", true))),
        )
    }
}
