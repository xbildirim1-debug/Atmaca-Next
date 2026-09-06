package com.atmacanext.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.atmacanext.app.ui.screens.*
import com.atmacanext.app.ui.theme.*

private enum class AppTab(val title: String, val icon: ImageVector) {
    ACCOUNTS("Hesaplar", Icons.Default.PeopleOutline),
    TASKS("Görevler", Icons.Default.TaskAlt),
    SETTINGS("Ayarlar", Icons.Default.Tune),
}

@Composable
fun AtmacaNextApp() {
    var selected by rememberSaveable { mutableStateOf(AppTab.ACCOUNTS.name) }
    val tab = AppTab.valueOf(selected)
    Scaffold(containerColor = AppBackground, bottomBar = {
        NavigationBar(containerColor = AppBackground) {
            AppTab.entries.forEach { item ->
                NavigationBarItem(selected = tab == item, onClick = { selected = item.name },
                    icon = { Icon(item.icon, null) }, label = { Text(item.title) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = AtmacaSky,
                        selectedIconColor = AtmacaBlue, selectedTextColor = AtmacaBlue))
            }
        }
    }) { padding ->
        val modifier = Modifier.padding(padding).fillMaxSize()
        when (tab) {
            AppTab.ACCOUNTS -> AccountsScreen(modifier)
            AppTab.TASKS -> TasksPreviewScreen(modifier)
            AppTab.SETTINGS -> AccountSettingsScreen(modifier)
        }
    }
}
