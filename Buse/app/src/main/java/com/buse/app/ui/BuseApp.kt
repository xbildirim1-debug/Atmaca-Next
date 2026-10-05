package com.buse.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buse.app.automation.AccessibilityServiceState
import com.buse.app.automation.AccountSyncController
import com.buse.app.automation.AutomationController
import com.buse.app.core.AppServices
import com.buse.app.domain.model.*
import com.buse.app.domain.policy.BuseTaskMode
import com.buse.app.domain.policy.BuseTaskPolicy
import com.buse.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

@Composable
fun BuseApp() {
    var tab by rememberSaveable { mutableStateOf(0) }
    val ready by produceState(initialValue = AppServices.ready) {
        while (!AppServices.ready) delay(100)
        value = true
    }
    Scaffold(containerColor = AppBackground, bottomBar = {
        NavigationBar(containerColor = CardBackground) {
            NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                icon = { Icon(Icons.Outlined.People, null) }, label = { Text("Hesaplar") })
            NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                icon = { Icon(Icons.Outlined.Checklist, null) }, label = { Text("Görevler") })
        }
    }) { padding ->
        if (ready) {
            if (tab == 0) BuseAccounts(Modifier.padding(padding).fillMaxSize())
            else BuseTasks(Modifier.padding(padding).fillMaxSize())
        } else Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun BuseHeader(section: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(BusePink, BuseRose)), RoundedCornerShape(26.dp))
        .padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = Color.White.copy(alpha = .20f), shape = CircleShape, modifier = Modifier.size(56.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("B", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Buse", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(section, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.White.copy(alpha = .92f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PinkCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = CardBackground, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Divider)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun BuseAccounts(modifier: Modifier) {
    val accounts by AppServices.repository.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val health by AccessibilityServiceState.health.collectAsStateWithLifecycle()
    val sync by AccountSyncController.state.collectAsStateWithLifecycle()
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var connecting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var remove by remember { mutableStateOf<Account?>(null) }
    val busy = connecting || sync.active || queue.isActive

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BuseHeader("Hesaplar", "X hesaplarını ekle ve yönet") }
        item {
            PinkCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.PersonAdd, null, tint = BusePink)
                    Text("Hesap ekle", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text("X uygulamasında açık olan hesaplarını Buse’ye ekle.", color = TextSecondary)
                if (!health.enabled) Text("İlk kullanımda erişilebilirlik listesinden Buse’yi aç; ardından buraya dön ve Hesap Ekle’ye bas.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Button(onClick = {
                    AccessibilityServiceState.refreshEnabled(context)
                    if (!AccessibilityServiceState.health.value.enabled) context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    else scope.launch {
                        connecting = true
                        try {
                            val connected = withTimeoutOrNull(10_000L) { AccessibilityServiceState.health.first { it.connected } }
                            if (connected != null) AccountSyncController.startImport() else error = "Buse ekran okuma hizmeti bağlanmadı. Erişilebilirlikten Buse’yi açıp tekrar dene."
                        } finally { connecting = false }
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text(if (connecting) "Bağlanıyor…" else "Hesap Ekle")
                }
                TextButton(onClick = {
                    val launch = context.packageManager.getLaunchIntentForPackage("com.twitter.android")
                    if (launch != null) context.startActivity(launch) else error = "Önce X uygulamasını kur ve hesabına giriş yap."
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("X’i aç") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (health.connected) Icons.Outlined.CheckCircle else Icons.Outlined.Info, null,
                        Modifier.size(16.dp), tint = if (health.connected) Success else BusePink)
                    Spacer(Modifier.width(6.dp))
                    Text(if (health.connected) "Ekran okuma hazır" else "Buse erişilebilirlik bağlantısı gerekli", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
        if (sync.active || sync.completed || sync.failed) item {
            PinkCard {
                Text(sync.message, color = if (sync.failed) ErrorRed else TextPrimary)
                if (sync.active) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    OutlinedButton(onClick = AccountSyncController::cancel) { Text("Taramayı durdur") }
                }
            }
        }
        item { Text("Hesapların · ${accounts.size}/10", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        if (accounts.isEmpty()) item {
            PinkCard {
                Text("İlk hesabını ekle", fontWeight = FontWeight.Bold)
                Text("Hesap Ekle ile X’teki hesaplarını tara. Ardından Görevler’den işlem seçebilirsin.", color = TextSecondary)
            }
        }
        items(accounts, key = { it.id }) { account ->
            PinkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = BuseBlush, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(account.username.removePrefix("@").take(2).uppercase(), color = BusePink, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account.username, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (account.isCurrent) "X’te açık" else "Eklenen hesap", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { remove = account }, enabled = !busy) { Icon(Icons.Outlined.DeleteOutline, "Hesabı Buse’den kaldır", tint = BusePink) }
                }
                Text("${account.followers} takipçi  ·  ${account.following} takip edilen", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                if (!account.active) Text(account.inactiveReason ?: "Hesap duraklatılmış", color = ErrorRed)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { AccountSyncController.switchTo(account) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Hesaba geç") }
                    if (!account.active) Button(onClick = { scope.launch { AppServices.repository.upsertAccount(account.copy(active = true)) } }, enabled = !busy) { Text("Etkinleştir") }
                }
            }
        }
    }
    remove?.let { account ->
        AlertDialog(onDismissRequest = { remove = null }, title = { Text("Hesap kaydını kaldır?") },
            text = { Text("${account.username} Buse’den ve bağlı görevlerden kaldırılır. X oturumu açık kalır.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                scope.launch { runCatching { AppServices.repository.deleteAccount(account.id) }.onFailure { error = "Hesap kaldırılamadı." } }
                remove = null
            }) { Text("Kaldır") } }, dismissButton = { TextButton(onClick = { remove = null }) { Text("Vazgeç") } })
    }
    error?.let { BuseMessage(it) { error = null } }
}

@Composable
private fun BuseTasks(modifier: Modifier) {
    val tasks by AppServices.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by AppServices.repository.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val runtime by AutomationController.state.collectAsStateWithLifecycle()
    val sync by AccountSyncController.state.collectAsStateWithLifecycle()
    val health by AccessibilityServiceState.health.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var createMode by remember { mutableStateOf<BuseTaskMode?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf<List<ScheduledTask>?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingStart?.let(AppServices.orchestrator::startSelection)
        pendingStart = null
    }
    val busy = queue.isActive || sync.active || saving
    val activeAccounts = accounts.filter { it.active }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(queue.status, runtime.status) { while (true) { clock = System.currentTimeMillis(); delay(1000) } }

    fun start(group: List<ScheduledTask>) {
        AccessibilityServiceState.refreshEnabled(context)
        if (!AccessibilityServiceState.health.value.connected) { error = "Önce Hesaplar’dan Buse erişilebilirlik bağlantısını aç."; return }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingStart = group
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else AppServices.orchestrator.startSelection(group)
    }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BuseHeader("Görevler", "İki görev. Kontrol sende.") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Bolt, null, tint = BusePink, modifier = Modifier.size(18.dp))
                Text("En hızlı · 10× hız artışı", color = BusePink, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
        if (queue.isActive || runtime.taskId != null) item {
            PinkCard {
                Text(if (queue.isActive) "Görev durumu" else "Son işlem", fontWeight = FontWeight.Bold)
                Text(runtime.message, color = TextSecondary)
                Text("Doğrulanan ${runtime.verifiedCount}/${runtime.limit}", fontWeight = FontWeight.SemiBold)
                if (runtime.depthUniqueUsers > 0) Text("Taranan ${runtime.depthUniqueUsers} kişi", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                val until = runtime.cycleWaitUntil
                if (until != null && until > clock) {
                    val remaining = (until - clock + 999) / 1000
                    Text("Sonraki tur: ${remaining / 60} dk ${remaining % 60} sn", color = BusePink)
                }
                if (queue.isActive) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { if (queue.status == QueueStatus.PAUSED) AppServices.orchestrator.resume() else AppServices.orchestrator.pause() }) {
                        Text(if (queue.status == QueueStatus.PAUSED) "Devam et" else "Duraklat")
                    }
                    Button(onClick = AppServices.orchestrator::stop) { Text("Durdur") }
                }
            }
        }
        BuseTaskMode.entries.forEach { mode ->
            item {
                PinkCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (mode == BuseTaskMode.NON_FOLLOWERS) Icons.Outlined.PersonSearch else Icons.Outlined.PersonRemove, null, tint = BusePink)
                        Text(mode.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                    Text(mode.description, color = TextSecondary)
                    if (mode == BuseTaskMode.NON_FOLLOWERS) Text("“Seni takip ediyor” yazan kişiler kalır. Liste 200 kişiden kısaysa kimse çıkarılmaz.", style = MaterialTheme.typography.bodySmall, color = BusePink)
                    Button(onClick = { createMode = mode }, enabled = !busy && activeAccounts.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Görev oluştur")
                    }
                }
            }
        }
        if (activeAccounts.isEmpty()) item { Text("Görev oluşturmak için önce Hesaplar’dan bir hesap ekle.", color = TextSecondary) }
        val groups = tasks.filter(BuseTaskPolicy::accepts).groupBy { it.time.takeIf { t -> t.startsWith("group:") } ?: it.id }.values.toList()
        if (groups.isNotEmpty()) item { Text("Kayıtlı görevler", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        items(groups, key = { it.first().id }) { group ->
            val task = group.first()
            PinkCard {
                Text(BuseTaskPolicy.mode(task).title, fontWeight = FontWeight.Bold)
                Text("Her hesap ${task.limit} kişi · ${task.repeatCount} tur · ${task.intervalMinutes} dk ara", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                group.forEach { row -> Text("${row.username}  ·  ${row.progress}/${row.totalLimit}  ·  ${buseStatus(row.status)}", style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { scope.launch { runCatching { AppServices.repository.deleteTasks(group.map { it.id }) }.onFailure { error = "Görev silinemedi." } } }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Sil") }
                    Button(onClick = { start(group) }, enabled = !busy && group.any { it.status != TaskStatus.COMPLETED && it.progress < it.totalLimit }, modifier = Modifier.weight(1f)) { Text("Başlat") }
                }
            }
        }
        item { Text(if (health.connected) "Buse bağlantısı hazır · Hesaplar sırayla işlenir" else "Buse erişilebilirlik bağlantısı gerekli", color = TextSecondary, style = MaterialTheme.typography.bodySmall) }
    }
    createMode?.let { mode ->
        BuseTaskDialog(mode, activeAccounts, saving, error, onDismiss = { if (!saving) { createMode = null; error = null } }) { created ->
            scope.launch {
                saving = true
                try { AppServices.repository.upsertTasks(created); createMode = null; error = null }
                catch (failure: Exception) { error = failure.message ?: "Görev kaydedilemedi." }
                finally { saving = false }
            }
        }
    }
    if (createMode == null) error?.let { BuseMessage(it) { error = null } }
}

@Composable
private fun BuseTaskDialog(mode: BuseTaskMode, accounts: List<Account>, saving: Boolean, error: String?, onDismiss: () -> Unit, onSave: (List<ScheduledTask>) -> Unit) {
    var selected by remember(accounts) { mutableStateOf(setOfNotNull(accounts.firstOrNull { it.isCurrent }?.id ?: accounts.firstOrNull()?.id)) }
    var limit by rememberSaveable { mutableStateOf("20") }
    var repeat by rememberSaveable { mutableStateOf("1") }
    var interval by rememberSaveable { mutableStateOf("5") }
    val valid = selected.isNotEmpty() && limit.toIntOrNull() in 1..35 && repeat.toIntOrNull() in 1..100 && interval.toIntOrNull() in 1..1440
    AlertDialog(onDismissRequest = onDismiss, title = { Text(mode.title) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(mode.description, color = TextSecondary)
            error?.let { Text(it, color = ErrorRed) }
            Text("Hesap seç", fontWeight = FontWeight.SemiBold)
            accounts.forEach { account ->
                Row(Modifier.fillMaxWidth().clickable(enabled = !saving) { selected = if (account.id in selected) selected - account.id else selected + account.id }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = account.id in selected, onCheckedChange = if (saving) null else { checked -> selected = if (checked) selected + account.id else selected - account.id })
                    Text(account.username, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            OutlinedTextField(limit, { limit = it.filter(Char::isDigit).take(2) }, label = { Text("Her tur kaç kişi? · 1–35") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), enabled = !saving)
            OutlinedTextField(repeat, { repeat = it.filter(Char::isDigit).take(3) }, label = { Text("Tur sayısı · 1–100") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), enabled = !saving)
            OutlinedTextField(interval, { interval = it.filter(Char::isDigit).take(4) }, label = { Text("Turlar arası dakika · 1–1440") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), enabled = !saving)
            Text("Birden fazla hesap seçersen her tur aynı sırayla çalışır.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(enabled = valid && !saving, onClick = {
        val group = "group:${UUID.randomUUID()}"
        onSave(accounts.filter { it.id in selected }.map { account -> ScheduledTask(id = UUID.randomUUID().toString(), accountId = account.id,
            username = account.username, type = TaskType.UNFOLLOW, time = group, limit = requireNotNull(limit.toIntOrNull()),
            repeatCount = requireNotNull(repeat.toIntOrNull()), intervalMinutes = requireNotNull(interval.toIntOrNull()),
            contentPrompt = if (mode == BuseTaskMode.NON_FOLLOWERS) BuseTaskPolicy.NON_FOLLOWER_MARKER else null) })
    }) { Text(if (saving) "Kaydediliyor…" else "Kaydet") } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Vazgeç") } })
}

private fun buseStatus(status: TaskStatus): String = when (status) {
    TaskStatus.QUEUED -> "Hazır"
    TaskStatus.RUNNING -> "Çalışıyor"
    TaskStatus.PAUSED -> "Duraklatıldı"
    TaskStatus.COMPLETED -> "Bitti"
    TaskStatus.FAILED -> "Kontrol gerekli"
}

@Composable private fun BuseMessage(message: String, dismiss: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, text = { Text(message) }, confirmButton = { TextButton(onClick = dismiss) { Text("Tamam") } })
}
