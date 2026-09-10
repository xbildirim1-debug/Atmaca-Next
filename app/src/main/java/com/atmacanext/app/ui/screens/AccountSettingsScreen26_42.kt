package com.atmacanext.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.automation.AccessibilityServiceState
import com.atmacanext.app.automation.AccountSyncController
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.data.settings.AppSettings
import com.atmacanext.app.ui.theme.AtmacaBlue
import com.atmacanext.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AccountSettingsScreen26_42(modifier: Modifier = Modifier) {
    val health by AccessibilityServiceState.health.collectAsStateWithLifecycle()
    val sync by AccountSyncController.state.collectAsStateWithLifecycle()
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val settings by AppServices.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var logs by remember { mutableStateOf(false) }
    var showWorkSettings by remember { mutableStateOf(false) }

    if (logs) {
        Column(modifier) {
            TextButton(onClick = { logs = false }) {
                Icon(Icons.Default.ArrowBack, null)
                Text(" Ayarlara dön")
            }
            LogsScreen(Modifier.weight(1f))
        }
        return
    }

    if (showWorkSettings) {
        WorkSettingsDialog26_42(
            settings = settings,
            enabled = !sync.active && !queue.isActive,
            onDismiss = { showWorkSettings = false },
            onSave = { actionMs, switchMs, taskMs, days, cooldown, continueOnError ->
                scope.launch {
                    AppServices.settings.setAutomationTiming(actionMs, switchMs, cooldown, taskMs)
                    AppServices.settings.setKeepLogDays(days)
                    AppServices.settings.setContinueAfterFailedTask(continueOnError)
                    AppServices.repository.pruneLogs(days)
                }
                showWorkSettings = false
            },
        )
    }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Ayarlar", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Otomasyon davranışı, kayıtlar ve sistem bağlantısı", color = TextSecondary)

        Card {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SettingsSuggest, null, tint = AtmacaBlue)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Çalışma ayarları", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Hız, hesap geçişi, görev aralığı ve hata davranışı", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsMiniMetric26_42("İşlem", "${settings.betweenActionsMs} ms", Modifier.weight(1f))
                    SettingsMiniMetric26_42("Hesap", "${settings.accountSwitchSettleMs} ms", Modifier.weight(1f))
                    SettingsMiniMetric26_42("Görev", "${settings.betweenTasksMs} ms", Modifier.weight(1f))
                }
                Text("20 sn ilerleme/kuyruk geçişi kurtarması etkin", color = TextSecondary, fontSize = 10.sp)
                Button(
                    onClick = { showWorkSettings = true },
                    enabled = !sync.active && !queue.isActive,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Çalışma Ayarlarını Düzenle") }
                if (queue.isActive) Text("Düzenlemek için çalışan görevi durdur.", color = TextSecondary, fontSize = 10.sp)
            }
        }

        Card {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kayıt ve tanılama", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("İşlem kayıtları ${settings.keepLogDays} gün saklanıyor.", color = TextSecondary, fontSize = 11.sp)
                OutlinedButton(onClick = { logs = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.History, null)
                    Spacer(Modifier.width(8.dp))
                    Text("İşlem Kayıtlarını Aç")
                }
            }
        }

        Text("Atmaca Next · 26.42", color = TextSecondary, fontSize = 12.sp)

        // Ekran okuma bölümü küçük ve en altta.
        Card {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.AccessibilityNew, null, tint = AtmacaBlue)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ekran okuma", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    StatusLabel(
                        if (health.connected) "Bağlı ve hazır" else if (health.enabled) "İzin açık · bağlantı bekleniyor" else "İzin kapalı",
                        health.connected,
                    )
                }
                TextButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    enabled = !sync.active,
                ) { Text("Ayar") }
            }
        }
    }
}

@Composable
private fun SettingsMiniMetric26_42(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label.uppercase(), color = TextSecondary, fontSize = 8.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun WorkSettingsDialog26_42(
    settings: AppSettings,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (Long, Long, Long, Int, Int, Boolean) -> Unit,
) {
    var actionMs by remember(settings.betweenActionsMs) { mutableStateOf(settings.betweenActionsMs.toString()) }
    var switchMs by remember(settings.accountSwitchSettleMs) { mutableStateOf(settings.accountSwitchSettleMs.toString()) }
    var taskMs by remember(settings.betweenTasksMs) { mutableStateOf(settings.betweenTasksMs.toString()) }
    var days by remember(settings.keepLogDays) { mutableStateOf(settings.keepLogDays.toString()) }
    var cooldown by remember(settings.rateLimitCooldownMinutes) { mutableStateOf(settings.rateLimitCooldownMinutes.toString()) }
    var continueOnError by remember(settings.continueAfterFailedTask) { mutableStateOf(settings.continueAfterFailedTask) }

    val action = actionMs.toLongOrNull()
    val accountSwitch = switchMs.toLongOrNull()
    val taskGap = taskMs.toLongOrNull()
    val keepDays = days.toIntOrNull()
    val cooldownMinutes = cooldown.toIntOrNull()
    val valid = action in 100L..15_000L &&
        accountSwitch in 500L..15_000L &&
        taskGap in 250L..60_000L &&
        keepDays in 1..365 &&
        cooldownMinutes in 5..180

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 620.dp),
        title = { Text("Çalışma Ayarları") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(500.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                item { PreferenceNumber26_42("Tüm işlemlerin hızı (100–15000 ms)", actionMs, enabled) { actionMs = it } }
                item { PreferenceNumber26_42("Hesap geçişi (500–15000 ms)", switchMs, enabled) { switchMs = it } }
                item { PreferenceNumber26_42("Görevler arası (250–60000 ms)", taskMs, enabled) { taskMs = it } }
                item { PreferenceNumber26_42("Rate-limit bekleme (5–180 dk)", cooldown, enabled) { cooldown = it } }
                item { PreferenceNumber26_42("Kayıt saklama (1–365 gün)", days, enabled) { days = it } }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Hatalı görevden sonra devam et", fontWeight = FontWeight.SemiBold)
                            Text("Açıkken sıradaki hesap/göreve geçilir.", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(checked = continueOnError, onCheckedChange = { continueOnError = it }, enabled = enabled)
                    }
                }
                item {
                    Text(
                        "Görev hedefleri burada tutulmaz; hedef hesapları Hesaplar bölümündeki ‘Hedef hesabı düzenle’ alanından yönet.",
                        color = TextSecondary,
                        fontSize = 10.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = enabled && valid,
                onClick = { onSave(action!!, accountSwitch!!, taskGap!!, keepDays!!, cooldownMinutes!!, continueOnError) },
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun PreferenceNumber26_42(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) onChange(it) },
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
