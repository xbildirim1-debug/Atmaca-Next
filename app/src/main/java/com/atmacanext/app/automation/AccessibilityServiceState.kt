package com.atmacanext.app.automation

import android.content.Context
import com.atmacanext.app.scheduling.DeviceReadinessChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AccessibilityHealth(
    val enabled: Boolean = false,
    val connected: Boolean = false,
    val operational: Boolean = false,
    val lastEventAt: Long? = null,
    val activePackage: String? = null,
    val activeScreen: XScreen = XScreen.UNKNOWN,
    val popup: PopupType = PopupType.NONE,
    val revoked: Boolean = false,
)

object AccessibilityServiceState {
    private val _health = MutableStateFlow(AccessibilityHealth())
    val health = _health.asStateFlow()

    fun onConnected() {
        _health.update { it.copy(enabled = true, connected = true, operational = true, revoked = false) }
    }

    fun onDisconnected() {
        _health.update { it.copy(connected = false, operational = false) }
    }

    fun onEvent(packageName: String?, screen: XScreen, popup: PopupType) {
        _health.update { current -> current.copy(
            enabled = true,
            connected = true,
            operational = true,
            lastEventAt = System.currentTimeMillis(),
            activePackage = packageName,
            activeScreen = screen,
            popup = popup,
        ) }
    }

    /** Android ayarındaki kalıcı yetki ile proses içindeki canlı bağlantı aynı şey değildir. */
    fun refreshEnabled(context: Context) {
        val enabled = DeviceReadinessChecker.read(context.applicationContext).accessibilityEnabled
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wasEnabled = prefs.getBoolean(KEY_WAS_ENABLED, false)
        if (enabled) prefs.edit().putBoolean(KEY_WAS_ENABLED, true).apply()
        val alive = AtmacaAccessibilityService.peek() != null
        _health.update { current ->
            current.copy(
                enabled = enabled,
                connected = if (enabled) current.connected || alive else false,
                operational = enabled && (current.connected || alive),
                revoked = wasEnabled && !enabled,
            )
        }
    }

    private const val PREFS = "atmaca_a11y"
    private const val KEY_WAS_ENABLED = "was_enabled"
}
