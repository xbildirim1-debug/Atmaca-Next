package com.atmacanext.app

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.atmacanext.app.automation.AppForegroundState
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import com.atmacanext.app.automation.AccessibilityServiceState
import com.atmacanext.app.ui.AtmacaNextApp
import com.atmacanext.app.ui.theme.AtmacaNextTheme

class MainActivity : ComponentActivity() {
    private var connectionCheck: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AccessibilityServiceState.refreshEnabled(this)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            AtmacaNextTheme {
                AtmacaNextApp()
            }
        }
    }

    override fun onPause() {
        AppForegroundState.resumed = false
        connectionCheck?.cancel()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        AppForegroundState.resumed = true
        getSystemService(android.app.NotificationManager::class.java)?.cancel(1210)
        connectionCheck?.cancel()
        connectionCheck = lifecycleScope.launch {
            repeat(20) {
                AccessibilityServiceState.refreshEnabled(this@MainActivity)
                if (AccessibilityServiceState.health.value.connected) return@launch
                delay(500L)
            }
        }
    }
}
