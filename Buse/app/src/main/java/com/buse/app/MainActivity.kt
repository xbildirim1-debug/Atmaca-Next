package com.buse.app

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.buse.app.automation.AppForegroundState
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import com.buse.app.automation.AccessibilityServiceState
import com.buse.app.ui.BuseApp
import com.buse.app.ui.theme.BuseTheme

class MainActivity : ComponentActivity() {
    private var connectionCheck: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AccessibilityServiceState.refreshEnabled(this)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        setContent {
            BuseTheme {
                BuseApp()
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
