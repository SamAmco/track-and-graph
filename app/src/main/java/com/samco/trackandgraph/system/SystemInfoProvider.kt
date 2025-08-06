package com.samco.trackandgraph.system

import android.os.Build
import javax.inject.Inject

interface SystemInfoProvider {
    val buildVersionSdkInt: Int
}

class SystemInfoProviderImpl @Inject constructor(): SystemInfoProvider {
    override val buildVersionSdkInt: Int
        get() = Build.VERSION.SDK_INT
}