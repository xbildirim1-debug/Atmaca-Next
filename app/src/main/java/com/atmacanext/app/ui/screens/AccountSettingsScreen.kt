package com.atmacanext.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.automation.AccessibilityServiceState
import com.atmacanext.app.automation.AccountSyncController
import com.atmacanext.app.ui.theme.*
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.data.settings.AppSettings
import kotlinx.coroutines.launch

@Composable fun AccountSettingsScreen(modifier: Modifier = Modifier) {
    val health by AccessibilityServiceState.health.collectAsStateWithLifecycle()
    val sync by AccountSyncController.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var logs by remember { mutableStateOf(false) }
    if (logs) {
        Column(modifier) {
            TextButton(onClick = { logs = false }) { Icon(Icons.Default.ArrowBack, null); Text(" Ayarlara dön") }
            LogsScreen(Modifier.weight(1f))
        }
        return
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Text("Ayarlar", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Kontrol her zaman sende.", color = TextSecondary)
        Card {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Default.AccessibilityNew, null, tint = AtmacaBlue)
                Text("Ekran okuma", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                StatusLabel(if (health.connected) "Bağlı ve hazır" else if (health.enabled) "İzin açık, bağlantı bekleniyor" else "İzin gerekli", health.connected)
                Text("Başlattığın işlem sırasında X ekranındaki hesap adları ve sayaçlar okunur, hesap değiştirmek için ekrana dokunulur. Şifre veya oturum anahtarı istenmez.", color = TextSecondary)
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, enabled = !sync.active) {
                    Text("Erişilebilirlik ayarlarını aç")
                }
                Text("Listeden Atmaca Next Otomasyon Servisi'ni seçip etkinleştir. Android “Kısıtlanmış ayar” gösterirse uygulamanın sistem bilgi ekranındaki menüyü kontrol et.", fontSize = 12.sp, color = TextSecondary)
            }
        }
        Card {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.PhonelinkLock, null, tint = Teal)
                Text("Bu telefonda kalır", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Bu sürümün internet izni yoktur. Hesap bilgileri cihazda saklanır. Taramayı dilediğin an Durdur düğmesiyle sonlandırabilirsin.", color = TextSecondary)
                Text("İşlem bittiğinde Atmaca Next öne gelir. Android, X'in zorla kapatılmasına izin vermeyebilir; oturumların açık kalır.", color = TextSecondary)
            }
        }
        AutomationPreferences(enabled = !sync.active)
        TaskTargetsPreferences(enabled = !sync.active)
        OutlinedButton(onClick = { logs = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.History, null); Spacer(Modifier.width(8.dp)); Text("İşlem kayıtları")
        }
        Text("Atmaca Next · 26.4\nHesaplar, görevler ve işlem kayıtları", color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun AutomationPreferences(enabled: Boolean) {
    val settings by AppServices.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var actionMs by remember(settings.betweenActionsMs) { mutableStateOf(settings.betweenActionsMs.toString()) }
    var switchMs by remember(settings.accountSwitchSettleMs) { mutableStateOf(settings.accountSwitchSettleMs.toString()) }
    var taskMs by remember(settings.betweenTasksMs) { mutableStateOf(settings.betweenTasksMs.toString()) }
    var days by remember(settings.keepLogDays) { mutableStateOf(settings.keepLogDays.toString()) }
    var continueOnError by remember(settings.continueAfterFailedTask) { mutableStateOf(settings.continueAfterFailedTask) }
    var saving by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    val editable = enabled && !queue.isActive && !saving
    val valid = (actionMs.toLongOrNull() ?: -1L) in 800L..15_000L && (switchMs.toLongOrNull() ?: -1L) in 1_500L..15_000L &&
        (taskMs.toLongOrNull() ?: -1L) in 1_000L..60_000L && (days.toIntOrNull() ?: -1) in 1..365
    Card {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Çalışma ayarları", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Yavaş yüklenen X ekranlarında hesap geçişi beklemesini artırabilirsin. 1000 ms = 1 saniye.", color = TextSecondary)
            PreferenceNumber("İşlemler arası (800–15000 ms)", actionMs, editable) { actionMs = it }
            PreferenceNumber("Hesap geçişi (1500–15000 ms)", switchMs, editable) { switchMs = it }
            PreferenceNumber("Görevler arası (1000–60000 ms)", taskMs, editable) { taskMs = it }
            PreferenceNumber("Kayıt saklama süresi (1–365 gün)", days, editable) { days = it }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Hatalı görevden sonra sıradakine geç", Modifier.weight(1f))
                Switch(checked = continueOnError, onCheckedChange = { continueOnError = it }, enabled = editable)
            }
            Button(enabled = editable && valid, onClick = {
                saving = true
                scope.launch {
                    try {
                        AppServices.settings.setAutomationTiming(actionMs.toLong(), switchMs.toLong(), settings.rateLimitCooldownMinutes, taskMs.toLong())
                        AppServices.settings.setKeepLogDays(days.toInt())
                        AppServices.settings.setContinueAfterFailedTask(continueOnError)
                        feedback = "Ayarlar kaydedildi. Kayıt saklama süresi sonraki açılışta uygulanır."
                    } catch (_: Exception) { feedback = "Ayarlar kaydedilemedi. Yeniden dene." }
                    finally { saving = false }
                }
            }) { Text(if (saving) "Kaydediliyor…" else "Ayarları kaydet") }
            if (!valid) Text("Değerleri alanlarda belirtilen aralıklarda gir.", color = MaterialTheme.colorScheme.error)
            if (!editable && !saving) Text("Ayarları değiştirmek için çalışan işlemi durdur.", color = TextSecondary)
            if (feedback.isNotEmpty()) Text(feedback, color = TextSecondary)
        }
    }
}

@Composable
private fun PreferenceNumber(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) onChange(it) },
        label = { Text(label) }, enabled = enabled, singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
}

@Composable
private fun TaskTargetsPreferences(enabled: Boolean) {
    val accounts by AppServices.repository.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val targets by AppServices.repository.targetAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var ownerId by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var handle by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    val owner = accounts.firstOrNull { it.id == ownerId } ?: accounts.firstOrNull()
    val normalized = handle.trim().removePrefix("@").lowercase(java.util.Locale.ROOT)
    val editable = enabled && !queue.isActive
    Card {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Görev hedefleri", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Yorumcuları ve yeniden paylaşanları tarayan görevler için hedef profilleri ekle. Liste her hesabın kendisine aittir.", color = TextSecondary)
            Box {
                OutlinedButton(enabled = editable && accounts.isNotEmpty(), onClick = { menu = true }) {
                    Text(owner?.username ?: "Önce hesap ekle")
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    accounts.forEach { account ->
                        DropdownMenuItem(text = { Text(account.username) }, onClick = { ownerId = account.id; menu = false })
                    }
                }
            }
            OutlinedTextField(value = handle, onValueChange = { handle = it.take(16) }, label = { Text("Hedef @kullanıcı_adı") }, singleLine = true, enabled = editable, modifier = Modifier.fillMaxWidth())
            Button(enabled = editable && owner != null && normalized.matches(Regex("[a-z0-9_]{1,15}")), onClick = {
                val accountId = owner?.id ?: return@Button
                val targetHandle = normalized
                scope.launch {
                    try {
                        AppServices.repository.upsertTarget(com.atmacanext.app.domain.model.TargetAccount(
                            id = java.util.UUID.nameUUIDFromBytes("$accountId:$targetHandle".toByteArray()).toString(),
                            ownerAccountId = accountId, handle = targetHandle))
                        handle = ""; feedback = "Hedef eklendi"
                    } catch (_: Exception) { feedback = "Hedef kaydedilemedi. Her hesaba en fazla 1 hedef eklenebilir." }
                }
            }) { Text("Hedef ekle") }
            targets.filter { it.ownerAccountId == owner?.id }.forEach { target ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("@${target.handle}", Modifier.weight(1f))
                    IconButton(enabled = editable, onClick = {
                        scope.launch {
                            try { AppServices.repository.deleteTarget(target.id) }
                            catch (_: Exception) { feedback = "Hedef silinemedi" }
                        }
                    }) { Icon(Icons.Default.Delete, "Hedefi sil") }
                }
            }
            if (feedback.isNotEmpty()) Text(feedback, color = TextSecondary)
        }
    }
}
