package com.samco.trackandgraph.screenshots

import android.app.Application
import android.content.Context
import com.karumi.shot.ShotTestRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : ShotTestRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
