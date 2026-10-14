package com.atmacanext.app.ai

import android.content.Context
import com.atmacanext.app.automation.AccountSyncController
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.data.settings.TweetTextPool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PilotChatLine(val mine: Boolean, val text: String)
data class PilotGeneration(val concept: String, val requested: Int, val saved: Int = 0, val active: Boolean = true)
data class PilotChatState(val lines: List<PilotChatLine>, val chatting: Boolean = false,
                          val generation: PilotGeneration? = null, val poolCount: Int = 0)

/** Conversation and generation outlive a tab change; only explicit cancellation stops production. */
class PilotChatCoordinator(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PilotChatState(
        PilotChatStore.load(app).map { PilotChatLine(it.mine, it.text) }.ifEmpty {
            listOf(PilotChatLine(false, "Pilot hazır. Sohbet edebilir, görevleri yönetebilir ve istediğin konseptte tweet metinlerini havuza ekleyebilirim."))
        }, poolCount = TweetTextPool.count(app)))
    val state = _state.asStateFlow()
    private var generationJob: Job? = null

    private fun say(mine: Boolean, text: String) {
        _state.update { it.copy(lines = (it.lines + PilotChatLine(mine, text)).takeLast(200), poolCount = TweetTextPool.count(app)) }
        PilotChatStore.save(app, _state.value.lines.map { PilotChatStore.Line(it.mine, it.text) })
    }

    fun stopGeneration() { generationJob?.cancel() }

    fun send(raw: String) {
        val text = raw.trim().take(2_000)
        if (text.isBlank()) return
        say(true, text)
        if ((text.contains("üretim", true) || text.contains("uretim", true)) && text.contains("durdur", true)) {
            if (generationJob?.isActive == true) stopGeneration() else say(false, "Etkin metin üretimi yok.")
            return
        }
        val request = TweetGenerationRequest.parse(text)
        if (request != null) { generate(request); return }
        val runtime = AutomationController.state.value
        val local = PilotPolicy.plan(text, runtime.status.name, runtime.activeScreen.name, runtime.flowStage.name,
            runtime.username, runtime.message, TweetTextPool.count(app), PilotBoard.lastMove, PilotMemory.summary(app))
        // Stop/pause/continue are not queued behind a network response or 500-text job.
        if (local.command in setOf(PilotPolicy.Command.STOP, PilotPolicy.Command.PAUSE, PilotPolicy.Command.RESUME,
                PilotPolicy.Command.SKIP_TASK, PilotPolicy.Command.SKIP_ACCOUNT, PilotPolicy.Command.STATUS,
                PilotPolicy.Command.REPORT, PilotPolicy.Command.CLEAR_MEMORY, PilotPolicy.Command.START)) {
            scope.launch { execute(local) }
            return
        }
        if (_state.value.chatting) { say(false, "Önceki sohbet yanıtı geliyor; görev komutlarını hemen işleyebilirim."); return }
        _state.update { it.copy(chatting = true) }
        scope.launch {
            try {
                val settings = AppServices.settings.settings.first()
                val turns = _state.value.lines.takeLast(12).map { (if (it.mine) "Kullanıcı" else "Pilot") to it.text }
                val situation = "${runtime.status}/${runtime.activeScreen}/${runtime.flowStage}; ${runtime.verifiedCount}/${runtime.limit}; ${runtime.message}"
                val result = AppServices.contentService.pilotChat(turns, situation, settings.geminiModel)
                val reply = result.say?.let(PilotPolicy::parseModel)
                // General chat never executes a model-invented task command.
                say(false, reply?.say ?: result.failure ?: local.say)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { say(false, "Sohbet yanıtı alınamadı: ${error.message.orEmpty().take(160)}") }
            finally { _state.update { it.copy(chatting = false) } }
        }
    }

    private suspend fun execute(plan: PilotPlan) {
        val response = when (plan.command) {
            PilotPolicy.Command.STOP -> { AppServices.orchestrator.stop(); "Görev durduruldu." }
            PilotPolicy.Command.PAUSE -> { AppServices.orchestrator.pause(); "Görev duraklatıldı." }
            PilotPolicy.Command.RESUME -> { AppServices.orchestrator.resume(); "Kaldığı ilerlemeden devam komutu verildi." }
            PilotPolicy.Command.SKIP_TASK -> { AppServices.orchestrator.skipCurrentTask(); plan.say }
            PilotPolicy.Command.SKIP_ACCOUNT -> { AppServices.orchestrator.skipCurrentAccount(); plan.say }
            PilotPolicy.Command.START -> {
                if (AppServices.orchestrator.state.value.isActive || AccountSyncController.isActive) "Çalışan görev var; ikinci kuyruk açılmadı."
                else {
                    val picked = PilotPolicy.select(AppServices.repository.tasks.first(), plan.taskType, plan.account, plan.all)
                    if (picked.isEmpty()) "Bu talimata uygun kayıtlı görev yok. Önce Görevler'den kaydet."
                    else { AppServices.orchestrator.startSelection(picked); "${picked.size} kayıtlı görev başlatıldı." }
                }
            }
            PilotPolicy.Command.REPORT -> PilotPolicy.formatHotspots(PilotIncident.byLocation(app), PilotIncident.longestStreak(app))
            PilotPolicy.Command.CLEAR_MEMORY -> { PilotMemory.clear(app); PilotIncident.clear(app); plan.say }
            else -> plan.say
        }
        say(false, response)
    }

    private fun generate(request: TweetGenerationRequest) {
        if (generationJob?.isActive == true) { say(false, "Metin üretimi sürüyor; ilerlemeyi üstte görebilirsin."); return }
        if (!AppServices.contentService.hasApiKey()) { say(false, "Metin üretmek için Ayarlar > Pilot API anahtarını ekle ve bağlantıyı test et."); return }
        if (request.count !in 1..TweetGenerationRequest.MAX_COUNT) { say(false, "Tek istekte 1–1000 metin ekleyebilirsin."); return }
        val existing = TweetTextPool.lines(app)
        if (existing.size + request.count > TweetPoolPolicy.MAX_LINES) { say(false, "Havuzda ${request.count} yeni metin için yeterli yer yok."); return }
        _state.update { it.copy(generation = PilotGeneration(request.concept, request.count)) }
        say(false, "${request.count} metin üretip havuza ekliyorum; ilerlemeyi burada göstereceğim.")
        generationJob = scope.launch {
            try {
                val model = AppServices.settings.settings.first().geminiModel
                val generator = TweetBatchGeneration(
                    produce = { topic, count, batch, avoid ->
                        var attempts = 0
                        var drafts: List<String>? = null
                        while (drafts == null) {
                            try { drafts = AppServices.contentService.generateTweetBatch(topic, count, batch, avoid, model) }
                            catch (error: GeminiApiException) {
                                if (error.statusCode !in setOf(429, 503) || ++attempts >= 3) throw error
                                delay(error.retryAfterMs.coerceIn(5_000L, 60_000L))
                            }
                        }
                        drafts
                    },
                    save = { values ->
                        TweetTextPool.appendBatch(app, values).also { added ->
                            _state.update { state -> state.copy(generation = state.generation?.let { it.copy(saved = it.saved + added.size) },
                                poolCount = TweetTextPool.count(app)) }
                        }
                    },
                )
                val result = generator.generate(request, existing) { saved, _ ->
                    _state.update { it.copy(generation = it.generation?.copy(saved = saved), poolCount = TweetTextPool.count(app)) }
                }
                say(false, "${result.saved}/${result.requested} farklı metin havuza kaydedildi. Havuzda toplam ${TweetTextPool.count(app)} metin var.")
            } catch (cancelled: CancellationException) {
                val saved = _state.value.generation?.saved ?: 0
                say(false, "Üretim durduruldu; $saved/${request.count} metin havuzda korundu.")
            } catch (error: Exception) {
                val saved = _state.value.generation?.saved ?: 0
                say(false, "Üretim tamamlanamadı; $saved/${request.count} metin kaydedildi ve korundu. ${error.message.orEmpty().take(180)}")
            } finally {
                _state.update { it.copy(generation = it.generation?.copy(active = false), poolCount = TweetTextPool.count(app)) }
                generationJob = null
            }
        }
    }
}
