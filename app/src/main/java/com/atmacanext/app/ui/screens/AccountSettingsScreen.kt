package com.atmacanext.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.automation.AccessibilityServiceState
import com.atmacanext.app.automation.AccountSyncController
import com.atmacanext.app.ui.theme.*

@Composable fun AccountSettingsScreen(modifier: Modifier = Modifier) {
    val health by AccessibilityServiceState.health.collectAsStateWithLifecycle()
    val sync by AccountSyncController.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var logs by remember { mutableStateOf(false) }
    if (logs) {
        Column(modifier) {
            TextButton(onClick = { logs = false }) { Icon(Icons.Default.ArrowBack, null); Text(" Ayarlara dön") }
            LogsScreen(Modifier.weight(1f))
        }
        return
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Text("Ayarlar", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Kontrol her zaman sende.", color = TextSecondary)
        Card {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Default.AccessibilityNew, null, tint = AtmacaBlue)
                Text("Ekran okuma", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                StatusLabel(if (health.connected) "Bağlı ve hazır" else "İzin gerekli", health.connected)
                Text("Başlattığın işlem sırasında X ekranındaki hesap adları ve sayaçlar okunur, hesap değiştirmek için ekrana dokunulur. Şifre veya oturum anahtarı istenmez.", color = TextSecondary)
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, enabled = !sync.active) {
                    Text("Erişilebilirlik ayarlarını aç")
                }
                Text("Listeden Atmaca Next Otomasyon Servisi'ni seçip etkinleştir. Android “Kısıtlanmış ayar” gösterirse uygulamanın sistem bilgi ekranındaki menüyü kontrol et.", fontSize = 12.sp, color = TextSecondary)
            }
        }
        Card {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.PhonelinkLock, null, tint = Teal)
                Text("Bu telefonda kalır", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Bu sürümün internet izni yoktur. Hesap bilgileri cihazda saklanır. Taramayı dilediğin an Durdur düğmesiyle sonlandırabilirsin.", color = TextSecondary)
                Text("İşlem bittiğinde Atmaca Next öne gelir. Android, X'in zorla kapatılmasına izin vermeyebilir; oturumların açık kalır.", color = TextSecondary)
            }
        }
        OutlinedButton(onClick = { logs = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.History, null); Spacer(Modifier.width(8.dp)); Text("İşlem kayıtları")
        }
        Text("Atmaca Next · 26.1\nHesaplar önizleme sürümü", color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable fun TasksPreviewScreen(modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Görevler", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Icon(Icons.Default.TaskAlt, null, Modifier.size(56.dp), tint = AtmacaBlue)
        Text("Sıradaki adım", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Önce hesaplarını bağlayalım.", fontSize = 18.sp)
        Text("Hesap tarama ve geçiş akışını telefonunda doğruladıktan sonra görevlerini burada birlikte oluşturacağız.",
            color = TextSecondary, lineHeight = 24.sp)
    }
}
