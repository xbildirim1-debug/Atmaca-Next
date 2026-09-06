package com.atmacanext.app.scheduling

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager


data class DeviceReadiness(
    val interactive: Boolean,
    val unlocked: Boolean,
    val accessibilityEnabled: Boolean,
) {
    val readyForUiAutomation: Boolean
        get() = interactive && unlocked && accessibilityEnabled

    val blockingReason: String?
        get() = when {
            !interactive -> "Ekran kapalı"
            !unlocked -> "Cihaz kilitli"
            !accessibilityEnabled -> "Atmaca erişilebilirlik servisi kapalı"
            else -> null
        }
}

object DeviceReadinessChecker {
    fun read(context: Context): DeviceReadiness {
        val power = context.getSystemService(PowerManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
        val packageName = context.packageName
        val enabledByManager = accessibilityManager
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { it.resolveInfo?.serviceInfo?.packageName == packageName }
            ?: false
        // MIUI can keep the service enabled in Settings while the manager list
        // is briefly empty after a process restart. The persisted component list
        // is an independent permission signal; live connectivity remains tracked
        // separately by AccessibilityServiceState.
        val enabledBySettings = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )?.split(':')?.any { component ->
            component.substringBefore('/').equals(packageName, ignoreCase = true)
        } == true
        val enabled = enabledByManager || enabledBySettings
        return DeviceReadiness(
            interactive = power?.isInteractive ?: true,
            unlocked = !(keyguard?.isDeviceLocked ?: false),
            accessibilityEnabled = enabled,
        )
    }
}
