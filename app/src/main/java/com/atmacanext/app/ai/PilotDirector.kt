package com.atmacanext.app.ai

import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** One-second local checks remain independent of an asynchronous API response. */
class PilotDirector(private val content: GeminiContentService, private val settings: Flow<AppSettings>) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var aiJob: Job? = null
    @Volatile private var scene = ""
    private var unchangedAt = 0L
    private var lastMoveAt = 0L
    private var lastAiAt = 0L
    @Volatile private var generation = 0L

    fun reset() {
        generation++
        aiJob?.cancel(); aiJob = null
        scene = ""; unchangedAt = 0L; lastMoveAt = 0L; lastAiAt = 0L
    }

    fun observe(observation: PilotScreenObservation?, enabled: Boolean, now: Long = System.currentTimeMillis()) {
        if (!enabled || observation == null) { reset(); return }
        val key = observation.signature + "|" + observation.fingerprint
        if (scene != key) {
            scene = key; unchangedAt = now; lastMoveAt = 0L
            generation++; aiJob?.cancel(); aiJob = null
        }
        val allowed = FastPilotPolicy.allowed(observation, now)
        if (allowed.isEmpty()) return
        val age = now - unchangedAt
        val local = FastPilotPolicy.localMove(observation, age, now)
        if (local != PilotMove.NONE && now - lastMoveAt >= FastPilotPolicy.PULSE_MS) {
            lastMoveAt = now
            AutomationController.applyPilotMove(observation, local, "Cihazdaki Pilot")
        }
        // Ask on an incident, not on every heartbeat. A normal moving list never waits for the server.
        if (age < 3_000L || !content.hasApiKey() || aiJob?.isActive == true || now - lastAiAt < 15_000L) return
        lastAiAt = now
        val token = generation
        aiJob = scope.launch {
            val result = content.pilotAdvice(observation, allowed, settings.first().geminiModel)
            if (generation == token && scene == key && result != null) {
                AutomationController.applyPilotMove(observation, result, "Yapay zekâ Pilot")
            }
        }
    }
}
