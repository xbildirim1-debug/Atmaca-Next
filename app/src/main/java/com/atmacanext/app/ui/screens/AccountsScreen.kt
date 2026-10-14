package com.atmacanext.app.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.automation.AccessibilityServiceState
import com.atmacanext.app.automation.AccountSyncController
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.data.settings.AccountAvatarStore
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.ui.theme.AppBackground
import com.atmacanext.app.ui.theme.AtmacaSky
import com.atmacanext.app.ui.theme.CardBackground
import com.atmacanext.app.ui.theme.Divider
import com.atmacanext.app.ui.theme.ErrorRed
import com.atmacanext.app.ui.theme.Success
import com.atmacanext.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun AccountsScreen(modifier: Modifier = Modifier) {
    val accounts by AppServices.repository.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val sync by AccountSyncController.state.collectAsStateWithLifecycle()
    val health by AccessibilityServiceState.health.collectAsStateWithLifecycle()
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val allTargets by AppServices.repository.targetAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var targetOwner by remember { mutableStateOf<Account?>(null) }
    var quoteOwner by remember { mutableStateOf<Account?>(null) }
    var deleteCandidate by remember { mutableStateOf<Account?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var connecting by remember { mutableStateOf(false) }
    val busy = sync.active || queue.isActive || connecting
    var photoStamp by remember { mutableIntStateOf(0) }
    LaunchedEffect(sync.completed) {
        if (!sync.active) {
            AccountAvatarStore.dropCopies(context)
            photoStamp++
        }
    }

    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hesaplar", fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    Text("En fazla 10 oturum", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
        item {
            Surface(color = CardBackground, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("X hesaplarını tara", fontWeight = FontWeight.Bold)
                        Text(if (health.connected) "Her hesabın kendi resmi satırından alınır" else "Ekran okuma bağlantısını kontrol et", color = TextSecondary, fontSize = 10.sp)
                    }
                    Button(onClick = {
                        AccessibilityServiceState.refreshEnabled(context)
                        if (!AccessibilityServiceState.health.value.enabled) context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        else scope.launch {
                            connecting = true
                            try {
                                val ready = withTimeoutOrNull(10_000L) { AccessibilityServiceState.health.first { it.connected } }
                                if (ready != null) AccountSyncController.startImport() else error = "Ekran okuma hizmeti henüz bağlanmadı."
                            } finally { connecting = false }
                        }
                    }, enabled = !busy, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                        Icon(Icons.Default.Radar, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text(if (connecting) "Bekle…" else "Tara")
                    }
                }
            }
        }
        if (sync.active || sync.completed || sync.failed) item {
            Surface(shape = RoundedCornerShape(16.dp), color = CardBackground) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (sync.failed) Icons.Default.Info else Icons.Default.CheckCircle, null, tint = if (sync.failed) ErrorRed else Success)
                    Spacer(Modifier.width(9.dp)); Text(sync.message, Modifier.weight(1f), fontSize = 12.sp)
                    if (sync.active) TextButton(onClick = AccountSyncController::cancel) { Text("Durdur") }
                }
            }
        }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Hesapların", fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("${accounts.size} / 10", color = TextSecondary, fontSize = 12.sp) } }
        items(accounts, key = { it.id }) { account ->
            val standardCount = allTargets.count { it.ownerAccountId == account.id && it.kind.name == "STANDARD" && it.active }
            val quoteCount = allTargets.count { it.ownerAccountId == account.id && it.kind.name == "QUOTE" && it.active }
            CompactAccountCard(account, busy, standardCount, quoteCount, photoStamp, { targetOwner = account }, { quoteOwner = account }, { AccountSyncController.switchTo(account) }, { deleteCandidate = account })
        }
        if (accounts.isEmpty()) item { Text("Henüz X hesabı eklenmedi. Yukarıdaki Tara düğmesini kullan.", color = TextSecondary, modifier = Modifier.padding(10.dp)) }
    }
    targetOwner?.let { AccountTargetsDialog(it, busy) { targetOwner = null } }
    quoteOwner?.let { QuoteTargetsDialog(it, busy) { quoteOwner = null } }
    deleteCandidate?.let { account ->
        AlertDialog(onDismissRequest = { deleteCandidate = null }, title = { Text("Hesap kaydını kaldır?") }, text = { Text("${account.username} Atmaca listesinden ve bağlı görevlerden kaldırılır. X oturumu açık kalır.") },
            confirmButton = { TextButton(enabled = !busy, onClick = { scope.launch { runCatching { AppServices.repository.deleteAccount(account.id) }.onFailure { error = "Hesap kaldırılamadı." } }; deleteCandidate = null }) { Text("Kaldır", color = ErrorRed) } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Vazgeç") } })
    }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("Tamam") } }) }
}

@Composable private fun AccountAvatar26_78(username: String, current: Boolean, stamp: Int) {
    val context = LocalContext.current
    val file = AccountAvatarStore.file(context, username)
    val bitmap = remember(username, stamp, file.lastModified()) {
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
    if (bitmap != null) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).border(1.dp, Divider, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap.asImageBitmap(),
                contentDescription = username,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp),
            )
        }
    } else {
        Surface(color = if (current) AtmacaSky else Divider, shape = CircleShape, modifier = Modifier.size(56.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(username.removePrefix("@").take(2).uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
    }
}

@Composable private fun CompactAccountCard(
    account: Account,
    locked: Boolean,
    standardCount: Int,
    quoteCount: Int,
    photoStamp: Int,
    onTargets: () -> Unit,
    onQuoteTargets: () -> Unit,
    onSwitch: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(color = CardBackground, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccountAvatar26_78(account.username, account.isCurrent, photoStamp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(account.username, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${account.followers} takipçi  ·  ${account.following} takip", color = TextSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onDelete, enabled = !locked, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Hesabı kaldır", Modifier.size(16.dp), tint = TextSecondary)
                }
            }
            Surface(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp), color = AppBackground) {
                Row(Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    StripAction("Hedef", standardCount.toString(), !locked, Modifier.weight(1f), onTargets)
                    Box(Modifier.width(1.dp).fillMaxHeight().background(Divider))
                    StripAction("Alıntı", quoteCount.toString(), !locked, Modifier.weight(1f), onQuoteTargets)
                    Box(Modifier.width(1.dp).fillMaxHeight().background(Divider))
                    StripAction("Geç", "", !locked, Modifier.weight(1f), onSwitch)
                }
            }
        }
    }
}

@Composable private fun StripAction(label: String, value: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxHeight(), contentPadding = PaddingValues(0.dp)) {
        Text(if (value.isBlank()) label else "$label  $value", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
