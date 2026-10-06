package com.atmacanext.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.automation.AccountSyncController
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskStatus
import com.atmacanext.app.domain.model.TaskType
import com.atmacanext.app.domain.model.QueueStatus
import com.atmacanext.app.domain.model.TargetKind
import com.atmacanext.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

private enum class TaskTab43(val title: String) {
    FOLLOW("Takip"),
    ENGAGEMENT("Etkileşim"),
    QUOTE_COMMENT("Yorum Alıntısı"),
}

@Composable
fun TasksScreen26_43(modifier: Modifier = Modifier) {
    val tasks by AppServices.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by AppServices.repository.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val runtime by AutomationController.state.collectAsStateWithLifecycle()
    val sync by AccountSyncController.state.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(TaskTab43.FOLLOW.name) }
    val tab = TaskTab43.valueOf(selected)

    Column(modifier.fillMaxSize()) {
        Surface(color = AppBackground) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text("Görevler", style = MaterialTheme.typography.headlineSmall)
                Text("Görev türünü seç ve çalıştır", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(9.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    TaskTab43.entries.forEach { item ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = tab == item,
                            onClick = { selected = item.name },
                            label = { Text(item.title, maxLines = 2, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }
            }
        }

        when (tab) {
            TaskTab43.FOLLOW -> TasksScreen26_42(Modifier.fillMaxSize(), "FOLLOW")
            TaskTab43.ENGAGEMENT -> TasksScreen26_42(Modifier.fillMaxSize(), "ENGAGEMENT")
            TaskTab43.QUOTE_COMMENT -> QuoteCommentTaskSetup43(
                modifier = Modifier.fillMaxSize(),
                tasks = tasks.filter { it.type == TaskType.COMMENT_QUOTE_TARGETS },
                accounts = accounts.filter { it.active },
                locked = queue.isActive || sync.active,
                active = runtime.taskType == TaskType.COMMENT_QUOTE_TARGETS,
            )
        }
    }
}

@Composable
private fun QuoteCommentTaskSetup43(
    modifier: Modifier,
    tasks: List<ScheduledTask>,
    accounts: List<Account>,
    locked: Boolean,
    active: Boolean,
) {
    val scope = rememberCoroutineScope()
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val runtime by AutomationController.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = CardBackground, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Yorum Alıntısı", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Alıntı Hedefleri listesindeki profillerin son gönderilerine belirlediğin yorumu bırakır.",
                            color = TextSecondary,
                        )
                    }
                    Button(
                        onClick = { showCreate = true },
                        enabled = !locked && accounts.isNotEmpty(),
                    ) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Yeni Görev")
                    }
                }

                Text(
                    "Limit 1 → her hedefin son 1 gönderisi. Limit 5 → her hedefin son 5 gönderisi.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Hedef profilleri Hesaplar > Alıntı Hedefleri alanından yönetilir.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )

                if (active) Text("Yorum Alıntısı şu anda çalışıyor.", color = Success, fontWeight = FontWeight.SemiBold)
                if (active && queue.isActive) {
                    Text(runtime.message, color = TextSecondary)
                    Text("Doğrulanan " + runtime.verifiedCount + "/" + runtime.limit)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            if (queue.status == QueueStatus.PAUSED) AppServices.orchestrator.resume() else AppServices.orchestrator.pause()
                        }) { Text(if (queue.status == QueueStatus.PAUSED) "Devam et" else "Duraklat") }
                        OutlinedButton(onClick = AppServices.orchestrator::stop) { Text("Durdur") }
                    }
                }
                if (locked && !active) Text("Çalışan görev nedeniyle yeni Yorum Alıntısı görevi eklenemez.", color = TextSecondary)
                if (accounts.isEmpty()) Text("Önce aktif bir X hesabı ekle.", color = TextSecondary)
            }
        }

        if (tasks.isEmpty()) {
            Surface(color = CardBackground, shape = MaterialTheme.shapes.large) {
                Column(
                    Modifier.fillMaxWidth().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Henüz Yorum Alıntısı görevi yok", fontWeight = FontWeight.Bold)
                    Text("Yeni Görev ile hesap, yorum ve limit ayarlarını seç.", color = TextSecondary)
                    if (accounts.isNotEmpty()) {
                        OutlinedButton(onClick = { showCreate = true }, enabled = !locked) {
                            Icon(Icons.Filled.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Yorum Alıntısı Görevi Oluştur")
                        }
                    }
                }
            }
        } else {
            tasks.groupBy { it.time.takeIf { value -> value.startsWith("group:") } ?: it.id }.values.forEach { group ->
                val task = group.first()
                Surface(color = CardBackground, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(group.joinToString(" · ") { it.username }, fontWeight = FontWeight.Bold)
                                Text(
                                    "Limit ${task.limit} • Tekrar ${task.repeatCount} • ${task.intervalMinutes} dk",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (group.all { it.status == TaskStatus.COMPLETED }) Icon(Icons.Filled.CheckCircle, null, tint = Success)
                        }
                        Text(task.contentText.orEmpty().take(180), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        group.forEach { row -> Text(row.username + " · " + row.progress + "/" + row.totalLimit + " · " + quoteTaskStatus(row.status), color = TextSecondary, style = MaterialTheme.typography.bodySmall) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { scope.launch { AppServices.repository.deleteTasks(group.map { it.id }) } },
                                enabled = !locked,
                            ) { Text("Sil") }
                            Button(
                                onClick = { AppServices.orchestrator.startSelection(group) },
                                enabled = !locked && group.any { it.status != TaskStatus.COMPLETED && it.progress < it.totalLimit },
                            ) { Text("Başlat") }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        QuoteCommentCreateDialog43(
            accounts = accounts,
            errorMessage = saveError,
            onDismiss = { showCreate = false },
            onSave = { created ->
                scope.launch {
                    try { AppServices.repository.upsertTasks(created); showCreate = false; saveError = null }
                    catch (error: Exception) { saveError = error.message ?: "Görevler kaydedilemedi" }
                }
            },
        )
    }
}

@Composable
private fun QuoteCommentCreateDialog43(
    accounts: List<Account>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (List<ScheduledTask>) -> Unit,
) {
    val available = accounts.distinctBy { it.username.lowercase() }.take(10)
    val targets by AppServices.repository.targetAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedIds by remember(available) {
        mutableStateOf(setOfNotNull(available.firstOrNull { it.isCurrent }?.id ?: available.firstOrNull()?.id))
    }
    var comment by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("1") }
    var repeatText by remember { mutableStateOf("1") }
    var intervalText by remember { mutableStateOf("5") }

    val limit = limitText.toIntOrNull()
    val repeat = repeatText.toIntOrNull()
    val interval = intervalText.toIntOrNull()
    val valid = selectedIds.isNotEmpty() && comment.isNotBlank() && comment.length <= 280 &&
        limit != null && limit in 1..20 && repeat != null && repeat in 1..100 && interval != null && interval in 1..1440 &&
        selectedIds.all { id -> targets.any { it.ownerAccountId == id && it.kind == TargetKind.QUOTE && it.active } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Yorum Alıntısı Görevi", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Hesaplar", fontWeight = FontWeight.SemiBold)
                available.forEach { account ->
                    val checked = account.id in selectedIds
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            selectedIds = if (checked) selectedIds - account.id else selectedIds + account.id
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selectedIds = if (checked) selectedIds - account.id else selectedIds + account.id
                            },
                        )
                        Text(account.username, modifier = Modifier.weight(1f))
                        if (targets.none { it.ownerAccountId == account.id && it.kind == TargetKind.QUOTE && it.active }) Text("Hedef yok", color = MaterialTheme.colorScheme.error)
                        if (account.isCurrent) Text("X'te açık", color = Success, style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it.take(280) },
                    label = { Text("Yazılacak yorum") },
                    supportingText = { Text("${comment.length}/280") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = repeatText,
                        onValueChange = { repeatText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Tekrar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { intervalText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Aralık dk") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "Alıntı hedeflerini Hesaplar bölümünden yönet. Bu görev her seçili hesabın kendi Alıntı Hedefleri listesini kullanır.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    val batch = UUID.randomUUID().toString()
                    val created = available.filter { it.id in selectedIds }.map { account ->
                        ScheduledTask(
                            id = UUID.randomUUID().toString(),
                            accountId = account.id,
                            username = account.username,
                            time = "group:$batch:${TaskType.COMMENT_QUOTE_TARGETS.name}",
                            type = TaskType.COMMENT_QUOTE_TARGETS,
                            status = TaskStatus.QUEUED,
                            progress = 0,
                            limit = limit!!,
                            repeatCount = repeat!!,
                            intervalMinutes = interval!!,
                            contentText = comment.trim(),
                            useGemini = false,
                        )
                    }
                    onSave(created)
                },
            ) { Text("Görevi Oluştur") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

private fun quoteTaskStatus(status: TaskStatus): String = when (status) {
    TaskStatus.QUEUED -> "Bekliyor"
    TaskStatus.RUNNING -> "Çalışıyor"
    TaskStatus.PAUSED -> "Duraklatıldı"
    TaskStatus.COMPLETED -> "Tamamlandı"
    TaskStatus.FAILED -> "Başarısız"
}
