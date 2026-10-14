package com.atmacanext.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.data.settings.AppSettings
import com.atmacanext.app.ui.theme.CardBackground
import com.atmacanext.app.ui.theme.TextPrimary
import com.atmacanext.app.ui.theme.TextSecondary

@Composable
fun PilotScreen(modifier: Modifier = Modifier) {
    val runtime by AutomationController.state.collectAsStateWithLifecycle()
    val chat by AppServices.pilotChat.state.collectAsStateWithLifecycle()
    val settings by AppServices.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    var draft by remember { mutableStateOf("") }
    val list = rememberLazyListState()
    val hasKey = AppServices.geminiKeyStore.hasKey()
    LaunchedEffect(chat.lines.size) { if (chat.lines.isNotEmpty()) list.animateScrollToItem(chat.lines.lastIndex) }
    Column(modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Pilot", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(if (!settings.pilotEnabled) "Hızlı görev denetimi kapalı · sohbet ve metin üretimi kullanılabilir"
            else if (hasKey) "Sohbet ve metin üretimi bağlı · görev ekranı her saniye denetlenir"
            else "API anahtarını Ayarlar > Pilot bölümünden ekle. Yerel görev denetimi hazır.", color = TextSecondary, fontSize = 12.sp)
        Text("${runtime.status} · ${runtime.activeScreen} · havuz ${chat.poolCount}", color = TextSecondary, fontSize = 12.sp)
        chat.generation?.let { job ->
            Text("${job.saved}/${job.requested} metin havuza kaydedildi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            if (job.active) {
                LinearProgressIndicator(progress = { job.saved.toFloat() / job.requested.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { AppServices.pilotChat.stopGeneration() }) { Text("Üretimi durdur") }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chat.lines) { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.mine) Arrangement.End else Arrangement.Start) {
                    Surface(color = CardBackground, shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 420.dp)) {
                        Text(line.text, Modifier.padding(12.dp), color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }
        }
        if (chat.chatting) Text("Pilot yanıtlıyor…", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = draft, onValueChange = { draft = it.take(2_000) }, modifier = Modifier.weight(1f),
                placeholder = { Text("Örnek: futbol konseptinde 500 twit metni ekle") }, maxLines = 4)
            IconButton(enabled = draft.isNotBlank(), onClick = {
                val text = draft.trim(); draft = ""; AppServices.pilotChat.send(text)
            }) { Icon(Icons.Filled.Send, "Gönder") }
        }
    }
}
