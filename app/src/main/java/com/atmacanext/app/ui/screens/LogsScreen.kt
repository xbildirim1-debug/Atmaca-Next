package com.atmacanext.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.data.local.AutomationLogEntity
import com.atmacanext.app.ui.components.AtmacaCard
import com.atmacanext.app.ui.theme.AtmacaBlue
import com.atmacanext.app.ui.theme.ErrorRed
import com.atmacanext.app.ui.theme.TextSecondary
import com.atmacanext.app.ui.theme.Warning
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LogsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs by AppServices.repository.recentLogs.collectAsStateWithLifecycle(initialValue = emptyList())
    var level by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val shown = logs.filter { level == null || it.level == level }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 1_200.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Detaylı Loglar", style = MaterialTheme.typography.headlineSmall)
                        Text("En yeni ${logs.size} kayıt • metin seçilebilir", color = TextSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = { copyLogs(context, shown) }, enabled = shown.isNotEmpty()) { Icon(Icons.Filled.ContentCopy, "Tümünü kopyala") }
                    IconButton(onClick = { confirmClear = true }, enabled = logs.isNotEmpty()) { Icon(Icons.Filled.DeleteSweep, "Logları temizle") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "Tümü", "INFO" to "Bilgi", "WARN" to "Uyarı", "ERROR" to "Hata").forEach { (value, label) ->
                        AssistChip(
                            onClick = { level = value },
                            label = { Text(label) },
                            colors = if (level == value) AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else AssistChipDefaults.assistChipColors(),
                        )
                    }
                }
            }
            if (shown.isEmpty()) {
                item { AtmacaCard { Text("Bu filtrede log kaydı yok.", color = TextSecondary) } }
            } else {
                items(shown, key = { it.id }) { log -> LogDetailCard(log, onCopy = { copyText(context, log.asText()) }) }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Tüm loglar temizlensin mi?") },
            text = { Text("Bu işlem geri alınamaz. Hesaplar ve görevler etkilenmez.") },
            confirmButton = {
                TextButton(onClick = { scope.launch { AppServices.repository.clearLogs() }; confirmClear = false }) { Text("Temizle") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun LogDetailCard(log: AutomationLogEntity, onCopy: () -> Unit) {
    AtmacaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (log.level) { "ERROR" -> Icons.Filled.Error; "WARN" -> Icons.Filled.Warning; else -> Icons.Filled.Info },
                null,
                tint = when (log.level) { "ERROR" -> ErrorRed; "WARN" -> Warning; else -> AtmacaBlue },
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("${log.level} • ${log.category}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(formatLogTime(log.timestamp), color = TextSecondary, fontSize = 9.sp)
            }
            log.username?.let { Text(it, color = TextSecondary, fontSize = 10.sp) }
            IconButton(onClick = onCopy) { Icon(Icons.Filled.ContentCopy, "Bu logu kopyala") }
        }
        SelectionContainer {
            Column {
                Text(log.message, fontSize = 12.sp)
                log.details?.takeIf(String::isNotBlank)?.let {
                    Spacer(Modifier.padding(top = 4.dp))
                    Text(it, color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                log.taskId?.let { Text("taskId=$it", color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 9.sp) }
            }
        }
    }
}

private fun copyLogs(context: Context, logs: List<AutomationLogEntity>) = copyText(context, logs.asReversed().joinToString("\n") { it.asText() })

private fun copyText(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Atmaca Next log", value))
    Toast.makeText(context, "Log panoya kopyalandı", Toast.LENGTH_SHORT).show()
}

private fun AutomationLogEntity.asText(): String = buildString {
    append("${formatLogTime(timestamp)} [$level] [$category]")
    username?.let { append(" [$it]") }
    append(" $message")
    details?.takeIf(String::isNotBlank)?.let { append(" | $it") }
    taskId?.let { append(" | taskId=$it") }
}

private fun formatLogTime(value: Long): String = runCatching {
    Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss.SSS", Locale("tr", "TR")))
}.getOrDefault(value.toString())
