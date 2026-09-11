package com.atmacanext.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.R
import com.atmacanext.app.automation.AccessibilityServiceState
import com.atmacanext.app.automation.AccountSyncController
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.ui.theme.*
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

    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = AtmacaBlue, modifier = Modifier.size(38.dp)) { Image(painterResource(R.drawable.ic_atmaca), null) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text("Hesaplar", fontWeight = FontWeight.Bold, fontSize = 24.sp); Text("X hesaplarını yönet", color = TextSecondary, fontSize = 11.sp) }
            }
        }
        item {
            Surface(color = CardBackground, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Divider)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("X hesaplarını tara", fontWeight = FontWeight.Bold)
                        Text(if (health.connected) "Ekran okuma hazır" else "Ekran okuma bağlantısını kontrol et", color = TextSecondary, fontSize = 10.sp)
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
            Surface(shape = RoundedCornerShape(16.dp), color = CardBackground, border = BorderStroke(1.dp, Divider)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (sync.failed) Icons.Default.Info else Icons.Default.CheckCircle, null, tint = if (sync.failed) ErrorRed else Success)
                    Spacer(Modifier.width(9.dp)); Text(sync.message, Modifier.weight(1f), fontSize = 12.sp)
                    if (sync.active) TextButton(onClick = AccountSyncController::cancel) { Text("Durdur") }
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { CompactStat("Hesap", accounts.size.toString(), Modifier.weight(1f)); CompactStat("Kapasite", "10", Modifier.weight(1f)); CompactStat("Aktif", accounts.count { it.active }.toString(), Modifier.weight(1f)) } }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Hesapların", fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("${accounts.size} / 10", color = TextSecondary, fontSize = 12.sp) } }
        items(accounts, key = { it.id }) { account ->
            val standardCount = allTargets.count { it.ownerAccountId == account.id && it.kind.name == "STANDARD" && it.active }
            val quoteCount = allTargets.count { it.ownerAccountId == account.id && it.kind.name == "QUOTE" && it.active }
            CompactAccountCard(account, busy, standardCount, quoteCount, { targetOwner = account }, { quoteOwner = account }, { AccountSyncController.switchTo(account) }, { deleteCandidate = account })
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

@Composable private fun CompactStat(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = CardBackground, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Divider)) { Column(Modifier.padding(11.dp)) { Text(label, color = TextSecondary, fontSize = 9.sp); Text(value, fontWeight = FontWeight.Bold, fontSize = 19.sp) } }
}

@Composable private fun CompactAccountCard(account: Account, locked: Boolean, standardCount: Int, quoteCount: Int, onTargets: () -> Unit, onQuoteTargets: () -> Unit, onSwitch: () -> Unit, onDelete: () -> Unit) {
    Surface(color = CardBackground, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (account.isCurrent) AtmacaBlue.copy(alpha = .45f) else Divider)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = if (account.isCurrent) AtmacaSky else Divider, shape = CircleShape, modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Text(account.username.removePrefix("@").take(2).uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp) } }
                Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(account.username, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(if (account.isCurrent) "X'te açık" else "X oturumu", color = TextSecondary, fontSize = 9.sp) }
                IconButton(onClick = onDelete, enabled = !locked) { Icon(Icons.Default.Close, "Hesabı kaldır", Modifier.size(18.dp), tint = TextSecondary) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) { Text("Takipçi ${account.followers}", fontSize = 10.sp); Text("Takip ${account.following}", fontSize = 10.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onTargets, enabled = !locked, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Hedef $standardCount/1", fontSize = 10.sp) }
                OutlinedButton(onClick = onQuoteTargets, enabled = !locked, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Alıntı $quoteCount/5", fontSize = 10.sp) }
            }
            OutlinedButton(onClick = onSwitch, enabled = !locked, modifier = Modifier.fillMaxWidth().height(40.dp), contentPadding = PaddingValues(vertical = 0.dp)) { Text("Hesaba geç", fontSize = 11.sp) }
        }
    }
}
