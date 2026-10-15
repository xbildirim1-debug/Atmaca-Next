package com.atmacanext.app.ai

import android.content.Context
import android.net.Uri
import com.atmacanext.app.automation.AccountSyncController
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.data.settings.ContentPoolStore
import com.atmacanext.app.domain.model.QueueStatus
import com.atmacanext.app.domain.model.TaskType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class PilotChatLine(val mine: Boolean, val text: String, val imagePath: String? = null)
data class PilotGeneration(val concept: String, val requested: Int, val saved: Int = 0,
                           val active: Boolean = true, val pool: ContentPool = ContentPool.TWEET)
data class PilotChatState(val lines: List<PilotChatLine>, val chatting: Boolean = false,
    val generation: PilotGeneration? = null, val poolCount: Int = 0, val replyCount: Int = 0,
    val pendingImage: PilotImage? = null, val imageLoading: Boolean = false)

/** Executes typed native actions and reports receipts; chat alone cannot claim a mutation. */
class PilotChatCoordinator(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val execution = Mutex()
    private var generationJob: Job? = null
    private var generationVersion = 0L
    private var conversationJob: Job? = null
    private var conversationVersion = 0L
    private var lastApiFailure: String? = null
    private var imageVersion = 0L
    private val memory = app.getSharedPreferences("pilot_agent_v2767", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(PilotChatState(
        PilotChatStore.load(app).map { PilotChatLine(it.mine, it.text, it.imagePath) }.ifEmpty {
            listOf(PilotChatLine(false, "Pilot hazır. Görevleri ve iki metin havuzunu yönetebilir, resimleri ve gerçek işlem kayıtlarını inceleyebilirim."))
        }, poolCount = ContentPoolStore.lines(app, ContentPool.TWEET).size,
        replyCount = ContentPoolStore.lines(app, ContentPool.REPLY).size))
    val state = _state.asStateFlow()

    private fun refreshCounts() { _state.update { it.copy(poolCount = ContentPoolStore.lines(app, ContentPool.TWEET).size,
        replyCount = ContentPoolStore.lines(app, ContentPool.REPLY).size) } }
    private fun say(mine: Boolean, text: String, image: String? = null) {
        _state.update { it.copy(lines = (it.lines + PilotChatLine(mine, text, image)).takeLast(200)) }
        refreshCounts()
        PilotChatStore.save(app, _state.value.lines.map { PilotChatStore.Line(it.mine, it.text, it.imagePath) })
    }
    private suspend fun receipt(action: PilotAction, result: String) {
        memory.edit().putString("last_receipt", "${action.tool}: $result").apply()
        say(false, result)
        // A log write cannot turn a successfully committed pool edit into a reported failure.
        try {
            AppServices.repository.log("INFO", "PILOT_ACTION", null, AutomationController.state.value.username,
                result.take(1500), "tool=${action.tool}; pool=${action.pool}; destination=${action.destination}; count=${action.count}")
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* The actual result is already visible and durable in chat. */ }
    }
    fun attachImage(uri: Uri) {
        val token = ++imageVersion
        _state.update { it.copy(imageLoading = true) }
        scope.launch {
            try { val value = withContext(Dispatchers.IO) { PilotImages.import(app, uri) }
                if (token == imageVersion) _state.update { it.copy(pendingImage = value) }
            } catch (error: Exception) { if (token == imageVersion) say(false, "Resim eklenemedi: ${error.message.orEmpty().take(160)}") }
            finally { if (token == imageVersion) _state.update { it.copy(imageLoading = false) } }
        }
    }
    fun clearImage() { imageVersion++; _state.update { it.copy(pendingImage = null, imageLoading = false) } }
    fun stopGeneration() {
        val job = _state.value.generation
        generationVersion++; generationJob?.cancel(); generationJob = null
        if (job?.active == true) {
            _state.update { it.copy(generation = job.copy(active = false)) }
            say(false, "Üretim durduruldu; ${job.saved}/${job.requested} metin ${job.pool.title} havuzunda korundu.")
        }
    }
    fun send(raw: String) {
        if (_state.value.imageLoading) return
        val text = raw.trim().take(4_000)
        val attachment = _state.value.pendingImage
        if (text.isBlank() && attachment == null) return
        clearImage()
        say(true, text.ifBlank { "Bu resmi incele." }, attachment?.path)
        val token = ++conversationVersion
        conversationJob?.cancel()
        if (attachment == null) GeminiApiErrors.localExplanation(text, lastApiFailure)?.let {
            _state.update { state -> state.copy(chatting = false) }
            say(false, it)
            return
        }
        val local = if (attachment == null) PilotIntentPolicy.local(text) else null
        if (local != null) {
            _state.update { it.copy(chatting = false) }
            scope.launch { executePlan(local, text) }
            // A report is real local data. The model may additionally explain the evidence.
            if (local.actions.none { it.tool == PilotTool.REPORT }) return
        }
        if (!AppServices.contentService.hasApiKey()) {
            if (local == null) say(false, "Ayarlar > Pilot API anahtarını ekle. Görev kontrolü ve havuz işlemleri cihazda çalışıyor; bu mesajı anlamak için sohbet bağlantısı gerekli.")
            return
        }
        _state.update { it.copy(chatting = true) }
        conversationJob = scope.launch {
            try {
                val settings = AppServices.settings.settings.first()
                val tasks = AppServices.repository.tasks.first()
                val logs = AppServices.repository.snapshotLogs(2_000)
                val runtime = AutomationController.state.value
                val q = AppServices.orchestrator.state.value
                val context = buildString {
                    appendLine("${runtime.status}/${runtime.activeScreen}/${runtime.flowStage}; ${runtime.username}; ${runtime.verifiedCount}/${runtime.limit}; ${runtime.message}")
                    appendLine("Kuyruk=${q.status}; ${q.message}")
                    appendLine("TWEET=${ContentPoolStore.lines(app, ContentPool.TWEET).size}; REPLY=${ContentPoolStore.lines(app, ContentPool.REPLY).size}")
                    appendLine("Son GERÇEK işlem: ${memory.getString("last_receipt", "yok")}")
                    appendLine("Üretim=${_state.value.generation}")
                    appendLine("Kayıtlı görevler: ${tasks.take(80).joinToString { "${it.username}:${it.type}:${it.progress}/${it.totalLimit}:${it.status}" }}")
                    appendLine(PilotLogReport.summarize(logs))
                }
                val turns = _state.value.lines.takeLast(24).map { (if (it.mine) "Kullanıcı" else "Pilot") to it.text }
                val bytes = attachment?.let { withContext(Dispatchers.IO) { PilotImages.bytes(app, it) } }
                val rawPlan = AppServices.contentService.pilotConversation(turns, context, settings.geminiModel, bytes)
                if (token != conversationVersion) return@launch
                lastApiFailure = null
                val plan = PilotIntentPolicy.parseModel(rawPlan) ?: error("Pilot yanıtı işlem şemasına uymadı; hiçbir işlem uygulanmadı")
                if (local?.actions?.any { it.tool == PilotTool.REPORT } == true) {
                    if (plan.say.isNotBlank()) say(false, PilotIntentPolicy.honestChat(plan.say))
                } else if (plan.actions.isNotEmpty()) {
                    // Execute outside the network job; a newer chat cannot cancel a committed edit's receipt.
                    scope.launch { executePlan(plan, text) }
                } else say(false, PilotIntentPolicy.honestChat(plan.say.ifBlank { "Ne yapmamı istediğini biraz daha açık yazabilir misin?" }))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (token == conversationVersion) {
                if (error is GeminiApiException && error.statusCode == 429) {
                    lastApiFailure = error.message
                    say(false, error.message.orEmpty())
                } else say(false, "Pilot yanıtı alınamadı: ${error.message.orEmpty().take(220)}")
            } }
            finally { if (token == conversationVersion) _state.update { it.copy(chatting = false) } }
        }
    }

    private suspend fun executePlan(plan: PilotToolPlan, raw: String) = execution.withLock {
        if (plan.actions.any { !PilotIntentPolicy.authorized(raw, it) }) {
            say(false, "Bu mesaj bu işlemler için açık talimat içermiyor; hiçbir işlem uygulanmadı."); return@withLock
        }
        for (action in plan.actions) {
            try {
                val result = execute(action)
                if (result.isNotBlank()) receipt(action, result)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                say(false, "İşlem uygulanamadı: ${error.message.orEmpty().take(240)}")
                AppServices.repository.log("WARN", "PILOT_ACTION", null, null, "${action.tool}: ${error.message.orEmpty().take(180)}")
                break
            }
        }
    }

    private suspend fun execute(action: PilotAction): String {
        val q = AppServices.orchestrator.state.value
        return when (action.tool) {
            PilotTool.GENERATE -> { generate(action); "" }
            PilotTool.TRANSFER, PilotTool.COPY, PilotTool.DELETE_LAST, PilotTool.UNDO_POOL -> {
                check(_state.value.generation?.active != true) { "Metin üretimi sürüyor. Önce üretimi durdur, sonra havuz işlemini ver." }
                val active = AutomationController.state.value
                check(!q.isActive || active.taskType !in setOf(TaskType.TEXT_TWEET, TaskType.IMAGE_TWEET, TaskType.COMMENT_QUOTE_TARGETS)) {
                    "Havuz metinlerini kullanan bir görev çalışıyor; önce görevi durdur."
                }
                val result = withContext(Dispatchers.IO) {
                    if (action.tool == PilotTool.UNDO_POOL) ContentPoolStore.undo(app)
                    else ContentPoolStore.edit(app, requireNotNull(action.pool), action.destination, requireNotNull(action.count), action.tool == PilotTool.COPY)
                }
                refreshCounts(); result
            }
            PilotTool.POOLS -> { refreshCounts(); "Tweet metinleri: ${_state.value.poolCount}. Hazır yorum metinleri: ${_state.value.replyCount}. Havuzlar ayrı tutuluyor." }
            PilotTool.REPORT -> PilotLogReport.summarize(AppServices.repository.snapshotLogs(2_000))
            PilotTool.STATUS -> AutomationController.state.value.let { "${it.username.orEmpty()} · ${it.status} · ${it.activeScreen}/${it.flowStage} · ${it.verifiedCount}/${it.limit}\n${q.message}" }
            PilotTool.STOP_GENERATION -> { check(_state.value.generation?.active == true) { "Etkin metin üretimi yok" }; stopGeneration(); "" }
            PilotTool.STOP -> {
                check(q.isActive) { "Durdurulacak etkin görev yok" }; AppServices.orchestrator.stop()
                withTimeoutOrNull(1_500L) { AppServices.orchestrator.state.first { !it.isActive } }
                "Durdurma isteği motora iletildi. Kuyruk: ${AppServices.orchestrator.state.value.status}."
            }
            PilotTool.PAUSE -> {
                check(q.isActive) { "Duraklatılacak etkin görev yok" }; AppServices.orchestrator.pause()
                withTimeoutOrNull(1_500L) { AppServices.orchestrator.state.first { it.status == QueueStatus.PAUSED } }
                "Duraklatma isteği motora iletildi. Kuyruk: ${AppServices.orchestrator.state.value.status}."
            }
            PilotTool.RESUME -> { check(q.status == QueueStatus.PAUSED) { "Devam ettirilecek duraklatılmış görev yok" }
                AppServices.orchestrator.resume(); "Devam isteği motora iletildi; aynı görev ve ilerleme korunuyor." }
            PilotTool.SKIP_TASK -> { check(q.isActive) { "Etkin görev yok" }; AppServices.orchestrator.skipCurrentTask(); "Sıradaki göreve geçiş isteği motora iletildi." }
            PilotTool.SKIP_ACCOUNT -> { check(q.isActive) { "Etkin görev yok" }; AppServices.orchestrator.skipCurrentAccount(); "Sıradaki hesaba geçiş isteği motora iletildi." }
            PilotTool.START -> {
                check(!q.isActive && !AccountSyncController.isActive) { "Çalışan görev veya hesap taraması var; ikinci kuyruk açılmadı" }
                require(action.types.isNotEmpty() || action.all) { "Hangi kayıtlı görevi başlatacağımı belirt" }
                val tasks = AppServices.repository.tasks.first().filter { action.account == null || it.username.removePrefix("@").equals(action.account, true) }
                val chosen = if (action.types.isEmpty()) tasks.filter { it.type != TaskType.SYNC }
                    else action.types.flatMap { type -> tasks.filter { it.type == type } }.distinctBy { it.id }
                require(chosen.isNotEmpty()) { "Bu seçime uygun kayıtlı görev bulunamadı" }
                AppServices.orchestrator.startSelection(chosen)
                withTimeoutOrNull(1_500L) { AppServices.orchestrator.state.first { it.sessionId != q.sessionId } }
                "${chosen.size} kayıtlı görev için başlatma isteği verildi. Sıra: ${chosen.map { it.type.title }.distinct().joinToString(" → ")}. Kuyruk: ${AppServices.orchestrator.state.value.status}."
            }
        }
    }

    private fun generate(action: PilotAction) {
        check(_state.value.generation?.active != true) { "Metin üretimi zaten sürüyor" }
        check(AppServices.contentService.hasApiKey()) { "Ayarlar > Pilot API anahtarını ekle" }
        val pool = requireNotNull(action.pool); val count = requireNotNull(action.count)
        require(count in 1..TweetGenerationRequest.MAX_COUNT) { "Tek istekte 1–1000 metin üretebilirsin" }
        require(action.concept.isNotBlank()) { "Metin konseptini belirt" }
        val existing = ContentPoolStore.lines(app, pool)
        require(existing.size + count <= TweetPoolPolicy.MAX_LINES) { "Havuzda yeterli yer yok" }
        val token = ++generationVersion
        val request = TweetGenerationRequest("${pool.title} için: ${action.concept}", count)
        _state.update { it.copy(generation = PilotGeneration(action.concept, count, pool = pool)) }
        say(false, "$count metin üretilip ${pool.title} havuzuna eklenecek. Kaydedilen gerçek adedi göstereceğim.")
        generationJob = scope.launch {
            try {
                val model = AppServices.settings.settings.first().geminiModel
                val engine = TweetBatchGeneration(produce = { topic, wanted, batch, avoid ->
                    var attempts = 0; var values: List<String>? = null
                    while (values == null) {
                        try { values = AppServices.contentService.generateTweetBatch(topic, wanted, batch, avoid, model) }
                        catch (e: GeminiApiException) {
                            val wait = GeminiApiErrors.generationRetryDelay(e, ++attempts) ?: throw e
                            say(false, "Gemini kısa süreli sınır veya yoğunluk bildirdi; en az ${(wait + 999) / 1000} saniye bekleyip yeniden deneyeceğim. Kaydedilen metinler korunuyor.")
                            delay(wait)
                        }
                    }; values
                }, save = { values ->
                    currentCoroutineContext().ensureActive()
                    if (token != generationVersion) throw CancellationException("Yeni üretim sürümü")
                    ContentPoolStore.append(app, pool, values).also { added ->
                        _state.update { s -> s.copy(generation = s.generation?.let { it.copy(saved = it.saved + added.size) }) }
                        refreshCounts()
                    }
                })
                val result = engine.generate(request, existing) { saved, _ ->
                    if (token == generationVersion) _state.update { it.copy(generation = it.generation?.copy(saved = saved)) }
                }
                if (token == generationVersion) {
                    lastApiFailure = null
                    receipt(action, "${result.saved}/${result.requested} farklı metin ${pool.title} havuzuna kaydedildi.")
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (token == generationVersion) {
                val saved = _state.value.generation?.saved ?: 0
                if (error is GeminiApiException && error.statusCode == 429) lastApiFailure = error.message
                val reason = if (error is GeminiApiException && error.statusCode == 429) error.message.orEmpty() else error.message.orEmpty().take(200)
                say(false, "Üretim tamamlanamadı; $saved/$count metin ${pool.title} havuzunda korundu. $reason")
            } } finally { if (token == generationVersion) {
                _state.update { it.copy(generation = it.generation?.copy(active = false)) }; generationJob = null; refreshCounts()
            } }
        }
    }
}
