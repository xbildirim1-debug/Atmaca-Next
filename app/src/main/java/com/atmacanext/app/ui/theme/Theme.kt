package com.atmacanext.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private fun scheme(palette: AtmacaPalette) = if (palette.lightBars) {
    lightColorScheme(
        primary = palette.accent, onPrimary = palette.onAccent,
        primaryContainer = palette.sky, onPrimaryContainer = palette.accent,
        secondary = palette.teal, onSecondary = palette.background,
        background = palette.background, onBackground = palette.text,
        surface = palette.card, onSurface = palette.text,
        surfaceVariant = palette.background, onSurfaceVariant = palette.muted,
        outline = palette.line, error = palette.error,
    )
} else {
    darkColorScheme(
        primary = palette.accent, onPrimary = palette.onAccent,
        primaryContainer = palette.sky, onPrimaryContainer = palette.accent,
        secondary = palette.teal, onSecondary = palette.background,
        background = palette.background, onBackground = palette.text,
        surface = palette.card, onSurface = palette.text,
        surfaceVariant = palette.background, onSurfaceVariant = palette.muted,
        outline = palette.line, error = palette.error,
    )
}

@Composable
fun AtmacaNextTheme(content: @Composable () -> Unit) {
    val palette = AtmacaThemeState.palette
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = palette.lightBars
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = palette.lightBars
        }
    }
    MaterialTheme(
        colorScheme = scheme(palette),
        typography = Typography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)),
        content = content,
    )
}
