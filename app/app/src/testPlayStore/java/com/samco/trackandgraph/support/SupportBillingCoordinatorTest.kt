/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SupportBillingCoordinatorTest {
    private lateinit var platform: FakeSupportBillingGateway
    private lateinit var coordinator: SupportBillingCoordinator
    private val host = object : SupportBillingFlowHost {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Before
    fun setUp() {
        platform = FakeSupportBillingGateway()
        coordinator = SupportBillingCoordinatorImpl(platform)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `coordinator registers purchase listener`() {
        assertTrue(platform.purchaseUpdateListenerInitialized)
    }

    @Test
    fun `connected load recovers purchases before querying product`() {
        val results = mutableListOf<SupportLoadResult>()

        coordinator.load(results::add)

        assertEquals(1, platform.purchaseQueries.size)
        assertTrue(platform.productQueries.isEmpty())
        platform.completePurchaseQuery()
        assertEquals(1, platform.productQueries.size)
    }

    @Test
    fun `load connects before recovery when platform is not ready`() {
        platform.isReady = false

        coordinator.load {}

        assertEquals(1, platform.connections.size)
        assertTrue(platform.purchaseQueries.isEmpty())
        platform.finishConnection()
        assertEquals(1, platform.purchaseQueries.size)
    }

    @Test
    fun `concurrent loads share a connection`() {
        platform.isReady = false

        coordinator.load {}
        coordinator.load {}

        assertEquals(1, platform.connections.size)
        platform.finishConnection()
        assertEquals(2, platform.purchaseQueries.size)
    }

    @Test
    fun `canceled connection waiter does not start recovery`() {
        platform.isReady = false
        val first = coordinator.load {}
        coordinator.load {}

        first.cancel()
        platform.finishConnection()

        assertEquals(1, platform.purchaseQueries.size)
    }

    @Test
    fun `connection failure reports unavailable`() {
        platform.isReady = false
        val results = mutableListOf<SupportLoadResult>()
        coordinator.load(results::add)

        platform.finishConnection(PlatformBillingResponse.Error)

        assertEquals(listOf(SupportLoadResult.Unavailable(false)), results)
    }

    @Test
    fun `disconnect while connecting reports unavailable`() {
        platform.isReady = false
        val results = mutableListOf<SupportLoadResult>()
        coordinator.load(results::add)

        platform.disconnect()

        assertEquals(listOf(SupportLoadResult.Unavailable(false)), results)
    }

    @Test
    fun `old connection callback cannot affect newer attempt`() {
        platform.isReady = false
        val firstResults = mutableListOf<SupportLoadResult>()
        val secondResults = mutableListOf<SupportLoadResult>()
        coordinator.load(firstResults::add)
        platform.disconnect()

        coordinator.load(secondResults::add)
        platform.finishConnection(index = 1)
        platform.disconnect(index = 0)

        assertEquals(1, platform.purchaseQueries.size)
        assertTrue(secondResults.isEmpty())
    }

    @Test
    fun `purchase query failure still queries product`() {
        coordinator.load {}

        platform.completePurchaseQuery(PlatformBillingResponse.Error)

        assertEquals(1, platform.productQueries.size)
    }

    @Test
    fun `offers are filtered sorted mapped and highlighted`() {
        val results = mutableListOf<SupportLoadResult>()
        coordinator.load(results::add)
        platform.completePurchaseQuery()
        platform.completeProductQuery(
            product(
                offer("preorder", 500_000, "£0.50", isPreorder = true),
                offer("large", 5_000_000, "£5.00"),
                offer("small", 1_000_000, "£1.00", tags = listOf("highlighted")),
                offer("rental", 2_000_000, "£2.00", isRental = true),
            )
        )

        assertEquals(
            listOf(
                SupportLoadResult.Available(
                    description = "Support development",
                    options = listOf(
                        SupportPurchaseOption("small", "£1.00", 1_000_000, true),
                        SupportPurchaseOption("large", "£5.00", 5_000_000, false),
                    ),
                    hasPendingPurchase = false,
                )
            ),
            results,
        )
    }

    @Test
    fun `missing product reports unavailable`() {
        val results = mutableListOf<SupportLoadResult>()
        coordinator.load(results::add)
        platform.completePurchaseQuery()

        platform.completeProductQuery(null)

        assertEquals(listOf(SupportLoadResult.Unavailable(false)), results)
    }

    @Test
    fun `empty eligible offers report unavailable`() {
        val results = mutableListOf<SupportLoadResult>()
        coordinator.load(results::add)
        platform.completePurchaseQuery()

        platform.completeProductQuery(product(offer("rental", 1, "£1", isRental = true)))

        assertEquals(listOf(SupportLoadResult.Unavailable(false)), results)
    }

    @Test
    fun `pending recovery is carried into available result`() {
        val results = mutableListOf<SupportLoadResult>()
        coordinator.load(results::add)
        platform.completePurchaseQuery(
            purchases = listOf(purchase("pending", PlatformPurchaseState.Pending))
        )
        platform.completeProductQuery(product(offer("small", 1_000_000, "£1.00")))

        assertTrue((results.single() as SupportLoadResult.Available).hasPendingPurchase)
    }

    @Test
    fun `pending recovery is retained when products are unavailable`() {
        val results = mutableListOf<SupportLoadResult>()
        coordinator.load(results::add)
        platform.completePurchaseQuery(
            purchases = listOf(purchase("pending", PlatformPurchaseState.Pending))
        )
        platform.completeProductQuery(null)

        assertEquals(listOf(SupportLoadResult.Unavailable(true)), results)
    }

    @Test
    fun `recovered purchases are consumed before product query`() {
        coordinator.load {}
        platform.completePurchaseQuery(
            purchases = listOf(
                purchase("one", PlatformPurchaseState.Purchased),
                purchase("two", PlatformPurchaseState.Purchased),
            )
        )

        assertEquals(listOf("one", "two"), platform.consumeCalls.map(ConsumeCall::token))
        assertTrue(platform.productQueries.isEmpty())
        platform.completeConsume("one")
        assertTrue(platform.productQueries.isEmpty())
        platform.completeConsume("two")
        assertEquals(1, platform.productQueries.size)
    }

    @Test
    fun `recovery continues after consume failure`() {
        coordinator.load {}
        platform.completePurchaseQuery(
            purchases = listOf(purchase("retained", PlatformPurchaseState.Purchased))
        )

        platform.completeConsume("retained", PlatformBillingResponse.Error)

        assertEquals(1, platform.productQueries.size)
    }

    @Test
    fun `overlapping recovery coalesces consumption by token`() {
        val retained = purchase("retained", PlatformPurchaseState.Purchased)
        coordinator.load {}
        coordinator.load {}
        platform.completePurchaseQuery(index = 0, purchases = listOf(retained))
        platform.completePurchaseQuery(index = 1, purchases = listOf(retained))

        assertEquals(1, platform.consumeCalls.size)
        platform.completeConsume("retained")
        assertEquals(2, platform.productQueries.size)
    }

    @Test
    fun `canceled load ignores late product response`() {
        val results = mutableListOf<SupportLoadResult>()
        val request = coordinator.load(results::add)
        platform.completePurchaseQuery()

        request.cancel()
        platform.completeProductQuery(product(offer("small", 1_000_000, "£1.00")))

        assertTrue(results.isEmpty())
    }

    @Test
    fun `canceled load still consumes purchase already returned by Play`() {
        val request = coordinator.load {}
        request.cancel()

        platform.completePurchaseQuery(
            purchases = listOf(purchase("paid", PlatformPurchaseState.Purchased))
        )

        assertEquals(listOf("paid"), platform.consumeCalls.map(ConsumeCall::token))
        platform.completeConsume("paid")
        assertTrue(platform.productQueries.isEmpty())
    }

    @Test
    fun `purchase launches selected offer`() {
        val results = mutableListOf<SupportPurchaseResult>()

        coordinator.purchase(host, "small", results::add)

        assertEquals(listOf("small"), platform.launchCalls.map(LaunchCall::offerId))
        assertTrue(results.isEmpty())
    }

    @Test
    fun `purchase reconnects before launching when platform disconnected`() {
        platform.isReady = false

        coordinator.purchase(host, "small") {}

        assertEquals(1, platform.connections.size)
        assertTrue(platform.launchCalls.isEmpty())
        platform.finishConnection()
        assertEquals(listOf("small"), platform.launchCalls.map(LaunchCall::offerId))
    }

    @Test
    fun `purchase reports failure when reconnection fails`() {
        platform.isReady = false
        val results = mutableListOf<SupportPurchaseResult>()
        coordinator.purchase(host, "small", results::add)

        platform.finishConnection(PlatformBillingResponse.Error)

        assertTrue(platform.launchCalls.isEmpty())
        assertEquals(listOf(SupportPurchaseResult.Failed), results)
    }

    @Test
    fun `canceled purchase waiting for connection never launches`() {
        platform.isReady = false
        val request = coordinator.purchase(host, "small") {}

        request.cancel()
        platform.finishConnection()

        assertTrue(platform.launchCalls.isEmpty())
    }

    @Test
    fun `second active purchase is rejected`() {
        val secondResults = mutableListOf<SupportPurchaseResult>()
        coordinator.purchase(host, "small") {}

        coordinator.purchase(host, "small", secondResults::add)

        assertEquals(1, platform.launchCalls.size)
        assertEquals(listOf(SupportPurchaseResult.Failed), secondResults)
    }

    @Test
    fun `canceling caller does not let a new flow replace outstanding Play callback`() {
        val first = coordinator.purchase(host, "small") {}
        first.cancel()
        val secondResults = mutableListOf<SupportPurchaseResult>()

        coordinator.purchase(host, "small", secondResults::add)

        assertEquals(1, platform.launchCalls.size)
        assertEquals(listOf(SupportPurchaseResult.Failed), secondResults)
        platform.updatePurchases(PlatformBillingResponse.UserCanceled)
        coordinator.purchase(host, "small") {}
        assertEquals(2, platform.launchCalls.size)
    }

    @Test
    fun `immediate launch failure reports failure`() {
        platform.launchResult = result(PlatformBillingResponse.Error)
        val results = mutableListOf<SupportPurchaseResult>()

        coordinator.purchase(host, "small", results::add)

        assertEquals(listOf(SupportPurchaseResult.Failed), results)
    }

    @Test
    fun `immediate already owned response recovers retained purchases`() {
        platform.launchResult = result(PlatformBillingResponse.ItemAlreadyOwned)
        val results = mutableListOf<SupportPurchaseResult>()

        coordinator.purchase(host, "small", results::add)
        platform.completePurchaseQuery(
            purchases = listOf(purchase("retained", PlatformPurchaseState.Purchased))
        )
        platform.completeConsume("retained")

        assertEquals(listOf(SupportPurchaseResult.Recovered), results)
    }

    @Test
    fun `user cancellation reports cancellation`() {
        val results = mutableListOf<SupportPurchaseResult>()
        coordinator.purchase(host, "small", results::add)

        platform.updatePurchases(PlatformBillingResponse.UserCanceled)

        assertEquals(listOf(SupportPurchaseResult.Canceled), results)
    }

    @Test
    fun `purchase error reports failure`() {
        val results = mutableListOf<SupportPurchaseResult>()
        coordinator.purchase(host, "small", results::add)

        platform.updatePurchases(PlatformBillingResponse.Error)

        assertEquals(listOf(SupportPurchaseResult.Failed), results)
    }

    @Test
    fun `pending purchase reports pending`() {
        val results = mutableListOf<SupportPurchaseResult>()
        coordinator.purchase(host, "small", results::add)

        platform.updatePurchases(
            purchases = listOf(purchase("pending", PlatformPurchaseState.Pending))
        )

        assertEquals(listOf(SupportPurchaseResult.Pending), results)
    }

    @Test
    fun `completed purchase reports completion after consumption`() {
        val results = mutableListOf<SupportPurchaseResult>()
        coordinator.purchase(host, "small", results::add)
        platform.updatePurchases(
            purchases = listOf(purchase("paid", PlatformPurchaseState.Purchased))
        )

        assertTrue(results.isEmpty())
        platform.completeConsume("paid")

        assertEquals(listOf(SupportPurchaseResult.Completed), results)
    }

    @Test
    fun `consume failure reports deferred consumption rather than payment failure`() {
        val results = mutableListOf<SupportPurchaseResult>()
        coordinator.purchase(host, "small", results::add)
        platform.updatePurchases(
            purchases = listOf(purchase("paid", PlatformPurchaseState.Purchased))
        )

        platform.completeConsume("paid", PlatformBillingResponse.Error)

        assertEquals(listOf(SupportPurchaseResult.ConsumptionDeferred), results)
    }

    @Test
    fun `canceled purchase request does not receive late result`() {
        val results = mutableListOf<SupportPurchaseResult>()
        val request = coordinator.purchase(host, "small", results::add)
        platform.updatePurchases(
            purchases = listOf(purchase("paid", PlatformPurchaseState.Purchased))
        )

        request.cancel()
        platform.completeConsume("paid")

        assertTrue(results.isEmpty())
    }

    @Test
    fun `duplicate purchase callbacks coalesce consumption`() {
        val results = mutableListOf<SupportPurchaseResult>()
        coordinator.purchase(host, "small", results::add)
        val paid = purchase("paid", PlatformPurchaseState.Purchased)

        platform.updatePurchases(purchases = listOf(paid))
        platform.updatePurchases(purchases = listOf(paid))

        assertEquals(1, platform.consumeCalls.size)
        platform.completeConsume("paid")
        assertEquals(listOf(SupportPurchaseResult.Completed), results)
    }

    @Test
    fun `concurrent duplicate purchase callbacks only start one consume`() {
        coordinator.purchase(host, "small") {}
        val paid = purchase("paid", PlatformPurchaseState.Purchased)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val threads = List(2) {
            Thread {
                ready.countDown()
                start.await()
                platform.updatePurchases(purchases = listOf(paid))
            }.apply(Thread::start)
        }

        ready.await()
        start.countDown()
        threads.forEach(Thread::join)

        assertEquals(1, platform.consumeCalls.size)
    }

    @Test
    fun `unsolicited completed purchase is consumed`() {
        platform.updatePurchases(
            purchases = listOf(purchase("retained", PlatformPurchaseState.Purchased))
        )

        assertEquals(listOf("retained"), platform.consumeCalls.map(ConsumeCall::token))
    }

    @Test
    fun `callback without support product fails active purchase`() {
        val results = mutableListOf<SupportPurchaseResult>()
        coordinator.purchase(host, "small", results::add)

        platform.updatePurchases(
            purchases = listOf(
                purchase("other", PlatformPurchaseState.Purchased, listOf("other_product"))
            )
        )

        assertEquals(listOf(SupportPurchaseResult.Failed), results)
        assertTrue(platform.consumeCalls.isEmpty())
    }

    @Test
    fun `already owned recovers retained purchases before reporting recovered`() {
        val results = mutableListOf<SupportPurchaseResult>()
        coordinator.purchase(host, "small", results::add)

        platform.updatePurchases(PlatformBillingResponse.ItemAlreadyOwned)
        platform.completePurchaseQuery(
            purchases = listOf(purchase("retained", PlatformPurchaseState.Purchased))
        )
        assertTrue(results.isEmpty())
        platform.completeConsume("retained")

        assertEquals(listOf(SupportPurchaseResult.Recovered), results)
    }

    @Test
    fun `already owned preserves an outstanding pending purchase`() {
        val results = mutableListOf<SupportPurchaseResult>()
        coordinator.purchase(host, "small", results::add)

        platform.updatePurchases(PlatformBillingResponse.ItemAlreadyOwned)
        val overlappingResults = mutableListOf<SupportPurchaseResult>()
        coordinator.purchase(host, "small", overlappingResults::add)
        assertEquals(listOf(SupportPurchaseResult.Failed), overlappingResults)
        platform.completePurchaseQuery(
            purchases = listOf(purchase("pending", PlatformPurchaseState.Pending))
        )

        assertEquals(listOf(SupportPurchaseResult.Pending), results)
        assertTrue(platform.consumeCalls.isEmpty())
    }

    private fun product(vararg offers: PlatformOffer) = PlatformProduct(
        productId = "developer_support",
        description = "Support development",
        offers = offers.toList(),
    )

    private fun offer(
        id: String,
        priceMicros: Long,
        formattedPrice: String,
        tags: List<String> = emptyList(),
        isRental: Boolean = false,
        isPreorder: Boolean = false,
    ) = PlatformOffer(id, formattedPrice, priceMicros, tags, isRental, isPreorder)

    private fun purchase(
        token: String,
        state: PlatformPurchaseState,
        productIds: List<String> = listOf("developer_support"),
    ) = PlatformPurchase(token, productIds, state)

    private fun SupportBillingCoordinator.load(
        onResult: (SupportLoadResult) -> Unit,
    ): Job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        onResult(load())
    }

    private fun SupportBillingCoordinator.purchase(
        host: SupportBillingFlowHost,
        optionId: String,
        onResult: (SupportPurchaseResult) -> Unit,
    ): Job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        onResult(purchase(host, optionId))
    }

    private class FakeSupportBillingGateway : SupportBillingGateway {
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
        var launchResult = result()

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
            connections[index].onFinished(result(response))
        }

        fun disconnect(index: Int = 0) {
            connections[index].onDisconnected()
        }

        fun completePurchaseQuery(
            response: PlatformBillingResponse = PlatformBillingResponse.Ok,
            index: Int = 0,
            purchases: List<PlatformPurchase> = emptyList(),
        ) {
            purchaseQueries[index].callback(result(response), purchases)
        }

        fun completeProductQuery(product: PlatformProduct?, index: Int = 0) {
            productQueries[index].callback(result(), product)
        }

        fun updatePurchases(
            response: PlatformBillingResponse = PlatformBillingResponse.Ok,
            purchases: List<PlatformPurchase>? = null,
        ) {
            purchaseUpdateListener(result(response), purchases)
        }

        fun completeConsume(
            token: String,
            response: PlatformBillingResponse = PlatformBillingResponse.Ok,
        ) {
            consumeCalls.single { it.token == token }.callback(result(response))
        }
    }

    private companion object {
        fun result(response: PlatformBillingResponse = PlatformBillingResponse.Ok) =
            PlatformBillingResult(response, "test")
    }
}

private data class ConnectionCall(
    val onFinished: (PlatformBillingResult) -> Unit,
    val onDisconnected: () -> Unit,
)

private data class PurchaseQueryCall(
    val callback: (PlatformBillingResult, List<PlatformPurchase>) -> Unit,
)

private data class ProductQueryCall(
    val productId: String,
    val callback: (PlatformBillingResult, PlatformProduct?) -> Unit,
)

private data class LaunchCall(
    val host: SupportBillingFlowHost,
    val offerId: String,
)

private data class ConsumeCall(
    val token: String,
    val callback: (PlatformBillingResult) -> Unit,
)
