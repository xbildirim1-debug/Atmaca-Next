package com.atmacanext.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AtmacaScheme = darkColorScheme(
    primary = AtmacaBlue, onPrimary = AtmacaNavy,
    primaryContainer = AtmacaSky, onPrimaryContainer = AtmacaBlue,
    secondary = Teal, onSecondary = AppBackground,
    background = AppBackground, surface = CardBackground,
    onBackground = TextPrimary, onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary, outline = Divider, error = ErrorRed,
)

@Composable
fun AtmacaNextTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AtmacaScheme, typography = Typography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        content = content)
}
