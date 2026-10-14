package com.atmacanext.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.ai.PilotBoard
import com.atmacanext.app.ai.PilotChatStore
import com.atmacanext.app.ai.PilotIncident
import com.atmacanext.app.ai.PilotMemory
import com.atmacanext.app.ai.PilotPolicy
import com.atmacanext.app.ai.PilotPlan
import com.atmacanext.app.automation.AccountSyncController
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.data.settings.TweetTextPool
import com.atmacanext.app.ui.theme.CardBackground
import com.atmacanext.app.ui.theme.TextPrimary
import com.atmacanext.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private data class PilotLine(val mine: Boolean, val text: String)

@Composable
fun PilotScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val runtime by AutomationController.state.collectAsStateWithLifecycle()
    val tasks by AppServices.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val settings by AppServices.settings.settings.collectAsStateWithLifecycle(initialValue = com.atmacanext.app.data.settings.AppSettings())
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var hasKey by remember { mutableStateOf(AppServices.geminiKeyStore.hasKey()) }
    val lines = remember {
        mutableStateListOf<PilotLine>().apply {
            val saved = PilotChatStore.load(context)
            if (saved.isEmpty()) {
                val hello = PilotLine(false, "Pilot hazır. Görev yönetirim, takılınca müdahale ederim. Sohbet burada kalır.")
                add(hello)
                PilotChatStore.save(context, listOf(PilotChatStore.Line(false, hello.text)))
            } else saved.forEach { add(PilotLine(it.mine, it.text)) }
        }
    }
    fun rememberLine(line: PilotLine) {
        lines += line
        PilotChatStore.save(context, lines.map { PilotChatStore.Line(it.mine, it.text) })
    }
    var draft by remember { mutableStateOf("") }
    val list = rememberLazyListState()
    LaunchedEffect(Unit) { hasKey = AppServices.geminiKeyStore.hasKey() }
    LaunchedEffect(lines.size) { if (lines.isNotEmpty()) list.animateScrollToItem(lines.lastIndex) }
    Column(modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Pilot", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        if (!hasKey) {
            Text("Gemini anahtarı yok. Ayarlar'dan ekle. Sohbet bu telefonda durur.", color = TextSecondary, fontSize = 12.sp)
        }
        Text(
            "${runtime.status} · ${runtime.activeScreen} · havuz ${TweetTextPool.count(context)}",
            color = TextSecondary,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(lines) { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.mine) Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = CardBackground,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(max = 280.dp),
                    ) {
                        Text(line.text, Modifier.padding(12.dp), color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("onaylı takibi başlat") },
                singleLine = true,
            )
            IconButton(onClick = {
                val text = draft.trim()
                if (text.isBlank() || busy) return@IconButton
                draft = ""
                busy = true
                rememberLine(PilotLine(true, text))
                val history = lines.map { (if (it.mine) "Kullanıcı" else "Pilot") to it.text }
                val memory = PilotMemory.summary(context)
                val hotspots = PilotIncident.byLocation(context).take(3).joinToString("; ") { "${it.first}=${it.second}" }
                val situation = "${runtime.status} ${runtime.activeScreen} ${runtime.flowStage} @${runtime.username.orEmpty()} ${runtime.message} hafıza=$memory sık=$hotspots"
                scope.launch {
                    try {
                    val remote = if (hasKey && !com.atmacanext.app.ai.PilotQuota.blocked()) {
                        runCatching { AppServices.contentService.pilotChat(history, situation, settings.geminiModel) }.getOrNull()
                    } else null
                    val plan = remote?.say?.let { PilotPolicy.parseModel(it) }
                        ?: remote?.say?.trim()?.takeIf { it.isNotBlank() && !it.startsWith("{") }?.let { PilotPlan(it.take(400)) }
                        ?: PilotPolicy.plan(
                        text,
                        runtime.status.name,
                        runtime.activeScreen.name,
                        runtime.flowStage.name,
                        runtime.username,
                        runtime.message,
                        TweetTextPool.count(context),
                        PilotBoard.lastMove,
                        memory,
                    )
                    val say = when (plan.command) {
                        PilotPolicy.Command.STOP -> {
                            AppServices.orchestrator.stop()
                            plan.say
                        }
                        PilotPolicy.Command.PAUSE -> {
                            AppServices.orchestrator.pause()
                            plan.say
                        }
                        PilotPolicy.Command.RESUME -> {
                            AppServices.orchestrator.resume()
                            plan.say
                        }
                        PilotPolicy.Command.SKIP_TASK -> {
                            AppServices.orchestrator.skipCurrentTask()
                            plan.say
                        }
                        PilotPolicy.Command.SKIP_ACCOUNT -> {
                            AppServices.orchestrator.skipCurrentAccount()
                            plan.say
                        }
                        PilotPolicy.Command.TWEETS -> {
                            TweetTextPool.append(context, plan.tweets.joinToString("\n"))
                            plan.say
                        }
                        PilotPolicy.Command.START -> when {
                            queue.isActive || AccountSyncController.isActive -> "Şu an iş var. Bitmeden ikinci görev açmam. Durdur, sonra söyle."
                            else -> {
                                val picked = PilotPolicy.select(tasks, plan.taskType, plan.account, plan.all)
                                if (picked.isEmpty()) "Bu talimata kayıtlı görev yok. Önce görev ekranından kaydet."
                                else {
                                    AppServices.orchestrator.startSelection(picked)
                                    "${picked.size} görev ben aldım: ${picked.joinToString { "@${it.username} ${it.type.title}" }}"
                                }
                            }
                        }
                        PilotPolicy.Command.REPORT -> {
                            PilotPolicy.formatHotspots(
                                PilotIncident.byLocation(context),
                                PilotIncident.longestStreak(context),
                            )
                        }
                        PilotPolicy.Command.CLEAR_MEMORY -> {
                            PilotMemory.clear(context)
                            PilotIncident.clear(context)
                            plan.say
                        }
                        PilotPolicy.Command.STATUS -> plan.say
                        PilotPolicy.Command.NONE -> plan.say
                    }
                    if (plan.command != PilotPolicy.Command.NONE && plan.command != PilotPolicy.Command.STATUS) {
                        PilotMemory.remember(context, "${plan.command.name} ${plan.taskType?.name.orEmpty()} @${plan.account.orEmpty()}".trim())
                    }
                    val spoken = if (remote?.say == null && !remote?.failure.isNullOrBlank() && plan.command == PilotPolicy.Command.NONE) {
                        "${remote?.failure}\n${plan.say}"
                    } else say
                    rememberLine(PilotLine(false, spoken))
                    } finally {
                        busy = false
                    }
                }
            }) {
                Icon(Icons.Filled.Send, "Gönder")
            }
        }
    }
}
