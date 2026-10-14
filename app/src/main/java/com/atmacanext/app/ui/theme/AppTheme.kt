package com.atmacanext.app.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val title: String) { LIGHT("Açık"), DARK("Koyu"), GRAY("Gri") }

data class AtmacaPalette(
    val background: Color,
    val card: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val sky: Color,
    val success: Color,
    val warning: Color,
    val purple: Color,
    val teal: Color,
    val error: Color,
    val lightBars: Boolean,
)

object AtmacaThemeState {
    var mode by mutableStateOf(AppThemeMode.LIGHT)
        private set

    val palette: AtmacaPalette
        get() = palettes.getValue(mode)

    fun apply(next: AppThemeMode) {
        mode = next
    }

    private val palettes = mapOf(
        AppThemeMode.LIGHT to AtmacaPalette(
            background = Color(0xFFF3F4F0), card = Color(0xFFFFFFFF), text = Color(0xFF171A1F),
            muted = Color(0xFF667068), line = Color(0xFFE3E6E0), accent = Color(0xFF1A1D23),
            onAccent = Color(0xFFF7F8F5), sky = Color(0xFFE7F6C9), success = Color(0xFF1C8C54),
            warning = Color(0xFFB7791F), purple = Color(0xFF5B4B8A), teal = Color(0xFF1C6B66),
            error = Color(0xFFC24141), lightBars = true,
        ),
        AppThemeMode.DARK to AtmacaPalette(
            background = Color(0xFF0E1116), card = Color(0xFF171B22), text = Color(0xFFF4F6F8),
            muted = Color(0xFF9AA3B2), line = Color(0xFF2A3140), accent = Color(0xFFC6F369),
            onAccent = Color(0xFF142006), sky = Color(0xFF24301A), success = Color(0xFF7DDCB0),
            warning = Color(0xFFFFC56D), purple = Color(0xFFB7A6FF), teal = Color(0xFF7EE0FF),
            error = Color(0xFFFF9B9B), lightBars = false,
        ),
        AppThemeMode.GRAY to AtmacaPalette(
            background = Color(0xFF2A2D31), card = Color(0xFF3A3E44), text = Color(0xFFF3F4F6),
            muted = Color(0xFFC2C6CC), line = Color(0xFF525860), accent = Color(0xFFE7E9EC),
            onAccent = Color(0xFF1C1E21), sky = Color(0xFF4A4F56), success = Color(0xFF8FCB9A),
            warning = Color(0xFFE2C27A), purple = Color(0xFFC7B8E8), teal = Color(0xFFB7D4D2),
            error = Color(0xFFFFB4B4), lightBars = false,
        ),
    )
}

object AppThemeStore {
    private const val PREFS = "atmaca_theme_v1"
    private const val KEY = "mode"

    fun load(context: Context) {
        val stored = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, AppThemeMode.LIGHT.name)
        AtmacaThemeState.apply(AppThemeMode.entries.firstOrNull { it.name == stored } ?: AppThemeMode.LIGHT)
    }

    fun select(context: Context, mode: AppThemeMode) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, mode.name).apply()
        AtmacaThemeState.apply(mode)
    }
}
