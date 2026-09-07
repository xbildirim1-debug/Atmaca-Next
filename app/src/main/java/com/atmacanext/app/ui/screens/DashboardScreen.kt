package com.atmacanext.app.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.automation.AccessibilityServiceState
import com.atmacanext.app.automation.AccountSyncController
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.automation.RuntimeStatus
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.domain.model.QueueStatus
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskStatus
import com.atmacanext.app.ui.components.AtmacaCard
import com.atmacanext.app.ui.components.CircleIcon
import com.atmacanext.app.ui.components.SectionTitle
import com.atmacanext.app.ui.components.StatusPill
import com.atmacanext.app.ui.theme.AtmacaBlue
import com.atmacanext.app.ui.theme.AtmacaNavy
import com.atmacanext.app.ui.theme.Success
import com.atmacanext.app.ui.theme.TextSecondary
import com.atmacanext.app.ui.theme.Warning

private const val LOGICAL_TASK_PREFIX = "group:"

private fun dashboardLogicalTaskKey(task: ScheduledTask): String =
    task.time.takeIf { it.startsWith(LOGICAL_TASK_PREFIX) } ?: "task:${task.id}"

private fun dashboardLogicalTaskCount(tasks: List<ScheduledTask>): Int =
    tasks.asSequence().map(::dashboardLogicalTaskKey).distinct().count()

private fun dashboardCompletedLogicalTaskCount(tasks: List<ScheduledTask>): Int =
    tasks.groupBy(::dashboardLogicalTaskKey).count { (_, members) ->
        members.isNotEmpty() && members.all { it.status == TaskStatus.COMPLETED }
    }

@Composable
fun DashboardScreen(modifier: Modifier = Modifier, onAccounts: () -> Unit = {}, onTasks: () -> Unit = {}) {
    val context = LocalContext.current
    val accessibility by AccessibilityServiceState.health.collectAsStateWithLifecycle()
    val runtime by AutomationController.state.collectAsStateWithLifecycle()
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val accounts by AppServices.repository.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by AppServices.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val logs by AppServices.repository.recentLogs.collectAsStateWithLifecycle(initialValue = emptyList())
    val sync by AccountSyncController.state.collectAsStateWithLifecycle()
    val currentAccount = accounts.firstOrNull { it.isCurrent }
    var accountMenu by remember { mutableStateOf(false) }
    val notifications by AppServices.notifications.notifications.collectAsStateWithLifecycle()
    var showNotifications by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val unread = notifications.count { !it.read }
    if (showNotifications) {
        AlertDialog(onDismissRequest = { showNotifications = false }, title = { Text("Bildirimler") },
            text = {
                if (notifications.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Henüz bildirim yok", fontWeight = FontWeight.SemiBold)
                        Text("Hesaplarda durdurulan onaylı takip işlemleri ve sonraki hesaba geçişler burada görünecek.")
                    }
                } else LazyColumn(Modifier.height(420.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(notifications, key = { it.id }) { event ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(event.username, color = AtmacaBlue, fontWeight = FontWeight.Bold)
                            Text("Onaylı takip durduruldu", fontWeight = FontWeight.SemiBold)
                            Text(event.message, fontSize = 13.sp)
                            Text(SimpleDateFormat("dd MMM • HH:mm", Locale.forLanguageTag("tr-TR")).format(Date(event.createdAt)),
                                fontSize = 11.sp, color = TextSecondary)
                            HorizontalDivider()
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { showNotifications = false }) { Text("Kapat") } })
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 1_000.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIcon(Icons.Filled.Flight, AtmacaBlue)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Anasayfa", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("ATMACA NEXT", color = TextSecondary, fontSize = 11.sp)
                    }
                    IconButton(onClick = {
                        showNotifications = true
                        scope.launch { withContext(Dispatchers.IO) { AppServices.notifications.markAllRead() } }
                    }) {
                        BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 99) "99+" else unread.toString()) } }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Bildirimler, $unread okunmamış")
                        }
                    }
                }
            }
            item {
                AtmacaCard {
                    Text("Kontrol sende.", fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Text("Hesaplarını izle, görevlerini yönet.", color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    val statusText = when {
                        queue.status == QueueStatus.PAUSED -> "Duraklatıldı"
                        queue.isActive -> "Çalışıyor"
                        !accessibility.enabled -> "Servis kapalı"
                        !accessibility.connected -> "Bağlantı bekleniyor"
                        else -> "Hazır"
                    }
                    StatusPill(
                        statusText,
                        accessibility.operational && queue.status !in setOf(
                            QueueStatus.PAUSED,
                            QueueStatus.STOPPED,
                            QueueStatus.PARTIAL,
                            QueueStatus.FAILED,
                        ),
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onTasks, modifier = Modifier.weight(1f)) {
                        Text("Görevlere git")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.ArrowForward, null)
                    }
                    OutlinedButton(onClick = onAccounts, modifier = Modifier.weight(1f)) { Text("Hesaplarım") }
                }
            }

            // Kullanıcının isteği: Motor Durumu yalnız erişilebilirlik kapalıyken görünür.
            if (!accessibility.enabled) {
                item {
                    AtmacaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Shield, null, tint = Warning)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Motor Durumu", fontWeight = FontWeight.Bold)
                                Text("Erişilebilirlik kapalı. Android bu onayı bir kez sistem ayarından ister.", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Erişilebilirliği Aç") }
                    }
                }
            }

            item {
                SectionTitle("Aktif X Hesabı")
                AtmacaCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircleIcon(Icons.Filled.Person, AtmacaNavy)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(currentAccount?.username ?: "Henüz hesap seçilmedi", fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                currentAccount?.displayName?.ifBlank { "X uygulamasındaki açık hesap" }
                                    ?: "Hesaplar bölümünden X hesaplarını ekle",
                                color = TextSecondary,
                                fontSize = 11.sp,
                            )
                        }
                        Box {
                            OutlinedButton(
                                onClick = { accountMenu = true },
                                enabled = accounts.isNotEmpty() && !queue.isActive && !sync.active,
                            ) {
                                Text("Hesap seç")
                                Icon(Icons.Filled.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                                accounts.forEach { account ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(account.username)
                                                Text(
                                                    when {
                                                        account.isCurrent -> "X'te açık hesap"
                                                        !account.active -> "Pasif"
                                                        else -> account.displayName
                                                    },
                                                    color = TextSecondary,
                                                    fontSize = 10.sp,
                                                )
                                            }
                                        },
                                        enabled = account.active && !account.isCurrent,
                                        onClick = {
                                            accountMenu = false
                                            AccountSyncController.switchTo(account)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    currentAccount?.let { account ->
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Takipçi ${account.followers}", fontSize = 11.sp)
                            Text("Takip ${account.following}", fontSize = 11.sp)
                            StatusPill(if (account.active) "Aktif" else "Pasif", account.active)
                        }
                    }
                    if (sync.active || sync.completed || sync.failed) {
                        Spacer(Modifier.height(8.dp))
                        Text(sync.message, color = if (sync.failed) Warning else TextSecondary, fontSize = 10.sp)
                    }
                }
            }

            if (runtime.taskId != null || queue.isActive) {
                item {
                    AtmacaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (runtime.status == RuntimeStatus.PAUSED) Icons.Filled.PauseCircle else Icons.Filled.Flight, null, tint = AtmacaBlue)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Çalışan Görev", fontWeight = FontWeight.Bold)
                                Text("${runtime.username.orEmpty()} • ${runtime.taskType?.title.orEmpty()}", fontSize = 12.sp)
                            }
                            Text("${runtime.verifiedCount}/${runtime.limit}", fontWeight = FontWeight.Bold, color = AtmacaBlue)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(runtime.message, color = TextSecondary, fontSize = 11.sp)
                        Text("Döngü ${runtime.cycleIndex + 1}/${runtime.repeatCount}", color = TextSecondary, fontSize = 11.sp)
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = AppServices.orchestrator::stop, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Stop, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Görevi Durdur")
                        }
                    }
                }
            }

            item {
                SectionTitle("Özet")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Aktif Hesap", accounts.count { it.active }.toString(), "${accounts.size} toplam", Modifier.weight(1f))
                    MetricCard(
                        "Görev",
                        dashboardLogicalTaskCount(tasks).toString(),
                        "${dashboardCompletedLogicalTaskCount(tasks)} tamamlandı",
                        Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Doğrulanan", tasks.sumOf { it.progress }.toString(), "İşlem sonucu", Modifier.weight(1f))
                    MetricCard("Hata", logs.count { it.level == "ERROR" }.toString(), "Son 1000 log", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, detail: String, modifier: Modifier) {
    AtmacaCard(modifier) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(detail, color = Success, fontSize = 10.sp)
    }
}
