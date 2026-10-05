package com.buse.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BuseScheme = lightColorScheme(
    primary = BusePink, onPrimary = Color.White,
    primaryContainer = BuseBlush, onPrimaryContainer = TextPrimary,
    secondary = BuseRose, onSecondary = TextPrimary,
    secondaryContainer = BuseBlush, onSecondaryContainer = TextPrimary,
    background = AppBackground, surface = CardBackground,
    onBackground = TextPrimary, onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary, outline = Divider, error = ErrorRed,
)

@Composable
fun BuseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BuseScheme, typography = Typography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        content = content)
}
