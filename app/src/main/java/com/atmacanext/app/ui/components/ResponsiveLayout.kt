package com.atmacanext.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * UI-only screen profile. Automation must never depend on these dimensions.
 */
enum class AtmacaWindowClass { COMPACT, MEDIUM, EXPANDED }

@Composable
fun rememberAtmacaWindowClass(): AtmacaWindowClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp >= 840 -> AtmacaWindowClass.EXPANDED
        widthDp >= 600 -> AtmacaWindowClass.MEDIUM
        else -> AtmacaWindowClass.COMPACT
    }
}

const val MAX_ATMACA_ACCOUNTS = 10
