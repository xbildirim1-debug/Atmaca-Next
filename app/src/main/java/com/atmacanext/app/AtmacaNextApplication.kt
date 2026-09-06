package com.atmacanext.app

import android.app.Application
import com.atmacanext.app.core.AppServices

class AtmacaNextApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppServices.initialize(this)
    }
}
