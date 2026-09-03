/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

internal class FakeSupportBillingGateway : SupportBillingGateway {
    override var isReady = true
    private lateinit var purchaseUpdateListener:
        (PlatformBillingResult, List<PlatformPurchase>?) -> Unit
    val purchaseUpdateListenerInitialized: Boolean
        get() = ::purchaseUpdateListener.isInitialized
    val connections = mutableListOf<ConnectionCall>()
    val purchaseQueries = mutableListOf<PurchaseQueryCall>()
    val productQueries = mutableListOf<ProductQueryCall>()
    val launchCalls = mutableListOf<LaunchCall>()
    val consumeCalls = mutableListOf<ConsumeCall>()
    var launchResult = billingResult()

    override fun setPurchaseUpdateListener(
        listener: (PlatformBillingResult, List<PlatformPurchase>?) -> Unit,
    ) {
        purchaseUpdateListener = listener
    }

    override fun startConnection(
        onFinished: (PlatformBillingResult) -> Unit,
        onDisconnected: () -> Unit,
    ) {
        connections += ConnectionCall(onFinished, onDisconnected)
    }

    override fun queryPurchases(
        callback: (PlatformBillingResult, List<PlatformPurchase>) -> Unit,
    ) {
        purchaseQueries += PurchaseQueryCall(callback)
    }

    override fun queryProduct(
        productId: String,
        callback: (PlatformBillingResult, PlatformProduct?) -> Unit,
    ) {
        productQueries += ProductQueryCall(productId, callback)
    }

    override fun launchBillingFlow(
        host: SupportBillingFlowHost,
        offerId: String,
    ): PlatformBillingResult {
        launchCalls += LaunchCall(host, offerId)
        return launchResult
    }

    override fun consume(
        purchaseToken: String,
        callback: (PlatformBillingResult) -> Unit,
    ) {
        consumeCalls += ConsumeCall(purchaseToken, callback)
    }

    fun finishConnection(
        response: PlatformBillingResponse = PlatformBillingResponse.Ok,
        index: Int = 0,
    ) {
        isReady = response == PlatformBillingResponse.Ok
        connections[index].onFinished(billingResult(response))
    }

    fun disconnect(index: Int = 0) {
        connections[index].onDisconnected()
    }

    fun completePurchaseQuery(
        response: PlatformBillingResponse = PlatformBillingResponse.Ok,
        index: Int = 0,
        purchases: List<PlatformPurchase> = emptyList(),
    ) {
        purchaseQueries[index].callback(billingResult(response), purchases)
    }

    fun completeProductQuery(product: PlatformProduct?, index: Int = 0) {
        productQueries[index].callback(billingResult(), product)
    }

    fun updatePurchases(
        response: PlatformBillingResponse = PlatformBillingResponse.Ok,
        purchases: List<PlatformPurchase>? = null,
    ) {
        purchaseUpdateListener(billingResult(response), purchases)
    }

    fun completeConsume(
        token: String,
        response: PlatformBillingResponse = PlatformBillingResponse.Ok,
    ) {
        consumeCalls.single { it.token == token }.callback(billingResult(response))
    }

    internal data class ConnectionCall(
        val onFinished: (PlatformBillingResult) -> Unit,
        val onDisconnected: () -> Unit,
    )

    internal data class PurchaseQueryCall(
        val callback: (PlatformBillingResult, List<PlatformPurchase>) -> Unit,
    )

    internal data class ProductQueryCall(
        val productId: String,
        val callback: (PlatformBillingResult, PlatformProduct?) -> Unit,
    )

    internal data class LaunchCall(
        val host: SupportBillingFlowHost,
        val offerId: String,
    )

    internal data class ConsumeCall(
        val token: String,
        val callback: (PlatformBillingResult) -> Unit,
    )
}

internal fun billingResult(
    response: PlatformBillingResponse = PlatformBillingResponse.Ok,
) = PlatformBillingResult(response, "test")
