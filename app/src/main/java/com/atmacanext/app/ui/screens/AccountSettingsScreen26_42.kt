package com.atmacanext.app.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.atmacanext.app.data.settings.AccountPulse
import com.atmacanext.app.data.settings.ActivityStats
import com.atmacanext.app.data.settings.AppSettings
import com.atmacanext.app.data.settings.ReplyTextPool
import com.atmacanext.app.data.settings.TweetTextPool
import com.atmacanext.app.ui.theme.AppThemeMode
import com.atmacanext.app.ui.theme.AppThemeStore
import com.atmacanext.app.ui.theme.AtmacaThemeState
import com.atmacanext.app.ui.theme.AtmacaBlue
import com.atmacanext.app.ui.theme.Teal
import com.atmacanext.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AccountSettingsScreen26_42(modifier: Modifier = Modifier, tabReset: Int = 0) {
    val health by AccessibilityServiceState.health.collectAsStateWithLifecycle()
    val sync by AccountSyncController.state.collectAsStateWithLifecycle()
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val settings by AppServices.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var logs by remember { mutableStateOf(false) }
    var showWorkSettings by remember { mutableStateOf(false) }
    var confirmStatsReset by remember { mutableStateOf(false) }
    LaunchedEffect(tabReset) {
        if (tabReset > 0) {
            logs = false
            showWorkSettings = false
        }
    }
    var poolCount by remember { mutableStateOf(ReplyTextPool.count(context)) }
    var poolCursor by remember { mutableStateOf(ReplyTextPool.cursor(context)) }
    var poolSample by remember { mutableStateOf(ReplyTextPool.sample(context)) }
    var tweetCount by remember { mutableStateOf(TweetTextPool.count(context)) }
    var tweetCursor by remember { mutableStateOf(TweetTextPool.cursor(context)) }
    var tweetSample by remember { mutableStateOf(TweetTextPool.sample(context)) }
    val poolFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val raw = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull().orEmpty()
        poolCount = ReplyTextPool.replace(context, raw)
        poolCursor = 0
        poolSample = ReplyTextPool.sample(context)
        Toast.makeText(
            context,
            if (poolCount == 0) "Dosyada satır yok" else "$poolCount hazır metin kaydedildi",
            Toast.LENGTH_SHORT,
        ).show()
    }
    val tweetFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val raw = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull().orEmpty()
        tweetCount = TweetTextPool.replace(context, raw)
        tweetCursor = 0
        tweetSample = TweetTextPool.sample(context)
        Toast.makeText(
            context,
            if (tweetCount == 0) "Dosyada tweet yok" else "$tweetCount tweet kaydedildi",
            Toast.LENGTH_SHORT,
        ).show()
    }

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

    if (confirmStatsReset) {
        AlertDialog(
            onDismissRequest = { confirmStatsReset = false },
            title = { Text("İstatistikler sıfırlansın mı?") },
            text = { Text("Takip, çıkış, tweet, yorum ve büyüme sayaçları sıfırlanır. Hesaplar ve görevler durur.") },
            confirmButton = {
                Button(onClick = {
                    confirmStatsReset = false
                    ActivityStats.clear(context)
                    AccountPulse.resetDisplay(context)
                    scope.launch { AppServices.repository.clearDailyUsage() }
                    Toast.makeText(context, "İstatistikler sıfırlandı", Toast.LENGTH_SHORT).show()
                }) { Text("Sıfırla") }
            },
            dismissButton = { TextButton(onClick = { confirmStatsReset = false }) { Text("Vazgeç") } },
        )
    }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Ayarlar", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Hazır yorumlar, toplu tweetler, otomasyon ve sistem bağlantısı", color = TextSecondary)

        Card {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Görünüm", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Açık, koyu veya gri. Kartlar, ayarlar ve açılan pencereler birlikte değişir.", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { item ->
                        FilterChip(
                            selected = AtmacaThemeState.mode == item,
                            onClick = { AppThemeStore.select(context, item) },
                            label = { Text(item.title) },
                        )
                    }
                }
            }
        }

        Card {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("İstatistik", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Ana sayfadaki büyüme grafiği, toplam artış, yorum ve tweet sayaçları. Sıfırlayınca grafik bugünden yeniden başlar.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
                OutlinedButton(onClick = { confirmStatsReset = true }, enabled = !sync.active && !queue.isActive) {
                    Text("İstatistikleri sıfırla")
                }
            }
        }

        GeminiKeyCard26_42(enabled = !sync.active && !queue.isActive)

        Card {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Hazır yorum metinleri", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Metin dosyası yükle. Her satır bir hazır yorumdur, 500 satıra kadar telefonda kalır. Yorum atılırken hazır satırın yanına bir boşluk koyulur, sonra görev kutusuna yazdığın ek gelir. Hesaplar aynı sırayı kullanır, bitince 1. satıra döner.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
                Text(
                    if (poolCount == 0) "Henüz dosya yok." else "$poolCount satır kayıtlı. Sıradaki ${poolCursor + 1}.",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                if (poolSample.isNotBlank()) {
                    Text("İlk satır: ${poolSample.take(80)}", color = TextSecondary, fontSize = 12.sp)
                }
                Button(onClick = { poolFile.launch(arrayOf("text/plain", "text/*", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (poolCount == 0) "Metin dosyası yükle" else "Metin dosyasını değiştir")
                }
                if (poolCount > 0) {
                    OutlinedButton(
                        onClick = {
                            poolCount = ReplyTextPool.replace(context, "")
                            poolCursor = 0
                            poolSample = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Havuzu sil") }
                }
            }
        }

        Card {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Toplu tweet metinleri", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Metin dosyası yükle. Her satır bir tweet, 500 satıra kadar. Görev sırayla atar, bitince 1. satıra döner. Hesaplar aynı sırayı kullanır.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
                Text(
                    if (tweetCount == 0) "Henüz dosya yok." else "$tweetCount tweet kayıtlı. Sıradaki ${tweetCursor + 1}.",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                if (tweetSample.isNotBlank()) {
                    Text("İlk satır: ${tweetSample.take(80)}", color = TextSecondary, fontSize = 12.sp)
                }
                Button(onClick = { tweetFile.launch(arrayOf("text/plain", "text/*", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (tweetCount == 0) "Tweet dosyası yükle" else "Tweet dosyasını değiştir")
                }
                if (tweetCount > 0) {
                    OutlinedButton(
                        onClick = {
                            tweetCount = TweetTextPool.replace(context, "")
                            tweetCursor = 0
                            tweetSample = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Tweet havuzunu sil") }
                }
            }
        }

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

        Text("Atmaca Next · " + com.atmacanext.app.BuildConfig.VERSION_NAME, color = TextSecondary, fontSize = 12.sp)

        Card {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.AccessibilityNew, null, tint = AtmacaBlue)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ekran okuma", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    ConnectionStatusLabel26_42(
                        when {
                            health.connected -> "Bağlı ve hazır"
                            health.revoked -> "Telefon servisi kapatmış · yeniden aç"
                            health.enabled -> "İzin açık · bağlantı bekleniyor"
                            else -> "İzin kapalı"
                        },
                        health.connected,
                    )
                }
                TextButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    enabled = !sync.active,
                ) { Text("Ayar") }
            }
        }
        if (health.revoked || health.enabled) {
            Text(
                "Uygulamayı son uygulamalardan kaydırma. Bildirim durduğu sürece erişilebilirlik açık kalır. Kaydırınca bazı telefonlar servisi kapatır.",
                color = TextSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun GeminiKeyCard26_42(enabled: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by AppServices.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    var draft by remember { mutableStateOf("") }
    var modelText by remember(settings.geminiModel) { mutableStateOf(settings.geminiModel) }
    var keys by remember { mutableStateOf(AppServices.geminiKeyStore.history()) }
    var current by remember { mutableStateOf(AppServices.geminiKeyStore.load()) }
    fun refresh() {
        keys = AppServices.geminiKeyStore.history()
        current = AppServices.geminiKeyStore.load()
    }
    Card {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Gemini", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(
                "Pilot sohbeti ve görev metni bu anahtarla bağlanır. Önceki anahtarlar telefonda durur.",
                color = TextSecondary,
                fontSize = 12.sp,
            )
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(300) },
                label = { Text(if (current == null) "Gemini API anahtarı" else "Yeni anahtar") },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = modelText,
                onValueChange = { modelText = it.take(80) },
                label = { Text("Gemini modeli (auto önerilir)") },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val error = runCatching { AppServices.geminiKeyStore.save(draft) }.exceptionOrNull()
                    if (error != null) {
                        Toast.makeText(context, error.message ?: "Anahtar kaydedilemedi", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch { AppServices.settings.setGeminiModel(modelText) }
                    draft = ""
                    refresh()
                    Toast.makeText(context, "Anahtar kaydedildi", Toast.LENGTH_SHORT).show()
                },
                enabled = enabled && draft.trim().length >= 20,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Anahtarı kaydet") }
            if (keys.isEmpty()) {
                Text("Kayıtlı anahtar yok.", color = TextSecondary, fontSize = 12.sp)
            } else {
                keys.forEach { key ->
                    val active = key == current
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (active) "Kullanılan\n$key" else key,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!active) {
                                TextButton(onClick = {
                                    runCatching { AppServices.geminiKeyStore.save(key) }
                                    refresh()
                                }, enabled = enabled) { Text("Kullan") }
                            }
                            TextButton(onClick = {
                                AppServices.geminiKeyStore.remove(key)
                                refresh()
                            }, enabled = enabled) { Text("Sil") }
                        }
                    }
                }
            }
            TextButton(
                onClick = { scope.launch { AppServices.settings.setGeminiModel(modelText) } },
                enabled = enabled,
            ) { Text("Modeli kaydet") }
        }
    }
}

@Composable
private fun ConnectionStatusLabel26_42(text: String, positive: Boolean) {
    Text(
        text = text,
        color = if (positive) Teal else TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
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
    val valid = action in 71L..15_000L &&
        accountSwitch in 100L..15_000L &&
        taskGap in 100L..60_000L &&
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
                item {
                    OutlinedButton(enabled = enabled, onClick = { actionMs = "71"; switchMs = "257"; taskMs = "214" }) {
                        Text("En hızlı · 7×")
                    }
                    Text("Hesap ekleme ve tüm görevlerde 7× işlem temposu. X yüklenirken ekran ve işlem sonucu doğrulanır.", color = TextSecondary, fontSize = 10.sp)
                }
                item { PreferenceNumber26_42("Tüm işlemlerin hızı (71–15000 ms)", actionMs, enabled) { actionMs = it } }
                item { PreferenceNumber26_42("Hesap geçişi (100–15000 ms)", switchMs, enabled) { switchMs = it } }
                item { PreferenceNumber26_42("Görevler arası (100–60000 ms)", taskMs, enabled) { taskMs = it } }
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
