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
)

object AccessibilityServiceState {
    private val _health = MutableStateFlow(AccessibilityHealth())
    val health = _health.asStateFlow()

    fun onConnected() {
        _health.update { it.copy(enabled = true, connected = true, operational = true) }
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
        _health.update { current ->
            current.copy(
                enabled = enabled,
                connected = if (enabled) current.connected else false,
                operational = enabled && current.connected,
            )
        }
    }
}
