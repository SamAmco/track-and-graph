package com.samco.trackandgraph.screenshots

import android.app.Application
import android.content.Context
import android.os.Bundle
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
    }
}
