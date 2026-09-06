package com.atmacanext.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import com.atmacanext.app.automation.AccessibilityServiceState
import com.atmacanext.app.ui.AtmacaNextApp
import com.atmacanext.app.ui.theme.AtmacaNextTheme

class MainActivity : ComponentActivity() {
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

    override fun onResume() {
        super.onResume()
        AccessibilityServiceState.refreshEnabled(this)
    }
}
