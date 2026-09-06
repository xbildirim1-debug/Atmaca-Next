package com.atmacanext.app.automation

import android.view.accessibility.AccessibilityNodeInfo
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.UUID
import kotlin.math.min

enum class RuntimeStatus {
    IDLE, PREPARING, NAVIGATING, SWITCHING_ACCOUNT, RUNNING, VERIFYING, WAITING,
    RECOVERING, COOLDOWN, PAUSED, COMPLETED, FAILED,
}

enum class XFlowStage {
    VERIFY_ACCOUNT,
    OPEN_REQUIRED_SURFACE,
    SEEK_UNFOLLOW_DEPTH,
    PROCESS_UNFOLLOW,
    OPEN_MY_FOLLOWERS,
    FIND_RECENT_FOLLOWER,
    OPEN_SOURCE_PROFILE,
    OPEN_SOURCE_FOLLOWERS,
    OPEN_VERIFIED_TAB,
    PROCESS_VERIFIED_FOLLOW,
    OPEN_TARGET_LINK,
    PERFORM_LINK_ACTION,
    OPEN_COMPOSER,
    FILL_COMPOSER,
    SUBMIT_COMPOSER,
    OPEN_DISCOVERY_TARGET,
    SCAN_LATEST_TWEETS,
    OPEN_ENGAGEMENT,
    PROCESS_ENGAGEMENT,
    WAIT_INTERVAL,
}

data class AutomationRuntimeState(
    val sessionId: String? = null,
    val taskId: String? = null,
    val username: String? = null,
    val taskType: TaskType? = null,
    val action: AutomationAction? = null,
    val status: RuntimeStatus = RuntimeStatus.IDLE,
    val verifiedCount: Int = 0,
    /** Total verified target for all cycles. */
    val limit: Int = 1,
    val repeatCount: Int = 1,
    val perCycleLimit: Int = 1,
    val intervalMinutes: Int = 1,
    val cycleIndex: Int = 0,
    val cycleWaitUntil: Long? = null,
    val message: String = "Motor hazır",
    val lastTarget: String? = null,
    val lastActionAt: Long? = null,
    val activeScreen: XScreen = XScreen.UNKNOWN,
    val detectedAccount: String? = null,
    val accountVerified: Boolean = false,
    val navigationRecoveries: Int = 0,
    val popupRecoveries: Int = 0,
    val listScrolls: Int = 0,
    val listRecoveries: Int = 0,
    val cooldownUntil: Long? = null,
    val rateLimitRetries: Int = 0,
    val flowStage: XFlowStage = XFlowStage.VERIFY_ACCOUNT,
    val depthUniqueUsers: Int = 0,
    val sourceIndex: Int = 0,
    val sourceHandle: String? = null,
    val unfollowRevertCount: Int = 0,
)

/**
 * Atmaca V24 task motoru. Eski görev state-machine'ini kullanmaz.
 *
 * Kurallar:
 * - Aynı anda tek session.
 * - Her görevden önce foreground paket + aktif @handle doğrulanır.
 * - Her node her event'te yeniden bulunur; node cache yoktur.
 * - Sayaç yalnız görünür postcondition doğrulanınca artar.
 * - Stop session kimliğini geçersiz kılar; gecikmiş callback tıklayamaz.
 * - Sabit ekran koordinatı kullanılmaz.
 */
object AutomationController {
    private const val TICK_MS = 350L
    private const val UI_TIMEOUT_MS = 6_000L
    private const val ACTION_TIMEOUT_MS = 5_000L
    private const val MAX_NAV_RECOVERIES = 4
    private const val MAX_STAGE_ATTEMPTS = 12
    private const val FOLLOWING_DEPTH_TARGET = 100
    private const val MAX_DEPTH_SCROLLS = 250
    private const val END_STABLE_COUNT = 2
    private const val UNFOLLOW_END_STABLE_COUNT = 6
    private const val UNFOLLOW_SCROLL_SETTLE_MS = 900L
    private const val UNFOLLOW_RESULT_STABLE_MS = 1_500L
    private const val MIN_DISCOVERY_AGE_MINUTES = 90L
    private const val DISCOVERY_TWEET_LIMIT = 5

    private enum class PendingKind { UNFOLLOW, FOLLOW, DIRECT_FOLLOW, LIKE, RETWEET, BOOKMARK, POST }
    private data class PendingAction(
        val kind: PendingKind,
        val target: String?,
        val startedAt: Long = System.currentTimeMillis(),
        val confirmationClicked: Boolean = false,
    )

    private val _state = MutableStateFlow(AutomationRuntimeState())
    val state = _state.asStateFlow()

    private var serviceRef: WeakReference<AtmacaAccessibilityService>? = null
    private var activeTask: ScheduledTask? = null
    private var preparedContents: List<String> = emptyList()
    private var discoveryTargets: List<String> = emptyList()
    private var activeSessionToken: String? = null
    private var pendingAction: PendingAction? = null
    private var stageStartedAt = 0L
    private var stageAttempts = 0
    private var nextActionNotBefore = 0L
    private var accountSettleUntil = 0L
    private var accountSelectionMade = false
    private var cycleStartProgress = 0
    private var navRecoveries = 0
    private var popupRecoveries = 0
    private var listScrolls = 0
    private var listEndStable = 0
    private var lastListSignature = ""
    private val loopGuard = LoopGuard()

    // Use concrete constructors here. This source is also compiled by the
    // APK patch pipeline, where Kotlin inline-only zero-argument helpers must
    // never survive as runtime calls.
    private val processedHandles = LinkedHashSet<String>()
    private val skippedHandles = LinkedHashSet<String>()
    private val depthHandles = LinkedHashSet<String>()
    private val unfollowBeforeAnchorHandles = LinkedHashSet<String>()
    private val sourceHandles = LinkedHashSet<String>()
    private var sourceHandle: String? = null
    private var unfollowProfileStatClickIssued = false
    private var unfollowAnchorPending = false
    private var unfollowNeedForwardScroll = false
    private var unfollowReverseMode = false
    private var unfollowFollowObservedAt = 0L

    private var discoveryTargetIndex = 0
    private var discoveryTweetKey: String? = null
    private val discoverySeenTweets = LinkedHashMap<String, LinkedHashMap<String, Long?>>()
    private val discoveryProcessedTweets = LinkedHashSet<String>()

    fun attach(service: AtmacaAccessibilityService) {
        serviceRef = WeakReference(service)
    }

    fun detach(service: AtmacaAccessibilityService) {
        if (serviceRef?.get() === service) serviceRef = null
    }

    @Synchronized
    fun start(
        task: ScheduledTask,
        targets: List<String> = emptyList(),
        contents: List<String> = listOfNotNull(task.contentText),
        initialUnfollowReverts: Int = 0,
    ): Boolean {
        val current = _state.value
        if (current.taskId != null && current.status !in terminalStatuses()) return false
        val action = task.type.toAction() ?: return false
        if (task.type.requiresLink && !validXUrl(task.targetUrl)) return false
        if (task.type.isDiscoveryFollow && targets.isEmpty()) return false
        if (task.type in CONTENT_TYPES && contents.none(String::isNotBlank)) return false
        if (task.type == TaskType.IMAGE_TWEET && task.mediaUri.isNullOrBlank()) return false

        resetTransient(clearSession = true)
        val session = UUID.randomUUID().toString()
        activeSessionToken = session
        activeTask = task
        preparedContents = contents.filter(String::isNotBlank).ifEmpty { listOfNotNull(task.contentText) }
        discoveryTargets = targets.map(XIdentityDetector::normalizeUsername).filter(String::isNotBlank).distinct().take(10)
        val perCycle = task.limit.coerceIn(1, 100)
        val repeats = task.repeatCount.coerceIn(1, 100)
        val total = perCycle * repeats
        val progress = task.progress.coerceIn(0, total)
        cycleStartProgress = progress

        _state.value = AutomationRuntimeState(
            sessionId = session,
            taskId = task.id,
            username = task.username,
            taskType = task.type,
            action = action,
            status = if (progress >= total) RuntimeStatus.COMPLETED else RuntimeStatus.PREPARING,
            verifiedCount = progress,
            limit = total,
            repeatCount = repeats,
            perCycleLimit = perCycle,
            intervalMinutes = task.intervalMinutes.coerceIn(1, 1_440),
            cycleIndex = (progress / perCycle).coerceAtMost(repeats - 1),
            message = if (progress >= total) "Görev daha önce tamamlanmış" else "${task.username} aktif X hesabı doğrulanıyor",
            flowStage = XFlowStage.VERIFY_ACCOUNT,
            unfollowRevertCount = initialUnfollowReverts.coerceIn(0, 5),
        )
        if (progress >= total) return true
        val service = serviceRef?.get()
        if (service == null) {
            pause("Erişilebilirlik servisi bağlı değil")
            return true
        }
        if (!service.launchXHome()) fail("X uygulaması açılamadı") else service.requestAutomationTick(TICK_MS)
        return true
    }

    fun pause(reason: String = "Görev duraklatıldı") {
        val current = _state.value
        if (current.taskId == null || current.status in terminalStatuses()) return
        pendingAction = null
        _state.value = current.copy(status = RuntimeStatus.PAUSED, message = reason)
    }

    fun resume() {
        val current = _state.value
        if (current.status != RuntimeStatus.PAUSED || activeTask == null) return
        resetCycleNavigation()
        _state.value = current.copy(
            status = RuntimeStatus.PREPARING,
            accountVerified = false,
            activeScreen = XScreen.UNKNOWN,
            flowStage = XFlowStage.VERIFY_ACCOUNT,
            message = "Görev yeniden başlıyor; hesap ve ekran sıfırdan doğrulanacak",
        )
        serviceRef?.get()?.let { service ->
            service.launchXHome()
            service.requestAutomationTick(TICK_MS)
        }
    }

    @Synchronized
    fun stop() {
        activeSessionToken = null
        resetTransient(clearSession = true)
        _state.value = AutomationRuntimeState(message = "Görev durduruldu")
    }

    /** Called by the queue only after every explicitly selected item is terminal. */
    fun returnToAtmaca(): Boolean =
        serviceRef?.get()?.launchAtmacaOnAutomationThread() == true

    fun onOutsideXSnapshot(service: AtmacaAccessibilityService, packageName: String?) {
        attach(service)
        val current = _state.value
        if (!isSessionActive(current) || current.status in terminalStatuses() || current.status == RuntimeStatus.PAUSED) return
        if (service.isOutsideSuppressed()) {
            service.requestAutomationTick(TICK_MS)
            return
        }
        if (packageName?.contains("permissioncontroller", true) == true || packageName?.contains("packageinstaller", true) == true) {
            pause("Android sistem izin ekranı açıldı; otomasyon izin düğmelerine basmaz")
            return
        }
        pendingAction = null
        _state.value = current.copy(
            status = RuntimeStatus.RECOVERING,
            accountVerified = false,
            flowStage = XFlowStage.VERIFY_ACCOUNT,
            activeScreen = XScreen.UNKNOWN,
            message = "X ön plandan çıktı; hedef hesap baştan doğrulanıyor",
        )
        if (!service.launchXHome()) fail("X tekrar açılamadı") else service.requestAutomationTick(TICK_MS)
    }

    fun onAccessibilitySnapshot(
        service: AtmacaAccessibilityService,
        root: AccessibilityNodeInfo?,
        screen: XScreen,
        popup: PopupType,
    ) {
        attach(service)
        var current = _state.value
        if (!isSessionActive(current) || current.status in terminalStatuses() || current.status == RuntimeStatus.PAUSED) return
        if (current.activeScreen != screen) {
            current = current.copy(activeScreen = screen)
            _state.value = current
        }
        val now = System.currentTimeMillis()

        if (current.flowStage == XFlowStage.WAIT_INTERVAL) {
            handleIntervalWait(service, now)
            return
        }
        if (now < nextActionNotBefore) {
            _state.value = current.copy(status = RuntimeStatus.WAITING, message = "X ekran değişiminin oturması bekleniyor")
            service.requestAutomationTick((nextActionNotBefore - now).coerceAtLeast(100L))
            return
        }

        val pending = pendingAction
        if (pending?.kind == PendingKind.UNFOLLOW && popup == PopupType.ACTION_CONFIRMATION && !pending.confirmationClicked) {
            if (XUiActions.clickUnfollowConfirmation(service, root)) {
                pendingAction = pending.copy(confirmationClicked = true, startedAt = now)
                _state.value = current.copy(status = RuntimeStatus.VERIFYING, message = "Takibi bırak onayı verildi; ilişki durumu yeniden okunuyor")
                service.requestAutomationTick(500L)
            } else fail("Takibi bırak onay düğmesi güvenle bulunamadı")
            return
        }

        if (popup != PopupType.NONE) {
            if (popup == PopupType.RATE_LIMIT) {
                pendingAction = null
                pause("X işlem limiti gösterdi; otomatik hesap döndürme veya limit aşma denenmedi")
                return
            }
            val resolution = PopupHandler.handle(root, popup)
            popupRecoveries++
            if (pending != null) {
                pendingAction = null
                pause("İşlem sırasında X penceresi açıldı; belirsiz sonuç sayılmadı: ${resolution.message}")
                return
            }
            if (resolution.type in setOf(PopupResolutionType.DISMISSED, PopupResolutionType.RETRIED)) {
                _state.value = current.copy(popupRecoveries = popupRecoveries, status = RuntimeStatus.RECOVERING, message = resolution.message)
                service.requestAutomationTick(700L)
            } else {
                pause("X penceresinde güvenli otomatik karar verilemedi: ${resolution.message}")
            }
            return
        }

        if (pending != null) {
            verifyPendingAction(service, root, screen, now, pending)
            return
        }

        if (!current.accountVerified || current.flowStage == XFlowStage.VERIFY_ACCOUNT) {
            verifyTargetAccount(service, root, screen, now)
            return
        }

        when (current.taskType) {
            TaskType.UNFOLLOW -> handleUnfollow(service, root, screen, now)
            TaskType.VERIFIED_FOLLOW -> handleVerifiedFollow(service, root, screen, now)
            TaskType.COMMENTER_FOLLOW, TaskType.RETWEETER_FOLLOW, TaskType.QUOTER_FOLLOW -> handleDiscoveryFollow(service, root, screen, now)
            TaskType.TEXT_TWEET, TaskType.IMAGE_TWEET -> handlePost(service, root, screen, now)
            TaskType.FOLLOW, TaskType.LIKE, TaskType.RETWEET, TaskType.BOOKMARK, TaskType.COMMENT, TaskType.QUOTE -> handleLinkedAction(service, root, screen, now)
            else -> fail("Bu eski görev türü V24 motorunda çalıştırılmaz")
        }
    }

    private fun verifyTargetAccount(
        service: AtmacaAccessibilityService,
        root: AccessibilityNodeInfo?,
        screen: XScreen,
        now: Long,
    ) {
        val current = _state.value
        val target = XIdentityDetector.normalizeUsername(current.username.orEmpty())
        if (target.isBlank()) {
            fail("Görev hesabının @handle bilgisi geçersiz")
            return
        }

        if (accountSettleUntil > now) {
            _state.value = current.copy(status = RuntimeStatus.SWITCHING_ACCOUNT, message = "X hesap değişiminin tamamlanması bekleniyor")
            service.requestAutomationTick((accountSettleUntil - now).coerceAtLeast(100L))
            return
        }
        if (accountSettleUntil > 0L) {
            accountSettleUntil = 0L
            stageStartedAt = now
            _state.value = current.copy(status = RuntimeStatus.SWITCHING_ACCOUNT, message = "Seçilen hesabın kendi profiliyle doğrulama yapılıyor")
            // X'in hesap seçici alt sayfası hesap değiştikten sonra açık kalabiliyor.
            // Bu yüzey açıkken gönderilen profil bağlantısı bazı X sürümlerinde yutuluyor.
            // Önce seçiciyi kapat; ardından aynı actor turunda hedef profili açıp yeni
            // snapshot'ı zamanla. Profil kimliği yine aşağıdaki kesin @handle kanıtıyla
            // doğrulanmadan hiçbir görev eylemi başlamaz.
            if (screen == XScreen.ACCOUNT_SWITCHER) {
                service.pressBack()
                accountSettleUntil = now + 450L
                _state.value = current.copy(
                    status = RuntimeStatus.SWITCHING_ACCOUNT,
                    message = "X hesap seçicisi kapatılıyor; hedef profil ardından doğrulanacak",
                )
                service.requestAutomationTick(450L)
                return
            }
            if (!service.launchXProfile(target)) recoverAccount(service, "Seçilen hesabın profili açılamadı")
            else service.requestAutomationTick(700L)
            return
        }

        if (screen == XScreen.PROFILE) {
            val identity = XIdentityDetector.inspectOwnProfile(root, target)
            if (identity.isOwnProfile && identity.expectedAccountVisible) {
                markAccountVerified(service, identity.detectedUsername ?: target)
                return
            }
            val detected = identity.detectedUsername?.let(XIdentityDetector::normalizeUsername)
            val explicitMismatch = identity.isOwnProfile && detected != null && detected != target
            if (explicitMismatch) {
                accountSelectionMade = false
                accountSettleUntil = 0L
                OperationLog.w(
                    "RUNTIME",
                    "Hesap doğrulama reddedildi: beklenen @$target, açık @${detected.orEmpty()}",
                )
            } else if (accountSelectionMade && now - stageStartedAt < UI_TIMEOUT_MS) {
                service.requestAutomationTick(TICK_MS)
                return
            }
            if (!service.launchXHome()) recoverAccount(service, "Açık profil hedef hesabın kendi profili değil")
            else service.requestAutomationTick(600L)
            return
        }

        when (screen) {
            XScreen.HOME -> {
                if (!performStep(service, root, screen, "open_drawer", target) { XUiActions.clickDrawer(service, root) }) {
                    retryOrRecover(service, "X hesap çekmecesi açılamadı")
                }
            }
            XScreen.ACCOUNT_DRAWER -> {
                val activeDrawerAccount = AccountSwitcherInspector.readActiveDrawerAccount(root).username
                val visibleHandles = AccountSwitcherInspector.visibleAccountHandles(root)
                val targetConfirmed = activeDrawerAccount == target || visibleHandles.singleOrNull() == target
                if (targetConfirmed) {
                    // Çekmece açıkken profil deep-link'i bazı X sürümlerinde yutuluyor.
                    // Aktif hesap kesin olarak kanıtlandıktan sonra çekmecedeki gerçek
                    // "Profil" satırını tıkla; profil ekranındaki @handle kanıtı gelmeden
                    // hesabı doğrulanmış veya görevi başlamış sayma.
                    accountSelectionMade = true
                    stageStartedAt = now
                    val profileOpened = XNavigator.execute(
                        service = service,
                        root = root,
                        command = NavigationCommand.OPEN_PROFILE_FROM_DRAWER,
                        targetUsername = target,
                        detectedUsername = activeDrawerAccount,
                    )
                    if (profileOpened) {
                        _state.value = current.copy(
                            status = RuntimeStatus.SWITCHING_ACCOUNT,
                            message = "@$target aktif; çekmecedeki Profil satırı açıldı ve kimlik doğrulanacak",
                        )
                        service.requestAutomationTick(650L)
                    } else {
                        // Semantik profil satırı görünmüyorsa çekmeceyi kapatıp profil
                        // bağlantısını ayrı actor turunda dene. Böylece BACK ile intent
                        // aynı anda yarışmaz.
                        accountSettleUntil = now + 450L
                        _state.value = current.copy(
                            status = RuntimeStatus.SWITCHING_ACCOUNT,
                            message = "@$target aktif; çekmece kapatılıp profil bağlantısıyla doğrulanacak",
                        )
                        service.pressBack()
                        service.requestAutomationTick(450L)
                    }
                } else if (!performStep(service, root, screen, "open_account_switcher", target) { XUiActions.clickAccountSwitcher(service, root) }) {
                    retryOrRecover(service, "X hesap seçici açılamadı")
                }
            }
            XScreen.ACCOUNT_SWITCHER -> {
                val visibleHandles = AccountSwitcherInspector.visibleAccountHandles(root)
                if (AccountSwitcherInspector.isSelectedAccount(root, target)) {
                    // Yeşil seçim işareti + aynı satırdaki kesin @handle aktif hesap
                    // kanıtıdır. X alt sayfayı kendisi kapatmadığı için geri kapat;
                    // profil intent'ini ayrı actor turunda göndererek geri eylemiyle
                    // yarışmasını önle.
                    accountSelectionMade = true
                    accountSettleUntil = now + 450L
                    _state.value = current.copy(
                        status = RuntimeStatus.SWITCHING_ACCOUNT,
                        message = "@$target seçili; hesap sayfası kapatılıp profil doğrulanacak",
                    )
                    service.pressBack()
                    service.requestAutomationTick(450L)
                } else if (performStep(service, root, screen, "select_account", target) { XUiActions.clickExactHandle(service, root, target) }) {
                    accountSelectionMade = true
                    accountSettleUntil = now + AutomationTuning.accountSwitchSettleMs
                    _state.value = current.copy(status = RuntimeStatus.SWITCHING_ACCOUNT, message = "@$target seçildi; X oturumu doğrulanacak")
                    // X hesap değişimini uygular fakat Hesaplar alt sayfasını açık
                    // bırakabilir. Seçim tıklaması tamamlandıktan sonra kapatmak,
                    // 2.5 saniyelik bekleme sonunda profil intent'inin yutulmasını önler.
                    service.pressBack()
                    service.requestAutomationTick(AutomationTuning.accountSwitchSettleMs)
                } else if (target in visibleHandles) {
                    // Görünür olmak seçili olmak değildir. Yanlış hesaptan işlem
                    // yapılmaması için bu durumu başarı sayma; güvenli yeniden dene.
                    retryOrRecover(service, "@$target satırı görünür ancak seçimi doğrulanamadı")
                } else retryOrRecover(service, "@$target hesap satırı seçicide bulunamadı")
            }
            XScreen.UNKNOWN, XScreen.DIALOG -> retryOrRecover(service, "X hesap doğrulama ekranı tanınamadı")
            else -> {
                if (!service.launchXHome()) recoverAccount(service, "Hesap doğrulama için X ana sayfasına dönülemedi")
                else service.requestAutomationTick(600L)
            }
        }
    }

    private fun markAccountVerified(service: AtmacaAccessibilityService, detected: String) {
        val current = _state.value
        navRecoveries = 0
        stageAttempts = 0
        accountSelectionMade = false
        loopGuard.clear()
        _state.value = current.copy(
            status = RuntimeStatus.RUNNING,
            accountVerified = true,
            detectedAccount = if (detected.startsWith("@")) detected else "@$detected",
            navigationRecoveries = 0,
            message = "${current.username} aktif X hesabı doğrulandı",
        )
        beginOperation(service)
    }

    private fun beginOperation(service: AtmacaAccessibilityService) {
        val current = _state.value
        resetOperationNavigation()
        when (current.taskType) {
            TaskType.UNFOLLOW -> {
                moveStage(XFlowStage.OPEN_REQUIRED_SURFACE, "Kendi profilindeki Takip ediliyor sayacı aranıyor")
                if (!service.launchXProfile(current.username.orEmpty())) fail("Kendi X profili açılamadı")
            }
            TaskType.VERIFIED_FOLLOW -> {
                moveStage(XFlowStage.OPEN_MY_FOLLOWERS, "En yeni takipçi için kendi takipçiler listesi açılıyor")
                if (!service.launchXFollowers(current.username.orEmpty())) fail("Takipçiler bağlantısı açılamadı")
            }
            TaskType.COMMENTER_FOLLOW, TaskType.RETWEETER_FOLLOW, TaskType.QUOTER_FOLLOW -> openDiscoveryTarget(service)
            TaskType.TEXT_TWEET, TaskType.IMAGE_TWEET -> {
                val content = currentCycleContent()
                moveStage(XFlowStage.OPEN_COMPOSER, "X gönderi oluşturucu açılıyor")
                if (!service.launchXComposer(content, activeTask?.mediaUri)) fail("X gönderi oluşturucu açılamadı")
            }
            TaskType.FOLLOW, TaskType.LIKE, TaskType.RETWEET, TaskType.BOOKMARK, TaskType.COMMENT, TaskType.QUOTE -> {
                moveStage(XFlowStage.OPEN_TARGET_LINK, "Görev bağlantısı X içinde açılıyor")
                if (!service.launchXWebUrl(activeTask?.targetUrl.orEmpty(), "task")) fail("Görev bağlantısı geçersiz veya X içinde açılamadı")
            }
            else -> fail("Desteklenmeyen eski görev türü")
        }
        service.requestAutomationTick(700L)
    }

    private fun handleUnfollow(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, screen: XScreen, now: Long) {
        val current = _state.value
        when (current.flowStage) {
            XFlowStage.OPEN_REQUIRED_SURFACE -> {
                val selectedRelationshipTab = RelationshipTabInspector.selectedTab(root)
                val followingScreen = screen == XScreen.FOLLOWING_LIST ||
                    selectedRelationshipTab == RelationshipTabInspector.FOLLOWING
                if (followingScreen && ListLoadVerifier.isLoaded(root, XScreen.FOLLOWING_LIST)) {
                    depthHandles.clear()
                    unfollowBeforeAnchorHandles.clear()
                    unfollowProfileStatClickIssued = false
                    unfollowAnchorPending = false
                    unfollowNeedForwardScroll = false
                    unfollowReverseMode = false
                    listEndStable = 0
                    lastListSignature = ""
                    moveStage(XFlowStage.SEEK_UNFOLLOW_DEPTH, "Takip edilenlerde 100 farklı kullanıcı derinliği aranıyor")
                    service.requestAutomationTick(150L)
                } else if (selectedRelationshipTab == RelationshipTabInspector.FOLLOWING) {
                    // Doğru sekme seçildi fakat satırlar henüz yüklenmedi. Profil
                    // seçicisini ikinci kez çalıştırma; listeyi bekle.
                    if (stageTimedOut(now)) {
                        recoverOperation(service, "Takip edilenler sekmesi seçildi ancak liste yüklenmedi")
                    } else service.requestAutomationTick(TICK_MS)
                } else if (
                    unfollowProfileStatClickIssued &&
                    (selectedRelationshipTab == RelationshipTabInspector.FOLLOWERS ||
                        selectedRelationshipTab == RelationshipTabInspector.OTHER)
                ) {
                    // X bazen sayaç tıklamasından sonra komşu Subscribers/Subscriptions
                    // sekmesini seçebiliyor. Doğru Following rotasını açıkça yeniden aç.
                    stageStartedAt = now
                    if (!service.launchXFollowing(current.username.orEmpty())) {
                        recoverOperation(service, "Yanlış ilişki sekmesinden Takip edilenlere dönülemedi")
                    } else {
                        _state.value = current.copy(
                            status = RuntimeStatus.NAVIGATING,
                            message = "Yanlış ilişki sekmesi düzeltildi; Takip edilenler yükleniyor",
                        )
                        service.requestAutomationTick(650L)
                    }
                } else if (screen == XScreen.PROFILE) {
                    val identity = XIdentityDetector.inspectOwnProfile(root, current.username.orEmpty())
                    if (!identity.isOwnProfile || !identity.expectedAccountVisible) {
                        recoverOperation(service, "Takip ediliyor sayacı için açılan profil hedef hesabın kendi profili değil")
                    } else if (!unfollowProfileStatClickIssued) {
                        if (UnfollowProfileStatSelector.click(service, root)) {
                            unfollowProfileStatClickIssued = true
                            stageStartedAt = now
                            _state.value = current.copy(
                                status = RuntimeStatus.NAVIGATING,
                                message = "Kendi profilindeki Takip ediliyor sayacı açıldı; liste doğrulanıyor",
                            )
                            service.requestAutomationTick(650L)
                        } else if (stageTimedOut(now)) {
                            recoverOperation(service, "Kendi profilindeki sayılı Takip ediliyor alanı semantik olarak bulunamadı")
                        } else service.requestAutomationTick(TICK_MS)
                    } else if (stageTimedOut(now)) {
                        recoverOperation(service, "Takip ediliyor sayacı tıklandı ancak liste açılmadı")
                    } else service.requestAutomationTick(TICK_MS)
                } else if (stageTimedOut(now)) recoverOperation(service, "Kendi profilinden Takip edilenler listesi açılamadı")
                else service.requestAutomationTick(TICK_MS)
            }
            XFlowStage.SEEK_UNFOLLOW_DEPTH -> {
                if (screen != XScreen.FOLLOWING_LIST) {
                    if (restoreFollowingTabWithoutAccountRecovery(service, root, now)) return
                    recoverOperation(service, "100 kullanıcı derinliği aranırken liste kayboldu")
                    return
                }
                depthHandles += XListInspector.visibleHandles(root)
                _state.value = current.copy(depthUniqueUsers = depthHandles.size, listScrolls = listScrolls)
                if (depthHandles.size >= FOLLOWING_DEPTH_TARGET) {
                    val anchor = depthHandles.lastOrNull()
                    unfollowBeforeAnchorHandles.clear()
                    if (anchor != null) unfollowBeforeAnchorHandles += depthHandles.filterNot { it == anchor }
                    unfollowAnchorPending = anchor != null
                    unfollowNeedForwardScroll = false
                    unfollowReverseMode = false
                    moveStage(XFlowStage.PROCESS_UNFOLLOW, "${depthHandles.size} kullanıcı derinliğine ulaşıldı; en alttan başlanıyor")
                    service.requestAutomationTick(150L)
                    return
                }
                if (listScrolls >= MAX_DEPTH_SCROLLS) {
                    fail("Takip edilenler listesinde güvenli kaydırma sınırı aşıldı")
                    return
                }
                if (!scrollForwardAndTrack(service, root)) {
                    listEndStable++
                    if (listEndStable >= UNFOLLOW_END_STABLE_COUNT) {
                        unfollowBeforeAnchorHandles.clear()
                        unfollowAnchorPending = false
                        unfollowNeedForwardScroll = false
                        unfollowReverseMode = true
                        moveStage(XFlowStage.PROCESS_UNFOLLOW, "Liste 100 kişiden önce bitti; bulunan ${depthHandles.size} kullanıcıyla en sondan başlanıyor")
                    }
                }
                service.requestAutomationTick(550L)
            }
            XFlowStage.PROCESS_UNFOLLOW -> {
                if (screen != XScreen.FOLLOWING_LIST) {
                    if (restoreFollowingTabWithoutAccountRecovery(service, root, now)) return
                    recoverOperation(service, "Takipten çıkma listesi kayboldu")
                    return
                }
                if (cycleTargetReached()) {
                    finishCycleOrTask(service, "Takipten çıkma döngü limiti tamamlandı")
                    return
                }
                if (unfollowNeedForwardScroll) {
                    if (scrollForwardAndTrack(service, root)) {
                        unfollowNeedForwardScroll = false
                        listEndStable = 0
                    } else {
                        listEndStable++
                        if (listEndStable >= UNFOLLOW_END_STABLE_COUNT) {
                            unfollowNeedForwardScroll = false
                            unfollowReverseMode = true
                            unfollowAnchorPending = false
                            unfollowBeforeAnchorHandles.clear()
                            listEndStable = 0
                            lastListSignature = ""
                            _state.value = current.copy(
                                status = RuntimeStatus.NAVIGATING,
                                message = "100. hesaptan sonra listenin sonuna ulaşıldı; kalan Limit için geriye doğru güvenle devam ediliyor",
                            )
                        }
                    }
                    service.requestAutomationTick(500L)
                    return
                }
                val excluded = processedHandles + skippedHandles +
                    if (unfollowReverseMode) emptySet() else unfollowBeforeAnchorHandles
                val target = XUiActions.findRelationshipTarget(
                    root,
                    acceptedLabels = XUiVocabulary.followingActions,
                    excludedHandles = excluded,
                    fromBottom = unfollowAnchorPending || unfollowReverseMode,
                )
                if (target != null) {
                    if (performStep(service, root, screen, "unfollow", target.handle) { XUiActions.clickRelationship(service, target) }) {
                        unfollowFollowObservedAt = 0L
                        pendingAction = PendingAction(PendingKind.UNFOLLOW, target.handle)
                        _state.value = current.copy(status = RuntimeStatus.VERIFYING, lastActionAt = now, message = "@${target.handle} için takipten çıkma sonucu doğrulanıyor")
                        service.requestAutomationTick(450L)
                    }
                    return
                }
                val scrolled = if (unfollowReverseMode) scrollBackwardAndTrack(service, root) else scrollForwardAndTrack(service, root)
                if (!scrolled) {
                    listEndStable++
                    if (listEndStable >= UNFOLLOW_END_STABLE_COUNT) {
                        if (!unfollowReverseMode) {
                            unfollowReverseMode = true
                            unfollowAnchorPending = false
                            unfollowBeforeAnchorHandles.clear()
                            listEndStable = 0
                            lastListSignature = ""
                            _state.value = current.copy(
                                status = RuntimeStatus.NAVIGATING,
                                message = "Listenin alt sınırına ulaşıldı; kalan Limit için işlenmemiş hesaplara geriye doğru devam ediliyor",
                            )
                        } else finishCycleOrTask(service, "Takip edilenler listesinin gerçek sınırına ulaşıldı; bulunan kadar işlem yapıldı")
                    }
                }
                service.requestAutomationTick(500L)
            }
            else -> recoverOperation(service, "Takipten çıkma state'i tutarsızlaştı")
        }
    }

    private fun handleVerifiedFollow(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, screen: XScreen, now: Long) {
        val current = _state.value
        when (current.flowStage) {
            XFlowStage.OPEN_MY_FOLLOWERS -> {
                if (screen == XScreen.FOLLOWERS_LIST) {
                    val signature = ListViewportController.signature(root)
                    if (lastListSignature == signature) listEndStable++ else listEndStable = 0
                    lastListSignature = signature
                    val moved = ListViewportController.tryScrollBackward(root) == ScrollAttemptResult.SCROLLED
                    if (moved) listScrolls++
                    if (!moved || listEndStable >= END_STABLE_COUNT) {
                        moveStage(XFlowStage.FIND_RECENT_FOLLOWER, "Takipçiler listesinin başı doğrulandı; en yeni ziyaret edilmemiş takipçi aranıyor")
                    }
                    service.requestAutomationTick(500L)
                } else if (stageTimedOut(now)) recoverOperation(service, "Kendi takipçiler listesi açılamadı")
                else service.requestAutomationTick(TICK_MS)
            }
            XFlowStage.FIND_RECENT_FOLLOWER -> {
                if (screen != XScreen.FOLLOWERS_LIST) {
                    recoverOperation(service, "Kaynak takipçi seçilirken liste kayboldu")
                    return
                }
                val own = XIdentityDetector.normalizeUsername(current.username.orEmpty())
                val row = XListInspector.visibleHandleRows(root).firstOrNull { it.handle != own && it.handle !in sourceHandles }
                if (row == null) {
                    if (!scrollForwardAndTrack(service, root)) finishCycleOrTask(service, "Ziyaret edilecek yeni takipçi kalmadı; bulunan kadar onaylı kullanıcı takip edildi")
                    service.requestAutomationTick(500L)
                    return
                }
                sourceHandle = row.handle
                if (performStep(service, root, screen, "open_source_follower", row.handle) { GestureClick.click(service, row.row) }) {
                    moveStage(XFlowStage.OPEN_SOURCE_PROFILE, "@${row.handle} profili açılıyor")
                    service.requestAutomationTick(500L)
                }
            }
            XFlowStage.OPEN_SOURCE_PROFILE -> {
                val source = sourceHandle ?: return nextVerifiedSource(service, "Kaynak takipçi kayboldu")
                if (screen == XScreen.PROFILE && XIdentityDetector.detectProfileHandle(root) == source) {
                    if (performStep(service, root, screen, "open_source_followers", source) { XUiActions.clickProfileFollowers(service, root) }) {
                        moveStage(XFlowStage.OPEN_SOURCE_FOLLOWERS, "@$source takipçileri açılıyor")
                        service.requestAutomationTick(500L)
                    }
                } else if (stageTimedOut(now)) nextVerifiedSource(service, "@$source profili doğrulanamadı")
                else service.requestAutomationTick(TICK_MS)
            }
            XFlowStage.OPEN_SOURCE_FOLLOWERS -> {
                if (screen == XScreen.FOLLOWERS_LIST || screen == XScreen.VERIFIED_FOLLOWERS_LIST) {
                    moveStage(XFlowStage.OPEN_VERIFIED_TAB, "Onaylı Takipçiler sekmesi aranıyor")
                    service.requestAutomationTick(150L)
                } else if (stageTimedOut(now)) nextVerifiedSource(service, "Kaynak takipçinin takipçileri açılamadı")
                else service.requestAutomationTick(TICK_MS)
            }
            XFlowStage.OPEN_VERIFIED_TAB -> {
                if (screen == XScreen.VERIFIED_FOLLOWERS_LIST) {
                    moveStage(XFlowStage.PROCESS_VERIFIED_FOLLOW, "Onaylı Takipçiler doğrulandı; yalnız Takip Et düğmeleri işlenecek")
                    service.requestAutomationTick(150L)
                } else if (stageAttempts >= 6) {
                    nextVerifiedSource(service, "Onaylı Takipçiler sekmesi bulunamadı")
                } else {
                    stageAttempts++
                    XUiActions.clickVerifiedTab(service, root)
                    service.requestAutomationTick(500L)
                }
            }
            XFlowStage.PROCESS_VERIFIED_FOLLOW -> {
                if (screen != XScreen.VERIFIED_FOLLOWERS_LIST) {
                    nextVerifiedSource(service, "Onaylı Takipçiler listesi kayboldu")
                    return
                }
                if (cycleTargetReached()) {
                    finishCycleOrTask(service, "Onaylı takip döngü limiti tamamlandı")
                    return
                }
                val target = XUiActions.findRelationshipTarget(
                    root,
                    acceptedLabels = setOf("takip et", "follow"),
                    excludedHandles = processedHandles + skippedHandles,
                    fromBottom = false,
                )
                if (target != null) {
                    if (performStep(service, root, screen, "follow_verified", target.handle) { XUiActions.clickRelationship(service, target) }) {
                        pendingAction = PendingAction(PendingKind.FOLLOW, target.handle)
                        _state.value = current.copy(status = RuntimeStatus.VERIFYING, lastActionAt = now, message = "@${target.handle} takip sonucu doğrulanıyor")
                        service.requestAutomationTick(450L)
                    }
                    return
                }
                if (!scrollForwardAndTrack(service, root)) {
                    listEndStable++
                    if (listEndStable >= END_STABLE_COUNT) nextVerifiedSource(service, "Bu kaynakta yeni Takip Et düğmesi kalmadı")
                }
                service.requestAutomationTick(500L)
            }
            else -> recoverOperation(service, "Onaylı takip state'i tutarsızlaştı")
        }
    }

    private fun handlePost(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, screen: XScreen, now: Long) {
        val current = _state.value
        val content = currentCycleContent()
        when (current.flowStage) {
            XFlowStage.OPEN_COMPOSER -> {
                if (screen == XScreen.COMPOSER) {
                    moveStage(XFlowStage.FILL_COMPOSER, "Gönderi metni X oluşturucusunda doğrulanıyor")
                    service.requestAutomationTick(120L)
                } else if (stageTimedOut(now)) fail("X gönderi oluşturucu açılmadı")
                else service.requestAutomationTick(TICK_MS)
            }
            XFlowStage.FILL_COMPOSER -> {
                if (screen != XScreen.COMPOSER) {
                    fail("Metin girilirken X oluşturucu kayboldu")
                    return
                }
                if (XUiActions.composerContains(root, content) || XUiActions.setComposerText(root, content)) {
                    moveStage(XFlowStage.SUBMIT_COMPOSER, "Gönderi metni doğrulandı; Gönder düğmesi aranıyor")
                    service.requestAutomationTick(300L)
                } else if (stageTimedOut(now)) fail("Gönderi metni oluşturucuya güvenle yazılamadı")
                else service.requestAutomationTick(TICK_MS)
            }
            XFlowStage.SUBMIT_COMPOSER -> submitComposer(service, root, screen, now)
            else -> recoverOperation(service, "Gönderi state'i tutarsızlaştı")
        }
    }

    private fun handleLinkedAction(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, screen: XScreen, now: Long) {
        val current = _state.value
        val type = current.taskType ?: return
        when (current.flowStage) {
            XFlowStage.OPEN_TARGET_LINK -> {
                val expected = when (type) {
                    TaskType.FOLLOW -> screen == XScreen.PROFILE
                    else -> screen == XScreen.TWEET_DETAIL || screen == XScreen.COMPOSER
                }
                if (expected) {
                    moveStage(XFlowStage.PERFORM_LINK_ACTION, "Bağlantı hedefi doğrulandı; ${type.title} işlemi hazırlanıyor")
                    service.requestAutomationTick(120L)
                } else if (stageTimedOut(now)) fail("Bağlantı X içinde beklenen ekrana açılmadı")
                else service.requestAutomationTick(TICK_MS)
            }
            XFlowStage.PERFORM_LINK_ACTION -> when (type) {
                TaskType.FOLLOW -> {
                    if (XUiActions.isDirectFollowing(root)) recordSuccess(service, XIdentityDetector.detectProfileHandle(root)?.let { "@$it" })
                    else if (performStep(service, root, screen, "direct_follow", activeTask?.targetUrl) { XUiActions.clickDirectFollow(service, root) }) {
                        pendingAction = PendingAction(PendingKind.DIRECT_FOLLOW, XIdentityDetector.detectProfileHandle(root))
                        _state.value = current.copy(status = RuntimeStatus.VERIFYING, lastActionAt = now, message = "Profil takip durumu doğrulanıyor")
                        service.requestAutomationTick(450L)
                    }
                }
                TaskType.LIKE -> {
                    if (XUiActions.isLiked(root)) recordSuccess(service, activeTask?.targetUrl)
                    else if (performStep(service, root, screen, "like", activeTask?.targetUrl) { XUiActions.clickLike(service, root) }) {
                        pendingAction = PendingAction(PendingKind.LIKE, activeTask?.targetUrl)
                        service.requestAutomationTick(450L)
                    }
                }
                TaskType.RETWEET -> {
                    if (XUiActions.isReposted(root)) recordSuccess(service, activeTask?.targetUrl)
                    else if (performStep(service, root, screen, "repost", activeTask?.targetUrl) { XUiActions.clickRepost(service, root) }) {
                        pendingAction = PendingAction(PendingKind.RETWEET, activeTask?.targetUrl, confirmationClicked = false)
                        service.requestAutomationTick(350L)
                    }
                }
                TaskType.BOOKMARK -> {
                    if (XUiActions.isBookmarked(root)) recordSuccess(service, activeTask?.targetUrl)
                    else if (performStep(service, root, screen, "bookmark", activeTask?.targetUrl) { XUiActions.clickBookmark(service, root) }) {
                        pendingAction = PendingAction(PendingKind.BOOKMARK, activeTask?.targetUrl)
                        service.requestAutomationTick(450L)
                    }
                }
                TaskType.COMMENT -> {
                    if (performStep(service, root, screen, "reply", activeTask?.targetUrl) { XUiActions.clickReply(service, root) }) {
                        moveStage(XFlowStage.OPEN_COMPOSER, "Yanıt oluşturucu açılıyor")
                        service.requestAutomationTick(450L)
                    }
                }
                TaskType.QUOTE -> {
                    if (XUiActions.hasQuoteAction(root) && XUiActions.clickQuote(service, root)) {
                        moveStage(XFlowStage.OPEN_COMPOSER, "Alıntı oluşturucu açılıyor")
                        service.requestAutomationTick(450L)
                    } else if (performStep(service, root, screen, "open_repost_menu", activeTask?.targetUrl) { XUiActions.clickRepost(service, root) }) {
                        _state.value = current.copy(status = RuntimeStatus.NAVIGATING, message = "Alıntı seçeneği açılıyor")
                        service.requestAutomationTick(350L)
                    } else {
                        retryOrRecover(service, "Alıntı menüsü açılamadı")
                    }
                }
                else -> Unit
            }
            XFlowStage.OPEN_COMPOSER -> {
                if (screen == XScreen.COMPOSER) {
                    moveStage(XFlowStage.FILL_COMPOSER, "Metin oluşturucuya aktarılıyor")
                    service.requestAutomationTick(120L)
                } else if (type == TaskType.QUOTE && XUiActions.clickQuote(service, root)) {
                    service.requestAutomationTick(450L)
                } else if (stageTimedOut(now)) fail("Yorum/alıntı oluşturucu açılmadı")
                else service.requestAutomationTick(TICK_MS)
            }
            XFlowStage.FILL_COMPOSER -> {
                val content = currentCycleContent()
                if (screen != XScreen.COMPOSER) fail("Metin yazılırken oluşturucu kayboldu")
                else if (XUiActions.composerContains(root, content) || XUiActions.setComposerText(root, content)) {
                    moveStage(XFlowStage.SUBMIT_COMPOSER, "Metin doğrulandı; gönderiliyor")
                    service.requestAutomationTick(300L)
                } else if (stageTimedOut(now)) fail("Yorum/alıntı metni yazılamadı")
            }
            XFlowStage.SUBMIT_COMPOSER -> submitComposer(service, root, screen, now)
            else -> recoverOperation(service, "Bağlantılı görev state'i tutarsızlaştı")
        }
    }

    private fun submitComposer(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, screen: XScreen, now: Long) {
        val current = _state.value
        if (screen != XScreen.COMPOSER) {
            if (stageTimedOut(now)) fail("Gönder düğmesi bulunmadan oluşturucu kapandı") else service.requestAutomationTick(TICK_MS)
            return
        }
        if (performStep(service, root, screen, "submit_post", current.taskType?.title) { XUiActions.clickSubmit(service, root) }) {
            pendingAction = PendingAction(PendingKind.POST, activeTask?.targetUrl)
            _state.value = current.copy(status = RuntimeStatus.VERIFYING, lastActionAt = now, message = "Gönderim sonucu X ekranında doğrulanıyor")
            service.requestAutomationTick(700L)
        } else if (stageTimedOut(now)) fail("X Gönder/Yanıtla düğmesi bulunamadı")
    }

    private fun handleDiscoveryFollow(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, screen: XScreen, now: Long) {
        val current = _state.value
        val target = discoveryTargets.getOrNull(discoveryTargetIndex)
            ?: return finishCycleOrTask(service, "Tanımlı hedef hesapların son 5 gönderisi tarandı; bulunan kadar kullanıcı takip edildi")
        when (current.flowStage) {
            XFlowStage.OPEN_DISCOVERY_TARGET -> {
                if (screen == XScreen.PROFILE && XIdentityDetector.detectProfileHandle(root) == target) {
                    moveStage(XFlowStage.SCAN_LATEST_TWEETS, "@$target profilindeki son 5 gönderi taranıyor; 90 dakikadan yeniler atlanacak")
                    service.requestAutomationTick(150L)
                } else if (stageTimedOut(now)) nextDiscoveryTarget(service, "@$target profili açılamadı")
                else service.requestAutomationTick(TICK_MS)
            }
            XFlowStage.SCAN_LATEST_TWEETS -> {
                if (screen != XScreen.PROFILE || XIdentityDetector.detectProfileHandle(root) != target) {
                    if (stageTimedOut(now)) nextDiscoveryTarget(service, "@$target gönderi listesi doğrulanamadı") else service.requestAutomationTick(TICK_MS)
                    return
                }
                val seen = discoverySeenTweets.getOrPut(target) { LinkedHashMap() }
                val rows = XTweetInspector.visibleTweets(root)
                rows.forEach { row -> if (seen.size < DISCOVERY_TWEET_LIMIT) seen.putIfAbsent(row.key, row.ageMinutes) }
                val eligible = rows.firstOrNull { row ->
                    row.key in seen && row.key !in discoveryProcessedTweets && (row.ageMinutes ?: -1L) >= MIN_DISCOVERY_AGE_MINUTES
                }
                if (eligible != null) {
                    discoveryTweetKey = eligible.key
                    discoveryProcessedTweets += eligible.key
                    if (performStep(service, root, screen, "open_discovery_tweet", eligible.key) { XTweetInspector.click(service, eligible) }) {
                        moveStage(XFlowStage.OPEN_ENGAGEMENT, "90+ dakikalık gönderinin etkileşimleri açılıyor")
                        service.requestAutomationTick(500L)
                    }
                    return
                }
                val fifthTweetKey = seen.keys.lastOrNull().takeIf { seen.size >= DISCOVERY_TWEET_LIMIT }
                if (fifthTweetKey != null && rows.any { it.key == fifthTweetKey }) {
                    nextDiscoveryTarget(service, "@$target için son 5 gönderide yeni uygun etkileşim kalmadı")
                    return
                }
                if (!scrollForwardAndTrack(service, root)) {
                    listEndStable++
                    if (listEndStable >= END_STABLE_COUNT) nextDiscoveryTarget(service, "@$target profilinde 5 gönderiden az içerik bulundu")
                }
                service.requestAutomationTick(550L)
            }
            XFlowStage.OPEN_ENGAGEMENT -> {
                if (screen != XScreen.TWEET_DETAIL) {
                    if (stageTimedOut(now)) nextDiscoveryTweet(service, "Gönderi detay ekranı açılamadı") else service.requestAutomationTick(TICK_MS)
                    return
                }
                when (current.taskType) {
                    TaskType.COMMENTER_FOLLOW -> {
                        moveStage(XFlowStage.PROCESS_ENGAGEMENT, "Yorum yapan kullanıcılar taranıyor")
                        service.requestAutomationTick(150L)
                    }
                    TaskType.RETWEETER_FOLLOW, TaskType.QUOTER_FOLLOW -> {
                        val quotes = current.taskType == TaskType.QUOTER_FOLLOW
                        if (XUiActions.clickEngagementList(service, root, quotes)) {
                            moveStage(XFlowStage.PROCESS_ENGAGEMENT, if (quotes) "Alıntı yapanlar açılıyor" else "Retweet yapanlar açılıyor")
                            service.requestAutomationTick(500L)
                        } else if (stageAttempts++ >= 5) nextDiscoveryTweet(service, "Bu gönderide ilgili etkileşim listesi yok")
                        else service.requestAutomationTick(TICK_MS)
                    }
                    else -> Unit
                }
            }
            XFlowStage.PROCESS_ENGAGEMENT -> {
                val expected = if (current.taskType == TaskType.COMMENTER_FOLLOW) XScreen.TWEET_DETAIL else XScreen.ENGAGEMENT_LIST
                if (screen != expected) {
                    if (stageTimedOut(now)) nextDiscoveryTweet(service, "Etkileşim listesi doğrulanamadı") else service.requestAutomationTick(TICK_MS)
                    return
                }
                if (cycleTargetReached()) {
                    finishCycleOrTask(service, "Etkileşim takip döngü limiti tamamlandı")
                    return
                }
                val excluded = processedHandles + skippedHandles + setOf(
                    XIdentityDetector.normalizeUsername(current.username.orEmpty()),
                    target,
                )
                val relationship = XUiActions.findRelationshipTarget(
                    root,
                    acceptedLabels = setOf("takip et", "follow"),
                    excludedHandles = excluded,
                    fromBottom = false,
                )
                if (relationship != null) {
                    if (performStep(service, root, screen, "follow_engager", relationship.handle) { XUiActions.clickRelationship(service, relationship) }) {
                        pendingAction = PendingAction(PendingKind.FOLLOW, relationship.handle)
                        _state.value = current.copy(status = RuntimeStatus.VERIFYING, lastActionAt = now, message = "@${relationship.handle} etkileşim takibi doğrulanıyor")
                        service.requestAutomationTick(450L)
                    }
                    return
                }
                if (!scrollForwardAndTrack(service, root)) {
                    listEndStable++
                    if (listEndStable >= 3) nextDiscoveryTweet(service, "Bu gönderide yeni takip edilebilir etkileşim kalmadı")
                }
                service.requestAutomationTick(500L)
            }
            else -> recoverOperation(service, "Etkileşim takip state'i tutarsızlaştı")
        }
    }

    private fun verifyPendingAction(
        service: AtmacaAccessibilityService,
        root: AccessibilityNodeInfo?,
        screen: XScreen,
        now: Long,
        pending: PendingAction,
    ) {
        val elapsed = now - pending.startedAt
        when (pending.kind) {
            PendingKind.UNFOLLOW -> {
                val handle = pending.target ?: return fail("Takipten çıkma hedefi kayboldu")
                val rowShowsFollow = XUiActions.rowHasAny(root, handle, setOf("takip et", "follow"))
                val rowStillFollowing = XUiActions.rowHasAny(root, handle, XUiVocabulary.followingActions)

                // Bir hedef ancak önce gerçekten "Takip et" durumuna geçtiği
                // görüldükten sonra başarı sayılır. Aynı satır daha sonra tekrar
                // "Takip ediliyor" olursa X'in günlük limiti doğrulanmış olur.
                if (pending.confirmationClicked && unfollowFollowObservedAt > 0L) {
                    when {
                        rowStillFollowing -> completeUnfollowForDailyLimit(service, handle)
                        now - unfollowFollowObservedAt >= UNFOLLOW_RESULT_STABLE_MS -> recordSuccess(service, "@$handle")
                        else -> service.requestAutomationTick(350L)
                    }
                    return
                }
                if (pending.confirmationClicked && rowShowsFollow) {
                    unfollowFollowObservedAt = now
                    _state.value = _state.value.copy(
                        status = RuntimeStatus.VERIFYING,
                        message = "@$handle Takip et durumuna geçti; geri dönüş kontrol ediliyor",
                    )
                    service.requestAutomationTick(350L)
                    return
                }
                when (UnfollowOutcomePolicy.evaluate(
                    confirmationClicked = pending.confirmationClicked,
                    elapsedMs = elapsed,
                    rowShowsFollow = rowShowsFollow,
                    rowStillFollowing = rowStillFollowing,
                    timeoutMs = ACTION_TIMEOUT_MS,
                )) {
                    // SUCCESS yukarıdaki gözlem penceresinde ele alınır.
                    UnfollowOutcome.SUCCESS -> service.requestAutomationTick(350L)
                    UnfollowOutcome.DAILY_LIMIT -> skipUnfollowTarget(service, handle, "Günlük limit için önce Takip et geçişi görülmedi; sonuç sayılmadı")
                    UnfollowOutcome.UNCONFIRMED -> skipUnfollowTarget(service, handle, "Takibi bırak onayı açılmadı; günlük limit sayılmadan hesap atlandı")
                    UnfollowOutcome.UNKNOWN -> skipUnfollowTarget(service, handle, "Takipten çıkma sonucu kesin doğrulanamadı; günlük limit sayılmadan hesap atlandı")
                    UnfollowOutcome.WAIT -> service.requestAutomationTick(400L)
                }
            }
            PendingKind.FOLLOW -> {
                val handle = pending.target ?: return fail("Takip hedefi kayboldu")
                if (XUiActions.rowHasAny(root, handle, XUiVocabulary.followingActions)) recordSuccess(service, "@$handle")
                else if (elapsed >= ACTION_TIMEOUT_MS) {
                    pendingAction = null
                    skippedHandles += handle
                    _state.value = _state.value.copy(status = RuntimeStatus.RUNNING, message = "@$handle takip sonucu doğrulanamadı; kullanıcı atlandı")
                    service.requestAutomationTick(250L)
                } else service.requestAutomationTick(400L)
            }
            PendingKind.DIRECT_FOLLOW -> {
                if (XUiActions.isDirectFollowing(root)) recordSuccess(service, pending.target?.let { "@$it" })
                else if (elapsed >= ACTION_TIMEOUT_MS) fail("Profil takip sonucu doğrulanamadı")
                else service.requestAutomationTick(400L)
            }
            PendingKind.LIKE -> {
                if (XUiActions.isLiked(root)) recordSuccess(service, pending.target)
                else if (elapsed >= ACTION_TIMEOUT_MS) fail("Beğeni sonucu doğrulanamadı") else service.requestAutomationTick(400L)
            }
            PendingKind.RETWEET -> {
                if (XUiActions.isReposted(root)) recordSuccess(service, pending.target)
                else if (!pending.confirmationClicked && XUiActions.clickRepostConfirmation(service, root)) {
                    pendingAction = pending.copy(confirmationClicked = true, startedAt = now)
                    service.requestAutomationTick(450L)
                } else if (elapsed >= ACTION_TIMEOUT_MS) fail("Retweet sonucu doğrulanamadı")
                else service.requestAutomationTick(350L)
            }
            PendingKind.BOOKMARK -> {
                if (XUiActions.isBookmarked(root)) recordSuccess(service, pending.target)
                else if (elapsed >= ACTION_TIMEOUT_MS) fail("Kaydetme sonucu doğrulanamadı") else service.requestAutomationTick(400L)
            }
            PendingKind.POST -> {
                if (screen in setOf(XScreen.HOME, XScreen.TWEET_DETAIL, XScreen.PROFILE) && elapsed >= 700L) recordSuccess(service, pending.target)
                else if (elapsed >= ACTION_TIMEOUT_MS + 2_000L) fail("Gönderim sonrası oluşturucu kapanmadı; gönderim doğrulanamadı")
                else service.requestAutomationTick(450L)
            }
        }
    }

    private fun recordSuccess(service: AtmacaAccessibilityService, target: String?) {
        val current = _state.value
        pendingAction = null
        unfollowFollowObservedAt = 0L
        target?.removePrefix("@")?.let(processedHandles::add)
        if (current.taskType == TaskType.UNFOLLOW) afterUnfollowTargetHandled()
        val next = (current.verifiedCount + 1).coerceAtMost(current.limit)
        _state.value = current.copy(
            status = RuntimeStatus.RUNNING,
            verifiedCount = next,
            lastTarget = target,
            lastActionAt = System.currentTimeMillis(),
            message = "$next/${current.limit} işlem sonucu X üzerinde doğrulandı",
        )
        if (cycleTargetReached(next)) finishCycleOrTask(service, "Döngüde ${current.perCycleLimit} doğrulanmış işlem tamamlandı")
        else {
            nextActionNotBefore = System.currentTimeMillis() + AutomationTuning.betweenActionsMs
            service.requestAutomationTick(AutomationTuning.betweenActionsMs)
        }
    }

    private fun skipUnfollowTarget(service: AtmacaAccessibilityService, handle: String, reason: String) {
        val current = _state.value
        pendingAction = null
        unfollowFollowObservedAt = 0L
        skippedHandles += handle
        afterUnfollowTargetHandled()
        _state.value = current.copy(status = RuntimeStatus.RUNNING, message = "$reason: @$handle")
        service.requestAutomationTick(350L)
    }

    private fun completeUnfollowForDailyLimit(service: AtmacaAccessibilityService, handle: String) {
        val current = _state.value
        pendingAction = null
        unfollowFollowObservedAt = 0L
        processedHandles += handle
        _state.value = current.copy(
            status = RuntimeStatus.COMPLETED,
            lastTarget = "@$handle",
            lastActionAt = System.currentTimeMillis(),
            message = "@$handle onaydan sonra yeniden Takip ediliyor oldu; X günlük takipten çıkma limiti dolu kabul edildi ve bu hesabın görevi tamamlandı.",
        )
        service.launchAtmacaOnAutomationThread()
    }

    private fun afterUnfollowTargetHandled() {
        if (!unfollowAnchorPending) return
        unfollowAnchorPending = false
        if (!unfollowReverseMode) unfollowNeedForwardScroll = true
    }

    private fun finishCycleOrTask(service: AtmacaAccessibilityService, reason: String) {
        val current = _state.value
        pendingAction = null
        val nextCycle = current.cycleIndex + 1
        if (nextCycle >= current.repeatCount || current.verifiedCount >= current.limit) {
            _state.value = current.copy(
                status = RuntimeStatus.COMPLETED,
                cycleIndex = current.repeatCount - 1,
                message = "$reason. Toplam ${current.verifiedCount}/${current.limit} doğrulandı; görev tamamlandı.",
            )
            service.launchAtmacaOnAutomationThread()
            return
        }
        val waitUntil = System.currentTimeMillis() + current.intervalMinutes * 60_000L
        cycleStartProgress = current.verifiedCount
        _state.value = current.copy(
            status = RuntimeStatus.WAITING,
            cycleIndex = nextCycle,
            cycleWaitUntil = waitUntil,
            accountVerified = false,
            flowStage = XFlowStage.WAIT_INTERVAL,
            message = "$reason. ${nextCycle + 1}/${current.repeatCount} döngü için ${current.intervalMinutes} dakika bekleniyor.",
        )
        service.launchAtmacaOnAutomationThread()
        service.requestAutomationTick(min(60_000L, current.intervalMinutes * 60_000L))
    }

    private fun handleIntervalWait(service: AtmacaAccessibilityService, now: Long) {
        val current = _state.value
        val until = current.cycleWaitUntil ?: now
        if (now < until) {
            val remaining = until - now
            _state.value = current.copy(status = RuntimeStatus.WAITING, message = "Sonraki döngüye ${formatRemaining(remaining)} kaldı")
            service.requestAutomationTick(min(60_000L, remaining.coerceAtLeast(1_000L)))
            return
        }
        resetCycleNavigation()
        _state.value = current.copy(
            status = RuntimeStatus.PREPARING,
            cycleWaitUntil = null,
            accountVerified = false,
            activeScreen = XScreen.UNKNOWN,
            flowStage = XFlowStage.VERIFY_ACCOUNT,
            message = "${current.cycleIndex + 1}/${current.repeatCount} döngü başlıyor; aktif hesap yeniden doğrulanacak",
        )
        if (!service.launchXHome()) fail("Yeni döngü için X açılamadı") else service.requestAutomationTick(TICK_MS)
    }

    private fun nextVerifiedSource(service: AtmacaAccessibilityService, reason: String) {
        sourceHandle?.let(sourceHandles::add)
        sourceHandle = null
        listEndStable = 0
        lastListSignature = ""
        moveStage(XFlowStage.OPEN_MY_FOLLOWERS, "$reason; kendi takipçiler listesine dönülüyor")
        if (!service.launchXFollowers(_state.value.username.orEmpty())) fail("Sıradaki kaynak için kendi takipçiler listesi açılamadı")
        else service.requestAutomationTick(650L)
    }

    private fun openDiscoveryTarget(service: AtmacaAccessibilityService) {
        val target = discoveryTargets.getOrNull(discoveryTargetIndex)
            ?: return finishCycleOrTask(service, "Tüm hedef hesaplar tarandı")
        listEndStable = 0
        lastListSignature = ""
        moveStage(XFlowStage.OPEN_DISCOVERY_TARGET, "@$target profili açılıyor (${discoveryTargetIndex + 1}/${discoveryTargets.size})")
        if (!service.launchXProfile(target)) nextDiscoveryTarget(service, "@$target açılamadı")
    }

    private fun nextDiscoveryTweet(service: AtmacaAccessibilityService, reason: String) {
        discoveryTweetKey = null
        listEndStable = 0
        lastListSignature = ""
        val target = discoveryTargets.getOrNull(discoveryTargetIndex)
            ?: return finishCycleOrTask(service, reason)
        moveStage(XFlowStage.SCAN_LATEST_TWEETS, "$reason; @$target profilindeki sıradaki uygun gönderiye dönülüyor")
        if (!service.launchXProfile(target)) nextDiscoveryTarget(service, "@$target profiline dönülemedi")
        else service.requestAutomationTick(650L)
    }

    private fun nextDiscoveryTarget(service: AtmacaAccessibilityService, reason: String) {
        discoveryTargetIndex++
        discoveryTweetKey = null
        listEndStable = 0
        lastListSignature = ""
        if (discoveryTargetIndex >= discoveryTargets.size) finishCycleOrTask(service, reason)
        else openDiscoveryTarget(service)
    }

    private fun recoverAccount(service: AtmacaAccessibilityService, reason: String) {
        navRecoveries++
        if (navRecoveries > MAX_NAV_RECOVERIES) {
            fail("Hesap doğrulama $MAX_NAV_RECOVERIES kurtarma denemesinden sonra başarısız: $reason")
            return
        }
        pendingAction = null
        accountSelectionMade = false
        accountSettleUntil = 0L
        loopGuard.clear()
        stageAttempts = 0
        stageStartedAt = System.currentTimeMillis()
        _state.value = _state.value.copy(
            status = RuntimeStatus.RECOVERING,
            accountVerified = false,
            flowStage = XFlowStage.VERIFY_ACCOUNT,
            navigationRecoveries = navRecoveries,
            message = "Hesap doğrulama kurtarma $navRecoveries/$MAX_NAV_RECOVERIES: $reason",
        )
        if (!service.launchXHome()) fail("X ana sayfası kurtarma için açılamadı") else service.requestAutomationTick(700L)
    }

    private fun recoverOperation(service: AtmacaAccessibilityService, reason: String) {
        _state.value = _state.value.copy(accountVerified = false, flowStage = XFlowStage.VERIFY_ACCOUNT, status = RuntimeStatus.RECOVERING, message = reason)
        recoverAccount(service, reason)
    }

    private fun retryOrRecover(service: AtmacaAccessibilityService, reason: String) {
        stageAttempts++
        if (stageAttempts >= MAX_STAGE_ATTEMPTS || stageTimedOut(System.currentTimeMillis())) recoverAccount(service, reason)
        else service.requestAutomationTick(TICK_MS)
    }

    private fun performStep(
        service: AtmacaAccessibilityService,
        root: AccessibilityNodeInfo?,
        screen: XScreen,
        action: String,
        target: String?,
        block: () -> Boolean,
    ): Boolean {
        val loop = loopGuard.record(LoopGuard.Step(screen, ListViewportController.signature(root), action, target))
        if (loop) {
            recoverAccount(service, "Aynı ekran/eylem döngüsü algılandı: $action")
            return false
        }
        // Keep this as a concrete try/catch. The partial APK compile pipeline must not
        // leave a direct call to Kotlin's private inline-only Result helpers in dex.
        val result = try {
            block()
        } catch (_: Throwable) {
            false
        }
        if (result) {
            stageStartedAt = System.currentTimeMillis()
            stageAttempts = 0
        }
        return result
    }

    private fun moveStage(stage: XFlowStage, message: String) {
        stageStartedAt = System.currentTimeMillis()
        stageAttempts = 0
        listEndStable = 0
        _state.value = _state.value.copy(flowStage = stage, status = RuntimeStatus.NAVIGATING, message = message, listScrolls = listScrolls)
    }

    private fun scrollForwardAndTrack(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        val signature = ListViewportController.signature(root)
        val changed = lastListSignature.isBlank() || signature != lastListSignature
        lastListSignature = signature
        val dispatched = dispatchListScroll(service, root, forward = true)
        if (dispatched) {
            listScrolls++
            _state.value = _state.value.copy(listScrolls = listScrolls)
            if (_state.value.taskType == TaskType.UNFOLLOW) {
                nextActionNotBefore = maxOf(nextActionNotBefore, System.currentTimeMillis() + UNFOLLOW_SCROLL_SETTLE_MS)
            }
        }
        if (changed) listEndStable = 0
        // dispatchGesture alt sınıra gelince de 'tamamlandı' dönebilir. Gerçek
        // ilerleme yalnız yeni erişilebilirlik imzasıyla kabul edilir.
        return changed
    }

    private fun scrollBackwardAndTrack(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?): Boolean {
        val signature = ListViewportController.signature(root)
        val changed = lastListSignature.isBlank() || signature != lastListSignature
        lastListSignature = signature
        val dispatched = dispatchListScroll(service, root, forward = false)
        if (dispatched) {
            listScrolls++
            _state.value = _state.value.copy(listScrolls = listScrolls)
            if (_state.value.taskType == TaskType.UNFOLLOW) {
                nextActionNotBefore = maxOf(nextActionNotBefore, System.currentTimeMillis() + UNFOLLOW_SCROLL_SETTLE_MS)
            }
        }
        if (changed) listEndStable = 0
        return changed
    }

    /**
     * The unfollow relationship screen contains a full-screen horizontal pager.
     * Start from a real user row and use its nearest proven vertical ancestor;
     * fall back to the row-bounded gesture only when the node action is absent
     * or rejected. Other task types retain the existing behavior.
     */
    private fun dispatchListScroll(
        service: AtmacaAccessibilityService,
        root: AccessibilityNodeInfo?,
        forward: Boolean,
    ): Boolean {
        if (_state.value.taskType == TaskType.UNFOLLOW) {
            val rowContainerResult = if (forward) {
                ListViewportController.tryScrollUserRowsForward(root)
            } else {
                ListViewportController.tryScrollUserRowsBackward(root)
            }
            if (rowContainerResult == ScrollAttemptResult.SCROLLED) return true
            return if (forward) ListGesture.forward(service, root) else ListGesture.backward(service, root)
        }
        val result = if (forward) {
            ListViewportController.tryScrollForward(root)
        } else {
            ListViewportController.tryScrollBackward(root)
        }
        return result == ScrollAttemptResult.SCROLLED ||
            if (forward) ListGesture.forward(service, root) else ListGesture.backward(service, root)
    }

    /**
     * A horizontal pager movement must not restart account verification. If X is
     * visibly on another relationship tab, reopen Following and keep the current
     * unfollow stage and accumulated depth intact.
     */
    private fun restoreFollowingTabWithoutAccountRecovery(
        service: AtmacaAccessibilityService,
        root: AccessibilityNodeInfo?,
        now: Long,
    ): Boolean {
        val selectedTab = RelationshipTabInspector.selectedTab(root)
        if (selectedTab != RelationshipTabInspector.FOLLOWERS && selectedTab != RelationshipTabInspector.OTHER) {
            return false
        }
        service.launchXFollowing(_state.value.username.orEmpty())
        lastListSignature = ""
        listEndStable = 0
        stageStartedAt = now
        service.requestAutomationTick(650L)
        return true
    }

    private fun stageTimedOut(now: Long): Boolean = stageStartedAt > 0L && now - stageStartedAt >= UI_TIMEOUT_MS

    private fun cycleTargetReached(progress: Int = _state.value.verifiedCount): Boolean =
        progress - cycleStartProgress >= _state.value.perCycleLimit || progress >= _state.value.limit

    private fun currentCycleContent(): String {
        val index = _state.value.cycleIndex.coerceAtLeast(0)
        return preparedContents.getOrNull(index)
            ?: preparedContents.getOrNull(index % preparedContents.size.coerceAtLeast(1))
            ?: activeTask?.contentText.orEmpty()
    }

    private fun resetOperationNavigation() {
        pendingAction = null
        stageStartedAt = System.currentTimeMillis()
        stageAttempts = 0
        listEndStable = 0
        lastListSignature = ""
        depthHandles.clear()
        unfollowBeforeAnchorHandles.clear()
        unfollowProfileStatClickIssued = false
        unfollowAnchorPending = false
        unfollowNeedForwardScroll = false
        unfollowReverseMode = false
        unfollowFollowObservedAt = 0L
        sourceHandle = null
    }

    private fun resetCycleNavigation() {
        resetOperationNavigation()
        processedHandles.clear()
        skippedHandles.clear()
        depthHandles.clear()
        sourceHandles.clear()
        discoveryTargetIndex = 0
        discoveryTweetKey = null
        discoverySeenTweets.clear()
        discoveryProcessedTweets.clear()
        accountSelectionMade = false
        accountSettleUntil = 0L
        navRecoveries = 0
        loopGuard.clear()
    }

    private fun resetTransient(clearSession: Boolean) {
        pendingAction = null
        nextActionNotBefore = 0L
        cycleStartProgress = 0
        popupRecoveries = 0
        listScrolls = 0
        resetCycleNavigation()
        if (clearSession) {
            activeTask = null
            preparedContents = emptyList()
            discoveryTargets = emptyList()
        }
    }

    private fun isSessionActive(current: AutomationRuntimeState): Boolean =
        current.sessionId != null && current.sessionId == activeSessionToken && current.taskId != null

    private fun fail(message: String) {
        pendingAction = null
        _state.value = _state.value.copy(status = RuntimeStatus.FAILED, message = message)
        serviceRef?.get()?.launchAtmacaOnAutomationThread()
    }

    private fun formatRemaining(ms: Long): String {
        val minutes = (ms + 59_999L) / 60_000L
        return if (minutes >= 60L) "${minutes / 60} sa ${minutes % 60} dk" else "$minutes dk"
    }

    private fun terminalStatuses(): Set<RuntimeStatus> = setOf(RuntimeStatus.IDLE, RuntimeStatus.COMPLETED, RuntimeStatus.FAILED)

    private fun validXUrl(value: String?): Boolean = try {
        val uri = android.net.Uri.parse(value?.trim())
        uri.scheme in setOf("https", "http") && uri.host?.lowercase() in setOf("x.com", "www.x.com", "twitter.com", "www.twitter.com")
    } catch (_: Throwable) {
        false
    }

    private fun TaskType.toAction(): AutomationAction? = when (this) {
        TaskType.TEXT_TWEET -> AutomationAction.POST_TEXT
        TaskType.IMAGE_TWEET -> AutomationAction.POST_IMAGE
        TaskType.FOLLOW -> AutomationAction.FOLLOW
        TaskType.LIKE -> AutomationAction.LIKE
        TaskType.RETWEET -> AutomationAction.RETWEET
        TaskType.BOOKMARK -> AutomationAction.BOOKMARK
        TaskType.COMMENT -> AutomationAction.COMMENT
        TaskType.QUOTE -> AutomationAction.QUOTE
        TaskType.UNFOLLOW -> AutomationAction.UNFOLLOW
        TaskType.VERIFIED_FOLLOW -> AutomationAction.FOLLOW_VERIFIED
        TaskType.COMMENTER_FOLLOW -> AutomationAction.FOLLOW_COMMENTER
        TaskType.RETWEETER_FOLLOW -> AutomationAction.FOLLOW_RETWEETER
        TaskType.QUOTER_FOLLOW -> AutomationAction.FOLLOW_QUOTER
        else -> null
    }

    private val CONTENT_TYPES = setOf(TaskType.TEXT_TWEET, TaskType.IMAGE_TWEET, TaskType.COMMENT, TaskType.QUOTE)
}
