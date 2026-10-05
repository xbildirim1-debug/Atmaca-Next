# Buse release rules.
# Room/KSP and AndroidX publish their own consumer rules; keep only app entry points referenced by XML/manifest.
-keep class com.buse.app.BuseApplication { *; }
-keep class com.buse.app.MainActivity { *; }
-keep class com.buse.app.automation.BuseAccessibilityService { *; }
-keep class com.buse.app.service.AutomationForegroundService { *; }
-keep class com.buse.app.scheduling.BootRescheduleReceiver { *; }
-keep class com.buse.app.scheduling.ScheduledTaskWorker { *; }
