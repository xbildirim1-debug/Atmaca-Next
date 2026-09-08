package com.atmacanext.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.data.settings.AppSettings
import com.atmacanext.app.domain.model.TargetAccount
import com.atmacanext.app.ui.components.AtmacaCard
import com.atmacanext.app.ui.components.SectionTitle
import com.atmacanext.app.ui.components.StatusPill
import com.atmacanext.app.ui.theme.AtmacaBlue
import com.atmacanext.app.ui.theme.Success
import com.atmacanext.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by AppServices.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val accounts by AppServices.repository.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val targets by AppServices.repository.targetAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(accounts.map { it.id to it.isCurrent }) {
        if (accounts.none { it.id == selectedAccountId }) {
            selectedAccountId = accounts.firstOrNull { it.isCurrent }?.id ?: accounts.firstOrNull()?.id
        }
    }
    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
    var accountMenu by remember { mutableStateOf(false) }
    var targetText by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var keyStored by remember { mutableStateOf(AppServices.geminiKeyStore.hasKey()) }
    var geminiTesting by remember { mutableStateOf(false) }
    var geminiTestMessage by remember { mutableStateOf<String?>(null) }
    var modelText by remember(settings.geminiModel) { mutableStateOf(settings.geminiModel) }
    var persona by remember(selectedAccount?.id, selectedAccount?.aiPersona) { mutableStateOf(selectedAccount?.aiPersona.orEmpty()) }
    var tone by remember(selectedAccount?.id, selectedAccount?.aiTone) { mutableStateOf(selectedAccount?.aiTone ?: "doğal") }
    var language by remember(selectedAccount?.id, selectedAccount?.aiLanguage) { mutableStateOf(selectedAccount?.aiLanguage ?: "tr") }
    var followLimit by remember(settings.defaultFollowLimit) { mutableStateOf(settings.defaultFollowLimit.toString()) }
    var unfollowLimit by remember(settings.defaultUnfollowLimit) { mutableStateOf(settings.defaultUnfollowLimit.toString()) }
    var actionMs by remember(settings.betweenActionsMs) { mutableStateOf(settings.betweenActionsMs.toString()) }
    var switchMs by remember(settings.accountSwitchSettleMs) { mutableStateOf(settings.accountSwitchSettleMs.toString()) }
    var taskGapMs by remember(settings.betweenTasksMs) { mutableStateOf(settings.betweenTasksMs.toString()) }
    var retention by remember(settings.keepLogDays) { mutableStateOf(settings.keepLogDays.toString()) }
    var reportMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 1_000.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Ayarlar", style = MaterialTheme.typography.headlineSmall)
                Text("Hesap hedefleri, Gemini ve güvenli görev aralıkları", color = TextSecondary, fontSize = 12.sp)
            }

            item {
                SectionTitle("Yorumcu / Retweetçi / Alıntıcı Hedefleri")
                AtmacaCard {
                    Text("Hesap başına tek hedef kullanılır. Yorumcu ve retweetçi takibi en az iki saatlik gönderilerden eskiye doğru ilerler.", color = TextSecondary, fontSize = 10.sp)
                    Spacer(Modifier.height(10.dp))
                    Box {
                        OutlinedButton(
                            onClick = { accountMenu = true },
                            enabled = accounts.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                                Text("Görevi çalıştıracak hesap", color = TextSecondary, fontSize = 10.sp)
                                Text(selectedAccount?.username ?: "Önce hesap ekle")
                            }
                            Icon(Icons.Filled.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.username) },
                                    onClick = { selectedAccountId = account.id; accountMenu = false },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { targetText = it.take(17) },
                            label = { Text("Hedef @kullanici") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            enabled = selectedAccount != null && validHandle(targetText),
                            onClick = {
                                val owner = selectedAccount ?: return@IconButton
                                val handle = normalizeHandle(targetText)
                                scope.launch {
                                    AppServices.repository.upsertTarget(
                                        TargetAccount(
                                            id = UUID.nameUUIDFromBytes("${owner.id}:$handle".toByteArray()).toString(),
                                            ownerAccountId = owner.id,
                                            handle = handle,
                                        )
                                    )
                                }
                                targetText = ""
                            },
                        ) { Icon(Icons.Filled.Add, "Hedef ekle") }
                    }
                    val ownTargets = targets.filter { it.ownerAccountId == selectedAccountId }
                    if (ownTargets.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Bu hesap için hedef yok. Tek hedef hesap ekleyebilirsin.", color = TextSecondary, fontSize = 10.sp)
                    } else {
                        ownTargets.forEach { target ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PersonSearch, null, tint = AtmacaBlue)
                                Spacer(Modifier.width(8.dp))
                                Text("@${target.handle}", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = { scope.launch { AppServices.repository.deleteTarget(target.id) } }) {
                                    Icon(Icons.Filled.Delete, "Hedefi sil")
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle("Gemini")
                AtmacaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, null, tint = AtmacaBlue)
                        Spacer(Modifier.width(8.dp))
                        Text("API Anahtarı", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        StatusPill(if (keyStored) "Kayıtlı" else "Yok", keyStored)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it.take(300) },
                        label = { Text(if (keyStored) "Yeni anahtarla değiştir" else "Gemini API anahtarı") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(modelText, { modelText = it.take(80) }, label = { Text("Gemini modeli (auto önerilir)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = apiKey.trim().length >= 20,
                            onClick = {
                                runCatching { AppServices.geminiKeyStore.save(apiKey) }
                                    .onSuccess {
                                        keyStored = true
                                        apiKey = ""
                                        scope.launch { AppServices.settings.setGeminiModel(modelText) }
                                        Toast.makeText(context, "Gemini anahtarı şifreli kaydedildi", Toast.LENGTH_SHORT).show()
                                    }
                                    .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Icon(Icons.Filled.AutoAwesome, null); Text("Kaydet") }
                        OutlinedButton(
                            enabled = keyStored,
                            onClick = {
                                AppServices.geminiKeyStore.clear()
                                keyStored = false
                                geminiTestMessage = null
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Anahtarı Sil") }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        enabled = keyStored && !geminiTesting,
                        onClick = {
                            geminiTesting = true
                            geminiTestMessage = "Erişilebilir modeller denetleniyor…"
                            scope.launch {
                                AppServices.settings.setGeminiModel(modelText)
                                geminiTestMessage = runCatching { AppServices.contentService.testConnection(modelText) }
                                    .fold(
                                        onSuccess = { result -> result.message },
                                        onFailure = { error -> "Bağlantı hatası: ${error.message ?: error.javaClass.simpleName}" },
                                    )
                                geminiTesting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (geminiTesting) "Test ediliyor…" else "Gemini Bağlantısını Test Et") }
                    geminiTestMessage?.let { message ->
                        Text(message, color = if (message.startsWith("Bağlantı başarılı")) Success else TextSecondary, fontSize = 10.sp)
                    }
                    Text("Anahtar Android Keystore ile şifrelenir; loglara ve hata raporuna yazılmaz.", color = TextSecondary, fontSize = 9.sp)
                }
            }

            if (selectedAccount != null) {
                item {
                    SectionTitle("Hesaba Özel Yapay Zeka Profili")
                    AtmacaCard {
                        Text(selectedAccount.username, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(persona, { persona = it.take(500) }, label = { Text("Kişilik / içerik odağı") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(language, { language = it.take(12) }, label = { Text("Dil") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(tone, { tone = it.take(40) }, label = { Text("Ton") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch { AppServices.repository.upsertAccount(selectedAccount.copy(aiPersona = persona.trim(), aiLanguage = language.trim(), aiTone = tone.trim())) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("AI Profilini Kaydet") }
                    }
                }
            }

            item {
                SectionTitle("Görev Güvenliği")
                AtmacaCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(followLimit, { followLimit = it.filter(Char::isDigit).take(3) }, label = { Text("Takip limiti") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(unfollowLimit, { unfollowLimit = it.filter(Char::isDigit).take(3) }, label = { Text("Çıkma limiti") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(actionMs, { actionMs = it.filter(Char::isDigit).take(5) }, label = { Text("İşlemler arası ms") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(switchMs, { switchMs = it.filter(Char::isDigit).take(5) }, label = { Text("Hesap geçişi doğrulama ms") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(taskGapMs, { taskGapMs = it.filter(Char::isDigit).take(5) }, label = { Text("Görevler arası tolerans ms") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                AppServices.settings.setLimits(followLimit.toIntOrNull() ?: 35, unfollowLimit.toIntOrNull() ?: 35)
                                AppServices.settings.setDailyAccountLimits(followLimit.toIntOrNull() ?: 35, unfollowLimit.toIntOrNull() ?: 35)
                                AppServices.settings.setAutomationTiming(
                                    actionMs.toLongOrNull() ?: 1_800L,
                                    switchMs.toLongOrNull() ?: 2_500L,
                                    settings.rateLimitCooldownMinutes,
                                    taskGapMs.toLongOrNull() ?: 3_000L,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Güvenlik Ayarlarını Kaydet") }
                    Text("Görevler seri çalışır. Aynı dakika/ekran çakışması oluşmaz; hesap doğrulanmadan hiçbir görev eylemi başlamaz.", color = TextSecondary, fontSize = 9.sp)
                }
            }

            item {
                SectionTitle("Log ve Tanılama")
                AtmacaCard {
                    OutlinedTextField(retention, { retention = it.filter(Char::isDigit).take(3) }, label = { Text("Log saklama günü") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { scope.launch { AppServices.settings.setKeepLogDays(retention.toIntOrNull() ?: 14); AppServices.repository.pruneLogs(retention.toIntOrNull() ?: 14) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Log Ayarını Kaydet") }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            reportMessage = "Rapor hazırlanıyor…"
                            scope.launch {
                                reportMessage = runCatching { withContext(Dispatchers.IO) { AppServices.reportExporter.export() } }
                                    .fold({ "Rapor hazır: ${it.name}" }, { "Rapor hatası: ${it.message}" })
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Icon(Icons.Filled.BugReport, null); Spacer(Modifier.width(8.dp)); Text("Tanılama ZIP'i Oluştur") }
                    reportMessage?.let { Text(it, color = if (it.startsWith("Rapor hazır")) Success else TextSecondary, fontSize = 10.sp) }
                }
            }
        }
    }
}

private fun normalizeHandle(value: String): String = value.trim().lowercase().removePrefix("@").filter { it.isLetterOrDigit() || it == '_' }.take(15)
private fun validHandle(value: String): Boolean = normalizeHandle(value).matches(Regex("[a-z0-9_]{1,15}"))
