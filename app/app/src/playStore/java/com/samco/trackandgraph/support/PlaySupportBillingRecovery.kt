/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.samco.trackandgraph.main.MainActivity
import dagger.Lazy
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
internal class PlaySupportBillingRecovery @Inject constructor(
    private val recoveryStore: SupportBillingRecoveryStore,
    private val coordinator: Lazy<SupportBillingCoordinator>,
) : SupportBillingRecovery, Application.ActivityLifecycleCallbacks {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var reconciliationJob: Job? = null

    override fun start(application: Application) {
        if (started.compareAndSet(false, true)) {
            application.registerActivityLifecycleCallbacks(this)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity !is MainActivity || reconciliationJob?.isActive == true) return
        reconciliationJob = scope.launch {
            runCatching {
                if (recoveryStore.reconciliationGeneration() != null) {
                    coordinator.get().reconcileIfNeeded()
                }
            }.onFailure { Timber.w(it, "Unable to reconcile support purchases") }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
