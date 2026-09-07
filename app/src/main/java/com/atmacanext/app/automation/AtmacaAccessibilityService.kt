package com.atmacanext.app.automation

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.atmacanext.app.MainActivity
import java.util.concurrent.atomic.AtomicBoolean

/** Yalnızca X/Twitter paketini izler; V19 deeplink koruması ve hesap senkronizasyonu içerir. */
class AtmacaAccessibilityService : AccessibilityService() {
    companion object {
        const val X_PACKAGE = "com.twitter.android"
        private const val RELAUNCH_GAP_MS = 2_800L
        private const val OUTSIDE_SUPPRESS_MS = 3_500L
        private const val EVENT_DEBOUNCE_MS = 220L
        private const val POST_PROCESS_DELAY_MS = 80L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scheduleLock = Any()
    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null
    private val processing = AtomicBoolean(false)
    private val refreshPending = AtomicBoolean(false)
    private var scheduledTick: Runnable? = null
    private var scheduledDue: Long? = null
    @Volatile private var pendingPackageName: String? = null
    private var lastLaunchAt = 0L
    private var lastLaunchKey = ""
    @Volatile private var suppressOutsideUntil: Long = 0L
    private var lastUiLogKey = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (workerThread == null) {
            workerThread = HandlerThread("atmaca-accessibility").also { thread ->
                thread.start()
                workerHandler = Handler(thread.looper)
            }
        }
        AccessibilityServiceState.onConnected()
        AutomationController.attach(this)
        AccountSyncController.attach(this)
        OperationLog.i("SVC", "Erişilebilirlik bağlandı")
        requestAutomationTick(250L)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentEvent = event ?: return
        if (currentEvent.eventType !in setOf(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOWS_CHANGED,
                AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED,
            )
        ) return

        pendingPackageName = currentEvent.packageName?.toString()
        val delay = if (currentEvent.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            currentEvent.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) 100L else EVENT_DEBOUNCE_MS
        requestAutomationTick(delay)
    }

    fun requestAutomationTick(delayMillis: Long) {
        synchronized(scheduleLock) {
            val due = SystemClock.uptimeMillis() + delayMillis.coerceAtLeast(0L)
            if (!AccountScanPolicy.replaceTick(scheduledDue, due)) return
            scheduledTick?.let(mainHandler::removeCallbacks)
            scheduledDue = due
            val tick = Runnable {
                synchronized(scheduleLock) { scheduledTick = null; scheduledDue = null }
                enqueueSnapshot()
            }
            scheduledTick = tick
            mainHandler.postDelayed(tick, delayMillis.coerceAtLeast(0L))
        }
    }

    private fun enqueueSnapshot() {
        if (!processing.compareAndSet(false, true)) {
            refreshPending.set(true)
            return
        }
        val handler = workerHandler
        if (handler == null || !handler.post {
                try {
                    processSnapshot(pendingPackageName)
                } finally {
                    processing.set(false)
                    if (refreshPending.getAndSet(false)) requestAutomationTick(POST_PROCESS_DELAY_MS)
                }
            }
        ) {
            processing.set(false)
        }
    }

    fun launchX(): Boolean {
        val intent=packageManager.getLaunchIntentForPackage(X_PACKAGE) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        return startSafely(intent,"launch")
    }
    fun launchXHome(): Boolean {
        val i=Intent(Intent.ACTION_VIEW,Uri.parse("https://x.com/home")).apply { setPackage(X_PACKAGE); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) }
        return if (startSafely(i,"home")) true else launchX()
    }
    fun launchXProfile(handle:String):Boolean=launchXUrl(handle,"profile") { "https://x.com/$it" }
    fun launchXFollowing(handle:String):Boolean=launchXUrl(handle,"following") { "https://x.com/$it/following" }
    fun launchXFollowers(handle:String):Boolean=launchXUrl(handle,"followers") { "https://x.com/$it/followers" }

    fun launchXWebUrl(url: String, key: String = "target"): Boolean {
        val uri = runCatching { Uri.parse(url.trim()) }.getOrNull() ?: return false
        if (uri.scheme !in setOf("https", "http") || uri.host?.lowercase() !in setOf("x.com", "www.x.com", "twitter.com", "www.twitter.com")) return false
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(X_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        return startSafely(intent, "$key/${uri.path.orEmpty().takeLast(48)}")
    }

    fun launchXComposer(text: String, mediaUri: String?): Boolean {
        if (text.isBlank() && mediaUri.isNullOrBlank()) return false
        val parsedMedia = mediaUri?.takeIf(String::isNotBlank)?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage(X_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(Intent.EXTRA_TEXT, text)
            if (parsedMedia != null) {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, parsedMedia)
                clipData = ClipData.newUri(contentResolver, "Atmaca görseli", parsedMedia)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }
        return startSafely(intent, if (parsedMedia == null) "compose/text" else "compose/image")
    }

    private fun launchXUrl(handle:String,kind:String,url:(String)->String):Boolean {
        val clean=handle.trim().removePrefix("@")
        if (clean.isBlank()) return false
        val i=Intent(Intent.ACTION_VIEW,Uri.parse(url(clean))).apply { setPackage(X_PACKAGE); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) }
        return startSafely(i,"$kind/$clean")
    }

    private fun isAtmacaForeground(): Boolean = runCatching {
        rootInActiveWindow?.packageName?.toString() == packageName
    }.getOrDefault(false)

    fun runOnAutomationThread(action: () -> Unit): Boolean =
        workerHandler?.post { action() } == true

    fun launchAtmaca(onResult: ((Boolean) -> Unit)? = null): Boolean {
        val launchEpoch = lastLaunchAt
        val token = returnGeneration.incrementAndGet()
        fun attempt(index: Int) {
            if (token != returnGeneration.get() || lastLaunchAt != launchEpoch) return
            if (AppForegroundState.resumed || isAtmacaForeground()) {
                OperationLog.i("NAV", "Atmaca Next'e dönüş doğrulandı")
                onResult?.invoke(true)
                return
            }
            if (index == 2) {
                OperationLog.w("NAV", "Doğrudan dönüş doğrulanamadı; son uygulamalar açılıyor")
                performGlobalAction(GLOBAL_ACTION_RECENTS)
                mainHandler.postDelayed({ attempt(3) }, 1_100L)
                return
            }
            if (index == 3) {
                val clicked = openOwnRecentTask()
                OperationLog.i("NAV", "Son uygulamalarda Atmaca kartı tıklama=$clicked")
                mainHandler.postDelayed({ attempt(4) }, 1_100L)
                return
            }
            if (index >= 4) {
                OperationLog.e("NAV", "Atmaca dönüşü doğrudan ve son uygulamalar yoluyla doğrulanamadı")
                runCatching {
                    val notification = com.atmacanext.app.service.AutomationNotification.build(this, "İşlem sona erdi", "Atmaca Next'e dönmek için dokun", includeActions = false)
                    if (android.os.Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        getSystemService(android.app.NotificationManager::class.java)?.notify(1210, notification)
                    }
                }.onFailure { OperationLog.w("NAV", "Dönüş bildirimi gösterilemedi: ${it.javaClass.simpleName}") }
                onResult?.invoke(false)
                return
            }
            // Bring back the existing task first, preserving the screen and saved UI state.
            runCatching {
                getSystemService(ActivityManager::class.java)?.appTasks?.firstOrNull {
                    it.taskInfo?.baseActivity?.packageName == packageName
                }?.moveToFront()
            }.onFailure { OperationLog.w("NAV", "Mevcut pencere öne alınamadı: ${it.javaClass.simpleName}") }
            runCatching {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                })
            }.onFailure { OperationLog.w("NAV", "Atmaca açılışı reddedildi: ${it.javaClass.simpleName}") }
            mainHandler.postDelayed({ attempt(index + 1) }, 700L)
        }
        return mainHandler.post { attempt(0) }
    }

    private fun openOwnRecentTask(): Boolean {
        val root = rootInActiveWindow ?: return false
        val rootPackage = root.packageName?.toString() ?: return false
        val launcher = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
        if (rootPackage !in setOf(launcher, "com.android.systemui", "com.miui.home")) return false
        val title = AccessibilityTree.nodes(root, 700).firstOrNull { node ->
            node.isVisibleToUser && listOf(node.text?.toString(), node.contentDescription?.toString()).any { it?.trim() == "Atmaca Next" }
        } ?: return false
        // Only the exact app-title card may be opened; never swipe or close another app.
        return GestureClick.click(this, title)
    }

    private val returnGeneration = java.util.concurrent.atomic.AtomicLong(0L)

    fun launchAtmacaOnAutomationThread(): Boolean = launchAtmaca()

    fun pressBack():Boolean=performGlobalAction(GLOBAL_ACTION_BACK)
    fun isOutsideSuppressed(now:Long=System.currentTimeMillis()):Boolean=now<suppressOutsideUntil

    private fun startSafely(intent:Intent,why:String):Boolean {
        returnGeneration.incrementAndGet() // Cancel old return attempts even when an X launch is throttled.
        val now=System.currentTimeMillis()
        if (why==lastLaunchKey && now-lastLaunchAt<RELAUNCH_GAP_MS) return true
        return try {
            startActivity(intent); lastLaunchAt=now; lastLaunchKey=why; suppressOutsideUntil=now+OUTSIDE_SUPPRESS_MS
            OperationLog.i("NAV","X acildi: $why"); true
        } catch(t:Throwable) { OperationLog.e("NAV","X acilamadi: $why ${t.javaClass.simpleName}"); false }
    }

    private fun processSnapshot(packageName:String?) {
        val runtime = AutomationController.state.value
        if (!AccountSyncController.isActive && (runtime.taskId == null || runtime.status in setOf(
                RuntimeStatus.IDLE, RuntimeStatus.COMPLETED, RuntimeStatus.FAILED))) return
        val root=rootInActiveWindow
        val resolvedPackage=root?.packageName?.toString() ?: packageName
        if (resolvedPackage!=X_PACKAGE) {
            AccessibilityServiceState.onEvent(resolvedPackage,XScreen.UNKNOWN,PopupType.NONE)
            if (isOutsideSuppressed()) { requestAutomationTick(400L); return }
            if (AccountSyncController.isActive) AccountSyncController.onOutsideX(this,resolvedPackage)
            else AutomationController.onOutsideXSnapshot(this,resolvedPackage)
            return
        }
        val snapshots = AccessibilityTree.snapshots(root)
        val screen=ScreenDetector.detect(root, snapshots)
        val popup=PopupClassifier.classify(snapshots)
        XUiDiagnostics.record(this,snapshots.take(260),screen,popup)
        AccessibilityServiceState.onEvent(resolvedPackage,screen,popup)
        val nodeCount=snapshots.take(50).size
        val key="$screen|$popup|$nodeCount"
        if (key!=lastUiLogKey) { lastUiLogKey=key; OperationLog.i("UI","screen=$screen popup=$popup nodes~$nodeCount") }
        if (AccountSyncController.isActive) AccountSyncController.onSnapshot(this,root,screen,popup)
        else AutomationController.onAccessibilitySnapshot(this,root,screen,popup)
    }

    override fun onInterrupt()=Unit
    override fun onDestroy() {
        synchronized(scheduleLock) {
            scheduledTick?.let(mainHandler::removeCallbacks)
            scheduledTick = null
            scheduledDue = null
        }
        workerHandler?.removeCallbacksAndMessages(null)
        workerHandler = null
        workerThread?.quitSafely()
        workerThread = null
        processing.set(false)
        refreshPending.set(false)
        AutomationController.detach(this); AccountSyncController.detach(this)
        AccessibilityServiceState.onDisconnected(); OperationLog.i("SVC","Erişilebilirlik koptu")
        super.onDestroy()
    }
}
