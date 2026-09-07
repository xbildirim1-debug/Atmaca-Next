package com.atmacanext.app.ui.screens

import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.atmacanext.app.R
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.automation.AccessibilityServiceState
import com.atmacanext.app.automation.AccountSyncController
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AccountsScreen(modifier: Modifier = Modifier) {
    val accounts by AppServices.repository.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val sync by AccountSyncController.state.collectAsStateWithLifecycle()
    val health by AccessibilityServiceState.health.collectAsStateWithLifecycle()
    val queue by AppServices.orchestrator.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var targetOwner by remember { mutableStateOf<Account?>(null) }
    val targets by AppServices.repository.targetAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    var deleteCandidate by remember { mutableStateOf<Account?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var connecting by remember { mutableStateOf(false) }
    val busy = sync.active || queue.isActive || connecting
    LazyColumn(modifier, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(16.dp), color = AtmacaBlue, modifier = Modifier.size(48.dp)) {
                    Image(painterResource(R.drawable.ic_atmaca), contentDescription = null)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ATMACA", fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp, fontSize = 20.sp)
                    Text("NEXT  /  HESAP MERKEZİ", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.5.sp)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(CardBackground, AtmacaSky)), RoundedCornerShape(28.dp)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StatusLabel(if (health.connected) "Ekran okuma hazır" else if (health.enabled) "İzin açık · bağlantı bekleniyor" else "Ekran okuma izni kapalı", health.connected)
                Text("Tüm hesapların.\nTek bir yerde.", fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
                Text("X'teki açık oturumlarını ekle. Hesaplarını gör, kontrol et ve aralarında geçiş yap.",
                    color = TextSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                Button(onClick = {
                    AccessibilityServiceState.refreshEnabled(context)
                    if (!AccessibilityServiceState.health.value.enabled) context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    else scope.launch {
                        connecting = true
                        try {
                            val ready = withTimeoutOrNull(10_000L) { AccessibilityServiceState.health.first { it.connected } }
                            if (ready != null) AccountSyncController.startImport()
                            else error = "Ekran okuma iznin açık, ancak Android hizmeti henüz bağlamadı. Biraz sonra tekrar dene."
                        } finally { connecting = false }
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                    Icon(if (health.connected) Icons.Default.Radar else Icons.Default.AccessibilityNew, null)
                    Spacer(Modifier.width(10.dp))
                    Text(if (connecting) "Bağlantı bekleniyor…" else if (health.enabled || health.connected) "X hesaplarını tara" else "Ekran okumayı etkinleştir")
                }
                Text("Yalnız bu telefonda saklanır · En fazla 10 hesap", fontSize = 11.sp, color = TextSecondary)
            }
        }
        if (sync.active || sync.completed || sync.failed) item {
            Surface(shape = RoundedCornerShape(20.dp), color = CardBackground,
                border = BorderStroke(1.dp, if (sync.failed) ErrorRed.copy(alpha = .4f) else Divider)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (sync.active) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Icon(if (sync.failed) Icons.Default.Info else Icons.Default.CheckCircle, null,
                            tint = if (sync.failed) ErrorRed else Success)
                        Spacer(Modifier.width(12.dp))
                        Text(sync.message, Modifier.weight(1f), fontSize = 14.sp)
                    }
                    if (sync.active) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${sync.processed} kaydedildi / ${sync.discovered} bulundu · ${sync.skipped} atlandı", Modifier.weight(1f),
                                color = TextSecondary, fontSize = 12.sp)
                            TextButton(onClick = { AccountSyncController.cancel() }) { Text("Durdur") }
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryTile("EKLENEN HESAP", "${accounts.size}", "10 hesap kapasitesi", Modifier.weight(1f))
                SummaryTile("X'TE SON ETKİN", accounts.firstOrNull { it.isCurrent }?.username ?: "—",
                    "Son doğrulanan oturum", Modifier.weight(1f))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hesapların", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${accounts.size} / 10", color = TextSecondary, fontSize = 13.sp)
            }
        }
        if (accounts.isEmpty()) item {
            Surface(color = CardBackground, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Divider)) {
                Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.PeopleOutline, null, Modifier.size(44.dp), tint = TextSecondary)
                    Text("İlk hesabına yer açtık", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text("X uygulamasında oturum açtıktan sonra yukarıdaki tarama düğmesini kullan.",
                        color = TextSecondary, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        items(accounts, key = { it.id }) { account ->
            ModernAccountCard(account, busy, targets.count { it.ownerAccountId == account.id }, onTargets = { targetOwner = account }, onSwitch = { AccountSyncController.switchTo(account) },
                onDelete = { deleteCandidate = account })
        }
        item { Text("Sayaçlar X ekranında göründüğü biçimde kaydedilir. Etkin hesap bilgisi son doğrulamayı gösterir.",
            color = TextSecondary, fontSize = 11.sp, lineHeight = 17.sp) }
    }
    targetOwner?.let { account -> AccountTargetsDialog(account, busy, onDismiss = { targetOwner = null }) }
    deleteCandidate?.let { account ->
        AlertDialog(onDismissRequest = { deleteCandidate = null }, title = { Text("Hesap kaydını kaldır?") },
            text = { Text("${account.username} Atmaca listesinden ve varsa bağlı eski görevlerden kaldırılır. X oturumun açık kalır.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                scope.launch { runCatching { AppServices.repository.deleteAccount(account.id) }
                    .onFailure { error = "Hesap kaldırılamadı. Yeniden dene." } }
                deleteCandidate = null
            }) { Text("Kaldır", color = ErrorRed) } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Vazgeç") } })
    }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { error = null }) { Text("Tamam") } }) }
}

@Composable
private fun SummaryTile(label: String, value: String, subtitle: String, modifier: Modifier) {
    Surface(modifier, color = CardBackground, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, fontSize = 9.sp, letterSpacing = 1.sp, color = TextSecondary)
            Text(value, fontSize = if (value.startsWith("@")) 16.sp else 28.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun ModernAccountCard(account: Account, locked: Boolean, targetCount: Int, onTargets: () -> Unit, onSwitch: () -> Unit, onDelete: () -> Unit) {
    Surface(color = CardBackground, shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (account.isCurrent) AtmacaBlue.copy(alpha = .45f) else Divider)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = if (account.isCurrent) AtmacaSky else Divider, shape = CircleShape, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(account.username.removePrefix("@").take(2).uppercase(), fontWeight = FontWeight.Bold,
                            color = if (account.isCurrent) AtmacaBlue else TextPrimary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(account.username, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (account.isCurrent) "Son doğrulanan etkin hesap" else "X oturumu", fontSize = 11.sp,
                        color = if (account.isCurrent) AtmacaBlue else TextSecondary)
                }
                IconButton(onClick = onDelete, enabled = !locked) {
                    Icon(Icons.Default.Close, contentDescription = "${account.username} kaydını kaldır", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Counter("Takipçi", account.followers, Modifier.weight(1f))
                Counter("Takip edilen", account.following, Modifier.weight(1f))
            }
            OutlinedButton(onClick = onTargets, enabled = !locked, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Hedef hesap ekle · $targetCount/3")
            }
            OutlinedButton(onClick = onSwitch, enabled = !locked, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                border = BorderStroke(1.dp, Divider)) {
                Text("Hesaba geç")
                Spacer(Modifier.width(8.dp)); Icon(Icons.Default.NorthEast, null, Modifier.size(18.dp))
            }
        }
    }
}

@Composable private fun Counter(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable internal fun StatusLabel(label: String, ready: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(6.dp).background(if (ready) AtmacaBlue else Warning, CircleShape))
        Text(label, color = if (ready) AtmacaBlue else Warning, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
