package com.atmacanext.app.ui.theme

import androidx.compose.ui.graphics.Color

/** All three palettes provide their own accessible user and assistant contrast pairs. */
object PilotBubbleColors {
    fun current(mine: Boolean): Pair<Color, Color> =
        if (mine) AtmacaBlue to AtmacaNavy else CardBackground to TextPrimary
}
