package com.atmacanext.app.data.report

import android.content.Context
import android.os.Build
import com.atmacanext.app.BuildConfig
import com.atmacanext.app.automation.AccessibilityServiceState
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.automation.XUiDiagnostics
import com.atmacanext.app.data.repository.AtmacaRepository
import com.atmacanext.app.data.settings.SettingsStore
import com.atmacanext.app.scheduling.DeviceReadinessChecker
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ErrorReportExporter(
    private val context: Context,
    private val repository: AtmacaRepository,
    private val settingsStore: SettingsStore,
) {
    suspend fun export(): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val directory = File(context.getExternalFilesDir(null) ?: context.filesDir, "reports").apply { mkdirs() }
        val target = File(directory, "AtmacaNext_Report_$stamp.zip")

        val runtime = AutomationController.state.value
        val accessibility = AccessibilityServiceState.health.value
        val settings = settingsStore.settings.first()
        val accounts = repository.snapshotAccounts()
        val tasks = repository.snapshotTasks()
        val logs = repository.snapshotLogs(2_000)
        val queueCheckpoint = repository.snapshotQueueCheckpoint()
        val queueItems = repository.snapshotQueueItems()
        val dailyUsage = repository.snapshotTodayUsage()
        val targets = repository.snapshotTargets()
        val deviceReadiness = DeviceReadinessChecker.read(context)

        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            zip.putText("device.txt", buildString {
                appendLine("appVersion=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("android=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}")
                appendLine("manufacturer=${Build.MANUFACTURER}")
                appendLine("model=${Build.MODEL}")
                appendLine("product=${Build.PRODUCT}")
                val dm = context.resources.displayMetrics
                appendLine("display=${dm.widthPixels}x${dm.heightPixels} density=${dm.density}")
                appendLine("interactive=${deviceReadiness.interactive}")
                appendLine("unlocked=${deviceReadiness.unlocked}")
                appendLine("accessibilityEnabled=${deviceReadiness.accessibilityEnabled}")
                appendLine("generatedAt=${System.currentTimeMillis()}")
            })

            zip.putText("runtime.txt", buildString {
                appendLine("taskId=${runtime.taskId}")
                appendLine("username=${runtime.username}")
                appendLine("taskType=${runtime.taskType}")
                appendLine("action=${runtime.action}")
                appendLine("status=${runtime.status}")
                appendLine("verified=${runtime.verifiedCount}/${runtime.limit}")
                appendLine("message=${runtime.message}")
                appendLine("lastTarget=${runtime.lastTarget}")
                appendLine("lastActionAt=${runtime.lastActionAt}")
                appendLine("activeScreen=${runtime.activeScreen}")
                appendLine("detectedAccount=${runtime.detectedAccount}")
                appendLine("accountVerified=${runtime.accountVerified}")
                appendLine("navigationRecoveries=${runtime.navigationRecoveries}")
                appendLine("popupRecoveries=${runtime.popupRecoveries}")
                appendLine("listScrolls=${runtime.listScrolls}")
                appendLine("listRecoveries=${runtime.listRecoveries}")
                appendLine("cooldownUntil=${runtime.cooldownUntil}")
                appendLine("rateLimitRetries=${runtime.rateLimitRetries}")
            })

            zip.putText("accessibility.txt", buildString {
                appendLine("connected=${accessibility.connected}")
                appendLine("package=${accessibility.activePackage}")
                appendLine("activeScreen=${accessibility.activeScreen}")
                appendLine("popup=${accessibility.popup}")
                appendLine("lastEventAt=${accessibility.lastEventAt}")
            })

            zip.putText("settings.txt", buildString {
                appendLine("defaultFollowLimit=${settings.defaultFollowLimit}")
                appendLine("defaultUnfollowLimit=${settings.defaultUnfollowLimit}")
                appendLine("dailyFollowLimitPerAccount=${settings.dailyFollowLimitPerAccount}")
                appendLine("dailyUnfollowLimitPerAccount=${settings.dailyUnfollowLimitPerAccount}")
                appendLine("keepLogDays=${settings.keepLogDays}")
                appendLine("betweenActionsMs=${settings.betweenActionsMs}")
                appendLine("accountSwitchSettleMs=${settings.accountSwitchSettleMs}")
                appendLine("betweenTasksMs=${settings.betweenTasksMs}")
                appendLine("continueAfterFailedTask=${settings.continueAfterFailedTask}")
                appendLine("rateLimitCooldownMinutes=${settings.rateLimitCooldownMinutes}")
                appendLine("minimumTweetAgeMinutes=${settings.minimumTweetAgeMinutes}")
                appendLine("tweetsPerTarget=${settings.tweetsPerTarget}")
                appendLine("geminiModel=${settings.geminiModel}")
                appendLine("geminiKeyStored=${com.atmacanext.app.core.AppServices.geminiKeyStore.hasKey()}")
                appendLine("lastReportAt=${settings.lastReportAt}")
            })

            zip.putText("accounts.csv", buildString {
                appendLine("id,username,displayName,followers,following,engagement,health,accent,active,isCurrent,inactiveReason,unfollowRevertCount,aiLanguage,aiTone,lastSyncAt,updatedAt")
                accounts.forEach { a ->
                    appendLine(listOf(a.id, a.username, a.displayName, a.followers, a.following, a.engagement, a.health, a.accent, a.active, a.isCurrent, a.inactiveReason, a.unfollowRevertCount, a.aiLanguage, a.aiTone, a.lastSyncAt, a.updatedAt).joinToString(",") { csv(it) })
                }
            })

            zip.putText("tasks.csv", buildString {
                appendLine("id,accountId,username,type,status,progress,perCycleLimit,repeatCount,intervalMinutes,targetUrl,useGemini,lastTarget,lastActionAt,updatedAt")
                tasks.forEach { t ->
                    appendLine(listOf(t.id, t.accountId, t.username, t.type, t.status, t.progress, t.taskLimit, t.repeatCount, t.intervalMinutes, t.targetUrl, t.useGemini, t.lastTarget, t.lastActionAt, t.updatedAt).joinToString(",") { csv(it) })
                }
            })


            zip.putText("queue.txt", buildString {
                appendLine("sessionId=${queueCheckpoint?.sessionId}")
                appendLine("status=${queueCheckpoint?.status}")
                appendLine("currentIndex=${queueCheckpoint?.currentIndex}")
                appendLine("message=${queueCheckpoint?.message}")
                appendLine("startedAt=${queueCheckpoint?.startedAt}")
                appendLine("updatedAt=${queueCheckpoint?.updatedAt}")
            })

            zip.putText("queue_items.csv", buildString {
                appendLine("taskId,ordinal,accountId,username,taskType,status,note,updatedAt")
                queueItems.forEach { item ->
                    appendLine(listOf(item.taskId, item.ordinal, item.accountId, item.username, item.taskType, item.status, item.note, item.updatedAt).joinToString(",") { csv(it) })
                }
            })

            zip.putText("daily_account_usage.csv", buildString {
                appendLine("dateKey,accountId,actionType,verifiedCount,updatedAt")
                dailyUsage.forEach { usage ->
                    appendLine(listOf(usage.dateKey, usage.accountId, usage.actionType, usage.verifiedCount, usage.updatedAt).joinToString(",") { csv(it) })
                }
            })

            zip.putText("target_accounts.csv", buildString {
                appendLine("id,ownerAccountId,handle,active,updatedAt")
                targets.forEach { target ->
                    appendLine(listOf(target.id, target.ownerAccountId, target.handle, target.active, target.updatedAt).joinToString(",") { csv(it) })
                }
            })

            zip.putText("automation_logs.csv", buildString {
                appendLine("id,timestamp,level,category,taskId,username,message,details")
                logs.asReversed().forEach { log ->
                    appendLine(listOf(log.id, log.timestamp, log.level, log.category, log.taskId, log.username, log.message, log.details).joinToString(",") { csv(it) })
                }
            })

            val probe = XUiDiagnostics.probeFile(context)
            if (probe.exists()) {
                zip.putText("x_accessibility_probe.txt", probe.readText())
            }
            val previousProbe = XUiDiagnostics.rotatedProbeFile(context)
            if (previousProbe.exists()) {
                zip.putText("x_accessibility_probe_previous.txt", previousProbe.readText())
            }
        }

        settingsStore.recordReport(target.absolutePath)
        repository.log("INFO", "REPORT", runtime.taskId, runtime.username, "Hata raporu oluşturuldu", target.absolutePath)
        return target
    }

    private fun ZipOutputStream.putText(name: String, text: String) {
        putNextEntry(ZipEntry(name))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun csv(value: Any?): String {
        val raw = value?.toString().orEmpty()
        return "\"${raw.replace("\"", "\"\"")}\""
    }
}
