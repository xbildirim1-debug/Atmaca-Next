package com.atmacanext.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.AutomationQueueState
import com.atmacanext.app.domain.model.QueueStatus
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskStatus
import com.atmacanext.app.domain.model.TaskType
import com.atmacanext.app.service.AutomationForegroundService
import com.atmacanext.app.ui.components.AtmacaWindowClass
import com.atmacanext.app.ui.components.StatusPill
import com.atmacanext.app.ui.components.rememberAtmacaWindowClass
import com.atmacanext.app.ui.theme.AtmacaBlue
import com.atmacanext.app.ui.theme.AtmacaNavy
import com.atmacanext.app.ui.theme.AtmacaSky
import com.atmacanext.app.ui.theme.Divider
import com.atmacanext.app.ui.theme.Purple
import com.atmacanext.app.ui.theme.Success
import com.atmacanext.app.ui.theme.Teal
import com.atmacanext.app.ui.theme.TextPrimary
import com.atmacanext.app.ui.theme.TextSecondary
import com.atmacanext.app.ui.theme.Warning
import kotlinx.coroutines.launch
import java.util.UUID

private enum class TaskGroup(val title: String, val shortTitle: String) {
    FOLLOW("Takip İşlemleri", "Takip"),
    ENGAGEMENT("Etkileşim", "Etkileşim"),
    CONTENT("Tweet ve Alıntı", "Tweet / Alıntı"),
}

private val TASK_TYPES_BY_GROUP = mapOf(
    TaskGroup.FOLLOW to listOf(
        TaskType.UNFOLLOW,
        TaskType.VERIFIED_FOLLOW,
        TaskType.COMMENTER_FOLLOW,
        TaskType.RETWEETER_FOLLOW,
    ),
    TaskGroup.ENGAGEMENT to listOf(
        TaskType.FOLLOW,
        TaskType.LIKE,
        TaskType.RETWEET,
        TaskType.BOOKMARK,
        TaskType.COMMENT,
    ),
    TaskGroup.CONTENT to listOf(
        TaskType.TEXT_TWEET,
        TaskType.IMAGE_TWEET,
        TaskType.QUOTE,
    ),
)

private val SCALABLE_TYPES = setOf(
    TaskType.UNFOLLOW,
    TaskType.VERIFIED_FOLLOW,
    TaskType.COMMENTER_FOLLOW,
    TaskType.RETWEETER_FOLLOW,
    TaskType.QUOTER_FOLLOW,
)
private val ScreenBackground = Color(0xFFF3F6FB)
private val SoftBlue = Color(0xFFEAF4FF)
private val SoftPurple = Color(0xFFF1EDFF)
private val SoftGreen = Color(0xFFEAF9F3)
private val SoftOrange = Color(0xFFFFF5E7)
private const val LOGICAL_TASK_PREFIX = "group:"

private data class LogicalTask(
    val key: String,
    val tasks: List<ScheduledTask>,
) {
    val representative: ScheduledTask get() = tasks.first()
    val taskIds: Set<String> get() = tasks.mapTo(linkedSetOf(), ScheduledTask::id)
}

private data class DeleteRequest(val ids: Set<String>, val logicalCount: Int)

/** Bir görev türü + birden çok hesap, veritabanında güvenli alt işler; ekranda tek görevdir. */
private fun logicalTasks(tasks: List<ScheduledTask>): List<LogicalTask> {
    val legacyCandidates = tasks.filterNot { it.time.startsWith(LOGICAL_TASK_PREFIX) }
        .groupBy(::legacyTaskSignature)
    val groups = linkedMapOf<String, MutableList<ScheduledTask>>()
    tasks.forEach { task ->
        val legacy = legacyCandidates[legacyTaskSignature(task)].orEmpty()
        val canGroupLegacy = legacy.size > 1 && legacy.map(ScheduledTask::accountId).distinct().size == legacy.size
        val key = when {
            task.time.startsWith(LOGICAL_TASK_PREFIX) -> task.time
            canGroupLegacy -> "legacy:${legacyTaskSignature(task)}"
            else -> "task:${task.id}"
        }
        groups.getOrPut(key, ::mutableListOf).add(task)
    }
    return groups.map { (key, rows) -> LogicalTask(key, rows) }
}

private fun legacyTaskSignature(task: ScheduledTask): String = listOf(
    task.type.name,
    task.limit,
    task.repeatCount,
    task.intervalMinutes,
    task.targetUrl.orEmpty(),
    task.contentPrompt.orEmpty(),
    task.contentText.orEmpty(),
    task.mediaUri.orEmpty(),
    task.useGemini,
).joinToString("\u001f")

@Composable
fun TasksScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val runtime by AutomationController.state.collectAsStateWithLifecycle()
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val tasks by AppServices.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val accounts by AppServices.repository.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by AppServices.settings.settings.collectAsStateWithLifecycle(initialValue = com.atmacanext.app.data.settings.AppSettings())
    var selectedKeys by remember { mutableStateOf(emptySet<String>()) }
    var activeFilter by remember { mutableStateOf<TaskGroup?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LogicalTask?>(null) }
    var deleteRequest by remember { mutableStateOf<DeleteRequest?>(null) }
    val queueLocked = queue.isActive
    val taskGroups = logicalTasks(tasks)
    val completedCycles = taskGroups.sumOf { group -> group.tasks.minOfOrNull(::successfulCycleCount) ?: 0 }
    val visibleTasks = taskGroups.filter { activeFilter == null || taskGroup(it.representative.type) == activeFilter }

    Box(modifier.fillMaxSize().background(ScreenBackground), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 1_000.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero") {
                TasksHero(
                    taskCount = taskGroups.size,
                    completedCycles = completedCycles,
                    queueActive = queueLocked,
                    activeAccountCount = accounts.count { it.active },
                    canCreate = !queueLocked && accounts.any { it.active },
                    onCreate = { showCreate = true },
                )
            }

            if (queue.items.isNotEmpty() && queue.status != QueueStatus.IDLE) {
                item { QueueSummary(queue) }
            }
            if (runtime.taskId != null) {
                item { RuntimeSummary(runtime = runtime, onStop = AppServices.orchestrator::stop) }
            }

            if (selectedKeys.isNotEmpty()) {
                item(key = "selection") {
                    SelectionActionBar(
                        selectedCount = selectedKeys.size,
                        locked = queueLocked,
                        onDelete = {
                            val selected = taskGroups.filter { it.key in selectedKeys }
                            deleteRequest = DeleteRequest(selected.flatMap { it.tasks }.mapTo(linkedSetOf(), ScheduledTask::id), selected.size)
                        },
                        onStart = {
                            runCatching { AutomationForegroundService.start(context) }
                            AppServices.orchestrator.startSelection(
                                taskGroups.filter { it.key in selectedKeys }.flatMap(LogicalTask::tasks),
                            )
                        },
                    )
                }
            }

            item(key = "filters") {
                TaskFilterBar(selected = activeFilter, tasks = taskGroups.map(LogicalTask::representative), onSelected = { activeFilter = it })
            }

            if (tasks.isEmpty()) {
                item(key = "empty") {
                    EmptyTasksCard(
                        hasActiveAccount = accounts.any { it.active },
                        onCreate = { showCreate = true },
                    )
                }
            } else if (visibleTasks.isEmpty()) {
                item(key = "filtered-empty") {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Divider),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Filled.Tune, null, tint = AtmacaBlue)
                            Spacer(Modifier.height(8.dp))
                            Text("Bu kategoride görev yok", fontWeight = FontWeight.Bold)
                            Text("Başka bir kategori seçebilir veya yeni görev ekleyebilirsin.", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            } else {
                items(visibleTasks, key = { it.key }) { logicalTask ->
                    val active = queue.currentItem?.taskId?.let { it in logicalTask.taskIds } == true && queue.isActive
                    TaskCard(
                        tasks = logicalTask.tasks,
                        selected = logicalTask.key in selectedKeys,
                        locked = queueLocked,
                        active = active,
                        runtimeTaskId = runtime.taskId,
                        runtimeProgress = runtime.verifiedCount,
                        onSelect = {
                            selectedKeys = if (logicalTask.key in selectedKeys) selectedKeys - logicalTask.key else selectedKeys + logicalTask.key
                        },
                        onStartStop = {
                            if (active) AppServices.orchestrator.stop()
                            else {
                                runCatching { AutomationForegroundService.start(context) }
                                AppServices.orchestrator.startSelection(logicalTask.tasks)
                            }
                        },
                        onEdit = { editing = logicalTask },
                        onDelete = { deleteRequest = DeleteRequest(logicalTask.taskIds, 1) },
                        onReset = { scope.launch { logicalTask.tasks.forEach { AppServices.repository.resetTask(it.id) } } },
                    )
                }
            }

            item {
                Button(
                    onClick = { showCreate = true },
                    enabled = !queueLocked && accounts.any { it.active },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AtmacaNavy),
                ) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (accounts.none { it.active }) "Önce aktif X hesabı ekle" else "Yeni Görev Oluştur",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    if (showCreate) {
        TaskEditorDialog(
            originals = null,
            accounts = accounts.filter { it.active },
            defaultFollowLimit = settings.defaultFollowLimit,
            defaultUnfollowLimit = settings.defaultUnfollowLimit,
            onDismiss = { showCreate = false },
            onSave = { created ->
                scope.launch { created.forEach { AppServices.repository.upsertTask(it) } }
                showCreate = false
            },
        )
    }

    editing?.let { logicalTask ->
        TaskEditorDialog(
            originals = logicalTask.tasks,
            accounts = accounts,
            defaultFollowLimit = settings.defaultFollowLimit,
            defaultUnfollowLimit = settings.defaultUnfollowLimit,
            onDismiss = { editing = null },
            onSave = { updated ->
                val removed = logicalTask.taskIds - updated.mapTo(linkedSetOf(), ScheduledTask::id)
                scope.launch {
                    if (removed.isNotEmpty()) AppServices.repository.deleteTasks(removed)
                    updated.forEach { AppServices.repository.upsertTask(it) }
                }
                editing = null
            },
        )
    }

    deleteRequest?.let { request ->
        AlertDialog(
            onDismissRequest = { deleteRequest = null },
            title = { Text(if (request.logicalCount == 1) "Görev silinsin mi?" else "${request.logicalCount} görev silinsin mi?") },
            text = { Text("Seçili görev akışı ve bağlı hesap alt işleri kalıcı olarak silinecek. Hesaplar ve loglar korunur.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { AppServices.repository.deleteTasks(request.ids) }
                    selectedKeys = emptySet()
                    deleteRequest = null
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { deleteRequest = null }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun TasksHero(
    taskCount: Int,
    completedCycles: Int,
    queueActive: Boolean,
    activeAccountCount: Int,
    canCreate: Boolean,
    onCreate: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF092A58), Color(0xFF145EAE), Color(0xFF1688E8)),
                ),
            )
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "GÖREV MERKEZİ",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Akışı kur, tek dokunuşla çalıştır",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Seçilen görevler hesap sırasına göre tek kilit altında yürütülür.",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                FilledIconButton(
                    onClick = onCreate,
                    enabled = canCreate,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = AtmacaNavy,
                        disabledContainerColor = Color.White.copy(alpha = 0.35f),
                    ),
                ) { Icon(Icons.Filled.Add, "Yeni görev") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroMetric(Modifier.weight(1f), taskCount.toString(), "Görev", Icons.Filled.TaskAlt)
                HeroMetric(Modifier.weight(1f), completedCycles.toString(), "Başarılı döngü", Icons.Filled.CheckCircle)
                HeroMetric(
                    Modifier.weight(1f),
                    if (queueActive) "Aktif" else activeAccountCount.toString(),
                    if (queueActive) "Kuyruk" else "Hesap",
                    if (queueActive) Icons.Filled.Bolt else Icons.Filled.AccountCircle,
                )
            }
        }
    }
}

@Composable
private fun HeroMetric(modifier: Modifier, value: String, label: String, icon: ImageVector) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.13f)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color.White.copy(alpha = 0.78f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun TaskFilterBar(selected: TaskGroup?, tasks: List<ScheduledTask>, onSelected: (TaskGroup?) -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Divider),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Görev akışı", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Kategoriye göre incele ve çalıştır", color = TextSecondary, fontSize = 10.sp)
                }
                Icon(Icons.Filled.Tune, null, tint = AtmacaBlue, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryFilterChip("Tümü", tasks.size, selected == null) { onSelected(null) }
                TaskGroup.entries.forEach { group ->
                    CategoryFilterChip(
                        group.shortTitle,
                        tasks.count { taskGroup(it.type) == group },
                        selected == group,
                    ) { onSelected(group) }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("$label  $count", fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AtmacaSky,
            selectedLabelColor = AtmacaNavy,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Divider,
            selectedBorderColor = AtmacaBlue.copy(alpha = 0.35f),
        ),
    )
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    locked: Boolean,
    onDelete: () -> Unit,
    onStart: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SoftBlue,
        border = BorderStroke(1.dp, AtmacaBlue.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(AtmacaBlue),
                contentAlignment = Alignment.Center,
            ) { Text(selectedCount.toString(), color = Color.White, fontWeight = FontWeight.Bold) }
            Text("görev seçili", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            OutlinedButton(onClick = onDelete, enabled = !locked, contentPadding = PaddingValues(horizontal = 11.dp)) {
                Icon(Icons.Filled.Delete, null, Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text("Sil")
            }
            Button(onClick = onStart, enabled = !locked, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Başlat")
            }
        }
    }
}

@Composable
private fun EmptyTasksCard(hasActiveAccount: Boolean, onCreate: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Divider),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(SoftBlue),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.TaskAlt, null, tint = AtmacaBlue, modifier = Modifier.size(30.dp)) }
            Spacer(Modifier.height(13.dp))
            Text("İlk görev akışını oluştur", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                if (hasActiveAccount) {
                    "Hesaplarını, görev türlerini ve döngü ayarlarını tek ekrandan seç."
                } else {
                    "Görev oluşturmak için önce aktif bir X hesabı ekle."
                },
                color = TextSecondary,
                fontSize = 11.sp,
            )
            if (hasActiveAccount) {
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = onCreate, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Görev Oluştur")
                }
            }
        }
    }
}

@Composable
private fun QueueSummary(queue: AutomationQueueState) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = SoftPurple,
        border = BorderStroke(1.dp, Purple.copy(alpha = 0.18f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TaskIconBubble(Icons.Filled.Lock, Purple, Color.White.copy(alpha = 0.72f))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("SERİ GÖREV KİLİDİ", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        queue.currentItem?.let { "${it.username} • ${it.taskType.title}" } ?: "Görev kuyruğu",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusPill(
                    queueStatusLabel(queue.status),
                    queue.status !in setOf(QueueStatus.PAUSED, QueueStatus.STOPPED, QueueStatus.PARTIAL, QueueStatus.FAILED),
                )
            }
            Spacer(Modifier.height(9.dp))
            Text(queue.message, color = TextSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "${queue.completedCount} tamamlandı • ${queue.skippedCount} atlandı • ${queue.failedCount} hata",
                color = TextSecondary,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun RuntimeSummary(runtime: com.atmacanext.app.automation.AutomationRuntimeState, onStop: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AtmacaBlue.copy(alpha = 0.2f)),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(Success))
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text("ŞİMDİ ÇALIŞIYOR", color = Success, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(runtime.taskType?.title.orEmpty(), fontWeight = FontWeight.Bold)
                }
                Text("${runtime.verifiedCount}/${runtime.limit}", color = AtmacaBlue, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp))
            Text(runtime.message, color = TextSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (runtime.limit > 0) runtime.verifiedCount.toFloat() / runtime.limit else 0f },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                trackColor = SoftBlue,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Hesap ${if (runtime.accountVerified) "doğrulandı" else "doğrulanıyor"} • ${runtime.activeScreen.name} • ${runtime.flowStage.name}",
                color = TextSecondary,
                fontSize = 9.sp,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.Stop, null)
                Spacer(Modifier.width(6.dp))
                Text("Akışı Durdur")
            }
        }
    }
}

@Composable
private fun TaskCard(
    tasks: List<ScheduledTask>,
    selected: Boolean,
    locked: Boolean,
    active: Boolean,
    runtimeTaskId: String?,
    runtimeProgress: Int,
    onSelect: () -> Unit,
    onStartStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReset: () -> Unit,
) {
    val task = tasks.first()
    var menu by remember { mutableStateOf(false) }
    val progress = tasks.sumOf { row -> if (row.id == runtimeTaskId) runtimeProgress else row.progress }
    val totalLimit = tasks.sumOf(ScheduledTask::totalLimit)
    val accountLabel = if (tasks.size == 1) task.username else {
        "${tasks.size} hesap • ${tasks.joinToString(", ") { it.username }.take(72)}"
    }
    val logicalStatus = when {
        tasks.any { it.status == TaskStatus.RUNNING } -> TaskStatus.RUNNING
        tasks.all { it.status == TaskStatus.COMPLETED } -> TaskStatus.COMPLETED
        tasks.any { it.status == TaskStatus.FAILED } -> TaskStatus.FAILED
        tasks.any { it.status == TaskStatus.PAUSED } -> TaskStatus.PAUSED
        else -> TaskStatus.QUEUED
    }
    val group = taskGroup(task.type)
    val accent = taskColor(task.type)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (selected) AtmacaBlue.copy(alpha = 0.5f) else Divider),
        shadowElevation = if (selected || active) 3.dp else 1.dp,
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = selected, onCheckedChange = { onSelect() }, enabled = !locked)
                TaskIconBubble(taskIcon(task.type), accent, taskSoftColor(task.type))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            group.shortTitle.uppercase(),
                            color = accent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp,
                        )
                        if (active) {
                            Spacer(Modifier.width(7.dp))
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Success))
                            Spacer(Modifier.width(4.dp))
                            Text("ÇALIŞIYOR", color = Success, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(task.type.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text(accountLabel, color = TextSecondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Görev menüsü") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Düzenle") },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                            enabled = !locked,
                            onClick = { menu = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("İlerlemeyi sıfırla") },
                            leadingIcon = { Icon(Icons.Filled.RestartAlt, null) },
                            enabled = !locked && tasks.any { it.progress > 0 },
                            onClick = { menu = false; onReset() },
                        )
                        DropdownMenuItem(
                            text = { Text("Sil") },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            enabled = !locked,
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TaskMetric("Döngü", task.repeatCount.toString(), Icons.Filled.Repeat, Modifier.weight(1f))
                TaskMetric("Limit", task.limit.toString(), Icons.Filled.TaskAlt, Modifier.weight(1f))
                TaskMetric("Aralık", "${task.intervalMinutes} dk", Icons.Filled.Schedule, Modifier.weight(1f))
            }
            if (task.targetUrl?.isNotBlank() == true) {
                Spacer(Modifier.height(9.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = ScreenBackground) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Link, null, Modifier.size(14.dp), tint = TextSecondary)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            task.targetUrl.orEmpty(),
                            color = TextSecondary,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (task.useGemini) {
                Spacer(Modifier.height(7.dp))
                Text(
                    "Gemini • ${task.contentPrompt.orEmpty().take(90)}",
                    color = Purple,
                    fontSize = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("İlerleme", color = TextSecondary, fontSize = 9.sp, modifier = Modifier.weight(1f))
                Text("$progress / $totalLimit", color = accent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = {
                    if (totalLimit > 0) {
                        progress.toFloat().coerceAtMost(totalLimit.toFloat()) / totalLimit
                    } else {
                        0f
                    }
                },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = accent,
                trackColor = taskSoftColor(task.type),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${tasks.sumOf(::successfulCycleCount)} başarılı hesap döngüsü",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(statusLabel(logicalStatus), color = if (active) Success else TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(11.dp))
            Button(
                onClick = onStartStop,
                enabled = active || (!locked && tasks.any { it.status != TaskStatus.COMPLETED && it.progress < it.totalLimit }),
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (active) Color(0xFFD92D20) else AtmacaNavy),
            ) {
                Icon(if (active) Icons.Filled.Stop else Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text(if (active) "Görevi Durdur" else "Görevi Başlat", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TaskMetric(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(13.dp), color = ScreenBackground) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, color = TextSecondary, fontSize = 8.sp, maxLines = 1)
            }
            Spacer(Modifier.height(2.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun TaskEditorDialog(
    originals: List<ScheduledTask>?,
    accounts: List<Account>,
    defaultFollowLimit: Int,
    defaultUnfollowLimit: Int,
    onDismiss: () -> Unit,
    onSave: (List<ScheduledTask>) -> Unit,
) {
    val original = originals?.firstOrNull()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val windowClass = rememberAtmacaWindowClass()
    val editorListState = rememberLazyListState()
    val editorMaxHeight = (configuration.screenHeightDp * if (windowClass == AtmacaWindowClass.COMPACT) 0.68f else 0.74f).dp
    val availableAccounts = accounts.distinctBy { it.username.lowercase() }.take(10)
    var selectedAccountIds by remember(originals, availableAccounts) {
        mutableStateOf(originals?.mapTo(linkedSetOf(), ScheduledTask::accountId) ?: setOfNotNull(availableAccounts.firstOrNull { it.isCurrent }?.id ?: availableAccounts.firstOrNull()?.id))
    }
    var selectedTypes by remember(original) { mutableStateOf(setOf(original?.type ?: TaskType.TEXT_TWEET)) }
    var limitText by remember(original) {
        mutableStateOf(
            (original?.limit ?: if (original?.type == TaskType.UNFOLLOW) defaultUnfollowLimit else defaultFollowLimit).toString(),
        )
    }
    var repeatText by remember(original) { mutableStateOf((original?.repeatCount ?: 1).toString()) }
    var intervalText by remember(original) { mutableStateOf((original?.intervalMinutes ?: 5).toString()) }
    var targetUrl by remember(original) { mutableStateOf(original?.targetUrl.orEmpty()) }
    var prompt by remember(original) { mutableStateOf(original?.contentPrompt.orEmpty()) }
    var content by remember(original) { mutableStateOf(original?.contentText.orEmpty()) }
    var mediaUri by remember(original) { mutableStateOf(original?.mediaUri) }
    var useGemini by remember(original) { mutableStateOf(original?.useGemini ?: false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            mediaUri = uri.toString()
        }
    }

    val needsLink = selectedTypes.any(TaskType::requiresLink)
    val needsContent = selectedTypes.any(TaskType::supportsGemini)
    val needsImage = TaskType.IMAGE_TWEET in selectedTypes
    val scalable = selectedTypes.any { it in SCALABLE_TYPES }
    val limit = limitText.toIntOrNull()
    val repeats = repeatText.toIntOrNull()
    val interval = intervalText.toIntOrNull()
    val valid = selectedAccountIds.isNotEmpty() && selectedTypes.isNotEmpty() &&
        limit != null && limit in 1..100 && repeats != null && repeats in 1..100 && interval != null && interval in 1..1_440 &&
        (!needsLink || validXUrl(targetUrl)) &&
        (!needsContent || if (useGemini) prompt.isNotBlank() else content.isNotBlank()) &&
        (!needsImage || !mediaUri.isNullOrBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(max = if (windowClass == AtmacaWindowClass.EXPANDED) 760.dp else 640.dp)
            .imePadding(),
        icon = {
            Box(Modifier.size(48.dp).clip(CircleShape).background(SoftBlue), contentAlignment = Alignment.Center) {
                Icon(if (original == null) Icons.Filled.Add else Icons.Filled.Edit, null, tint = AtmacaBlue)
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (original == null) "Yeni Görev Akışı" else "Görevi Düzenle", fontWeight = FontWeight.Bold)
                Text(
                    if (original == null) "Hesap, görev ve döngü ayarlarını tamamla" else "Mevcut görevin çalışma ayarlarını güncelle",
                    color = TextSecondary,
                    fontSize = 10.sp,
                )
            }
        },
        text = {
            LazyColumn(
                state = editorListState,
                modifier = Modifier.fillMaxWidth().heightIn(max = editorMaxHeight),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "accounts-title") { Text("Hesaplar", fontWeight = FontWeight.Bold) }
                items(availableAccounts, key = { it.id }) { account ->
                    val checked = account.id in selectedAccountIds
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = account.active) {
                            selectedAccountIds = if (checked) selectedAccountIds - account.id else selectedAccountIds + account.id
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selectedAccountIds = if (checked) selectedAccountIds - account.id else selectedAccountIds + account.id
                            },
                            enabled = account.active,
                        )
                        Text(account.username, modifier = Modifier.weight(1f))
                        if (account.isCurrent) Text("X'te açık", color = Success, fontSize = 9.sp)
                    }
                }

                item(key = "types-title") {
                    HorizontalDivider()
                    Spacer(Modifier.height(5.dp))
                    Text("Görev türleri", fontWeight = FontWeight.Bold)
                    Text("3 kategori • ${selectedTypes.size} tür seçili", color = TextSecondary, fontSize = 9.sp)
                }
                TaskGroup.entries.forEach { group ->
                    item(key = "group-${group.name}") {
                        Surface(
                            shape = RoundedCornerShape(15.dp),
                            color = taskGroupSoftColor(group),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                TaskIconBubble(groupIcon(group), taskGroupColor(group), Color.White.copy(alpha = 0.78f))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(group.title, color = taskGroupColor(group), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        when (group) {
                                            TaskGroup.FOLLOW -> "Kitle büyütme ve takip temizliği"
                                            TaskGroup.ENGAGEMENT -> "Bağlantı üzerinden tekil X işlemleri"
                                            TaskGroup.CONTENT -> "Metin, görsel ve alıntı yayınlama"
                                        },
                                        color = TextSecondary,
                                        fontSize = 8.sp,
                                    )
                                }
                                val count = selectedTypes.count { taskGroup(it) == group }
                                if (count > 0) Text("$count seçili", color = taskGroupColor(group), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    items(TASK_TYPES_BY_GROUP.getValue(group), key = { "${group.name}-${it.name}" }) { type ->
                        val checked = type in selectedTypes
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (checked) taskSoftColor(type) else Color.White,
                            border = BorderStroke(1.dp, if (checked) taskColor(type).copy(alpha = 0.32f) else Divider),
                            modifier = Modifier.fillMaxWidth().clickable(enabled = original == null) {
                                selectedTypes = if (checked) selectedTypes - type else selectedTypes + type
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        if (original == null) selectedTypes = if (checked) selectedTypes - type else selectedTypes + type
                                    },
                                    enabled = original == null,
                                )
                                Icon(taskIcon(type), null, Modifier.size(18.dp), tint = taskColor(type))
                                Spacer(Modifier.width(8.dp))
                                Text(type.title, modifier = Modifier.weight(1f), fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium)
                                if (checked) Icon(Icons.Filled.CheckCircle, null, Modifier.size(17.dp), tint = taskColor(type))
                            }
                        }
                    }
                }

                if (needsLink) {
                    item(key = "target-link") {
                        OutlinedTextField(
                            value = targetUrl,
                            onValueChange = { targetUrl = it.trim() },
                            label = { Text("X bağlantısı") },
                            placeholder = { Text("https://x.com/.../status/...") },
                            isError = targetUrl.isNotBlank() && !validXUrl(targetUrl),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }

                if (needsContent) {
                    item(key = "gemini-switch") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Gemini ile ayrı içerikler", fontWeight = FontWeight.SemiBold)
                                Text("Her döngü/hesap için farklı metin üretir; navigasyona karar vermez.", color = TextSecondary, fontSize = 9.sp)
                            }
                            Switch(checked = useGemini, onCheckedChange = { useGemini = it })
                        }
                    }
                    item(key = if (useGemini) "gemini-prompt" else "manual-content") {
                        if (useGemini) {
                            OutlinedTextField(
                                value = prompt,
                                onValueChange = { prompt = it },
                                label = { Text("Konu / anlatım / kaynak tweet özeti") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                            )
                        } else {
                            OutlinedTextField(
                                value = content,
                                onValueChange = { content = it.take(280) },
                                label = { Text("Yayınlanacak metin") },
                                supportingText = { Text("${content.length}/280") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                            )
                        }
                    }
                }

                if (needsImage) {
                    item(key = "image-picker") {
                        OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Image, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (mediaUri.isNullOrBlank()) "Görsel Seç" else "Görsel Seçildi • Değiştir")
                        }
                    }
                }

                item(key = "limits-and-timing") {
                    val fields: @Composable (Modifier) -> Unit = { fieldModifier ->
                        Column(fieldModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = if (scalable) limitText else "1",
                            onValueChange = { if (scalable) limitText = it.filter(Char::isDigit).take(3) },
                            label = { Text("Limit") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = scalable,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = repeatText,
                            onValueChange = { repeatText = it.filter(Char::isDigit).take(3) },
                            label = { Text("Tekrar") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = intervalText,
                            onValueChange = { intervalText = it.filter(Char::isDigit).take(4) },
                            label = { Text("Aralık dk") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        }
                    }
                    if (windowClass == AtmacaWindowClass.COMPACT) {
                        fields(Modifier.fillMaxWidth())
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = if (scalable) limitText else "1",
                                onValueChange = { if (scalable) limitText = it.filter(Char::isDigit).take(3) },
                                label = { Text("Limit") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = scalable,
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
                    }
                }
                item(key = "timing-help") {
                    Text(
                        "Saat alanı kaldırıldı. Çakışmalar tek görev kilidiyle önlenir; tekrarlar dakika aralığı dolmadan yeni X işlemi yapmaz.",
                        color = TextSecondary,
                        fontSize = 9.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                shape = RoundedCornerShape(13.dp),
                onClick = {
                    val selectedAccounts = availableAccounts.filter { it.id in selectedAccountIds }
                    val tasks = if (original != null) {
                        val perCycle = if (scalable) limit!! else 1
                        val existingByAccount = originals.orEmpty().associateBy(ScheduledTask::accountId)
                        val groupKey = originals.orEmpty().firstNotNullOfOrNull { it.time.takeIf { value -> value.startsWith(LOGICAL_TASK_PREFIX) } }
                            ?: "$LOGICAL_TASK_PREFIX${UUID.randomUUID()}:${original.type.name}"
                        selectedAccounts.map { account ->
                            val existing = existingByAccount[account.id]
                            (existing ?: original.copy(id = UUID.randomUUID().toString(), accountId = account.id, username = account.username, progress = 0)).copy(
                                accountId = account.id,
                                username = account.username,
                                time = groupKey,
                                limit = perCycle,
                                repeatCount = repeats!!,
                                intervalMinutes = interval!!,
                                targetUrl = targetUrl.takeIf(String::isNotBlank),
                                contentPrompt = prompt.takeIf(String::isNotBlank),
                                contentText = content.takeIf(String::isNotBlank),
                                mediaUri = mediaUri,
                                useGemini = useGemini,
                                progress = (existing?.progress ?: 0).coerceAtMost(perCycle * repeats!!),
                            )
                        }
                    } else {
                        val batchId = UUID.randomUUID().toString()
                        buildList {
                            selectedTypes.forEach { type ->
                                val groupKey = "$LOGICAL_TASK_PREFIX$batchId:${type.name}"
                                selectedAccounts.forEach { account ->
                                    val perCycle = if (type in SCALABLE_TYPES) limit!! else 1
                                    add(
                                        ScheduledTask(
                                            id = UUID.randomUUID().toString(),
                                            accountId = account.id,
                                            username = account.username,
                                            time = groupKey,
                                            type = type,
                                            status = TaskStatus.QUEUED,
                                            progress = 0,
                                            limit = perCycle,
                                            repeatCount = repeats!!,
                                            intervalMinutes = interval!!,
                                            targetUrl = targetUrl.takeIf { type.requiresLink && it.isNotBlank() },
                                            contentPrompt = prompt.takeIf { type.supportsGemini && it.isNotBlank() },
                                            contentText = content.takeIf { type.supportsGemini && it.isNotBlank() },
                                            mediaUri = mediaUri.takeIf { type == TaskType.IMAGE_TWEET },
                                            useGemini = type.supportsGemini && useGemini,
                                        )
                                    )
                                }
                            }
                        }
                    }
                    onSave(tasks)
                },
            ) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (original == null) "Görevleri Oluştur" else "Değişiklikleri Kaydet")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

private fun validXUrl(value: String): Boolean = runCatching {
    val uri = android.net.Uri.parse(value.trim())
    uri.scheme in setOf("https", "http") && uri.host?.lowercase() in setOf("x.com", "www.x.com", "twitter.com", "www.twitter.com")
}.getOrDefault(false)

@Composable
private fun TaskIconBubble(icon: ImageVector, color: Color, background: Color) {
    Box(
        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
}

private fun successfulCycleCount(task: ScheduledTask): Int {
    if (task.limit <= 0) return 0
    return (task.progress / task.limit).coerceIn(0, task.repeatCount.coerceAtLeast(0))
}

private fun taskGroup(type: TaskType): TaskGroup = when (type) {
    TaskType.UNFOLLOW,
    TaskType.VERIFIED_FOLLOW,
    TaskType.COMMENTER_FOLLOW,
    TaskType.RETWEETER_FOLLOW,
    TaskType.QUOTER_FOLLOW -> TaskGroup.FOLLOW

    TaskType.FOLLOW,
    TaskType.LIKE,
    TaskType.RETWEET,
    TaskType.BOOKMARK,
    TaskType.COMMENT -> TaskGroup.ENGAGEMENT

    else -> TaskGroup.CONTENT
}

private fun groupIcon(group: TaskGroup): ImageVector = when (group) {
    TaskGroup.FOLLOW -> Icons.Filled.Groups
    TaskGroup.ENGAGEMENT -> Icons.Filled.Bolt
    TaskGroup.CONTENT -> Icons.Filled.AutoAwesome
}

private fun taskGroupColor(group: TaskGroup): Color = when (group) {
    TaskGroup.FOLLOW -> Teal
    TaskGroup.ENGAGEMENT -> AtmacaBlue
    TaskGroup.CONTENT -> Purple
}

private fun taskGroupSoftColor(group: TaskGroup): Color = when (group) {
    TaskGroup.FOLLOW -> SoftGreen
    TaskGroup.ENGAGEMENT -> SoftBlue
    TaskGroup.CONTENT -> SoftPurple
}

private fun taskSoftColor(type: TaskType): Color = when (taskGroup(type)) {
    TaskGroup.FOLLOW -> if (type == TaskType.UNFOLLOW) SoftOrange else SoftGreen
    TaskGroup.ENGAGEMENT -> if (type == TaskType.LIKE) Color(0xFFFFEDF4) else SoftBlue
    TaskGroup.CONTENT -> SoftPurple
}

private fun statusLabel(status: TaskStatus): String = when (status) {
    TaskStatus.QUEUED -> "HAZIR"
    TaskStatus.RUNNING -> "ÇALIŞIYOR"
    TaskStatus.PAUSED -> "DURAKLATILDI"
    TaskStatus.COMPLETED -> "TAMAMLANDI"
    TaskStatus.FAILED -> "HATA"
}

private fun queueStatusLabel(status: QueueStatus): String = when (status) {
    QueueStatus.IDLE -> "Hazır"
    QueueStatus.PREPARING -> "Hazırlanıyor"
    QueueStatus.RUNNING -> "Çalışıyor"
    QueueStatus.BETWEEN_TASKS -> "Sıradaki görev"
    QueueStatus.PAUSED -> "Duraklatıldı"
    QueueStatus.STOPPED -> "Durduruldu"
    QueueStatus.COMPLETED -> "Tamamlandı"
    QueueStatus.PARTIAL -> "Kısmi"
    QueueStatus.FAILED -> "Hata"
}

private fun taskIcon(type: TaskType): ImageVector = when (type) {
    TaskType.TEXT_TWEET, TaskType.PUBLISH -> Icons.Filled.Send
    TaskType.IMAGE_TWEET -> Icons.Filled.Image
    TaskType.FOLLOW, TaskType.COMMENTER_FOLLOW, TaskType.RETWEETER_FOLLOW, TaskType.QUOTER_FOLLOW -> Icons.Filled.PersonAdd
    TaskType.LIKE -> Icons.Filled.Favorite
    TaskType.RETWEET, TaskType.QUOTE -> Icons.Filled.Repeat
    TaskType.BOOKMARK -> Icons.Filled.Bookmark
    TaskType.COMMENT -> Icons.Filled.ChatBubble
    TaskType.UNFOLLOW -> Icons.Filled.PersonRemove
    TaskType.VERIFIED_FOLLOW -> Icons.Filled.Verified
    else -> Icons.Filled.AutoAwesome
}

private fun taskColor(type: TaskType): Color = when (type) {
    TaskType.UNFOLLOW -> Warning
    TaskType.VERIFIED_FOLLOW -> Teal
    TaskType.COMMENTER_FOLLOW, TaskType.RETWEETER_FOLLOW, TaskType.QUOTER_FOLLOW -> Purple
    TaskType.LIKE -> Color(0xFFE34883)
    TaskType.BOOKMARK -> AtmacaNavy
    TaskType.TEXT_TWEET, TaskType.IMAGE_TWEET, TaskType.QUOTE -> Purple
    else -> AtmacaBlue
}
