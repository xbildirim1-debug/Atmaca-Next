# Atmaca Next release rules.
# Room/KSP and AndroidX publish their own consumer rules; keep only app entry points referenced by XML/manifest.
-keep class com.atmacanext.app.AtmacaNextApplication { *; }
-keep class com.atmacanext.app.MainActivity { *; }
-keep class com.atmacanext.app.automation.AtmacaAccessibilityService { *; }
-keep class com.atmacanext.app.service.AutomationForegroundService { *; }
-keep class com.atmacanext.app.scheduling.BootRescheduleReceiver { *; }
-keep class com.atmacanext.app.scheduling.ScheduledTaskWorker { *; }
