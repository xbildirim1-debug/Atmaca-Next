package com.atmacanext.app.automation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Reads whether Android has validated a real internet link. Does not open a connection. */
internal object NetworkStatus {
    fun online(context: Context): Boolean {
        val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
