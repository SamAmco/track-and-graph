package com.samco.trackandgraph.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

fun Fragment.resumeScoped(
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

            override fun onResume(owner: LifecycleOwner) {
                scope.launch { block() }
            }

            override fun onPause(owner: LifecycleOwner) {
                scope.cancel()
                lifecycle.removeObserver(this)
            }
        }
    )
}