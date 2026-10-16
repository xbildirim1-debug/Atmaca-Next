package com.atmacanext.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.data.settings.ReplyTextPool
import com.atmacanext.app.data.settings.TweetTextPool
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.QueueStatus
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TargetKind
import com.atmacanext.app.domain.model.TaskStatus
import com.atmacanext.app.domain.model.TaskType
import com.atmacanext.app.ui.theme.Success
import com.atmacanext.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.UUID

private enum class BulkKind(
    val title: String,
    val type: TaskType,
    val needsLink: Boolean = false,
    val needsTweetPool: Boolean = false,
    val needsCommentPool: Boolean = false,
    val needsQuoteTargets: Boolean = false,
) {
    TWEET("Tweet", TaskType.TEXT_TWEET, needsTweetPool = true),
    COMMENT("Yorum", TaskType.COMMENT_QUOTE_TARGETS, needsCommentPool = true, needsQuoteTargets = true),
    FOLLOW("Takip", TaskType.FOLLOW, needsLink = true),
    LIKE("Beğeni", TaskType.LIKE, needsLink = true),
    RETWEET("Retweet", TaskType.RETWEET, needsLink = true),
    UNFOLLOW("Çıkarma", TaskType.UNFOLLOW_NON_FOLLOWERS),
    VERIFIED("Onaylı", TaskType.VERIFIED_FOLLOW),
}

@Composable
fun TweetTaskScreen26_81(modifier: Modifier = Modifier, accounts: List<Account>, locked: Boolean) {
    BulkTaskBody(modifier, accounts, locked, focus = BulkKind.TWEET)
}

@Composable
fun BulkTaskScreen26_80(modifier: Modifier = Modifier, accounts: List<Account>, locked: Boolean) {
    BulkTaskBody(modifier, accounts, locked, focus = null)
}

@Composable
private fun BulkTaskBody(modifier: Modifier, accounts: List<Account>, locked: Boolean, focus: BulkKind?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tasks by AppServices.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val targets by AppServices.repository.targetAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val available = accounts.filter { it.active }.distinctBy { it.username.lowercase() }.take(10)
    var kind by remember { mutableStateOf(focus ?: BulkKind.TWEET) }
    val activeKind = focus ?: kind
    var selected by remember(available) {
        mutableStateOf(setOfNotNull(available.firstOrNull { it.isCurrent }?.id ?: available.firstOrNull()?.id))
    }
    var limitText by remember { mutableStateOf("1") }
    var link by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }
    var note by remember { mutableStateOf<String?>(null) }
    val singlePost = activeKind.type in setOf(TaskType.LIKE, TaskType.RETWEET, TaskType.BOOKMARK)
    val limit = if (singlePost) 1 else limitText.toIntOrNull()
    val linkOk = !activeKind.needsLink || if (singlePost) com.atmacanext.app.automation.AccountEngagementTweetPolicy.validTweetUrl(link) else link.trim().let { it.startsWith("http") && (it.contains("x.com") || it.contains("twitter.com")) }
    val poolOk = when {
        activeKind.needsTweetPool -> TweetTextPool.count(context) > 0
        activeKind.needsCommentPool -> ReplyTextPool.count(context) > 0 || extra.isNotBlank()
        else -> true
    }
    val targetsOk = !activeKind.needsQuoteTargets || selected.all { id ->
        targets.any { it.ownerAccountId == id && it.kind == TargetKind.QUOTE && it.active }
    }
    val valid = !locked && selected.isNotEmpty() && limit != null && limit in 1..20 && linkOk && poolOk && targetsOk
    val mine = tasks.filter { it.type == activeKind.type }

    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (focus == BulkKind.TWEET) "Tweet" else "Toplu görev", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            if (focus == BulkKind.TWEET) "Seçilen her hesap, ayarlardaki sıradaki tweeti atar."
            else "Seçtiğin hesaplara aynı görevi yazar. Tweet, yorum, takip, beğeni, retweet, çıkarma ve onaylı takip.",
            color = TextSecondary,
            fontSize = 12.sp,
        )
        if (focus == null) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BulkKind.entries.forEach { item ->
                    FilterChip(selected = activeKind == item, onClick = { kind = item }, label = { Text(item.title, fontSize = 12.sp) })
                }
            }
        }
        if (activeKind.needsTweetPool && TweetTextPool.count(context) == 0) {
            Text("Önce Ayarlar'dan tweet dosyası yükle.", color = TextSecondary, fontSize = 12.sp)
        }
        if (activeKind.needsCommentPool && ReplyTextPool.count(context) == 0) {
            Text("Yorum havuzu boşsa aşağıya bir cümle yaz.", color = TextSecondary, fontSize = 12.sp)
        }
        available.forEach { account ->
            val checked = account.id in selected
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = {
                    selected = if (checked) selected - account.id else selected + account.id
                })
                Text(account.username, fontSize = 14.sp)
                if (activeKind.needsQuoteTargets && targets.none { it.ownerAccountId == account.id && it.kind == TargetKind.QUOTE && it.active }) {
                    Spacer(Modifier.width(8.dp))
                    Text("hedef yok", color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        }
        if (activeKind.needsLink) {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it.take(240) },
                label = { Text(if (singlePost) "X gönderi bağlantısı" else "X bağlantısı") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (activeKind.needsCommentPool) {
            OutlinedTextField(
                value = extra,
                onValueChange = { extra = it.take(280) },
                label = { Text("Yorumun sonuna eklenecek söz, boş kalabilir") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = if (singlePost) "1" else limitText,
            enabled = !singlePost,
            onValueChange = { limitText = it.filter(Char::isDigit).take(2) },
            label = { Text("Adet") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        note?.let { Text(it, color = Success, fontSize = 12.sp) }
        Button(
            onClick = {
                val batch = UUID.randomUUID().toString()
                val created = available.filter { it.id in selected }.map { account ->
                    ScheduledTask(
                        id = UUID.randomUUID().toString(),
                        accountId = account.id,
                        username = account.username,
                        time = "group:$batch:${activeKind.type.name}",
                        type = activeKind.type,
                        status = TaskStatus.QUEUED,
                        limit = limit ?: 1,
                        repeatCount = 1,
                        intervalMinutes = 1,
                        targetUrl = link.trim().ifBlank { null },
                        contentText = extra.trim(),
                    )
                }
                scope.launch {
                    runCatching { AppServices.repository.upsertTasks(created) }
                        .onSuccess { note = "${created.size} görev yazıldı" }
                        .onFailure { note = it.message ?: "Görev yazılamadı" }
                }
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.width(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Oluştur")
        }
        if (locked) Text("Çalışan görev bitmeden yenisi eklenmez.", color = TextSecondary, fontSize = 12.sp)
        mine.groupBy { it.time }.values.forEach { group ->
            val task = group.first()
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(group.joinToString(" · ") { it.username }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${task.type.title} · ${task.limit} adet", color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { scope.launch { AppServices.repository.deleteTasks(group.map { it.id }) } }, enabled = !locked) {
                        Text("Sil")
                    }
                    Button(
                        onClick = { AppServices.orchestrator.startSelection(group) },
                        enabled = !locked && queue.status != QueueStatus.RUNNING && group.any { it.progress < it.totalLimit },
                    ) { Text("Başlat") }
                }
            }
        }
        if (available.isEmpty()) Text("Önce Hesaplar'dan Tara.", color = TextSecondary, fontSize = 12.sp)
    }
}
