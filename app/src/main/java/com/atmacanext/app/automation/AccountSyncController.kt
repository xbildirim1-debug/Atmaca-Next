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
    private enum class Stage { IDLE, HOME, DRAWER, SWITCHER, HARVEST, SELECT, SETTLE, PROFILE, STATS, SAVING }
    data class State(
        val active: Boolean = false, val mode: Mode = Mode.NONE,
        val message: String = "Hazır", val discovered: Int = 0, val processed: Int = 0,
        val target: String? = null, val completed: Boolean = false, val failed: Boolean = false,
    )
    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()
    val isActive get() = _state.value.active
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
    private var harvestSignature: String? = null
    private var stableHarvests = 0
    private var searchForward = false
    private var searchSignature: String? = null
    private var harvestDone = false

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
        discovered.clear(); processed.clear()
        target = username?.let(XIdentityDetector::normalizeUsername)
        if (mode == Mode.SWITCH && target?.matches(Regex("[A-Za-z0-9_]{1,15}")) != true) return false
        recoveries = 0; harvestDone = false; harvestSignature = null; stableHarvests = 0
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
        if (now - stageAt > 20_000L) {
            if (stage == Stage.SAVING || ++recoveries > 3) {
                finish(false, "${_state.value.message}: ekran doğrulanamadı. Kaydedilen hesaplar korundu.")
                return
            }
            move(Stage.HOME, "X ekranı yeniden açılıyor ($recoveries/3)")
            service.launchXHome(); tick(service, 800L); return
        }
        if (now < nextActionAt) { service.requestAutomationTick(nextActionAt - now); return }
        if (stage == Stage.SAVING) { tick(service); return }
        if (popup != PopupType.NONE) {
            // Account import never accepts arbitrary X confirmations.
            finish(false, "X bir uyarı gösteriyor. Uyarıyı kontrol edip yeniden dene.")
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
                XScreen.HOME -> XNavigator.execute(service, root, NavigationCommand.OPEN_ACCOUNT_DRAWER, "", null)
                else -> Unit
            }
            Stage.SWITCHER -> when (screen) {
                XScreen.ACCOUNT_SWITCHER -> enterSwitcher()
                XScreen.ACCOUNT_DRAWER -> XNavigator.execute(service, root, NavigationCommand.OPEN_ACCOUNT_SWITCHER, target.orEmpty(), null)
                else -> Unit
            }
            Stage.HARVEST -> {
                if (screen != XScreen.ACCOUNT_SWITCHER) { tick(service); return }
                AccountSwitcherInspector.visibleAccountHandles(root).forEach {
                    if (discovered.size < 10) discovered += XIdentityDetector.normalizeUsername(it)
                }
                val signature = ListViewportController.signature(root)
                stableHarvests = if (signature == harvestSignature) stableHarvests + 1 else 0
                harvestSignature = signature
                publish("${discovered.size} açık oturum bulundu")
                if (discovered.size >= 10 || stableHarvests >= 2) finishHarvest()
                else if (ListViewportController.tryScrollForward(root) != ScrollAttemptResult.SCROLLED) {
                    stableHarvests++
                    if (stableHarvests >= 2) finishHarvest()
                }
            }
            Stage.SELECT -> {
                val wanted = target ?: run { finish(false, "Hedef hesap bulunamadı"); return }
                if (screen != XScreen.ACCOUNT_SWITCHER) { tick(service); return }
                if (XNavigator.clickExactHandle(service, root, wanted)) {
                    move(Stage.SETTLE, "@$wanted hesabı açılıyor")
                    tick(service, AutomationTuning.accountSwitchSettleMs.coerceIn(1500L, 6000L)); return
                }
                // Harvest may leave the list at its bottom. Search both directions, with a deadline.
                val signature = ListViewportController.signature(root)
                if (signature == searchSignature && !searchForward) searchForward = true
                searchSignature = signature
                val result = if (searchForward) ListViewportController.tryScrollForward(root)
                    else ListViewportController.tryScrollBackward(root)
                if (result != ScrollAttemptResult.SCROLLED) {
                    if (!searchForward) { searchForward = true; searchSignature = null }
                    else { finish(false, "@$wanted açık oturumlar arasında bulunamadı. Yeniden tara."); return }
                }
            }
            Stage.SETTLE -> {
                if (service.launchXProfile(target.orEmpty())) move(Stage.PROFILE, "@${target} kimliği doğrulanıyor")
                else { finish(false, "Hesap profili açılamadı"); return }
            }
            Stage.PROFILE -> if (verified(root, screen)) move(Stage.STATS, "Takipçi ve takip edilen sayıları okunuyor")
            Stage.STATS -> {
                if (verified(root, screen)) {
                    val stats = AccountSwitcherInspector.readProfileStats(root)
                    if (stats.followers != null && stats.following != null) save(service, stats)
                }
            }
            Stage.SAVING, Stage.IDLE -> Unit
        }
        if (isActive) tick(service, 650L)
    }

    private fun enterSwitcher() {
        if (_state.value.mode == Mode.IMPORT && !harvestDone) move(Stage.HARVEST, "Açık oturumlar okunuyor")
        else beginSelection()
    }
    private fun finishHarvest() {
        if (discovered.isEmpty()) { finish(false, "X hesap seçicisinde okunabilir oturum bulunamadı."); return }
        harvestDone = true
        target = discovered.firstOrNull { it !in processed }
        if (target == null) finish(true, "${processed.size} hesap eklendi") else beginSelection()
    }
    private fun beginSelection() {
        searchForward = false; searchSignature = null
        move(Stage.SELECT, "@${target} hesabı seçiliyor")
    }
    private fun verified(root: AccessibilityNodeInfo?, screen: XScreen): Boolean {
        if (screen != XScreen.PROFILE) return false
        val identity = XIdentityDetector.inspectOwnProfile(root, target.orEmpty())
        return identity.isOwnProfile && identity.expectedAccountVisible
    }
    private fun save(service: AtmacaAccessibilityService, stats: AccountSwitcherInspector.ProfileStats) {
        val handle = target ?: return
        val token = generation
        move(Stage.SAVING, "@$handle kaydediliyor") // Before coroutine launch: only one write per snapshot sequence.
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
    @Synchronized private fun saved(token: Long, handle: String) {
        if (token != generation || !isActive || stage != Stage.SAVING) return
        processed += handle
        if (_state.value.mode == Mode.SWITCH) { finish(true, "@$handle artık X'te etkin"); return }
        target = discovered.firstOrNull { it !in processed }
        if (target == null) { finish(true, "${processed.size} hesap güncellendi"); return }
        move(Stage.HOME, "Sıradaki hesap açılıyor")
        serviceRef?.get()?.let { it.launchXHome(); tick(it, 800L) }
    }
    private fun move(next: Stage, message: String) {
        stage = next; stageAt = SystemClock.elapsedRealtime(); nextActionAt = 0L; publish(message)
    }
    private fun publish(message: String) {
        _state.value = _state.value.copy(message = message, discovered = discovered.size,
            processed = processed.size, target = target?.let { "@$it" })
    }
    private fun tick(service: AtmacaAccessibilityService, delay: Long = 450L) {
        nextActionAt = SystemClock.elapsedRealtime() + delay
        service.requestAutomationTick(delay)
    }
    private fun finish(success: Boolean, message: String, returnToApp: Boolean = true) {
        generation++; saveJob?.cancel(); saveJob = null; stage = Stage.IDLE
        _state.value = _state.value.copy(active = false, completed = success, failed = !success,
            message = message, processed = processed.size, discovered = discovered.size)
        OperationLog.i("SYNC", message)
        if (returnToApp) {
            val token = generation
            serviceRef?.get()?.let { service ->
                service.runOnAutomationThread {
                    synchronized(this@AccountSyncController) {
                        if (token == generation && !isActive && !service.launchAtmaca()) {
                            _state.value = _state.value.copy(message = "$message. Atmaca Next'i elle aç.")
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
