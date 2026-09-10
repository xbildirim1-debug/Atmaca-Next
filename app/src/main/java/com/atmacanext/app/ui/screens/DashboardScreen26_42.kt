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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.atmacanext.app.ui.components.AtmacaCard
import com.atmacanext.app.ui.components.CircleIcon
import com.atmacanext.app.ui.components.SectionTitle
import com.atmacanext.app.ui.components.StatusPill
import com.atmacanext.app.ui.theme.AtmacaBlue
import com.atmacanext.app.ui.theme.AtmacaNavy
import com.atmacanext.app.ui.theme.Success
import com.atmacanext.app.ui.theme.TextSecondary
import com.atmacanext.app.ui.theme.Warning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DASHBOARD_GROUP_PREFIX_26_42 = "group:"
private fun dashboardTaskKey26_42(task: ScheduledTask): String =
    task.time.takeIf { it.startsWith(DASHBOARD_GROUP_PREFIX_26_42) } ?: "task:${task.id}"

@Composable
fun DashboardScreen26_42(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val accessibility by AccessibilityServiceState.health.collectAsStateWithLifecycle()
    val runtime by AutomationController.state.collectAsStateWithLifecycle()
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val accounts by AppServices.repository.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by AppServices.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val logs by AppServices.repository.recentLogs.collectAsStateWithLifecycle(initialValue = emptyList())
    val sync by AccountSyncController.state.collectAsStateWithLifecycle()
    val notifications by AppServices.notifications.notifications.collectAsStateWithLifecycle()
    val currentAccount = accounts.firstOrNull { it.isCurrent }
    val taskCount = tasks.asSequence().map(::dashboardTaskKey26_42).distinct().count()
    val unread = notifications.count { !it.read }
    val scope = rememberCoroutineScope()
    var accountMenu by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }

    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text("Bildirimler") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (notifications.isEmpty()) Text("Henüz bildirim yok", color = TextSecondary)
                    else notifications.take(8).forEach { event ->
                        Text(event.username, color = AtmacaBlue, fontWeight = FontWeight.Bold)
                        Text(event.message, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNotifications = false }) { Text("Kapat") } },
        )
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
                            Icon(Icons.Filled.Notifications, contentDescription = "Bildirimler")
                        }
                    }
                }
            }

            item {
                AtmacaCard {
                    val statusText = when {
                        queue.status == QueueStatus.PAUSED -> "Duraklatıldı"
                        queue.isActive -> "Çalışıyor"
                        !accessibility.enabled -> "Servis kapalı"
                        !accessibility.connected -> "Bağlantı bekleniyor"
                        else -> "Hazır"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Otomasyon durumu", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                queue.currentItem?.let { "${it.username} • ${it.taskType.title}" }
                                    ?: "Hesap, görev ve kuyruk durumunun kısa özeti",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        StatusPill(
                            statusText,
                            accessibility.operational && queue.status !in setOf(
                                QueueStatus.PAUSED, QueueStatus.STOPPED, QueueStatus.PARTIAL, QueueStatus.FAILED,
                            ),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardMiniMetric26_42("AKTİF HESAP", accounts.count { it.active }.toString(), Modifier.weight(1f))
                        DashboardMiniMetric26_42("GÖREV", taskCount.toString(), Modifier.weight(1f))
                        DashboardMiniMetric26_42(
                            "KUYRUK",
                            if (queue.items.isEmpty()) "—" else "${(queue.currentIndex + 1).coerceAtLeast(0)}/${queue.items.size}",
                            Modifier.weight(1f),
                        )
                    }
                    logs.firstOrNull()?.let { last ->
                        Spacer(Modifier.height(10.dp))
                        Text("Son kayıt: ${last.message}", color = TextSecondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (!accessibility.enabled) {
                item {
                    AtmacaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Shield, null, tint = Warning)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Motor Durumu", fontWeight = FontWeight.Bold)
                                Text("Erişilebilirlik kapalı", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Erişilebilirliği Aç")
                        }
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
                            Text(currentAccount?.username ?: "Henüz hesap seçilmedi", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                        text = { Text(account.username) },
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
                }
            }

            if (runtime.taskId != null || queue.isActive) {
                item {
                    AtmacaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (runtime.status == RuntimeStatus.PAUSED) Icons.Filled.PauseCircle else Icons.Filled.Flight, null, tint = AtmacaBlue)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (queue.items.size > 1) "Toplu Görev" else "Çalışan Görev", fontWeight = FontWeight.Bold)
                                Text("${runtime.username.orEmpty()} • ${runtime.taskType?.title.orEmpty()}", fontSize = 12.sp)
                            }
                            Text("${runtime.verifiedCount}/${runtime.limit}", fontWeight = FontWeight.Bold, color = AtmacaBlue)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(runtime.message, color = TextSecondary, fontSize = 11.sp)
                        if (queue.items.isNotEmpty()) {
                            Text("Alt iş ${(queue.currentIndex + 1).coerceAtLeast(1)}/${queue.items.size}", color = TextSecondary, fontSize = 10.sp)
                        }
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
                    DashboardMetricCard26_42("Doğrulanan", tasks.sumOf { it.progress }.toString(), "Toplam işlem", Modifier.weight(1f))
                    DashboardMetricCard26_42("Hata", logs.count { it.level == "ERROR" }.toString(), "Son 1000 kayıt", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DashboardMiniMetric26_42(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = TextSecondary, fontSize = 9.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
private fun DashboardMetricCard26_42(label: String, value: String, detail: String, modifier: Modifier) {
    AtmacaCard(modifier) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(detail, color = Success, fontSize = 10.sp)
    }
}
