package com.atmacanext.app.automation

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class PopupClassifierTest {
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
