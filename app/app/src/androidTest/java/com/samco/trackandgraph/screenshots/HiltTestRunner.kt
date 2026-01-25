package com.samco.trackandgraph.screenshots

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.work.Configuration
import androidx.work.WorkManager
import com.jakewharton.threetenabp.AndroidThreeTen
import com.karumi.shot.ShotTestRunner
import dagger.hilt.android.testing.HiltTestApplication
import timber.log.Timber

class HiltTestRunner : ShotTestRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }

    override fun onCreate(args: Bundle) {
        super.onCreate(args)
        Timber.plant(Timber.DebugTree())
        AndroidThreeTen.init(targetContext)
        // Initialize WorkManager with default configuration to avoid crashes during theme changes
        try {
            WorkManager.initialize(
                targetContext,
                Configuration.Builder()
                    .setMinimumLoggingLevel(android.util.Log.INFO)
                    .build()
            )
        } catch (e: IllegalStateException) {
            // WorkManager may already be initialized, ignore
        }
    }
}
