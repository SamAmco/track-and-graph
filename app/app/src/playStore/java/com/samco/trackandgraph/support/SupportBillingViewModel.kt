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
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
internal class SupportBillingViewModel @Inject constructor(
    private val billingManager: SupportBillingManager,
) : ViewModel() {
    val state: StateFlow<SupportBillingState> = billingManager.state

    fun load() = billingManager.load()

    fun purchase(activity: Activity, optionId: String) {
        billingManager.purchase(activity, optionId)
    }

    fun clearTransientState() = billingManager.clearTransientState()
}
