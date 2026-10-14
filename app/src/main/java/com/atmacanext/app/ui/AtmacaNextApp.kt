package com.atmacanext.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atmacanext.app.ui.screens.*
import com.atmacanext.app.ui.theme.*

private enum class AppTab(val title: String, val icon: ImageVector) {
    HOME("Ana", Icons.Filled.Home),
    ACCOUNTS("Hesap", Icons.Filled.People),
    TASKS("Görev", Icons.Filled.TaskAlt),
    PILOT("Pilot", Icons.Filled.AutoAwesome),
    SETTINGS("Ayar", Icons.Filled.Tune),
}

@Composable
fun AtmacaNextApp() {
    var selected by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    var reset by remember { mutableIntStateOf(0) }
    val tab = AppTab.valueOf(selected)
    Scaffold(containerColor = AppBackground, bottomBar = {
        Surface(color = CardBackground, shadowElevation = 0.dp) {
            Column(Modifier.navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
                Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppTab.entries.forEach { item ->
                        val on = tab == item
                        Column(
                            Modifier.weight(1f).clickable {
                                if (selected == item.name) reset += 1 else selected = item.name
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.width(18.dp).height(2.dp).background(if (on) BrandGreen else Color.Transparent))
                            Spacer(Modifier.height(6.dp))
                            Icon(item.icon, item.title, Modifier.size(20.dp), tint = if (on) TextPrimary else TextSecondary)
                            Text(item.title, fontSize = 11.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) TextPrimary else TextSecondary)
                        }
                    }
                }
            }
        }
    }) { padding ->
        val modifier = Modifier.padding(padding).fillMaxSize()
        when (tab) {
            AppTab.HOME -> DashboardScreen26_42(modifier, reset)
            AppTab.ACCOUNTS -> AccountsScreen(modifier)
            AppTab.TASKS -> TasksScreen26_43(modifier)
            AppTab.PILOT -> PilotScreen(modifier)
            AppTab.SETTINGS -> AccountSettingsScreen26_42(modifier, reset)
        }
    }
}
