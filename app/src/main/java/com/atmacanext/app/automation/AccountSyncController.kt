package com.atmacanext.app.automation

import android.app.KeyguardManager
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.AccountAccent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.UUID

/** User-started screen navigation only. All state mutations share this object's monitor. */
object AccountSyncController {
    enum class Mode { NONE, IMPORT, SWITCH }
    private enum class Stage { IDLE, HOME, DRAWER, SWITCHER, HARVEST, SELECT, SETTLE, VERIFY_DRAWER, PROFILE_PHOTO, SAVING, RETURN_PROFILE }
    data class State(
        val active: Boolean = false, val mode: Mode = Mode.NONE,
        val message: String = "Hazır", val discovered: Int = 0, val processed: Int = 0,
        val skipped: Int = 0, val target: String? = null, val completed: Boolean = false, val failed: Boolean = false,
    )
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()
    val isActive get() = _state.value.active
    @Synchronized internal fun recoveryKey(): String = "sync:$generation"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null
    private var serviceRef: WeakReference<AtmacaAccessibilityService>? = null
    private var stage = Stage.IDLE
    private var stageAt = 0L
    private var startedAt = 0L
    private var generation = 0L
    private var recoveries = 0
    private var nextActionAt = 0L
    private var target: String? = null
    private val discovered = linkedSetOf<String>()
    private val processed = linkedSetOf<String>()
    private val skipped = linkedSetOf<String>()
    private var popupAttempts = 0
    private var popupSince = 0L
    private var selectedAt = 0L
    private var targetStartedAt = 0L
    private var timedTarget: String? = null
    private var lastObserved = ""
    private var lastObservedAt = 0L
    private var harvestSignature: String? = null
    private var stableHarvests = 0
    private var avatarPage: String? = null
    private var avatarBusy = false
    private var searchForward = false
    private var searchSignature: String? = null
    private var harvestDone = false
    private var harvestEndConfirmed = false
    private var harvestHoldUntil = 0L
    private var pendingFollowers: String? = null
    private var pendingFollowing: String? = null
    private var profileTries = 0
    private var profileLeft = false
    private var profileReturnAttempts = 0
    private val navigationGate = AccountNavigationGate()

    @Synchronized fun attach(service: AtmacaAccessibilityService) { serviceRef = WeakReference(service) }
    @Synchronized fun detach(service: AtmacaAccessibilityService) {
        if (serviceRef?.get() === service) {
            serviceRef = null
            if (isActive) finish(false, "Ekran okuma bağlantısı kesildi. Yeniden tarayabilirsin.", returnToApp = false)
        }
    }

    @Synchronized fun startImport(): Boolean = start(Mode.IMPORT, null)
    @Synchronized fun switchTo(account: Account): Boolean = start(Mode.SWITCH, account.username)
    @Synchronized fun cancel() {
        if (isActive) finish(false, "İşlem durduruldu. Doğrulanan hesaplar korundu.")
    }

    private fun start(mode: Mode, username: String?): Boolean {
        if (isActive || automationBusy()) return false
        val service = serviceRef?.get()
        if (service == null || !AppServices.ready) {
            _state.value = State(mode = mode, failed = true, message = if (service == null)
                "Önce Ayarlar bölümünden ekran okuma iznini aç." else "Uygulama hazırlanıyor; birazdan tekrar dene.")
            return false
        }
        saveJob?.cancel(); generation++
        discovered.clear(); processed.clear(); skipped.clear(); lastObserved = ""
        popupAttempts = 0; popupSince = 0L
        target = username?.let(XIdentityDetector::normalizeUsername)
        if (mode == Mode.SWITCH && target?.matches(Regex("[A-Za-z0-9_]{1,15}")) != true) return false
        timedTarget = null; targetStartedAt = 0L
        recoveries = 0; harvestDone = false; harvestSignature = null; stableHarvests = 0; avatarPage = null; avatarBusy = false
        harvestEndConfirmed = false; harvestHoldUntil = 0L
        pendingFollowers = null; pendingFollowing = null; profileTries = 0; profileLeft = false; profileReturnAttempts = 0
        if (mode == Mode.IMPORT) com.atmacanext.app.data.settings.AccountAvatarStore.clear(service)
        navigationGate.clear()
        startedAt = SystemClock.elapsedRealtime()
        _state.value = State(active = true, mode = mode)
        move(Stage.HOME, "X açılıyor")
        if (!service.launchXHome()) {
            finish(false, "X uygulaması açılamadı. Telefonda yüklü olduğunu kontrol et.")
            return false
        }
        service.requestAutomationTick(600L)
        return true
    }

    private fun checkDeadline(service: AtmacaAccessibilityService): Boolean {
        val interactive = service.getSystemService(PowerManager::class.java)?.isInteractive != false
        val locked = service.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        if (!interactive || locked) {
            finish(false, "Ekran kapandı veya kilitlendi. Kilidi açtıktan sonra yeniden başlat.")
            return false
        }
        if (SystemClock.elapsedRealtime() - startedAt > 300_000L) {
            finish(false, "İşlem zaman aşımına uğradı. Kaydedilen hesaplar korundu.")
            return false
        }
        return true
    }

    @Synchronized fun onOutsideX(service: AtmacaAccessibilityService, packageName: String?) {
        if (!isActive || !checkDeadline(service)) return
        if (packageName?.contains("permissioncontroller", true) == true || packageName?.contains("packageinstaller", true) == true) {
            finish(false, "Android izin ekranı açıldı. İzni tamamlayıp yeniden başlat.")
            return
        }
        if (SystemClock.elapsedRealtime() - stageAt > 20_000L) {
            finish(false, "X ekranına ulaşılamadı. X'i açıp yeniden dene.")
            return
        }
        service.requestAutomationTick(500L)
    }

    @Synchronized fun onSnapshot(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, screen: XScreen, popup: PopupType) {
        if (!isActive || !checkDeadline(service)) return
        val now = SystemClock.elapsedRealtime()
        if (target != null && target == timedTarget && now - targetStartedAt > 45_000L && stage !in setOf(Stage.SAVING, Stage.RETURN_PROFILE)) {
            skipTarget(service, "45 saniyede hesap doğrulanamadı")
            return
        }
        if (now - stageAt > 20_000L) {
            OperationLog.w("ACCOUNT_SYNC", "Zaman aşımı stage=$stage target=$target screen=$screen")
            if (target != null && stage !in setOf(Stage.SAVING, Stage.RETURN_PROFILE)) {
                skipTarget(service, "20 saniyede hesap doğrulanamadı")
                return
            }
            if (stage in setOf(Stage.SAVING, Stage.RETURN_PROFILE) || ++recoveries > 3) {
                finish(false, "${_state.value.message}: ekran doğrulanamadı. Kaydedilen hesaplar korundu.")
                return
            }
            move(Stage.HOME, "X ekranı yeniden açılıyor ($recoveries/3)")
            service.launchXHome(); tick(service, 800L); return
        }
        val observed = "$stage/$screen/$popup/${target.orEmpty()}"
        if (observed != lastObserved || now - lastObservedAt >= 5_000L) {
            OperationLog.i("ACCOUNT_SYNC", "stage=$stage screen=$screen popup=$popup target=${target.orEmpty()} saved=${processed.size} skipped=${skipped.size}")
            lastObserved = observed; lastObservedAt = now
        }
        if (now < nextActionAt) { service.requestAutomationTickExact(nextActionAt - now); return }
        if (stage == Stage.SAVING) { tick(service); return }
        if (navigationGate.wait(screen, now)) { tick(service, 300L); return }
        if (popup != PopupType.NONE) {
            if (popupSince == 0L) {
                popupSince = now
                val evidence = AccessibilityTree.snapshots(root, 100).filter {
                    it.clickable || it.className.orEmpty().contains("dialog", true) ||
                        it.viewId.orEmpty().contains("alert", true)
                }.take(16).joinToString(" | ") {
                    "${XDiagnosticSanitizer.sanitizeViewId(it.viewId)}:${XDiagnosticSanitizer.sanitizeLabel(it.text)}:${it.className?.substringAfterLast('.')}"
                }
                OperationLog.w("ACCOUNT_SYNC", "Uyarı @$target tür=$popup stage=$stage controls=$evidence")
            }
            // Wait for the switch animation before dismissing; never approve an unknown dialog.
            if (now - popupSince < 60L) { tick(service, 40L); return }
            if (popupAttempts < 2) {
                popupAttempts++
                val closed = ScanPopupRecovery.dismiss(service, root, popup)
                OperationLog.w("ACCOUNT_SYNC", "Uyarı kapatma @$target deneme=$popupAttempts sonuç=$closed")
                tick(service, 1_000L)
            } else {
                service.pressBack()
                skipTarget(service, "X uyarısı kapatılamadı ($popup); diğer hesaplar deneniyor")
            }
            return
        }
        when (stage) {
            Stage.HOME -> when (screen) {
                XScreen.HOME -> move(Stage.DRAWER, "Hesap menüsü açılıyor")
                XScreen.ACCOUNT_DRAWER -> move(Stage.SWITCHER, "Hesap seçici açılıyor")
                XScreen.ACCOUNT_SWITCHER -> enterSwitcher()
                else -> service.launchXHome()
            }
            Stage.DRAWER -> when (screen) {
                XScreen.ACCOUNT_DRAWER -> move(Stage.SWITCHER, "Hesap seçici açılıyor")
                XScreen.ACCOUNT_SWITCHER -> enterSwitcher()
                XScreen.HOME -> { openDrawer(service, root); return }
                else -> { recoverNavigation(service, screen); return }
            }
            Stage.SWITCHER -> when (screen) {
                XScreen.ACCOUNT_SWITCHER -> enterSwitcher()
                XScreen.ACCOUNT_DRAWER -> {
                    if (XNavigator.execute(service, root, NavigationCommand.OPEN_ACCOUNT_SWITCHER, target.orEmpty(), null))
                        navigationGate.issued(screen, now, XScreen.ACCOUNT_SWITCHER)
                    tick(service, 1_200L); return
                }
                XScreen.HOME -> move(Stage.DRAWER, "Hesap menüsü yeniden açılıyor")
                else -> { recoverNavigation(service, screen); return }
            }
            Stage.HARVEST -> {
                if (screen != XScreen.ACCOUNT_SWITCHER) { tick(service); return }
                AccountSwitcherInspector.visibleAccountHandles(root).forEach {
                    if (discovered.size < 10) discovered += XIdentityDetector.normalizeUsername(it)
                }
                val signature = ListViewportController.signature(root)
                stableHarvests = if (signature == harvestSignature) stableHarvests + 1 else 0
                harvestSignature = signature
                if (avatarBusy) { tick(service, 400L); return }
                if (stableHarvests >= 1 && signature != avatarPage) {
                    avatarPage = signature
                    shootRowAvatars(service, root)
                    tick(service, 800L)
                    return
                }
                publish("${discovered.size} açık oturum bulundu")
                val nowHarvest = SystemClock.elapsedRealtime()
                if (discovered.size >= 10) { finishHarvest(); return }
                // 27.51 stopped on the second identical paint, before the last row
                // finished scrolling into view. Confirm the bottom once, then stop.
                if (stableHarvests >= 2 && !harvestEndConfirmed) {
                    harvestEndConfirmed = true
                    if (ListViewportController.tryScrollForward(root) == ScrollAttemptResult.SCROLLED) {
                        harvestHoldUntil = nowHarvest + 900L
                        stableHarvests = 0
                        harvestSignature = null
                    }
                }
                if (nowHarvest < harvestHoldUntil) { tick(service, 200L); return }
                if (stableHarvests >= 2 && harvestEndConfirmed) finishHarvest()
                else if (ListViewportController.tryScrollForward(root) != ScrollAttemptResult.SCROLLED) {
                    stableHarvests++
                    if (stableHarvests >= 2 && harvestEndConfirmed) finishHarvest()
                }
            }
            Stage.SELECT -> {
                val wanted = target ?: run { finish(false, "Hedef hesap bulunamadı"); return }
                if (screen != XScreen.ACCOUNT_SWITCHER) { tick(service); return }
                if (XNavigator.clickExactHandle(service, root, wanted)) {
                    selectedAt = now
                    move(Stage.SETTLE, "@$wanted hesabı açılıyor")
                    tick(service, 300L); return
                }
                // Harvest may leave the list at its bottom. Search both directions, with a deadline.
                val signature = ListViewportController.signature(root)
                if (signature == searchSignature && !searchForward) searchForward = true
                searchSignature = signature
                val result = if (searchForward) ListViewportController.tryScrollForward(root)
                    else ListViewportController.tryScrollBackward(root)
                if (result != ScrollAttemptResult.SCROLLED) {
                    if (!searchForward) { searchForward = true; searchSignature = null }
                    else { skipTarget(service, "Açık oturumlar arasında bulunamadı"); return }
                }
            }
            Stage.SETTLE -> {
                val settle = (AutomationTuning.accountSwitchSettleMs / 20L).coerceIn(20L, 200L)
                when (AccountSelectionReturnPolicy.decide(screen, now - selectedAt, settle)) {
                    AccountSelectionReturnPolicy.Decision.WAIT -> { tick(service, 300L); return }
                    AccountSelectionReturnPolicy.Decision.CLOSE_SWITCHER -> {
                        service.closeAccountSwitcher()
                        tick(service, 650L); return
                    }
                    else -> move(Stage.VERIFY_DRAWER, "@${target} hesap menüsünden doğrulanıyor")
                }
            }
            Stage.VERIFY_DRAWER -> when (screen) {
                XScreen.HOME -> { openDrawer(service, root); return }
                XScreen.ACCOUNT_SWITCHER -> { service.closeAccountSwitcher(); tick(service, 650L); return }
                XScreen.ACCOUNT_DRAWER -> {
                    val account = AccountSwitcherInspector.readActiveDrawerAccount(root)
                    if (AccountScanPolicy.canRecord(target, account.username, account.followers, account.following)) {
                        OperationLog.i("ACCOUNT_SYNC", "Doğrulandı @${account.username} takipçi=${account.followers} takip=${account.following}")
                        pendingFollowers = account.followers
                        pendingFollowing = account.following
                        profileTries = 0
                        profileLeft = false
                        profileReturnAttempts = 0
                        XNavigator.execute(service, root, NavigationCommand.OPEN_PROFILE_FROM_DRAWER, account.username.orEmpty(), null)
                        move(Stage.PROFILE_PHOTO, "@${account.username} profil resmi alınıyor")
                        tick(service, 350L); return
                    } else if (account.username != null && account.username != target) {
                        if (now - selectedAt < (AutomationTuning.accountSwitchSettleMs / 20L).coerceIn(20L, 200L)) {
                            tick(service, 250L); return
                        }
                        OperationLog.w("ACCOUNT_SYNC", "Hedef @$target; menüde @${account.username}. Yeniden seçiliyor.")
                        // Preserve the deadline when a selection did not actually take effect.
                        stage = Stage.SWITCHER
                    }
                }
                else -> { recoverNavigation(service, screen); return }
            }
            Stage.PROFILE_PHOTO -> {
                val followers = pendingFollowers
                val following = pendingFollowing
                if (followers == null || following == null) { move(Stage.VERIFY_DRAWER, "Hesap yeniden doğrulanıyor"); return }
                val waited = now - stageAt
                val metrics = service.resources.displayMetrics
                val nodes = AccessibilityTree.snapshots(root)
                val ownProfile = AccountProfileReturnPolicy.ownProfile(nodes, target)
                if (ownProfile && waited >= 280L) {
                    val minPx = (64 * metrics.density).toInt()
                    val maxPx = (160 * metrics.density).toInt()
                    val photo = ProfileAvatarEvidence.bounds(nodes, metrics.widthPixels, minPx, maxPx)
                    OperationLog.i("ACCOUNT_AVATAR", "@$target profil kare=${photo?.let { "${it.left},${it.top} ${it.width()}x${it.height()}" } ?: "yok"} beklenen=${waited}ms")
                    save(service, AccountSwitcherInspector.ProfileStats(followers, following), photo, metrics, profileVerified = true)
                    return
                }
                if (screen == XScreen.ACCOUNT_DRAWER && profileTries++ < 2) {
                    XNavigator.execute(service, root, NavigationCommand.OPEN_PROFILE_FROM_DRAWER, target.orEmpty(), null)
                    tick(service, 300L); return
                }
                // The supplied device log opens the profile in 1.9–2.4 seconds.
                // A transient early frame must not save it without its picture.
                if (waited < 3_500L) { tick(service, 150L); return }
                OperationLog.w("ACCOUNT_AVATAR", "@$target profil ${waited}ms içinde açılmadı; fotoğrafsız çıkılıyor")
                save(service, AccountSwitcherInspector.ProfileStats(followers, following), null, metrics)
            }
            Stage.SAVING -> Unit
            Stage.RETURN_PROFILE -> {
                val nodes = AccessibilityTree.snapshots(root)
                val action = AccountProfileReturnPolicy.decide(screen,
                    AccountProfileReturnPolicy.profilePresent(nodes), HomeTimelineEvidence.matches(nodes))
                if (action == AccountProfileReturnPolicy.Action.READY) {
                    profileLeft = true
                    OperationLog.i("ACCOUNT_PROFILE_RETURN", "@$target profilden çıkış doğrulandı screen=$screen")
                    nextAfterProfileReturn(service, screen)
                    return
                }
                if (action == AccountProfileReturnPolicy.Action.POP_PROFILE) {
                    val accepted = service.popProfile(target, forceSystemBack = profileReturnAttempts > 0)
                    profileReturnAttempts++
                    OperationLog.i("ACCOUNT_PROFILE_RETURN", "@$target deneme=$profileReturnAttempts kabul=$accepted screen=$screen; henüz çıkış sayılmadı")
                } else if (action == AccountProfileReturnPolicy.Action.LEAVE_DETAIL) {
                    // No corner taps: recover a confirmed detail surface within X.
                    service.closeAccountScanDetail()
                }
                // This transition delay is not divided by the user's speed setting.
                nextActionAt = now + 500L
                service.requestAutomationTickExact(500L)
                return
            }
            Stage.IDLE -> Unit
        }
        if (isActive) tick(service, 300L)
    }

    private fun shootRowAvatars(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?) = Unit

    private fun openDrawer(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?) {
        val clicked = XNavigator.execute(service, root, NavigationCommand.OPEN_ACCOUNT_DRAWER, "", null)
        if (clicked) navigationGate.issued(XScreen.HOME, SystemClock.elapsedRealtime(), XScreen.ACCOUNT_DRAWER)
        OperationLog.i("ACCOUNT_SYNC", "Hesap menüsü açma sonucu=$clicked stage=$stage")
        // Wait for the drawer animation before another click can close it again.
        tick(service, 1_200L)
    }

    private fun recoverNavigation(service: AtmacaAccessibilityService, screen: XScreen) {
        val now = SystemClock.elapsedRealtime()
        // Leaving a profile flashes an empty window. That is not a dead end.
        if (screen == XScreen.UNKNOWN && now - stageAt < 700L) { tick(service, 250L); return }
        if (++recoveries > 8) {
            finish(false, "Hesap menüsüne ulaşılamadı ($screen). Kaydedilen hesaplar korundu.")
            return
        }
        OperationLog.w("ACCOUNT_SYNC", "Beklenmeyen ekran=$screen stage=$stage; ana ekrana dönüş $recoveries/8")
        if (screen == XScreen.UNKNOWN) {
            move(Stage.HOME, "Geçiş ekranı kapandı; sıradaki hesaba geçiliyor")
            service.launchXHome()
            tick(service, 800L)
            return
        }
        service.pressBack()
        move(Stage.HOME, "Hesap menüsü için ana ekrana dönülüyor")
        tick(service, 1_000L)
    }

    private fun enterSwitcher() {
        if (_state.value.mode == Mode.IMPORT && !harvestDone) move(Stage.HARVEST, "Açık oturumlar okunuyor")
        else beginSelection()
    }
    private fun finishHarvest() {
        if (discovered.isEmpty()) { finish(false, "X hesap seçicisinde okunabilir oturum bulunamadı."); return }
        harvestDone = true
        target = AccountScanPolicy.next(discovered, processed, skipped)
        if (target == null) finish(true, "${processed.size} hesap eklendi") else beginSelection()
    }
    private fun skipTarget(service: AtmacaAccessibilityService, reason: String) {
        val failedHandle = target
        if (_state.value.mode != Mode.IMPORT || failedHandle == null) {
            finish(false, "${failedHandle?.let { "@$it: " }.orEmpty()}$reason"); return
        }
        skipped += failedHandle
        OperationLog.w("ACCOUNT_SYNC", "@$failedHandle atlandı: $reason")
        target = AccountScanPolicy.next(discovered, processed, skipped)
        if (target == null) {
            finish(false, "${processed.size} hesap kaydedildi; ${skipped.size} hesap okunamadı. Kayıtlara bak.")
        } else {
            move(Stage.HOME, "Sıradaki hesap deneniyor; ${skipped.size} hesap atlandı")
            service.launchXHome(); tick(service, 800L)
        }
    }

    private fun beginSelection() {
        if (timedTarget != target) {
            popupAttempts = 0; popupSince = 0L
            timedTarget = target; targetStartedAt = SystemClock.elapsedRealtime()
        }
        searchForward = false; searchSignature = null
        move(Stage.SELECT, "@${target} hesabı seçiliyor")
    }
    private fun save(
        service: AtmacaAccessibilityService,
        stats: AccountSwitcherInspector.ProfileStats,
        photo: android.graphics.Rect?,
        metrics: android.util.DisplayMetrics,
        profileVerified: Boolean = false,
    ) {
        val handle = target ?: return
        val token = generation
        move(Stage.SAVING, "@$handle profil resmi alınıyor")
        val done = java.util.concurrent.atomic.AtomicBoolean(false)
        fun usable(bitmap: android.graphics.Bitmap?): Boolean {
            if (bitmap == null) return false
            if (com.atmacanext.app.data.settings.AccountAvatarStore.looksLikeText(bitmap)) {
                OperationLog.w("ACCOUNT_AVATAR", "@$handle kesilen yer yazı, fotoğraf değil")
                return false
            }
            if (com.atmacanext.app.data.settings.AccountAvatarStore.matchesOther(service, handle, bitmap)) {
                OperationLog.w("ACCOUNT_SYNC", "@$handle profil resmi başka hesabın kopyasıydı, kaydedilmedi")
                return false
            }
            return true
        }
        fun persist(bitmap: android.graphics.Bitmap?) {
            synchronized(this@AccountSyncController) {
                if (generation != token || !isActive || stage != Stage.SAVING || target != handle || !done.compareAndSet(false, true)) {
                    bitmap?.recycle()
                    return
                }
                if (bitmap != null) {
                    val kept = runCatching { com.atmacanext.app.data.settings.AccountAvatarStore.write(service, handle, bitmap) }.getOrDefault(false)
                    if (!kept) OperationLog.w("ACCOUNT_SYNC", "@$handle profil resmi kaydedilmedi")
                    bitmap.recycle()
                }
                saveJob = scope.launch {
                    try {
                        val existing = AppServices.repository.snapshotAccountDomains().firstOrNull {
                            XIdentityDetector.normalizeUsername(it.username) == handle
                        }
                        ensureActive()
                        val account = existing?.copy(followers = stats.followers!!, following = stats.following!!)
                            ?: Account(UUID.nameUUIDFromBytes("x:$handle".toByteArray()).toString(), "@$handle", "",
                                stats.followers!!, stats.following!!, "—", 100, AccountAccent.BLUE)
                        AppServices.repository.upsertSyncedAccount(account)
                        com.atmacanext.app.data.settings.AccountPulse.noteScan(service, handle, stats.followers, stats.following)
                        service.runOnAutomationThread { saved(token, handle) }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) {
                        service.runOnAutomationThread {
                            synchronized(this@AccountSyncController) {
                                if (generation == token && isActive) finish(false,
                                    if (error is IllegalArgumentException) error.message ?: "Hesap sınırı doldu"
                                    else "@$handle kaydedilemedi. Yeniden dene.")
                            }
                        }
                    }
                }
            }
        }
        if (!profileVerified) { persist(null); return }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (generation != token || !isActive || stage != Stage.SAVING || done.get()) return@postDelayed
            if (photo != null) {
                service.captureProfileRegion(handle, photo) { bitmap ->
                    if (usable(bitmap)) persist(bitmap) else { bitmap?.recycle(); persist(null) }
                }
            } else {
                val top = (90 * metrics.density).toInt()
                val bottom = (420 * metrics.density).toInt().coerceAtMost(metrics.heightPixels - 1)
                val header = android.graphics.Rect(0, top, metrics.widthPixels, bottom)
                service.captureProfileRegion(handle, header) { bitmap ->
                    if (bitmap == null) { persist(null); return@captureProfileRegion }
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    val minSize = (56 * metrics.density).toInt()
                    val maxSize = (220 * metrics.density).toInt()
                    val box = AvatarPixels.square(pixels, bitmap.width, bitmap.height, minSize, maxSize)
                    val crop = if (box == null) null else runCatching {
                        android.graphics.Bitmap.createBitmap(bitmap, box[0], box[1], box[2], box[2])
                    }.getOrNull()
                    bitmap.recycle()
                    OperationLog.i("ACCOUNT_AVATAR", "@$handle profil başlığı=${box?.let { "${it[0]},${it[1]} ${it[2]}" } ?: "yok"}")
                    if (crop != null && usable(crop)) persist(crop) else { crop?.recycle(); persist(null) }
                }
            }
        }, 30L)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (done.get()) return@postDelayed
            OperationLog.w("ACCOUNT_AVATAR", "@$handle ekran görüntüsü yetişmedi; hesap fotoğrafsız kaydediliyor")
            persist(null)
        }, 1_400L)
    }
    @Synchronized private fun saved(token: Long, handle: String) {
        if (token != generation || !isActive || stage != Stage.SAVING) return
        processed += handle
        recoveries = 0
        profileReturnAttempts = 0
        move(Stage.RETURN_PROFILE, "@$handle kaydedildi; profilden çıkış doğrulanıyor")
        serviceRef?.get()?.let { it.requestAutomationTickExact(40L) }
    }
    private fun nextAfterProfileReturn(service: AtmacaAccessibilityService, screen: XScreen) {
        val handle = target ?: return
        if (_state.value.mode == Mode.SWITCH) { finish(true, "@$handle artık X'te etkin"); return }
        target = AccountScanPolicy.next(discovered, processed, skipped)
        if (target == null) { finish(skipped.isEmpty(), "${processed.size} hesap güncellendi, ${skipped.size} hesap okunamadı"); return }
        move(if (screen == XScreen.HOME) Stage.DRAWER else Stage.SWITCHER, "@$target için hesap menüsüne geçiliyor")
        tick(service, 100L)
    }
    private fun move(next: Stage, message: String) {
        stage = next; stageAt = SystemClock.elapsedRealtime(); nextActionAt = 0L; publish(message)
        OperationLog.i("ACCOUNT_SYNC", "$next | $message")
    }
    private fun publish(message: String) {
        _state.value = _state.value.copy(message = message, discovered = discovered.size,
            processed = processed.size, skipped = skipped.size, target = target?.let { "@$it" })
    }
    private fun tick(service: AtmacaAccessibilityService, delay: Long = 450L) {
        val wait = (AutomationTuning.scaleDelay(delay) / 20L).coerceIn(12L, 80L)
        nextActionAt = SystemClock.elapsedRealtime() + wait
        service.requestAutomationTickExact(wait)
    }
    private fun finish(success: Boolean, message: String, returnToApp: Boolean = true) {
        generation++; saveJob?.cancel(); saveJob = null; stage = Stage.IDLE
        _state.value = _state.value.copy(active = false, completed = success, failed = !success,
            message = message, processed = processed.size, discovered = discovered.size, skipped = skipped.size)
        OperationLog.i("SYNC", message)
        if (returnToApp) {
            val token = generation
            serviceRef?.get()?.let { service ->
                service.runOnAutomationThread {
                    synchronized(this@AccountSyncController) {
                        if (token == generation && !isActive) service.launchAtmaca { returned ->
                            synchronized(this@AccountSyncController) {
                                if (!returned && token == generation && !isActive) {
                                    _state.value = _state.value.copy(message = "$message. Android dönüşü engelledi; Atmaca Next'i aç.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private fun automationBusy(): Boolean {
        if (AppServices.ready && AppServices.orchestrator.state.value.isActive) return true
        val s = AutomationController.state.value
        return s.taskId != null && s.status !in setOf(RuntimeStatus.IDLE, RuntimeStatus.COMPLETED, RuntimeStatus.FAILED)
    }
}
