/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

/**
 * Translates Google Play's final SDK objects into app-owned values. It deliberately contains no
 * support-flow policy: eligibility, ordering, recovery, and consumption belong to the coordinator.
 */
internal interface SupportBillingGateway {
    val isReady: Boolean

    fun setPurchaseUpdateListener(
        listener: (PlatformBillingResult, List<PlatformPurchase>?) -> Unit,
    )

    fun startConnection(
        onFinished: (PlatformBillingResult) -> Unit,
        onDisconnected: () -> Unit,
    )

    fun queryPurchases(
        callback: (PlatformBillingResult, List<PlatformPurchase>) -> Unit,
    )

    fun queryProduct(
        productId: String,
        callback: (PlatformBillingResult, PlatformProduct?) -> Unit,
    )

    fun launchBillingFlow(
        host: SupportBillingFlowHost,
        offerId: String,
    ): PlatformBillingResult

    fun consume(
        purchaseToken: String,
        callback: (PlatformBillingResult) -> Unit,
    )
}
