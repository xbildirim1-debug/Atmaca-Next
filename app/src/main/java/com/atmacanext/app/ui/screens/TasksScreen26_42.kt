package com.atmacanext.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.domain.model.QueueItemStatus
import com.atmacanext.app.domain.model.QueueStatus
import com.atmacanext.app.ui.components.StatusPill
import com.atmacanext.app.ui.theme.AppBackground
import com.atmacanext.app.ui.theme.AtmacaBlue
import com.atmacanext.app.ui.theme.CardBackground
import com.atmacanext.app.ui.theme.Divider
import com.atmacanext.app.ui.theme.Purple
import com.atmacanext.app.ui.theme.Success
import com.atmacanext.app.ui.theme.TextSecondary

/**
 * A selected multi-task queue is shown as one logical task bar while it runs.
 * The existing task editor/list remains unchanged when no batch is active.
 */
@Composable
fun TasksScreen26_42(modifier: Modifier = Modifier) {
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val runtime by AutomationController.state.collectAsStateWithLifecycle()
    val batchActive = queue.isActive && queue.items.map { it.taskId }.distinct().size > 1

    if (!batchActive) {
        TasksScreen(modifier)
        return
    }

    val accounts = queue.items.distinctBy { it.accountId }
    val types = queue.items.map { it.taskType.title }.distinct()
    val terminalCount = queue.completedCount + queue.skippedCount + queue.failedCount
    val total = queue.items.size.coerceAtLeast(1)
    val overallProgress = terminalCount.toFloat() / total

    Box(modifier.fillMaxSize().background(AppBackground), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 1_000.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Görevler", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Toplu seçim tek görev çubuğu altında çalışıyor", color = TextSecondary, fontSize = 11.sp)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = CardBackground,
                    border = BorderStroke(1.dp, Purple.copy(alpha = 0.45f)),
                    shadowElevation = 3.dp,
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Purple.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.Lock, null, tint = Purple) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("TOPLU GÖREV", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(types.joinToString(" + "), fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${accounts.size} hesap • ${queue.items.size} alt iş", color = TextSecondary, fontSize = 10.sp)
                            }
                            StatusPill(
                                when (queue.status) {
                                    QueueStatus.PREPARING -> "Hazırlanıyor"
                                    QueueStatus.BETWEEN_TASKS -> "Geçiş"
                                    QueueStatus.PAUSED -> "Duraklatıldı"
                                    else -> "Çalışıyor"
                                },
                                queue.status != QueueStatus.PAUSED,
                            )
                        }

                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape),
                            trackColor = Divider,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Toplam ilerleme", color = TextSecondary, fontSize = 10.sp)
                            Text("$terminalCount / ${queue.items.size}", color = AtmacaBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        queue.currentItem?.let { current ->
                            Surface(shape = RoundedCornerShape(16.dp), color = AppBackground) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text("ŞİMDİ", color = Success, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("${current.username} • ${current.taskType.title}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(
                                        "Alt iş ${(queue.currentIndex + 1).coerceAtLeast(1)}/${queue.items.size}",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                    )
                                    if (runtime.taskId == current.taskId) {
                                        Text(
                                            "Görev içi ${runtime.verifiedCount}/${runtime.limit} • ${runtime.flowStage.name}",
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                        )
                                        Text(runtime.message, color = TextSecondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    } else {
                                        Text(queue.message, color = TextSecondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            accounts.forEach { account ->
                                val completedForAccount = queue.items.count {
                                    it.accountId == account.accountId && it.status == QueueItemStatus.COMPLETED
                                }
                                val totalForAccount = queue.items.count { it.accountId == account.accountId }
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(account.username, modifier = Modifier.weight(1f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "$completedForAccount/$totalForAccount",
                                        color = if (completedForAccount == totalForAccount) Success else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = AppServices.orchestrator::stop,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Filled.Stop, null)
                            Spacer(Modifier.width(7.dp))
                            Text("Toplu Görevi Durdur")
                        }
                    }
                }
            }

            item {
                Text(
                    "Kuyruk hesap bazlı ilerler: bir hesabın seçili görevleri bitmeden sonraki hesaba geçmez. Geçiş 20 saniye ilerlemezse kuyruk koruması yalnız kalan alt işleri yeniden kurar.",
                    color = TextSecondary,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
