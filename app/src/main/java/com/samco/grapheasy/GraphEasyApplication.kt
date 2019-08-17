package com.samco.grapheasy

import android.app.Application
import timber.log.Timber

class GraphEasyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}