package com.atmacanext.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.atmacanext.app.data.settings.ActivityStats
import com.atmacanext.app.data.settings.StatsRange
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
import com.atmacanext.app.data.notifications.AccountNotification
import com.atmacanext.app.domain.model.QueueStatus
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import com.atmacanext.app.automation.AudienceCount
import com.atmacanext.app.data.settings.AccountAvatarStore
import com.atmacanext.app.data.settings.AccountPulse
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.ui.components.AtmacaCard
import com.atmacanext.app.ui.components.CircleIcon
import com.atmacanext.app.ui.components.SectionTitle
import com.atmacanext.app.ui.components.StatusPill
import com.atmacanext.app.ui.theme.AtmacaBlue
import com.atmacanext.app.ui.theme.CardBackground
import com.atmacanext.app.ui.theme.BrandGreen
import com.atmacanext.app.ui.theme.AtmacaNavy
import com.atmacanext.app.ui.theme.Purple
import com.atmacanext.app.ui.theme.Success
import com.atmacanext.app.ui.theme.Teal
import com.atmacanext.app.ui.theme.TextPrimary
import com.atmacanext.app.ui.theme.TextSecondary
import com.atmacanext.app.ui.theme.Warning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DASHBOARD_GROUP_PREFIX_26_42 = "group:"
private val FOLLOW_USAGE = setOf("VERIFIED_FOLLOW", "FOLLOW", "COMMENTER_FOLLOW", "RETWEETER_FOLLOW", "QUOTER_FOLLOW")
private val LEAVE_USAGE = setOf("UNFOLLOW", "UNFOLLOW_NON_FOLLOWERS")
private fun dashboardTaskKey26_42(task: ScheduledTask): String =
    task.time.takeIf { it.startsWith(DASHBOARD_GROUP_PREFIX_26_42) } ?: "task:${task.id}"

@Composable
fun DashboardScreen26_42(modifier: Modifier = Modifier, tabReset: Int = 0) {
    val context = LocalContext.current
    val accessibility by AccessibilityServiceState.health.collectAsStateWithLifecycle()
    val runtime by AutomationController.state.collectAsStateWithLifecycle()
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val accounts by AppServices.repository.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by AppServices.repository.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val logs by AppServices.repository.recentLogs.collectAsStateWithLifecycle(initialValue = emptyList())
    val sync by AccountSyncController.state.collectAsStateWithLifecycle()
    val notifications by AppServices.notifications.notifications.collectAsStateWithLifecycle()
    val pulseRevision by AccountPulse.revision.collectAsStateWithLifecycle()
    var statsRangeName by rememberSaveable { mutableStateOf(StatsRange.DAY.name) }
    val statsRange = StatsRange.entries.firstOrNull { it.name == statsRangeName } ?: StatsRange.DAY
    var rangeFollow by remember { mutableIntStateOf(0) }
    var rangeLeave by remember { mutableIntStateOf(0) }
    var followsByAccount by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var followByDay by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var leaveByDay by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    LaunchedEffect(runtime.verifiedCount, accounts.size, queue.status, statsRange) {
        val rows = withContext(Dispatchers.IO) {
            val start = ActivityStats.startOf(statsRange, System.currentTimeMillis())
            val from = java.time.Instant.ofEpochMilli(start).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
            val today = java.time.LocalDate.now().toString()
            val ranged = runCatching { AppServices.repository.snapshotUsageSince(from) }.getOrDefault(emptyList())
            ranged to today
        }
        val (ranged, today) = rows
        val follows = ranged.filter { it.actionType in FOLLOW_USAGE }
        val leaves = ranged.filter { it.actionType in LEAVE_USAGE }
        rangeFollow = follows.sumOf { it.verifiedCount }
        rangeLeave = leaves.sumOf { it.verifiedCount }
        followsByAccount = follows.filter { it.dateKey == today }.groupingBy { it.accountId }.fold(0) { total, row -> total + row.verifiedCount }
        followByDay = follows.groupingBy { it.dateKey }.fold(0) { total, row -> total + row.verifiedCount }
        leaveByDay = leaves.groupingBy { it.dateKey }.fold(0) { total, row -> total + row.verifiedCount }
    }
    val currentAccount = accounts.firstOrNull { it.isCurrent }
    val taskCount = tasks.asSequence().map(::dashboardTaskKey26_42).distinct().count()
    val unread = notifications.count { !it.read }
    val scope = rememberCoroutineScope()
    var accountMenu by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    LaunchedEffect(tabReset) { if (tabReset > 0) showNotifications = false }
    val statsRevision by ActivityStats.revision.collectAsStateWithLifecycle()
    val totals = remember(statsRange, statsRevision, accounts, pulseRevision) {
        ActivityStats.totals(context, statsRange, accounts.map { it.username })
    }
    val followerTotal = remember(statsRevision, accounts, pulseRevision) {
        accounts.sumOf { account ->
            AccountPulse.read(context, account.username).followers
                ?: AudienceCount.parse(account.followers)
                ?: 0L
        }
    }
    val chart = remember(statsRange, statsRevision, accounts, pulseRevision, followByDay, leaveByDay) {
        val names = accounts.map { it.username }
        val base = ActivityStats.chart(context, statsRange, names)
        val zone = java.time.ZoneId.systemDefault()
        val follows = FloatArray(base.bucketStart.size)
        val leaves = FloatArray(base.bucketStart.size)
        fun place(days: Map<String, Int>, into: FloatArray) {
            days.forEach { (day, count) ->
                val time = runCatching { java.time.LocalDate.parse(day).atStartOfDay(zone).toInstant().toEpochMilli() }.getOrNull() ?: return@forEach
                val index = base.bucketStart.indexOfLast { it <= time }.coerceAtLeast(0)
                if (index < into.size) into[index] += count.toFloat()
            }
        }
        place(followByDay, follows)
        place(leaveByDay, leaves)
        listOf(
            ChartLine("Takip", AtmacaBlue, follows.toList()),
            ChartLine("Çıkış", Warning, leaves.toList()),
            ChartLine("Tweet", Purple, base.tweets.map { it.toFloat() }),
            ChartLine("Yorum", Teal, base.comments.map { it.toFloat() }),
            ChartLine("Takipçi", BrandGreen, base.gained.map { it.toFloat() }),
        )
    }
    val yieldRows = remember(followsByAccount, accounts, statsRevision, pulseRevision) {
        accounts.map { account ->
            AccountYield(account.username, followsByAccount[account.id] ?: 0, ActivityStats.gainedFor(context, account.username))
        }.sortedWith(
            compareBy<AccountYield> { if (it.follows == 0) 1 else 0 }
                .thenByDescending { if (it.follows == 0) 0.0 else it.gained.toDouble() / it.follows },
        )
    }
    LaunchedEffect(accounts.map { it.username }) {
        ActivityStats.seedBaselines(context, accounts.map { it.username })
    }

    if (showNotifications) {
        NotificationsPage26_08(modifier, notifications) { showNotifications = false }
        return
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 1_000.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Atmaca", fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        showNotifications = true
                        scope.launch { withContext(Dispatchers.IO) { AppServices.notifications.markAllRead() } }
                    }) {
                        BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 99) "99+" else unread.toString()) } }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Bildirimler", modifier = Modifier.size(20.dp))
                        }
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
                        ActiveAccountPhoto(currentAccount?.username)
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
                PeriodBoard(
                    range = statsRange,
                    onRange = { statsRangeName = it.name },
                    follow = rangeFollow,
                    leave = rangeLeave,
                    tweets = totals.tweets,
                    comments = totals.comments,
                    followers = totals.followers,
                    totalFollowers = followerTotal,
                    lines = chart,
                )
            }

            if (yieldRows.isNotEmpty()) {
                item { YieldList(yieldRows) }
            }
        }
    }
}

private data class AccountYield(val name: String, val follows: Int, val gained: Long)

@Composable
private fun YieldList(rows: List<AccountYield>) {
    Surface(Modifier.fillMaxWidth(), color = CardBackground, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Verim", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Bugün çok takip edip az kazanan alta iner.", color = TextSecondary, fontSize = 12.sp)
            if (rows.none { it.follows > 0 }) {
                Text("Bugün henüz takip yok.", color = TextSecondary, fontSize = 12.sp)
            }
            rows.forEach { row ->
                val worn = row.follows > 0 && row.gained <= 0L
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(row.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(signed(row.gained), color = if (worn) Warning else BrandGreen, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (row.follows == 0) "Bugün takip yok" else "${row.follows} takip",
                        color = if (worn) Warning else TextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

private data class ChartLine(val label: String, val color: Color, val values: List<Float>)

private data class RingSlice(val label: String, val value: String, val amount: Float, val color: Color)

@Composable
private fun PeriodBoard(
    range: StatsRange,
    onRange: (StatsRange) -> Unit,
    follow: Int,
    leave: Int,
    tweets: Int,
    comments: Int,
    followers: Long,
    totalFollowers: Long,
    lines: List<ChartLine>,
) {
    val slices = listOf(
        RingSlice("Takip", follow.toString(), follow.toFloat(), AtmacaBlue),
        RingSlice("Çıkış", leave.toString(), leave.toFloat(), Warning),
        RingSlice("Tweet", tweets.toString(), tweets.toFloat(), Purple),
        RingSlice("Yorum", comments.toString(), comments.toFloat(), Teal),
        RingSlice("Takipçi", signed(followers), kotlin.math.abs(followers.toFloat()), BrandGreen),
    )
    Surface(Modifier.fillMaxWidth(), color = CardBackground, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatsRange.entries.forEach { item ->
                    FilterChip(selected = range == item, onClick = { onRange(item) }, label = { Text(item.title, fontSize = 12.sp) })
                }
            }
            Spacer(Modifier.height(8.dp))
            MetricRing(slices)
            Text("Toplam takipçi ${grouped(totalFollowers)}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(8.dp))
            MultiSeriesChart(lines)
        }
    }
}

@Composable
private fun MetricRing(slices: List<RingSlice>) {
    val weights = slices.map { it.amount.coerceAtLeast(0f) }
    val sum = weights.sum()
    val parts = if (sum <= 0f) List(slices.size) { 1f } else weights
    val total = parts.sum().coerceAtLeast(1f)
    Box(Modifier.fillMaxWidth().height(248.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(236.dp)) {
            val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
            var start = -90f
            val gap = if (parts.count { it > 0f } > 1) 3f else 0f
            parts.forEachIndexed { index, weight ->
                val sweep = (weight / total) * 360f
                if (sweep > gap) {
                    drawArc(slices[index].color, start + gap / 2f, sweep - gap, false, style = stroke)
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            slices.forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(slice.color, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(slice.value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(slice.label, color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun MultiSeriesChart(lines: List<ChartLine>) {
    val drawable = lines.filter { it.values.size >= 2 && it.values.any { value -> value != 0f } }
    if (drawable.isEmpty()) {
        Text("Bu aralıkta çizilecek hareket yok.", color = TextSecondary, fontSize = 11.sp)
        return
    }
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        drawable.forEach { line ->
            val max = line.values.max().coerceAtLeast(1f)
            val min = line.values.min().coerceAtMost(0f)
            val span = (max - min).coerceAtLeast(1f)
            val step = size.width / (line.values.size - 1).coerceAtLeast(1)
            val path = Path()
            line.values.forEachIndexed { index, value ->
                val x = index * step
                val y = size.height - ((value - min) / span) * size.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, line.color, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        lines.forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(line.color, CircleShape))
                Spacer(Modifier.width(3.dp))
                Text(line.label, color = TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ActiveAccountPhoto(username: String?) {
    val context = LocalContext.current
    if (username.isNullOrBlank()) {
        CircleIcon(Icons.Filled.Person, androidx.compose.ui.graphics.Color(0xFFE7EBE3), AtmacaBlue)
        return
    }
    val file = AccountAvatarStore.file(context, username)
    val photo = remember(username, file.lastModified()) {
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
    if (photo != null) {
        Image(photo.asImageBitmap(), username, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(CircleShape))
    } else {
        CircleIcon(Icons.Filled.Person, androidx.compose.ui.graphics.Color(0xFFE7EBE3), AtmacaBlue)
    }
}

private fun signed(value: Long): String = when {
    value > 0 -> "+$value"
    else -> value.toString()
}

private fun grouped(value: Long): String =
    java.text.NumberFormat.getIntegerInstance(Locale("tr", "TR")).format(value)

@Composable
private fun DashboardMetricCard26_42(label: String, value: String, detail: String, modifier: Modifier) {
    AtmacaCard(modifier) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(detail, color = Success, fontSize = 10.sp)
    }
}

@Composable
private fun HomeAccountCard26_80(account: Account, revision: Int) {
    val context = LocalContext.current
    val pulse = remember(account.username, account.followers, account.following, revision) {
        AccountPulse.read(context, account.username)
    }
    val file = AccountAvatarStore.file(context, account.username)
    val photo = remember(account.username, file.lastModified()) {
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
    Surface(color = CardBackground, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (photo != null) {
                Image(
                    photo.asImageBitmap(),
                    contentDescription = account.username,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(58.dp).clip(CircleShape),
                )
            } else {
                CircleIcon(Icons.Filled.Person, androidx.compose.ui.graphics.Color(0xFFE7EBE3), AtmacaBlue)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account.username, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(
                        account.followers.ifBlank { "—" } + " takipçi",
                        account.following.ifBlank { "—" } + " takip",
                        (pulse.gainedFollowers?.let { if (it > 0) "+$it" else it.toString() } ?: "—") + " yeni",
                        pulse.tweetsPosted.toString() + " tweet",
                    ).joinToString("  ·  "),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeStat26_80(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun NotificationsPage26_08(
    modifier: Modifier,
    notifications: List<AccountNotification>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Column(modifier.fillMaxSize().widthIn(max = 1_000.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Geri") }
            Text("Bildirimler", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary, modifier = Modifier.weight(1f))
            IconButton(
                onClick = { copyNotificationText(context, notifications.joinToString("\n\n", transform = ::notificationText), "Tüm bildirimler kopyalandı") },
                enabled = notifications.isNotEmpty(),
            ) { Icon(Icons.Filled.ContentCopy, "Tümünü kopyala") }
            IconButton(
                onClick = { AppServices.notifications.clearAll() },
                enabled = notifications.isNotEmpty(),
            ) { Icon(Icons.Filled.Delete, "Tümünü sil") }
        }
        if (notifications.isEmpty()) {
            Text(
                "Henüz bildirim yok. Görev uyarıları burada durur, üstten bildirim gelmez.",
                color = TextSecondary,
                modifier = Modifier.padding(20.dp),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(notifications, key = { it.id }) { event ->
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = CardBackground) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(event.username, color = AtmacaBlue, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    copyNotificationText(context, notificationText(event), "Bildirim kopyalandı")
                                }) { Icon(Icons.Filled.ContentCopy, "Kopyala", Modifier.size(18.dp)) }
                            }
                            Text(event.message, fontSize = 14.sp, color = TextPrimary)
                            Text(notificationTime(event.createdAt), color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun notificationText(event: AccountNotification): String =
    "${event.username}\n${event.message}\n${notificationTime(event.createdAt)}"

private fun notificationTime(value: Long): String = runCatching {
    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(Date(value))
}.getOrDefault(value.toString())

private fun copyNotificationText(context: Context, value: String, toast: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Atmaca bildirim", value))
    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}
