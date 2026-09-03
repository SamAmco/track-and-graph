/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

internal enum class SupportCheckoutState {
    Idle,
    InProgress,
    PaymentFailed,
    PaymentPending,
}

internal sealed interface SupportBillingState {
    data object Loading : SupportBillingState

    data class Unavailable(
        val hasPendingPurchase: Boolean = false,
    ) : SupportBillingState

    data class Available(
        val description: String,
        val options: List<SupportPurchaseOption>,
        val checkoutState: SupportCheckoutState = SupportCheckoutState.Idle,
    ) : SupportBillingState

    data object ThankYou : SupportBillingState
}
