package com.buse.app

import android.app.Application
import com.buse.app.core.AppServices

class BuseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppServices.initialize(this)
    }
}
