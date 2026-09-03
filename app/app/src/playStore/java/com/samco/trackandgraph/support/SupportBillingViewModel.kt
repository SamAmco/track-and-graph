/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SupportBillingViewModel @Inject constructor(
    private val billingCoordinator: SupportBillingCoordinator,
) : ViewModel() {
    private val _state = MutableStateFlow<SupportBillingState>(SupportBillingState.Loading)
    val state: StateFlow<SupportBillingState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var purchaseJob: Job? = null

    fun load() {
        loadJob?.cancel()
        purchaseJob?.cancel()
        _state.value = SupportBillingState.Loading
        loadJob = viewModelScope.launch {
            val result = billingCoordinator.load()
            _state.value = when (result) {
                is SupportLoadResult.Available -> SupportBillingState.Available(
                    description = result.description,
                    options = result.options,
                    checkoutState = if (result.hasPendingPurchase) {
                        SupportCheckoutState.PaymentPending
                    } else {
                        SupportCheckoutState.Idle
                    },
                )

                is SupportLoadResult.Unavailable -> SupportBillingState.Unavailable(
                    hasPendingPurchase = result.hasPendingPurchase,
                )
            }
        }
    }

    fun purchase(host: SupportBillingFlowHost, optionId: String) {
        val available = _state.value as? SupportBillingState.Available ?: return
        if (
            available.checkoutState == SupportCheckoutState.InProgress ||
            available.checkoutState == SupportCheckoutState.PaymentPending
        ) return
        if (available.options.none { it.id == optionId }) {
            _state.value = available.copy(checkoutState = SupportCheckoutState.PaymentFailed)
            return
        }

        purchaseJob?.cancel()
        _state.value = available.copy(checkoutState = SupportCheckoutState.InProgress)
        purchaseJob = viewModelScope.launch {
            val result = billingCoordinator.purchase(host, optionId)
            val current = _state.value as? SupportBillingState.Available ?: return@launch
            _state.value = when (result) {
                SupportPurchaseResult.Completed -> SupportBillingState.ThankYou
                SupportPurchaseResult.Pending -> current.copy(
                    checkoutState = SupportCheckoutState.PaymentPending,
                )

                SupportPurchaseResult.Failed -> current.copy(
                    checkoutState = SupportCheckoutState.PaymentFailed,
                )

                SupportPurchaseResult.Canceled,
                SupportPurchaseResult.ConsumptionDeferred,
                SupportPurchaseResult.Recovered -> current.copy(
                    checkoutState = SupportCheckoutState.Idle,
                )
            }
        }
    }

    fun dismiss() {
        loadJob?.cancel()
        purchaseJob?.cancel()
        loadJob = null
        purchaseJob = null
        _state.value = SupportBillingState.Loading
    }

    override fun onCleared() {
        loadJob?.cancel()
        purchaseJob?.cancel()
    }
}
