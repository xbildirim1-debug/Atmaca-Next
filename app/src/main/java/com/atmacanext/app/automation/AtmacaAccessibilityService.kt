package com.atmacanext.app.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityManager
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.atmacanext.app.MainActivity
import java.util.concurrent.atomic.AtomicBoolean

/** Yalnızca X/Twitter paketini izler; V19 deeplink koruması ve hesap senkronizasyonu içerir. */
open class AtmacaAccessibilityService : AccessibilityService() {
    companion object {
        const val X_PACKAGE = "com.twitter.android"
        private const val RELAUNCH_GAP_MS = 2_800L
        private const val OUTSIDE_SUPPRESS_MS = 3_500L
        private const val EVENT_DEBOUNCE_MS = 220L
        private const val POST_PROCESS_DELAY_MS = 80L
        @Volatile private var live: AtmacaAccessibilityService? = null
        fun peek(): AtmacaAccessibilityService? = live
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
    private var homeFreshSerial = 0
    @Volatile private var suppressOutsideUntil: Long = 0L
    private var lastUiLogKey = ""
    private var lastFreshReadLogKey = ""
    private var missingFreshRootSince = 0L
    private var outsideReadSince = 0L
    private var readSessionKey = ""
    private val snapshotRetries = SnapshotRetryPolicy()

    override fun onServiceConnected() {
        super.onServiceConnected()
        live = this
        if (Build.VERSION.SDK_INT >= 33) {
            serviceInfo = serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR }
            OperationLog.i("REPLY_INPUT", "Android ${Build.VERSION.SDK_INT}: erişilebilirlik metin giriş bağlantısı etkin")
        }
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
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SELECTED,
                AccessibilityEvent.TYPE_VIEW_FOCUSED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
            )
        ) return

        pendingPackageName = currentEvent.packageName?.toString()
        val delay = if (currentEvent.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            currentEvent.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) 100L else EVENT_DEBOUNCE_MS
        requestAutomationTick(delay)
    }

    fun requestAutomationTick(delayMillis: Long) {
        scheduleAutomationTick(NonFollowerTiming.scaleDelay(delayMillis,
            AutomationController.state.value.taskType == com.atmacanext.app.domain.model.TaskType.UNFOLLOW_NON_FOLLOWERS))
    }

    /** Persisted task/account gaps are already absolute user values. */
    fun requestAutomationTickExact(delayMillis: Long) {
        scheduleAutomationTick(delayMillis)
    }

    private fun scheduleAutomationTick(delayMillis: Long) {
        val faster = (delayMillis / 5L).coerceIn(8L, 60_000L)
        synchronized(scheduleLock) {
            val due = SystemClock.uptimeMillis() + faster
            if (!AccountScanPolicy.replaceTick(scheduledDue, due)) return
            scheduledTick?.let(mainHandler::removeCallbacks)
            scheduledDue = due
            val tick = Runnable {
                synchronized(scheduleLock) { scheduledTick = null; scheduledDue = null }
                enqueueSnapshot()
            }
            scheduledTick = tick
            mainHandler.postDelayed(tick, faster)
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
                } catch (error: RuntimeException) {
                    // A stale accessibility node must not kill the HandlerThread,
                    // leaving all later accounts with no snapshot consumer.
                    val state = AutomationController.state.value
                    val frames = error.stackTrace.take(6).joinToString(" > ") { "${it.className}.${it.methodName}:${it.lineNumber}" }
                    val syncing = AccountSyncController.isActive
                    val key = if (syncing) AccountSyncController.recoveryKey() else state.sessionId.orEmpty()
                    val retry = snapshotRetries.retry(key)
                    OperationLog.e("SNAPSHOT_ERROR", "stage=${state.flowStage} screen=${state.activeScreen} error=${error.javaClass.simpleName} retry=$retry frames=$frames")
                    if (retry) {
                        if (!syncing) AutomationController.onSnapshotReadFailure(this)
                        requestAutomationTick(500L)
                    } else if (syncing) AccountSyncController.cancel()
                    else AutomationController.pause("X ekranı dört taze okumada alınamadı; ilerleme korundu. SNAPSHOT_ERROR kaydını kontrol et.")
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
    fun launchXHomeFresh(): Boolean {
        homeFreshSerial++
        // REORDER only. CLEAR_TOP on this X build finishes the task and shows the phone launcher.
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/home")).apply {
            setPackage(X_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        return startSafely(i, "home-fresh-$homeFreshSerial") || launchXHome()
    }

    private var handoffUnknownSince = 0L

    /** One in-app step off a finished account's tweet or profile.
     *  True only when Home, the drawer, or the switcher is already up.
     *  Never loops and never fires a home intent: both left the next account unstarted
     *  or flashed the phone launcher. */
    fun openDiscoveryHome(): Boolean {
        val root = freshNavigationRoot()
        if (root?.packageName?.toString() != X_PACKAGE) {
            OperationLog.w("NAV", "hesap devri X dışında; telefon ana ekranına çıkılmadı")
            return false
        }
        val screen = ScreenDetector.detect(root)
        val now = System.currentTimeMillis()
        if (screen == XScreen.UNKNOWN) {
            if (handoffUnknownSince == 0L) handoffUnknownSince = now
        } else handoffUnknownSince = 0L
        val unknownSettled = screen == XScreen.UNKNOWN && now - handoffUnknownSince >= 350L
        val backVisible = NavigationSurfaceEvidence.backIndex(
            AccessibilityTree.snapshots(root),
            XFlowStage.VERIFY_ACCOUNT,
            screen,
        ) != null
        when (DiscoveryHandoffPolicy.action(screen, XUiActions.bottomHomeVisible(this, root), backVisible, unknownSettled)) {
            DiscoveryHandoffPolicy.Action.DONE -> return true
            DiscoveryHandoffPolicy.Action.WAIT -> return false
            DiscoveryHandoffPolicy.Action.TAP_BOTTOM_HOME -> {
                val clicked = XUiActions.clickBottomHome(this, root)
                OperationLog.i("NAV", "hesap devri alt ana sayfa screen=$screen clicked=$clicked")
                return false
            }
            DiscoveryHandoffPolicy.Action.TAP_BACK -> {
                val clicked = XUiActions.clickVisibleBack(this, root, XFlowStage.VERIFY_ACCOUNT, screen)
                val up = if (!clicked) XUiActions.clickToolbarUp(this, root) else false
                OperationLog.i("NAV", "hesap devri geri screen=$screen clicked=$clicked toolbar=$up")
                return false
            }
            DiscoveryHandoffPolicy.Action.FAILED -> {
                if (screen == XScreen.COMPOSER || screen == XScreen.DIALOG) {
                    OperationLog.i("NAV", "hesap devri örtü screen=$screen")
                    pressBack()
                } else if (screen == XScreen.TWEET_DETAIL || screen == XScreen.PROFILE || screen == XScreen.ENGAGEMENT_LIST ||
                    screen == XScreen.FOLLOWERS_LIST || screen == XScreen.FOLLOWING_LIST || screen == XScreen.VERIFIED_FOLLOWERS_LIST
                ) {
                    val list = screen == XScreen.FOLLOWERS_LIST || screen == XScreen.FOLLOWING_LIST ||
                        screen == XScreen.VERIFIED_FOLLOWERS_LIST || screen == XScreen.ENGAGEMENT_LIST
                    val up = if (list) false else XUiActions.clickToolbarUp(this, root)
                    if (up) {
                        OperationLog.i("NAV", "hesap devri araç çubuğu screen=$screen clicked=true")
                    } else {
                        val accepted = performGlobalAction(GLOBAL_ACTION_BACK)
                        OperationLog.i("NAV", "hesap devri liste geri screen=$screen system=$accepted")
                        val after = freshNavigationRoot()
                        if (after?.packageName?.toString() != X_PACKAGE) {
                            OperationLog.w("NAV", "liste geri X dışına çıktı; ana sayfa açılıyor")
                            launchXHomeFresh()
                        }
                    }
                } else {
                    OperationLog.w("NAV", "hesap devri çıkış yok screen=$screen; hedeften ve X'ten çıkılmadı")
                }
                return false
            }
        }
    }

    fun launchDiscoveryProfile(handle: String, attempt: Int = 1): Boolean {
        val clean = XIdentityDetector.normalizeUsername(handle)
        if (!clean.matches(Regex("[a-z0-9_]{1,15}"))) return false
        val route = DiscoveryProfileRoutePolicy.route(clean, attempt)
        return startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(route.uri)).apply {
            setPackage(X_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }, "discovery-profile/${route.name}/$clean/${attempt.coerceAtLeast(1)}")
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

    /** Only after every selected account is finished. Never from a mid-queue return. */
    fun closeXAfterFullQueue() {
        val am = getSystemService(ActivityManager::class.java)
        val killed = runCatching {
            am?.killBackgroundProcesses(X_PACKAGE)
            true
        }.getOrDefault(false)
        OperationLog.i("NAV", "kuyruğun son hesabı bitti; X arka planda kapatıldı kill=$killed")
    }

    private fun openOwnRecentTask(): Boolean {
        val root = rootInActiveWindow ?: return false
        val rootPackage = root.packageName?.toString() ?: return false
        val launcher = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
        if (rootPackage !in setOf(launcher, "com.android.systemui", "com.miui.home")) return false
        val title = AccessibilityTree.nodes(root, 700).firstOrNull { node ->
            node.isVisibleToUser && listOf(node.text?.toString(), node.contentDescription?.toString()).any { it?.trim() == "Atmaca Next" }
        } ?: return false
        // A header/icon tap can open app-info menus. Prefer the preview in the same card.
        var ancestor: android.view.accessibility.AccessibilityNodeInfo? = title.parent
        repeat(6) {
            val card = ancestor ?: return false
            val preview = AccessibilityTree.nodes(card, 120).firstOrNull { node ->
                val marker = (node.viewIdResourceName.orEmpty() + " " + node.className?.toString().orEmpty()).lowercase()
                node.isVisibleToUser && listOf("thumbnail", "snapshot").any(marker::contains)
            }
            if (preview != null) return GestureClick.click(this, preview)
            val marker = (card.viewIdResourceName.orEmpty() + " " + card.className?.toString().orEmpty()).lowercase()
            if (listOf("taskview", "task_view", "taskcard", "task_card").any(marker::contains)) {
                return GestureClick.click(this, card)
            }
            ancestor = card.parent
        }
        return false
    }

    private val returnGeneration = java.util.concurrent.atomic.AtomicLong(0L)

    fun launchAtmacaOnAutomationThread(): Boolean = launchAtmaca()

    private fun freshNavigationRoot() = FreshRootReader.read(
        invalidate = { if (Build.VERSION.SDK_INT >= 33) clearCache() },
        acquire = { rootInActiveWindow }, refresh = { it.refresh() },
    )

    /** Dismiss the current sheet. A swallowed home deep link is never retried here. */
    fun closeAccountSwitcher(): Boolean {
        val root = freshNavigationRoot() ?: return false
        if (!AccountSelectionReturnPolicy.mayDismiss(root.packageName?.toString(), ScreenDetector.detect(root))) return false
        if (XUiActions.clickVisibleBack(this, root)) return true
        // Re-check immediately before a global action; HOME after selection is a hard boundary.
        val current = freshNavigationRoot() ?: return false
        if (!AccountSelectionReturnPolicy.mayDismiss(current.packageName?.toString(), ScreenDetector.detect(current))) return false
        val closed = performGlobalAction(GLOBAL_ACTION_BACK)
        OperationLog.i("ACCOUNT_SHEET_CLOSE", "Taze X hesap seçicisi kapatma=$closed; kimlik menüden doğrulanacak")
        return closed
    }

    /** One screenshot, then a crop for every account row. */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    fun captureRegions(bounds: List<android.graphics.Rect>, onDone: (List<android.graphics.Bitmap?>) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || bounds.isEmpty()) {
            onDone(List(bounds.size) { null })
            return
        }
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        val started = runCatching {
            takeScreenshot(android.view.Display.DEFAULT_DISPLAY, executor, object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    val full = runCatching { screenshotBitmap(screenshot) }.getOrNull()
                    val crops = bounds.map { rect -> full?.let { cropBitmap(it, rect) } }
                    full?.recycle()
                    mainHandler.post { onDone(crops) }
                    executor.shutdown()
                }
                override fun onFailure(errorCode: Int) {
                    OperationLog.w("ACCOUNT_AVATAR", "ekran görüntüsü alınamadı kod=$errorCode")
                    mainHandler.post { onDone(List(bounds.size) { null }) }
                    executor.shutdown()
                }
            })
        }
        if (started.isFailure) {
            executor.shutdown()
            onDone(List(bounds.size) { null })
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private fun screenshotBitmap(screenshot: ScreenshotResult): android.graphics.Bitmap? {
        val buffer = screenshot.hardwareBuffer
        val hardware = android.graphics.Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace) ?: run {
            buffer.close()
            return null
        }
        val software = hardware.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        hardware.recycle()
        buffer.close()
        return software
    }

    private fun cropBitmap(source: android.graphics.Bitmap, bounds: android.graphics.Rect): android.graphics.Bitmap? {
        val metrics = resources.displayMetrics
        val scaleX = if (metrics.widthPixels > 0) source.width.toFloat() / metrics.widthPixels else 1f
        val scaleY = if (metrics.heightPixels > 0) source.height.toFloat() / metrics.heightPixels else 1f
        val left = (bounds.left * scaleX).toInt().coerceIn(0, source.width - 1)
        val top = (bounds.top * scaleY).toInt().coerceIn(0, source.height - 1)
        val width = (bounds.width() * scaleX).toInt().coerceAtLeast(1).coerceAtMost(source.width - left)
        val height = (bounds.height() * scaleY).toInt().coerceAtLeast(1).coerceAtMost(source.height - top)
        return android.graphics.Bitmap.createBitmap(source, left, top, width, height)
    }

    /** Crops the account photo out of the current screen. Never blocks the scan if it fails. */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    fun captureRegion(bounds: android.graphics.Rect, onDone: (android.graphics.Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            onDone(null)
            return
        }
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        val started = runCatching {
            takeScreenshot(android.view.Display.DEFAULT_DISPLAY, executor, object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    val bitmap = runCatching { cropScreenshot(screenshot, bounds) }.getOrNull()
                    mainHandler.post { onDone(bitmap) }
                    executor.shutdown()
                }
                override fun onFailure(errorCode: Int) {
                    mainHandler.post { onDone(null) }
                    executor.shutdown()
                }
            })
        }
        if (started.isFailure) {
            executor.shutdown()
            onDone(null)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private fun cropScreenshot(screenshot: ScreenshotResult, bounds: android.graphics.Rect): android.graphics.Bitmap? {
        val buffer = screenshot.hardwareBuffer
        val hardware = android.graphics.Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace) ?: run {
            buffer.close()
            return null
        }
        val software = hardware.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        hardware.recycle()
        buffer.close()
        return cropBitmap(software, bounds)?.also { if (it !== software) software.recycle() }
    }

    fun closeAccountScanDetail(): Boolean {
        val root = freshNavigationRoot() ?: return false
        val snapshots = AccessibilityTree.snapshots(root)
        if (root.packageName?.toString() != X_PACKAGE || ScreenDetector.detect(root, snapshots) != XScreen.TWEET_DETAIL ||
            AccountProfileReturnPolicy.profilePresent(snapshots)) return false
        val accepted = performGlobalAction(GLOBAL_ACTION_BACK)
        OperationLog.i("ACCOUNT_PROFILE_RETURN", "Beklenmeyen gönderi detayından kontrollü geri=$accepted")
        return accepted
    }

    fun captureProfileRegion(expectedHandle: String, bounds: android.graphics.Rect,
                             onDone: (android.graphics.Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { onDone(null); return }
        fun matches(): Boolean {
            val root = freshNavigationRoot() ?: return false
            return root.packageName?.toString() == X_PACKAGE &&
                AccountProfileReturnPolicy.ownProfile(AccessibilityTree.snapshots(root), expectedHandle)
        }
        if (!matches()) { onDone(null); return }
        captureRegion(bounds) { bitmap ->
            if (bitmap != null && !matches()) {
                bitmap.recycle()
                OperationLog.w("ACCOUNT_AVATAR", "@$expectedHandle profil kimliği değişti; başka ekranın resmi kaydedilmedi")
                onDone(null)
            } else onDone(bitmap)
        }
    }

    /** The fresh account header is required even when the detector calls it HOME. */
    fun popProfile(expectedHandle: String? = null, forceSystemBack: Boolean = false): Boolean {
        val root = freshNavigationRoot() ?: return false
        if (root.packageName?.toString() != X_PACKAGE) return false
        val nodes = AccessibilityTree.nodes(root)
        val snapshots = nodes.map { it.toSnapshot() }
        if (!AccountProfileReturnPolicy.ownProfile(snapshots, expectedHandle)) return false
        val back = AccountProfileReturnPolicy.backIndex(snapshots, expectedHandle)?.let(nodes::get)
        if (!forceSystemBack && back != null && GestureClick.gestureTap(this, back)) {
            OperationLog.i("ACCOUNT_PROFILE_RETURN", "@$expectedHandle doğrulanmış geri oku kabul edildi; sonraki ekran beklenecek")
            return true
        }
        val fresh = freshNavigationRoot() ?: return false
        if (fresh.packageName?.toString() != X_PACKAGE ||
            !AccountProfileReturnPolicy.ownProfile(AccessibilityTree.snapshots(fresh), expectedHandle)) return false
        val backed = performGlobalAction(GLOBAL_ACTION_BACK)
        OperationLog.i("ACCOUNT_PROFILE_RETURN", "@$expectedHandle doğrulanmış profil sistem geri=$backed; sonraki ekran beklenecek")
        return backed
    }

    /** Fresh root proof prevents a stale sheet snapshot from backing out of HOME. */
    fun pressBack(): Boolean {
        val root = freshNavigationRoot()
        if (root?.packageName?.toString() != X_PACKAGE) return false
        val snapshots = AccessibilityTree.snapshots(root)
        val screen = ScreenDetector.detect(root, snapshots)
        if (screen == XScreen.HOME || screen == XScreen.UNKNOWN) {
            OperationLog.w("NAV", "X $screen üzerinde geri engellendi; telefon ana ekranına çıkılmadı")
            return false
        }
        if (screen == XScreen.ACCOUNT_SWITCHER) return closeAccountSwitcher()
        if (XUiActions.clickVisibleBack(this, root)) return true
        // Verified/followers tabs have a small icon at the top left. Tapping it
        // reselects the tab and never leaves the list. System back pops the screen.
        val relationshipList = screen in setOf(
            XScreen.VERIFIED_FOLLOWERS_LIST, XScreen.FOLLOWERS_LIST, XScreen.FOLLOWING_LIST, XScreen.ENGAGEMENT_LIST,
        )
        if (!relationshipList && XUiActions.clickToolbarUp(this, root)) {
            OperationLog.i("NAV", "araç çubuğu geri screen=$screen")
            return true
        }
        if (relationshipList) {
            val accepted = performGlobalAction(GLOBAL_ACTION_BACK)
            OperationLog.i("NAV", "liste geri screen=$screen system=$accepted")
            val after = freshNavigationRoot()
            if (after?.packageName?.toString() != X_PACKAGE) {
                OperationLog.w("NAV", "liste geri X dışına çıktı; ana sayfa açılıyor")
                launchXHomeFresh()
            }
            return accepted
        }
        if (screen !in setOf(XScreen.ACCOUNT_DRAWER, XScreen.COMPOSER, XScreen.DIALOG)) {
            OperationLog.w("NAV", "görünür geri yok screen=$screen; telefon ana ekranına çıkılmadı")
            return false
        }
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /** Close a drawer or sheet without tapping a node. A labeled control in the
     *  account drawer can be the profile header, and tapping it opens the profile. */
    fun dismissOverlay(): Boolean {
        val root = freshNavigationRoot()
        if (root?.packageName?.toString() != X_PACKAGE) return false
        val screen = ScreenDetector.detect(root)
        // One extra back after the drawer is already gone finishes X.
        if (screen != XScreen.ACCOUNT_DRAWER && screen != XScreen.ACCOUNT_SWITCHER) {
            OperationLog.w("NAV", "geri atlandı ekran=$screen; telefon ana ekranına çıkılmadı")
            return false
        }
        if (screen == XScreen.ACCOUNT_SWITCHER) return closeAccountSwitcher()
        val accepted = performGlobalAction(GLOBAL_ACTION_BACK)
        OperationLog.i("NAV", "geri ekran=$screen accepted=$accepted")
        return accepted
    }

    /** A switcher miss can open a photo sheet with almost no nodes. Close it and stay in X. */
    fun dismissThinOverlay(): Boolean {
        val root = freshNavigationRoot()
        if (root?.packageName?.toString() != X_PACKAGE) return false
        val screen = ScreenDetector.detect(root)
        if (XUiActions.clickVisibleBack(this, root, XFlowStage.VERIFY_ACCOUNT, screen)) return true
        if (XUiActions.clickToolbarUp(this, root)) return true
        val accepted = performGlobalAction(GLOBAL_ACTION_BACK)
        val after = freshNavigationRoot()
        if (after?.packageName?.toString() != X_PACKAGE) {
            OperationLog.w("NAV", "örtü X dışına çıktı; ana sayfa açılıyor")
            launchXHomeFresh()
        }
        return accepted
    }
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
        // A new rootInActiveWindow call alone may still return Android-cached
        // virtual descendants after X changes the relationship pager.
        val sessionReadKey = if (AccountSyncController.isActive) AccountSyncController.recoveryKey() else runtime.sessionId.orEmpty()
        if (sessionReadKey != readSessionKey) {
            readSessionKey = sessionReadKey
            missingFreshRootSince = 0L
            outsideReadSince = 0L
            snapshotRetries.recovered()
        }
        val verifiedTask = AccountSyncController.isActive || runtime.taskId != null
        var cacheCleared = false
        val root = if (verifiedTask) FreshRootReader.read(
            invalidate = {
                if (Build.VERSION.SDK_INT >= 33) cacheCleared = clearCache()
            },
            acquire = { rootInActiveWindow },
            refresh = { it.refresh() },
        ) else rootInActiveWindow
        if (verifiedTask) {
            val key = "${runtime.flowStage}|$cacheCleared|${root?.windowId}"
            if (key != lastFreshReadLogKey) {
                lastFreshReadLogKey = key
                OperationLog.i("UI_FRESH", "stage=${runtime.flowStage} sdk=${Build.VERSION.SDK_INT} cacheCleared=$cacheCleared rootRefreshed=${root != null} window=${root?.windowId}")
            }
        }
        if (verifiedTask && root == null) {
            val now = SystemClock.uptimeMillis()
            if (missingFreshRootSince == 0L) missingFreshRootSince = now
            if (now - missingFreshRootSince >= 15_000L) {
                if (AccountSyncController.isActive) AccountSyncController.onOutsideX(this, null)
                else AutomationController.pause("Güncel X ekran ağacı 15 saniyede alınamadı; eski ekranla işlem yapılmadı")
            }
            else requestAutomationTick(500L)
            return
        }
        missingFreshRootSince = 0L
        val resolvedPackage=root?.packageName?.toString() ?: packageName
        if (resolvedPackage!=X_PACKAGE) {
            val now = SystemClock.uptimeMillis()
            if (outsideReadSince == 0L) outsideReadSince = now
            if (ForegroundReadPolicy.wait(resolvedPackage, now - outsideReadSince)) {
                requestAutomationTick(350L)
                return
            }
            AccessibilityServiceState.onEvent(resolvedPackage,XScreen.UNKNOWN,PopupType.NONE)
            if (isOutsideSuppressed()) { requestAutomationTick(400L); return }
            if (AccountSyncController.isActive) AccountSyncController.onOutsideX(this,resolvedPackage)
            else AutomationController.onOutsideXSnapshot(this,resolvedPackage)
            return
        }
        outsideReadSince = 0L
        val snapshots = AccessibilityTree.snapshots(root)
        val screen=ScreenDetector.detect(root, snapshots)
        val popup=PopupClassifier.classify(snapshots)
        XUiDiagnostics.record(this,snapshots.take(260),screen,popup)
        AccessibilityServiceState.onEvent(resolvedPackage,screen,popup)
        val nodeCount=snapshots.size
        val key="$screen|$popup|$nodeCount"
        if (key!=lastUiLogKey) { lastUiLogKey=key; OperationLog.i("UI","screen=$screen popup=$popup nodes~$nodeCount") }
        if (AccountSyncController.isActive) AccountSyncController.onSnapshot(this,root,screen,popup)
        else AutomationController.onAccessibilitySnapshot(this,root,screen,popup)
        snapshotRetries.recovered()
    }

    override fun onInterrupt()=Unit
    override fun onDestroy() {
        if (live === this) live = null
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
