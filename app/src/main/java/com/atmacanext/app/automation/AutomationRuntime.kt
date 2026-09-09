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
    OPEN_VERIFIED_OWN_PROFILE,
    OPEN_MY_FOLLOWERS,
    FIND_RECENT_FOLLOWER,
    LOCATE_SOURCE_ROW,
    RETURN_VERIFIED_SOURCE,
    RETURN_DISCOVERY_TARGET,
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
    SEARCH_DISCOVERY_TARGET,
    SCAN_LATEST_TWEETS,
    OPEN_ENGAGEMENT,
    PROCESS_ENGAGEMENT,
    OPEN_ENGAGER_PROFILE,
    RETURN_ENGAGEMENT,
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
    val verifiedFollowAccountStopped: Boolean = false,
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
    private const val TICK_MS = 250L
    private const val UI_TIMEOUT_MS = 6_000L
    private const val ACTION_TIMEOUT_MS = 5_000L
    private const val MAX_NAV_RECOVERIES = 4
    private const val MAX_STAGE_ATTEMPTS = 12
    private const val FOLLOWING_DEPTH_TARGET = 100
    private const val MAX_DEPTH_SCROLLS = 250
    private const val END_STABLE_COUNT = 2
    private const val UNFOLLOW_END_STABLE_COUNT = 6
    private const val UNFOLLOW_SCROLL_SETTLE_MS = 900L
    private const val UNFOLLOW_RESULT_STABLE_MS = 650L
    private const val MIN_DISCOVERY_AGE_MINUTES = 90L
    private const val DISCOVERY_TWEET_LIMIT = 5

    private enum class PendingKind { UNFOLLOW, FOLLOW, ENGAGER_FOLLOW, DIRECT_FOLLOW, LIKE, RETWEET, BOOKMARK, POST }
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
    private var engagerHandle: String? = null
    private var engagerParentSignature = ""
    private var engagerParentKeys = emptyList<String>()
    private var engagementOpenAttempts = 0
    private var discoveryTargets: List<String> = emptyList()
    private var activeSessionToken: String? = null
    private var pendingAction: PendingAction? = null
    private var stageStartedAt = 0L
    private var stageAttempts = 0
    private var nextActionNotBefore = 0L
    private var accountSettleUntil = 0L
    private var accountSelectionMade = false
    private val verifiedSourceCandidates = LinkedHashSet<String>()
    private var verifiedFollowingObservedAt = 0L
    private var verifiedFollowingStableAt = 0L
    private var verifiedRevertStreak = 0
    private var cycleStartProgress = 0
    private var unfollowIssuedCount = 0
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

    private var discoverySearchStep = 0
    private var discoverySearchSubmitted = false
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
        discoveryTargets = targets.map(XIdentityDetector::normalizeUsername).filter(String::isNotBlank).distinct().take(1)
        val perCycle = task.limit.coerceIn(1, 100)
        val repeats = task.repeatCount.coerceIn(1, 100)
        val total = perCycle * repeats
        val progress = task.progress.coerceIn(0, total)
        cycleStartProgress = (progress / perCycle) * perCycle
        unfollowIssuedCount = progress

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
        if (pending?.kind == PendingKind.UNFOLLOW && popup == PopupType.ACTION_CONFIRMATION) {
            when (UnfollowConfirmationPolicy.decide(pending.confirmationClicked, now - pending.startedAt, ACTION_TIMEOUT_MS)) {
                UnfollowConfirmationDecision.CLICK -> {
                    if (XUiActions.clickUnfollowConfirmation(service, root)) {
                        pendingAction = pending.copy(confirmationClicked = true, startedAt = now)
                        _state.value = current.copy(status = RuntimeStatus.VERIFYING, message = "Takibi bırak onayı verildi; ilişki durumu yeniden okunuyor")
                        service.requestAutomationTick(500L)
                    } else fail("Takibi bırak onay düğmesi güvenle bulunamadı")
                }
                UnfollowConfirmationDecision.WAIT -> service.requestAutomationTick(350L)
                UnfollowConfirmationDecision.PAUSE -> {
                    pendingAction = null
                    pause("Takibi bırak onayı kapanmadı; sonuç doğrulanamadı")
                }
            }
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
            // Continue through the visible drawer/profile flow after switching.
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
                } else waitForAccountNavigation(service, now)
            }
            XScreen.ACCOUNT_DRAWER -> {
                val activeDrawerAccount = AccountSwitcherInspector.readActiveDrawerAccount(root).username
                val targetConfirmed = activeDrawerAccount == target
                if (targetConfirmed) {
                    // Çekmece açıkken profil deep-link'i bazı X sürümlerinde yutuluyor.
                    // Aktif hesap kesin olarak kanıtlandıktan sonra çekmecedeki gerçek
                    // "Profil" satırını tıkla; profil ekranındaki @handle kanıtı gelmeden
                    // hesabı doğrulanmış veya görevi başlamış sayma.
                    accountSelectionMade = true
                    val profileOpened = XNavigator.execute(
                        service = service,
                        root = root,
                        command = NavigationCommand.OPEN_PROFILE_FROM_DRAWER,
                        targetUsername = target,
                        detectedUsername = activeDrawerAccount,
                    )
                    if (profileOpened) {
                        stageStartedAt = now
                        _state.value = current.copy(
                            status = RuntimeStatus.SWITCHING_ACCOUNT,
                            message = "@$target aktif; çekmecedeki Profil satırı açıldı ve kimlik doğrulanacak",
                        )
                        service.requestAutomationTick(650L)
                        nextActionNotBefore = now + 1_200L
                    } else {
                        retryOrRecover(service, "Doğrulanan hesabın Profil satırı açılamadı")
                    }
                } else if (!performStep(service, root, screen, "open_account_switcher", target) { XUiActions.clickAccountSwitcher(service, root) }) {
                    retryOrRecover(service, "X hesap seçici açılamadı")
                } else waitForAccountNavigation(service, now)
            }
            XScreen.ACCOUNT_SWITCHER -> {
                val visibleHandles = AccountSwitcherInspector.visibleAccountHandles(root)
                // Import already proved this path on the user's device. Always click the
                // exact target row; a checkmark elsewhere in the sheet is not identity proof.
                if (performStep(service, root, screen, "select_account", target) { XUiActions.clickExactHandle(service, root, target) }) {
                    accountSelectionMade = true
                    accountSettleUntil = now + AutomationTuning.accountSwitchSettleMs
                    _state.value = current.copy(status = RuntimeStatus.SWITCHING_ACCOUNT, message = "@$target seçildi; X oturumu doğrulanacak")
                    // Wait for the selected account to settle before closing any remaining sheet.
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

    private fun waitForAccountNavigation(service: AtmacaAccessibilityService, now: Long) {
        nextActionNotBefore = now + 1_200L
        service.requestAutomationTick(1_200L)
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
                moveStage(XFlowStage.OPEN_VERIFIED_OWN_PROFILE, "En yeni takipçi için kendi profilin açılıyor")
                service.requestAutomationTick(300L)
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
                    moveStage(XFlowStage.PROCESS_UNFOLLOW, "Takip edilenler doğrulandı; görünür kullanıcılardan başlanıyor")
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
                                message = "Listenin sonuna ulaşıldı; işlenmemiş kullanıcılar kontrol ediliyor",
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
                    if (!UnfollowAttemptBudget.mayIssue(unfollowIssuedCount, current.limit, current.cycleIndex, current.perCycleLimit)) {
                        pause("Takipten çıkma işlem sınırına ulaşıldı: $unfollowIssuedCount/${current.limit}. Doğrulanamayan işlemler yerine ek kişiye basılmadı.")
                        return
                    }
                    if (performStep(service, root, screen, "unfollow", target.handle) { XUiActions.clickRelationship(service, target) }) {
                        unfollowIssuedCount++
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
            XFlowStage.OPEN_VERIFIED_OWN_PROFILE -> {
                val own = XIdentityDetector.normalizeUsername(current.username.orEmpty())
                if (screen == XScreen.PROFILE && XIdentityDetector.detectProfileHandle(root) == own) {
                    if (XUiActions.clickProfileFollowers(service, root)) {
                        moveStage(XFlowStage.OPEN_MY_FOLLOWERS, "Kendi takipçiler listenin başı aranıyor")
                        nextActionNotBefore = now + 700L
                    } else if (stageTimedOut(now)) return fail("Kendi profilinde takipçiler sayacı bulunamadı")
                } else {
                    if (now - stageStartedAt >= 30_000L || stageAttempts >= 10)
                        return fail("X içinde geri gezinmeyle kendi profil kimliği doğrulanamadı")
                    if (root != null && screen != XScreen.UNKNOWN) {
                        when (screen) {
                            XScreen.HOME -> XNavigator.execute(service, root, NavigationCommand.OPEN_ACCOUNT_DRAWER, own, null)
                            XScreen.ACCOUNT_DRAWER -> XNavigator.execute(service, root, NavigationCommand.OPEN_PROFILE_FROM_DRAWER, own, null)
                            else -> service.pressBack()
                        }
                        stageAttempts++
                        nextActionNotBefore = now + 1_200L
                    }
                }
                service.requestAutomationTick(700L)
            }
            XFlowStage.OPEN_MY_FOLLOWERS -> {
                if (now - stageStartedAt > 30_000L) return fail("Kendi takipçiler listesinin başı 30 saniyede doğrulanamadı")
                if (screen == XScreen.FOLLOWERS_LIST) {
                    val followerNodes = AccessibilityTree.snapshots(root)
                    // A source can already be followed or have no exposed follow button.
                    // Dedicated visible usernames, not action buttons, prove source rows.
                    if (RecentFollowerSelector.orderedHandles(followerNodes, emptySet()).isEmpty()) {
                        service.requestAutomationTick(500L)
                        return
                    }
                    val signature = RecentFollowerSelector.viewportSignature(followerNodes)
                    if (lastListSignature == signature) listEndStable++ else listEndStable = 0
                    lastListSignature = signature
                    // Never drag down at the top: X treats that as pull-to-refresh.
                    // Read the settled viewport before issuing another native scroll.
                    if (listEndStable >= END_STABLE_COUNT) {
                        moveStage(XFlowStage.FIND_RECENT_FOLLOWER, "Takipçiler listesinin başı doğrulandı; en yeni ziyaret edilmemiş takipçi aranıyor")
                        service.requestAutomationTick(150L)
                        return
                    }
                    val scroll = ListViewportController.tryScrollUserRowsBackward(root)
                    if (scroll == ScrollAttemptResult.NO_SCROLL_CONTAINER)
                        return pause("Takipçiler listesinin dikey kapsayıcısı okunamadı; yenileme hareketi yapılmadı")
                    if (scroll == ScrollAttemptResult.SCROLLED) listScrolls++
                    nextActionNotBefore = now + 700L
                    service.requestAutomationTick(700L)
                } else {
                    if (XUiActions.clickFollowersTab(service, root)) {
                        nextActionNotBefore = now + 600L
                        service.requestAutomationTick(600L)
                    }
                    else if (stageTimedOut(now)) fail("Kendi Followers sekmesi açılamadı")
                    else {
                        ListGesture.left(service, root)
                        nextActionNotBefore = now + 600L
                        service.requestAutomationTick(600L)
                    }
                }
            }
            XFlowStage.FIND_RECENT_FOLLOWER -> {
                if (screen != XScreen.FOLLOWERS_LIST) {
                    recoverOperation(service, "Kaynak takipçi seçilirken liste kayboldu")
                    return
                }
                val own = XIdentityDetector.normalizeUsername(current.username.orEmpty())
                val source = RecentFollowerSelector.orderedHandles(AccessibilityTree.snapshots(root), sourceHandles + own).firstOrNull()
                if (source == null) {
                    if (!scrollForwardAndTrack(service, root)) finishCycleOrTask(service, "Ziyaret edilecek yeni takipçi kalmadı; bulunan kadar onaylı kullanıcı takip edildi")
                    service.requestAutomationTick(500L)
                    return
                }
                sourceHandle = source
                if (performStep(service, root, screen, "open_source_follower", source) { XUiActions.clickSourceProfile(service, root, source) }) {
                    moveStage(XFlowStage.OPEN_SOURCE_PROFILE, "Followers listesinin en üstündeki @$source profili açılıyor")
                    service.requestAutomationTick(500L)
                }
            }
            XFlowStage.RETURN_VERIFIED_SOURCE -> {
                if (now - stageStartedAt > 30_000L) return pause("Yeni kaynak bulunamadı; ${current.verifiedCount}/${current.limit} işlem korundu")
                if (screen in setOf(XScreen.FOLLOWERS_LIST, XScreen.VERIFIED_FOLLOWERS_LIST)) {
                    val own = XIdentityDetector.normalizeUsername(current.username.orEmpty())
                    val handles = RecentFollowerSelector.orderedHandles(AccessibilityTree.snapshots(root), sourceHandles + own)
                    val candidate = if (screen == XScreen.VERIFIED_FOLLOWERS_LIST) handles.randomOrNull() else handles.firstOrNull()
                    if (candidate != null && XUiActions.clickSourceProfile(service, root, candidate)) {
                        sourceHandle = candidate
                        moveStage(XFlowStage.OPEN_SOURCE_PROFILE, "@$candidate yeni kaynak profili doğrulanıyor")
                    } else if (!scrollForwardAndTrack(service, root)) {
                        listEndStable++
                        if (listEndStable >= END_STABLE_COUNT) {
                            service.pressBack()
                            listEndStable = 0
                        }
                    }
                } else if (screen != XScreen.UNKNOWN) service.pressBack()
                nextActionNotBefore = now + 800L
                service.requestAutomationTick(800L)
            }
            XFlowStage.LOCATE_SOURCE_ROW -> {
                val source = sourceHandle ?: return pause("Rastgele kaynak seçimi kayboldu")
                if (screen != XScreen.VERIFIED_FOLLOWERS_LIST)
                    return pause("Yeni kaynak için onaylı liste görünmüyor; profil bağlantısıyla atlanmadı")
                if (XUiActions.clickSourceProfile(service, root, source)) {
                    moveStage(XFlowStage.OPEN_SOURCE_PROFILE, "Onaylı listedeki @$source adına dokunuldu; profil doğrulanıyor")
                    nextActionNotBefore = now + 900L
                } else {
                    if (now - stageStartedAt >= 30_000L) return pause("@$source kaynak satırına geri kaydırmayla ulaşılamadı")
                    val moved = ListViewportController.tryScrollUserRowsBackward(root) == ScrollAttemptResult.SCROLLED ||
                        ListGesture.backward(service, root)
                    if (!moved) return pause("Kaynak satırını bulmak için kaydırma gerçekleştirilemedi")
                    nextActionNotBefore = now + 700L
                }
                service.requestAutomationTick(700L)
            }
            XFlowStage.OPEN_SOURCE_PROFILE -> {
                val source = sourceHandle ?: return nextVerifiedSource(service, "Kaynak takipçi kayboldu")
                if (screen == XScreen.PROFILE && XIdentityDetector.detectProfileHandle(root) == source) {
                    sourceHandles += source
                    if (performStep(service, root, screen, "open_source_followers", source) { XUiActions.clickProfileFollowers(service, root) }) {
                        moveStage(XFlowStage.OPEN_SOURCE_FOLLOWERS, "@$source takipçileri açılıyor")
                        service.requestAutomationTick(500L)
                    } else if (stageTimedOut(now)) {
                        pause("@$source profili doğrulandı fakat takipçiler sayacı tıklanamadı")
                    } else service.requestAutomationTick(500L)
                } else {
                    val elapsed = now - stageStartedAt
                    if (elapsed >= 15_000L) {
                        val nodes = AccessibilityTree.snapshots(root)
                        val profile = ProfileSurfaceEvidence.read(nodes)
                        OperationLog.w("PROFILE_EVIDENCE", "screen=$screen nodes=${nodes.size} header=${XIdentityDetector.detectProfileHandle(root)} pairedCounters=${profile != null} selectedTab=${RelationshipTabInspector.selectedTab(root)}")
                        return pause("@$source profil kimliği doğrulanamadı; PROFILE_EVIDENCE teşhisi kaydedildi, takip yapılmadı")
                    }
                    // Retry once only when the same username is still on a verified list surface.
                    if (elapsed >= 2_000L && stageAttempts == 0 &&
                        screen in setOf(XScreen.FOLLOWERS_LIST, XScreen.VERIFIED_FOLLOWERS_LIST)) {
                        stageAttempts++
                        XUiActions.clickSourceProfile(service, root, source)
                        nextActionNotBefore = now + 900L
                    }
                    service.requestAutomationTick(TICK_MS)
                }
            }
            XFlowStage.OPEN_SOURCE_FOLLOWERS -> {
                if (screen == XScreen.FOLLOWERS_LIST || screen == XScreen.VERIFIED_FOLLOWERS_LIST ||
                    RelationshipTabInspector.selectedTab(root) == RelationshipTabInspector.OTHER) {
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
                    nextVerifiedSource(service, "Bu kaynakta Onaylı Takipçiler sekmesi açılamadı")
                } else {
                    stageAttempts++
                    if (!XUiActions.clickVerifiedTab(service, root)) ListGesture.right(service, root)
                    nextActionNotBefore = now + 650L
                    service.requestAutomationTick(650L)
                }
            }
            XFlowStage.PROCESS_VERIFIED_FOLLOW -> {
                if (screen != XScreen.VERIFIED_FOLLOWERS_LIST) {
                    stageAttempts++
                    if (stageAttempts >= 6) pauseVerifiedTab(root, screen, "Onaylı Takipçiler listesi doğrulanamadı")
                    else {
                        XUiActions.clickVerifiedTab(service, root)
                        nextActionNotBefore = now + 650L
                        service.requestAutomationTick(650L)
                    }
                    return
                }
                stageAttempts = 0
                if (cycleTargetReached()) {
                    finishCycleOrTask(service, "Onaylı takip döngü limiti tamamlandı")
                    return
                }
                RecentFollowerSelector.orderedHandles(AccessibilityTree.snapshots(root), sourceHandles +
                    XIdentityDetector.normalizeUsername(current.username.orEmpty())).forEach { handle ->
                    if (verifiedSourceCandidates.size < 200) verifiedSourceCandidates += handle
                }
                val target = XUiActions.findRelationshipTarget(
                    root,
                    acceptedLabels = VerifiedFollowPolicy.plainFollowLabels,
                    excludedHandles = processedHandles + skippedHandles,
                    fromBottom = false,
                )
                if (target != null) {
                    if (performStep(service, root, screen, "follow_verified", target.handle) { XUiActions.clickRelationship(service, target) }) {
                        verifiedFollowingObservedAt = 0L
                        verifiedFollowingStableAt = 0L
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
            ?: return finishCycleOrTask(service, "Hedefin erişilebilir uygun gönderileri tarandı; bulunan kadar kullanıcı takip edildi")
        when (current.flowStage) {
            XFlowStage.RETURN_DISCOVERY_TARGET -> {
                if ((screen == XScreen.PROFILE && XIdentityDetector.detectProfileHandle(root) == target) ||
                    (FeedRowEvidence.profileFeed(AccessibilityTree.snapshots(root)) &&
                        XIdentityDetector.detectProfileHandle(root).let { it == null || it == target } &&
                        XTweetInspector.visibleTweets(root).any { it.author == target })) {
                    moveStage(XFlowStage.SCAN_LATEST_TWEETS, "Hedefte sıradaki en az iki saatlik gönderi aranıyor")
                } else if (now - stageStartedAt >= 15_000L) return pause("Hedefin gönderi listesine dönüş doğrulanamadı")
                else if (screen != XScreen.UNKNOWN) service.pressBack()
                nextActionNotBefore = now + 650L
                service.requestAutomationTick(650L)
            }
            XFlowStage.OPEN_DISCOVERY_TARGET -> {
                if (screen == XScreen.PROFILE && XIdentityDetector.detectProfileHandle(root) == target) {
                    moveStage(XFlowStage.SCAN_LATEST_TWEETS, "@$target profilindeki en az 2 saatlik gönderiler taranıyor")
                    service.requestAutomationTick(150L)
                } else {
                    when (DiscoveryTargetLaunchPolicy.decide(
                        exactTargetVisible = false,
                        launchAttempts = stageAttempts,
                        elapsedMs = now - stageStartedAt,
                    )) {
                        DiscoveryTargetLaunchDecision.WAIT -> service.requestAutomationTick(TICK_MS)
                        DiscoveryTargetLaunchDecision.LAUNCH -> {
                            val attempt = ++stageAttempts
                            OperationLog.i(
                                "DISCOVERY_TARGET",
                                "Hedef profil yönlendirmesi gönderiliyor target=@$target attempt=$attempt screen=$screen",
                            )
                            if (!service.launchDiscoveryProfile(target, attempt)) {
                                beginDiscoverySearch(service, target)
                            } else {
                                nextActionNotBefore = now + DiscoveryTargetLaunchPolicy.SETTLE_MS
                                service.requestAutomationTick(DiscoveryTargetLaunchPolicy.SETTLE_MS)
                            }
                        }
                        DiscoveryTargetLaunchDecision.GIVE_UP -> {
                            beginDiscoverySearch(service, target)
                        }
                    }
                }
            }
            XFlowStage.SEARCH_DISCOVERY_TARGET -> handleDiscoverySearch(service, root, screen, now, target)
            XFlowStage.SCAN_LATEST_TWEETS -> {
                if (screen !in setOf(XScreen.PROFILE, XScreen.UNKNOWN)) {
                    if (stageTimedOut(now)) pause("@$target gönderi listesi doğrulanamadı; görev tamamlanmadı") else service.requestAutomationTick(TICK_MS)
                    return
                }
                val seen = discoverySeenTweets.getOrPut(target) { LinkedHashMap() }
                val rows = XTweetInspector.visibleTweets(root).filter { it.author == target }
                if (rows.isEmpty() && listScrolls % 3 == 0) {
                    val evidence = AccessibilityTree.snapshots(root).filter { it.visible && !it.editable }
                        .filter { it.text != null || it.contentDescription != null }.take(24)
                        .joinToString(" | ") { "${it.viewId}:${(it.text ?: it.contentDescription).orEmpty().take(140)}@${it.bounds}" }
                    OperationLog.w("DISCOVERY_READ", "Gönderi okunamadı; ekran kanıtı=$evidence")
                }
                if (now - stageStartedAt > 180_000L) {
                    pause("Üç dakikada yeni uygun gönderi açılamadı; tarama kanıtı DISCOVERY_READ kaydında")
                    return
                }
                rows.forEach { row -> seen.putIfAbsent(row.key, row.ageMinutes) }
                OperationLog.i("DISCOVERY_SCAN", "target=@$target screen=$screen rows=${rows.size} ages=${rows.map { it.ageMinutes }} scroll=$listScrolls")
                val eligible = rows.firstOrNull { row ->
                    row.key !in discoveryProcessedTweets && XTweetInspector.eligibleAge(row.ageMinutes)
                }
                if (eligible != null) {
                    discoveryTweetKey = eligible.key
                    OperationLog.i("DISCOVERY_TWEET", "target=@$target age=${eligible.ageMinutes} text=${eligible.textTarget != null} key=${eligible.key}")
                    if (eligible.textTarget == null) {
                        pause("Uygun gönderi bulundu fakat metin alanı okunamadı; yanlış yere basılmadı")
                        return
                    }
                    if (performStep(service, root, screen, "open_discovery_tweet", eligible.key) { XTweetInspector.click(service, eligible) }) {
                        lastListSignature = ""
                        engagementOpenAttempts = 0
                        moveStage(XFlowStage.OPEN_ENGAGEMENT, "Gönderi metni açıldı; detay ekranı doğrulanıyor")
                        service.requestAutomationTick(500L)
                    }
                    return
                }
                if (!scrollForwardAndTrack(service, root)) {
                    listEndStable++
                    if (listEndStable >= END_STABLE_COUNT) nextDiscoveryTarget(service, "@$target profilinde başka erişilebilir uygun gönderi kalmadı")
                }
                service.requestAutomationTick(550L)
            }
            XFlowStage.OPEN_ENGAGEMENT -> {
                if (current.taskType == TaskType.RETWEETER_FOLLOW && screen == XScreen.ENGAGEMENT_LIST) {
                    if (EngagementListEvidence.selected(root)) {
                        moveStage(XFlowStage.PROCESS_ENGAGEMENT, "Yeniden gönderenler sekmesi doğrulandı")
                    } else if (stageAttempts++ < 6) XUiActions.clickEngagementList(service, root, false)
                    else return nextDiscoveryTweet(service, "Yeniden gönderenler sekmesi doğrulanamadı")
                    service.requestAutomationTick(600L)
                    return
                }
                if (screen != XScreen.TWEET_DETAIL) {
                    if (stageTimedOut(now)) pause("Gönderi metnine dokunuldu fakat detay açılmadı; gönderi işlenmiş sayılmadı") else service.requestAutomationTick(TICK_MS)
                    return
                }
                discoveryTweetKey?.let(discoveryProcessedTweets::add)
                when (current.taskType) {
                    TaskType.COMMENTER_FOLLOW -> {
                        moveStage(XFlowStage.PROCESS_ENGAGEMENT, "Yorum yapan kullanıcılar taranıyor")
                        service.requestAutomationTick(150L)
                    }
                    TaskType.RETWEETER_FOLLOW, TaskType.QUOTER_FOLLOW -> {
                        val quotes = current.taskType == TaskType.QUOTER_FOLLOW
                        if (XUiActions.clickEngagementList(service, root, quotes)) {
                            // Count accepted taps too: dispatch success is not proof of navigation.
                            if (++engagementOpenAttempts >= 12) return pause("Alıntıları görüntüle dokunuşu listeyi açmadı")
                            // Searching below media must not consume the next screen's tab retries.
                            stageAttempts = 0
                            listEndStable = 0
                            lastListSignature = ""
                            nextActionNotBefore = now + 700L
                            service.requestAutomationTick(500L)
                        } else if (DiscoveryViewportEvidence.quotesExhausted(stageAttempts++, listEndStable)) nextDiscoveryTweet(service, "Bu gönderide Alıntıları görüntüle/etkileşim listesi yok")
                        else {
                            // Quotes entry sits below the post's media, not always in the first viewport.
                            if (!scrollForwardAndTrack(service, root)) listEndStable++
                            service.requestAutomationTick(800L)
                        }
                    }
                    else -> Unit
                }
            }
            XFlowStage.PROCESS_ENGAGEMENT -> {
                val expected = if (current.taskType == TaskType.COMMENTER_FOLLOW) XScreen.TWEET_DETAIL else XScreen.ENGAGEMENT_LIST
                if (screen != expected || (current.taskType == TaskType.RETWEETER_FOLLOW &&
                    !EngagementListEvidence.selected(root))) {
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
                val relationship = if (current.taskType == TaskType.COMMENTER_FOLLOW) null else XUiActions.findRelationshipTarget(
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
                if (current.taskType == TaskType.COMMENTER_FOLLOW) {
                    val author = XTweetInspector.visibleReplyAuthors(root).firstOrNull { it !in excluded }
                    if (author != null) {
                        engagerHandle = author
                        engagerParentSignature = DiscoveryViewportEvidence.signature(AccessibilityTree.snapshots(root))
                        engagerParentKeys = XTweetInspector.visibleTweets(root).map { it.key }
                        moveStage(XFlowStage.OPEN_ENGAGER_PROFILE, "Yorumcu @$author gönderisi veya profili doğrulanıyor")
                        if (!XTweetInspector.clickReplyAuthor(service, root, author)) {
                            skippedHandles += author
                            moveStage(XFlowStage.PROCESS_ENGAGEMENT, "Yorumcu profili açılmadı; yorumlarda sıradaki kullanıcı aranıyor")
                            service.requestAutomationTick(350L)
                        } else {
                            nextActionNotBefore = now + 700L
                            service.requestAutomationTick(700L)
                        }
                        return
                    }
                }
                if (!scrollForwardAndTrack(service, root)) {
                    listEndStable++
                    if (listEndStable >= 3) nextDiscoveryTweet(service, "Bu gönderide yeni takip edilebilir etkileşim kalmadı")
                }
                service.requestAutomationTick(500L)
            }
            XFlowStage.OPEN_ENGAGER_PROFILE -> {
                val handle = engagerHandle ?: return nextDiscoveryTweet(service, "Yorumcu kimliği bulunamadı")
                val detailNodes = AccessibilityTree.snapshots(root)
                val detailAuthor = if (screen == XScreen.TWEET_DETAIL) CommentDetailEvidence.header(detailNodes)?.handle else null
                if (detailAuthor == handle) {
                    val following = CommentDetailEvidence.has(detailNodes, handle, XUiVocabulary.followingActions + XUiVocabulary.requestedActions)
                    val available = CommentDetailEvidence.has(detailNodes, handle, VerifiedFollowPolicy.plainFollowLabels)
                    if (following && !available) {
                        skippedHandles += handle
                        returnFromEngager(service)
                    } else if (available && !following && performStep(service, root, screen, "follow_comment_detail_author", handle) {
                            XUiActions.clickCommentDetailFollow(service, root, handle)
                        }) {
                        pendingAction = PendingAction(PendingKind.ENGAGER_FOLLOW, handle)
                        _state.value = _state.value.copy(status = RuntimeStatus.VERIFYING, lastActionAt = now,
                            message = "@$handle gönderi başlığındaki takip sonucu doğrulanıyor")
                        service.requestAutomationTick(450L)
                    } else if (stageTimedOut(now)) {
                        skippedHandles += handle
                        returnFromEngager(service)
                    } else service.requestAutomationTick(350L)
                    return
                }
                if (screen != XScreen.PROFILE || XIdentityDetector.detectProfileHandle(root) != handle) {
                    if (stageTimedOut(now)) {
                        skippedHandles += handle
                        if (screen == XScreen.TWEET_DETAIL && isEngagerParent(root)) {
                            engagerHandle = null
                            moveStage(XFlowStage.PROCESS_ENGAGEMENT, "Profil açılmadı; sıradaki yorumcu aranıyor")
                            service.requestAutomationTick(350L)
                        } else pause("Yorumcu gönderisi/profili doğrulanamadı; alt yorumlara girilmedi")
                    }
                    else service.requestAutomationTick(350L)
                } else if (XUiActions.isDirectFollowing(root) || XUiActions.directRequested(root)) {
                    skippedHandles += handle
                    returnFromEngager(service)
                } else if (XUiActions.directFollowAvailable(root) && performStep(service, root, screen, "follow_comment_author", handle) { XUiActions.clickDirectFollow(service, root) }) {
                    pendingAction = PendingAction(PendingKind.ENGAGER_FOLLOW, handle)
                    service.requestAutomationTick(450L)
                } else if (stageTimedOut(now)) { skippedHandles += handle; returnFromEngager(service) }
                else service.requestAutomationTick(350L)
            }
            XFlowStage.RETURN_ENGAGEMENT -> {
                if (screen == XScreen.TWEET_DETAIL && isEngagerParent(root)) {
                    engagerHandle = null
                    lastListSignature = ""
                    moveStage(XFlowStage.PROCESS_ENGAGEMENT, "Sıradaki yorumcu aranıyor")
                    service.requestAutomationTick(250L)
                } else if (stageTimedOut(now)) pause("Ana gönderinin yorumlarına dönüş doğrulanamadı; alt yorumlar işlenmedi")
                else if (now - stageStartedAt >= 1_500L && stageAttempts == 0 &&
                    ((screen == XScreen.TWEET_DETAIL && CommentDetailEvidence.header(AccessibilityTree.snapshots(root))?.handle == engagerHandle) ||
                        (screen == XScreen.PROFILE && XIdentityDetector.detectProfileHandle(root) == engagerHandle))) {
                    stageAttempts++
                    service.pressBack()
                    nextActionNotBefore = now + 700L
                    service.requestAutomationTick(700L)
                }
                else service.requestAutomationTick(350L)
            }
            else -> recoverOperation(service, "Etkileşim takip state'i tutarsızlaştı")
        }
    }

    private fun returnFromEngager(service: AtmacaAccessibilityService) {
        moveStage(XFlowStage.RETURN_ENGAGEMENT, "Gönderinin yorumlarına dönülüyor")
        service.pressBack()
        nextActionNotBefore = System.currentTimeMillis() + 700L
        service.requestAutomationTick(700L)
    }

    private fun isEngagerParent(root: AccessibilityNodeInfo?): Boolean {
        val nodes = AccessibilityTree.snapshots(root)
        return DiscoveryViewportEvidence.returnedToParent(engagerParentSignature,
            DiscoveryViewportEvidence.signature(nodes), engagerParentKeys, XTweetInspector.visibleTweets(root).map { it.key },
            CommentDetailEvidence.header(nodes)?.handle, engagerHandle)
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
                val rowShowsFollow = XUiActions.rowHasAny(root, handle, XUiVocabulary.followActions)
                val rowStillFollowing = XUiActions.rowHasAny(root, handle, XUiVocabulary.followingActions)

                // Bir hedef ancak önce gerçekten "Takip et" durumuna geçtiği
                // görüldükten sonra başarı sayılır. Aynı satır daha sonra tekrar
                // "Takip ediliyor" olursa X'in günlük limiti doğrulanmış olur.
                if (pending.confirmationClicked && unfollowFollowObservedAt > 0L) {
                    when {
                        rowStillFollowing && !rowShowsFollow -> {
                            // A reverted relationship is not proof of a daily limit. Keep
                            // successes truthful and pause without rotating to another account.
                            pendingAction = null
                            pause("@$handle yeniden Takip ediliyor göründü; işlem doğrulanamadı. X durumunu kontrol et.")
                        }
                        rowShowsFollow && !rowStillFollowing && now - unfollowFollowObservedAt >= UNFOLLOW_RESULT_STABLE_MS -> recordSuccess(service, "@$handle")
                        elapsed >= ACTION_TIMEOUT_MS * 2L -> skipUnfollowTarget(service, handle, "İlişki sonucu kararsız; başarı sayılmadan kullanıcı atlandı")
                        else -> service.requestAutomationTick(350L)
                    }
                    return
                }
                if (pending.confirmationClicked && rowShowsFollow && !rowStillFollowing) {
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
                if (_state.value.taskType == TaskType.VERIFIED_FOLLOW) {
                    verifyVerifiedFollow(service, root, now, pending, handle)
                    return
                }
                if (XUiActions.rowHasAny(root, handle, XUiVocabulary.followingActions + XUiVocabulary.requestedActions) &&
                    !XUiActions.rowHasAny(root, handle, VerifiedFollowPolicy.plainFollowLabels)) recordSuccess(service, "@$handle")
                else if (elapsed >= ACTION_TIMEOUT_MS) {
                    pendingAction = null
                    pause("@$handle takip sonucu kesinleşmedi; limit aşılmaması için yeni takip yapılmadı")
                } else service.requestAutomationTick(400L)
            }
            PendingKind.ENGAGER_FOLLOW -> {
                val nodes = AccessibilityTree.snapshots(root)
                val handle = pending.target.orEmpty()
                val detailConfirmed = screen == XScreen.TWEET_DETAIL &&
                    CommentDetailEvidence.has(nodes, handle, XUiVocabulary.followingActions + XUiVocabulary.requestedActions) &&
                    !CommentDetailEvidence.has(nodes, handle, VerifiedFollowPolicy.plainFollowLabels)
                val profileConfirmed = screen == XScreen.PROFILE && XIdentityDetector.detectProfileHandle(root) == pending.target &&
                    (XUiActions.isDirectFollowing(root) || XUiActions.directRequested(root)) && !XUiActions.directFollowAvailable(root)
                if (detailConfirmed || profileConfirmed) {
                    recordSuccess(service, pending.target)
                } else if (elapsed >= ACTION_TIMEOUT_MS) {
                    pendingAction = null
                    pause("Yorumcu takip sonucu kesinleşmedi; limit aşılmaması için yeni takip yapılmadı")
                } else service.requestAutomationTick(350L)
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

    private fun verifyVerifiedFollow(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, now: Long,
                                     pending: PendingAction, handle: String) {
        val requested = XUiActions.rowHasAny(root, handle, XUiVocabulary.requestedActions)
        val following = XUiActions.rowHasAny(root, handle, XUiVocabulary.followingActions)
        val plainFollow = XUiActions.rowHasAny(root, handle, VerifiedFollowPolicy.plainFollowLabels)
        if (following && !plainFollow && verifiedFollowingObservedAt == 0L) verifiedFollowingObservedAt = now
        if (following && !plainFollow) {
            if (verifiedFollowingStableAt == 0L) verifiedFollowingStableAt = now
        } else verifiedFollowingStableAt = 0L
        val outcome = VerifiedFollowPolicy.outcome(verifiedFollowingObservedAt > 0L, following, plainFollow,
            if (verifiedFollowingStableAt > 0L) now - verifiedFollowingStableAt else 0L, now - pending.startedAt, requested)
        verifiedRevertStreak = VerifiedFollowPolicy.nextStreak(verifiedRevertStreak, outcome)
        when (outcome) {
            VerifiedFollowOutcome.WAIT -> service.requestAutomationTick(300L)
            VerifiedFollowOutcome.SUCCESS -> {
                if (requested) OperationLog.i("FOLLOW_REQUEST", "@$handle yeni isteği Beklemede; işlem sayıldı")
                recordSuccess(service, "@$handle")
            }
            VerifiedFollowOutcome.REVERTED -> {
                pendingAction = null
                skippedHandles += handle
                if (VerifiedFollowPolicy.stopAccount(verifiedRevertStreak)) {
                    _state.value = _state.value.copy(status = RuntimeStatus.FAILED, verifiedFollowAccountStopped = true,
                        message = "Üç ardışık takip Takip et durumuna döndü; bu hesap için takip durduruldu, sıradaki hesaba geçilecek")
                } else {
                    _state.value = _state.value.copy(status = RuntimeStatus.RUNNING,
                        message = "@$handle takip durumu geri döndü ($verifiedRevertStreak/3); başarı sayılmadı")
                    nextActionNotBefore = now + AutomationTuning.betweenActionsMs
                    service.requestAutomationTick(AutomationTuning.betweenActionsMs)
                }
            }
            VerifiedFollowOutcome.UNKNOWN -> {
                pendingAction = null
                pause("@$handle takip sonucu kesinleşmedi; geri dönüş veya başarı sayılmadı")
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
            if (current.flowStage == XFlowStage.OPEN_ENGAGER_PROFILE) returnFromEngager(service)
            nextActionNotBefore = System.currentTimeMillis() + AutomationTuning.betweenActionsMs
            service.requestAutomationTick(AutomationTuning.betweenActionsMs)
        }
    }

    private fun skipUnfollowTarget(service: AtmacaAccessibilityService, handle: String, reason: String) {
        val current = _state.value
        pendingAction = null
        unfollowFollowObservedAt = 0L
        if (current.taskType == TaskType.UNFOLLOW) {
            pause("$reason: @$handle. Sonuç belirsiz; yerine başka kullanıcı işlenmedi.")
            return
        }
        skippedHandles += handle
        afterUnfollowTargetHandled()
        _state.value = current.copy(status = RuntimeStatus.RUNNING, message = "$reason: @$handle")
        service.requestAutomationTick(350L)
    }

    private fun afterUnfollowTargetHandled() {
        if (!unfollowAnchorPending) return
        unfollowAnchorPending = false
        if (!unfollowReverseMode) unfollowNeedForwardScroll = true
    }

    private fun finishCycleOrTask(service: AtmacaAccessibilityService, reason: String) {
        val current = _state.value
        if (current.taskType?.isDiscoveryFollow == true && !cycleTargetReached()) {
            pause("$reason. ${current.verifiedCount}/${current.limit}; limit tamamlanmadı, ilerleme korundu")
            return
        }
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
        val own = XIdentityDetector.normalizeUsername(_state.value.username.orEmpty())
        val next = VerifiedFollowPolicy.nextSource(verifiedSourceCandidates, own, sourceHandles)
        // Preserve verified progress across sources; candidates belong to the list
        // just exhausted, not an unrelated earlier source.
        verifiedSourceCandidates.clear()
        listEndStable = 0
        lastListSignature = ""
        sourceHandle = next
        if (next != null) {
            moveStage(XFlowStage.LOCATE_SOURCE_ROW, "$reason; onaylı listede rastgele @$next satırı bulunup açılacak")
        } else {
            moveStage(XFlowStage.RETURN_VERIFIED_SOURCE, "$reason; önceki listeye dönülüp başka kaynak aranıyor")
            service.pressBack()
        }
        service.requestAutomationTick(650L)
    }

    private fun pauseVerifiedTab(root: AccessibilityNodeInfo?, screen: XScreen, reason: String) {
        val headers = AccessibilityTree.snapshots(root).filter { it.visible }.mapNotNull { node ->
            val labels = listOfNotNull(node.text, node.contentDescription)
            if (RelationshipTabInspector.classifySelectedLabels(labels) == RelationshipTabInspector.NONE) null
            else "${labels.joinToString("/").take(120)} selected=${node.selected} checked=${node.checked} bounds=${node.bounds}"
        }.distinct().take(24).joinToString(" | ")
        OperationLog.w("VERIFIED_TAB", "screen=$screen selected=${RelationshipTabInspector.selectedTab(root)} headers=$headers")
        pause("$reason; ekran teşhisi kaydedildi, sayfa yeniden açılmadı")
    }

    private fun beginDiscoverySearch(service: AtmacaAccessibilityService, target: String) {
        discoverySearchStep = 0
        discoverySearchSubmitted = false
        moveStage(XFlowStage.SEARCH_DISCOVERY_TARGET, "@$target X içi aramayla açılıyor")
        OperationLog.i("DISCOVERY_SEARCH", "Bağlantı hedefi açmadı; X içi arama target=@$target")
        service.requestAutomationTick(250L)
    }

    private fun handleDiscoverySearch(service: AtmacaAccessibilityService, root: AccessibilityNodeInfo?, screen: XScreen, now: Long, target: String) {
        if (screen == XScreen.PROFILE && XIdentityDetector.detectProfileHandle(root) == target) {
            OperationLog.i("DISCOVERY_SEARCH", "Hedef profil doğrulandı target=@$target")
            moveStage(XFlowStage.SCAN_LATEST_TWEETS, "@$target profilindeki en az 2 saatlik gönderiler taranıyor")
            service.requestAutomationTick(150L)
            return
        }
        if (now - stageStartedAt >= 45_000L) {
            pause("@$target X içi aramada açılamadı; hedef doğrulanmadığı için görev tamamlanmadı")
            return
        }
        val nodes = AccessibilityTree.nodes(root)
        val snapshots = nodes.map { it.toSnapshot() }
        var action = "wait"
        var accepted = false
        when (discoverySearchStep) {
            0 -> {
                // Leave the own-profile page through X's back stack. Its toolbar
                // magnifier searches only that profile, so it is never used here.
                val tab = DiscoverySearchSelector.searchTab(snapshots)
                if (tab != null) {
                    accepted = GestureClick.gestureTap(service, nodes[tab])
                    action = "open-search-tab"
                    if (accepted) discoverySearchStep = 1
                } else if (screen != XScreen.HOME && root != null && stageAttempts < 6) {
                    accepted = service.pressBack()
                    stageAttempts++
                    action = "back-to-navigation"
                }
            }
            1 -> {
                val field = DiscoverySearchSelector.searchField(snapshots)
                if (field != null) {
                    val args = android.os.Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "@$target")
                    }
                    accepted = nodes[field].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    action = "set-query"
                    if (accepted) discoverySearchStep = 2
                } else {
                    val entry = DiscoverySearchSelector.searchEntry(snapshots)
                    if (entry != null) {
                        accepted = GestureClick.gestureTap(service, nodes[entry])
                        action = "focus-search"
                    }
                }
            }
            2 -> {
                val field = DiscoverySearchSelector.searchField(snapshots)
                if (field != null && nodes[field].text?.toString()?.trim().equals("@$target", ignoreCase = true)) {
                    val people = DiscoverySearchSelector.peopleTab(snapshots)
                    if (discoverySearchSubmitted && (people == null || !snapshots[people].selected)) {
                        if (people != null) GestureClick.gestureTap(service, nodes[people])
                        nextActionNotBefore = now + 1_000L
                        service.requestAutomationTick(1_000L)
                        return
                    }
                    val result = DiscoverySearchSelector.result(snapshots, target, field)
                    if (result != null) {
                        accepted = GestureClick.gestureTap(service, nodes[result])
                        action = "open-exact-result"
                        if (accepted) discoverySearchStep = 3
                    } else if (!discoverySearchSubmitted && now - stageStartedAt > 18_000L && android.os.Build.VERSION.SDK_INT >= 30) {
                        accepted = nodes[field].performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
                        discoverySearchSubmitted = true
                        action = "submit-search"
                    }
                }
            }
            // Wait for the profile, without tapping any other result or button.
            3 -> Unit
        }
        if (action != "wait") OperationLog.i("DISCOVERY_SEARCH", "target=@$target step=$discoverySearchStep action=$action accepted=$accepted screen=$screen")
        nextActionNotBefore = now + 1_000L
        service.requestAutomationTick(1_000L)
    }

    private fun openDiscoveryTarget(service: AtmacaAccessibilityService) {
        val target = discoveryTargets.getOrNull(discoveryTargetIndex)
            ?: return finishCycleOrTask(service, "Tüm hedef hesaplar tarandı")
        listEndStable = 0
        lastListSignature = ""
        // 26.17 device evidence: all three deep links leave X on the own profile.
        // Use the proven in-app search immediately after account verification.
        beginDiscoverySearch(service, target)
    }

    private fun nextDiscoveryTweet(service: AtmacaAccessibilityService, reason: String) {
        discoveryTweetKey = null
        engagerHandle = null
        engagerParentSignature = ""
        engagerParentKeys = emptyList()
        listEndStable = 0
        lastListSignature = ""
        val target = discoveryTargets.getOrNull(discoveryTargetIndex)
            ?: return finishCycleOrTask(service, reason)
        moveStage(XFlowStage.RETURN_DISCOVERY_TARGET, "$reason; @$target gönderilerine geri dönülüyor")
        service.pressBack()
        service.requestAutomationTick(650L)
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
        val signature = if (_state.value.taskType?.isDiscoveryFollow == true)
            DiscoveryViewportEvidence.signature(AccessibilityTree.snapshots(root)) else ListViewportController.signature(root)
        val changed = lastListSignature.isBlank() || signature != lastListSignature
        lastListSignature = signature
        val dispatched = dispatchListScroll(service, root, forward = true)
        if (dispatched) {
            listScrolls++
            _state.value = _state.value.copy(listScrolls = listScrolls)
            if (_state.value.taskType?.isDiscoveryFollow == true) nextActionNotBefore = System.currentTimeMillis() + 850L
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
            if (_state.value.taskType?.isDiscoveryFollow == true) nextActionNotBefore = System.currentTimeMillis() + 850L
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
        if (_state.value.taskType?.isDiscoveryFollow == true) {
            // ACTION_SCROLL_FORWARD on X's profile pager changes Posts to Replies
            // and Videos. Discovery must use an explicitly vertical gesture only.
            return if (forward) ListGesture.forward(service, root) else ListGesture.backward(service, root)
        }
        if (_state.value.taskType in setOf(TaskType.UNFOLLOW, TaskType.VERIFIED_FOLLOW)) {
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
        engagementOpenAttempts = 0
        engagerHandle = null
        engagerParentSignature = ""
        engagerParentKeys = emptyList()
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
        verifiedFollowingObservedAt = 0L
        verifiedFollowingStableAt = 0L
        verifiedRevertStreak = 0
        verifiedSourceCandidates.clear()
        unfollowIssuedCount = 0
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
