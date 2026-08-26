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
import android.text.TextUtils
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import java.util.concurrent.CountDownLatch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SupportBillingManagerTest {
    private lateinit var client: FakeSupportBillingClient
    private lateinit var manager: SupportBillingManager
    private lateinit var textUtils: MockedStatic<TextUtils>

    @Before
    fun setUp() {
        textUtils = Mockito.mockStatic(TextUtils::class.java)
        textUtils.`when`<Boolean> { TextUtils.isEmpty(null) }.thenReturn(true)
        textUtils.`when`<Boolean> { TextUtils.isEmpty("") }.thenReturn(true)
        textUtils.`when`<Boolean> { TextUtils.isEmpty("small") }.thenReturn(false)
        client = FakeSupportBillingClient()
        manager = SupportBillingManager(client)
    }

    @After
    fun tearDown() {
        textUtils.close()
    }

    @Test
    fun `manager registers itself as purchase listener`() {
        assertTrue(client.purchaseListener === manager)
    }

    @Test
    fun `load when connected recovers purchases before querying products`() {
        manager.load()

        assertEquals(SupportProductsState.Loading, manager.state.value.products)
        assertEquals(1, client.purchaseQueries.size)
        assertTrue(client.productQueries.isEmpty())

        client.completePurchaseQuery()

        assertEquals(1, client.productQueries.size)
    }

    @Test
    fun `load connects when client is not ready`() {
        client.isReady = false

        manager.load()

        assertEquals(1, client.connectionListeners.size)
        assertTrue(client.purchaseQueries.isEmpty())

        client.finishConnection()

        assertEquals(1, client.purchaseQueries.size)
    }

    @Test
    fun `two loads during connection share connection and continue latest session`() {
        client.isReady = false

        manager.load()
        manager.load()

        assertEquals(1, client.connectionListeners.size)
        client.finishConnection()
        assertEquals(1, client.purchaseQueries.size)

        client.completePurchaseQuery()
        assertEquals(1, client.productQueries.size)
    }

    @Test
    fun `connection failure makes products unavailable`() {
        client.isReady = false
        manager.load()

        client.finishConnection(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)

        assertEquals(SupportProductsState.Unavailable, manager.state.value.products)
    }

    @Test
    fun `disconnect while loading makes products unavailable`() {
        client.isReady = false
        manager.load()

        client.disconnect()

        assertEquals(SupportProductsState.Unavailable, manager.state.value.products)
    }

    @Test
    fun `disconnect from old connection attempt cannot fail newer load`() {
        client.isReady = false
        manager.load()
        client.disconnect()

        manager.load()
        client.finishConnection(index = 1)
        client.disconnect(index = 0)

        assertEquals(SupportProductsState.Loading, manager.state.value.products)
        assertEquals(1, client.purchaseQueries.size)
    }

    @Test
    fun `purchase query failure does not prevent products loading`() {
        manager.load()

        client.completePurchaseQuery(BillingClient.BillingResponseCode.ERROR)

        assertEquals(1, client.productQueries.size)
    }

    @Test
    fun `available offers are sorted mapped and highlighted`() {
        manager.load()
        client.completePurchaseQuery()
        client.completeProductQuery(
            productDetails(
                offer("large", 5_000_000, "£5.00"),
                offer("small", 1_000_000, "£1.00", listOf("highlighted")),
            )
        )

        assertEquals(
            SupportProductsState.Available(
                description = "Support development",
                options = listOf(
                    SupportPurchaseOption("small", "£1.00", 1_000_000, true),
                    SupportPurchaseOption("large", "£5.00", 5_000_000, false),
                ),
            ),
            manager.state.value.products,
        )
    }

    @Test
    fun `missing product makes products unavailable`() {
        manager.load()
        client.completePurchaseQuery()

        client.completeProductQuery(null)

        assertEquals(SupportProductsState.Unavailable, manager.state.value.products)
    }

    @Test
    fun `pending recovered purchase is shown after products load`() {
        manager.load()
        client.completePurchaseQuery(purchases = listOf(purchase("pending", Purchase.PurchaseState.PENDING)))
        client.completeProductQuery(productDetails(offer("small", 1_000_000, "£1.00")))

        assertEquals(SupportBillingMessage.PaymentPending, manager.state.value.message)
    }

    @Test
    fun `recovered purchases are consumed before products are queried`() {
        manager.load()
        client.completePurchaseQuery(
            purchases = listOf(
                purchase("one", Purchase.PurchaseState.PURCHASED),
                purchase("two", Purchase.PurchaseState.PURCHASED),
            )
        )

        assertEquals(2, client.consumeCalls.size)
        assertTrue(client.productQueries.isEmpty())

        client.completeConsume("one")
        assertTrue(client.productQueries.isEmpty())
        client.completeConsume("two", BillingClient.BillingResponseCode.ERROR)
        assertEquals(1, client.productQueries.size)
    }

    @Test
    fun `overlapping recovery coalesces consume and only latest load continues`() {
        val retained = purchase("retained", Purchase.PurchaseState.PURCHASED)
        manager.load()
        client.completePurchaseQuery(purchases = listOf(retained))
        manager.load()
        client.completePurchaseQuery(index = 1, purchases = listOf(retained))

        assertEquals(1, client.consumeCalls.size)

        client.completeConsume("retained")

        assertEquals(1, client.productQueries.size)
    }

    @Test
    fun `stale product response cannot replace newer load`() {
        manager.load()
        client.completePurchaseQuery()
        manager.load()
        client.completePurchaseQuery(index = 1)

        client.completeProductQuery(
            productDetails(offer("new", 2_000_000, "£2.00")),
            index = 1,
        )
        client.completeProductQuery(
            productDetails(offer("old", 1_000_000, "£1.00")),
            index = 0,
        )

        val available = manager.state.value.products as SupportProductsState.Available
        assertEquals(listOf("new"), available.options.map(SupportPurchaseOption::id))
    }

    @Test
    fun `clearing dialog invalidates late product response`() {
        manager.load()
        client.completePurchaseQuery()

        manager.clearTransientState()
        client.completeProductQuery(productDetails(offer("small", 1_000_000, "£1.00")))

        assertEquals(SupportProductsState.Loading, manager.state.value.products)
    }

    @Test
    fun `purchase launches selected offer and enters progress`() {
        loadAvailableProduct()

        manager.purchase(mock(), "small")

        assertEquals(1, client.launchCalls.size)
        assertTrue(manager.state.value.purchaseInProgress)
        assertNull(manager.state.value.message)
    }

    @Test
    fun `rapid repeated purchase only launches once`() {
        loadAvailableProduct()
        val activity = mock<Activity>()

        manager.purchase(activity, "small")
        manager.purchase(activity, "small")

        assertEquals(1, client.launchCalls.size)
    }

    @Test
    fun `unknown option reports payment failure`() {
        loadAvailableProduct()

        manager.purchase(mock(), "unknown")

        assertTrue(client.launchCalls.isEmpty())
        assertEquals(SupportBillingMessage.PaymentFailed, manager.state.value.message)
    }

    @Test
    fun `immediate launch failure exits progress and reports failure`() {
        loadAvailableProduct()
        client.launchResult = billingResult(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE)

        manager.purchase(mock(), "small")

        assertFalse(manager.state.value.purchaseInProgress)
        assertEquals(SupportBillingMessage.PaymentFailed, manager.state.value.message)
    }

    @Test
    fun `user cancellation exits progress without an error`() {
        loadAvailableProduct()
        manager.purchase(mock(), "small")

        client.updatePurchases(BillingClient.BillingResponseCode.USER_CANCELED)

        assertFalse(manager.state.value.purchaseInProgress)
        assertNull(manager.state.value.message)
    }

    @Test
    fun `purchase error exits progress and reports failure`() {
        loadAvailableProduct()
        manager.purchase(mock(), "small")

        client.updatePurchases(BillingClient.BillingResponseCode.ERROR)

        assertFalse(manager.state.value.purchaseInProgress)
        assertEquals(SupportBillingMessage.PaymentFailed, manager.state.value.message)
    }

    @Test
    fun `pending purchase exits progress and reports pending`() {
        loadAvailableProduct()
        manager.purchase(mock(), "small")

        client.updatePurchases(
            purchases = listOf(purchase("pending", Purchase.PurchaseState.PENDING))
        )

        assertFalse(manager.state.value.purchaseInProgress)
        assertEquals(SupportBillingMessage.PaymentPending, manager.state.value.message)
    }

    @Test
    fun `completed purchase thanks user only after successful consume`() {
        loadAvailableProduct()
        manager.purchase(mock(), "small")

        client.updatePurchases(
            purchases = listOf(purchase("paid", Purchase.PurchaseState.PURCHASED))
        )

        assertFalse(manager.state.value.showThankYou)
        assertEquals(1, client.consumeCalls.size)

        client.completeConsume("paid")

        assertTrue(manager.state.value.showThankYou)
        assertFalse(manager.state.value.purchaseInProgress)
    }

    @Test
    fun `consume failure exits progress without claiming payment failed`() {
        loadAvailableProduct()
        manager.purchase(mock(), "small")
        client.updatePurchases(
            purchases = listOf(purchase("paid", Purchase.PurchaseState.PURCHASED))
        )

        client.completeConsume("paid", BillingClient.BillingResponseCode.ERROR)

        assertFalse(manager.state.value.purchaseInProgress)
        assertFalse(manager.state.value.showThankYou)
        assertNull(manager.state.value.message)
    }

    @Test
    fun `closing dialog before consume response prevents late thank you`() {
        loadAvailableProduct()
        manager.purchase(mock(), "small")
        client.updatePurchases(
            purchases = listOf(purchase("paid", Purchase.PurchaseState.PURCHASED))
        )

        manager.clearTransientState()
        client.completeConsume("paid")

        assertFalse(manager.state.value.showThankYou)
        assertFalse(manager.state.value.purchaseInProgress)
    }

    @Test
    fun `duplicate purchase callbacks coalesce consume by token`() {
        loadAvailableProduct()
        manager.purchase(mock(), "small")
        val paid = purchase("paid", Purchase.PurchaseState.PURCHASED)

        client.updatePurchases(purchases = listOf(paid))
        client.updatePurchases(purchases = listOf(paid))

        assertEquals(1, client.consumeCalls.size)
        client.completeConsume("paid")
        assertTrue(manager.state.value.showThankYou)
    }

    @Test
    fun `concurrent duplicate purchase callbacks only start one consume`() {
        loadAvailableProduct()
        manager.purchase(mock(), "small")
        val paid = purchase("paid", Purchase.PurchaseState.PURCHASED)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val threads = List(2) {
            Thread {
                ready.countDown()
                start.await()
                client.updatePurchases(purchases = listOf(paid))
            }.apply { start() }
        }

        ready.await()
        start.countDown()
        threads.forEach(Thread::join)

        assertEquals(1, client.consumeCalls.size)
    }

    @Test
    fun `unsolicited completed purchase is consumed without showing thank you`() {
        loadAvailableProduct()

        client.updatePurchases(
            purchases = listOf(purchase("retained", Purchase.PurchaseState.PURCHASED))
        )
        client.completeConsume("retained")

        assertFalse(manager.state.value.showThankYou)
    }

    @Test
    fun `callback without support product fails active purchase`() {
        loadAvailableProduct()
        manager.purchase(mock(), "small")

        client.updatePurchases(
            purchases = listOf(
                purchase("other", Purchase.PurchaseState.PURCHASED, listOf("other_product"))
            )
        )

        assertFalse(manager.state.value.purchaseInProgress)
        assertEquals(SupportBillingMessage.PaymentFailed, manager.state.value.message)
    }

    @Test
    fun `already owned recovers retained purchase then refreshes products`() {
        loadAvailableProduct()
        manager.purchase(mock(), "small")

        client.updatePurchases(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
        assertEquals(2, client.purchaseQueries.size)
        client.completePurchaseQuery(
            index = 1,
            purchases = listOf(purchase("retained", Purchase.PurchaseState.PURCHASED)),
        )
        client.completeConsume("retained")

        assertEquals(2, client.productQueries.size)
        assertFalse(manager.state.value.purchaseInProgress)
    }

    private fun loadAvailableProduct() {
        manager.load()
        client.completePurchaseQuery()
        client.completeProductQuery(productDetails(offer("small", 1_000_000, "£1.00")))
    }

    private fun purchase(
        token: String,
        state: Int,
        products: List<String> = listOf("developer_support"),
    ): Purchase = mock {
        on { purchaseToken } doReturn token
        on { purchaseState } doReturn state
        on { this.products } doReturn products
    }

    private fun offer(
        token: String,
        priceMicros: Long,
        formattedPrice: String,
        tags: List<String> = emptyList(),
    ): ProductDetails.OneTimePurchaseOfferDetails = mock {
        on { offerToken } doReturn token
        on { priceAmountMicros } doReturn priceMicros
        on { this.formattedPrice } doReturn formattedPrice
        on { offerTags } doReturn tags
        on { rentalDetails } doReturn null
        on { preorderDetails } doReturn null
    }

    private fun productDetails(
        vararg offers: ProductDetails.OneTimePurchaseOfferDetails,
    ): ProductDetails = mock {
        on { zza() } doReturn "product-details-json"
        on { productId } doReturn "developer_support"
        on { description } doReturn "Support development"
        on { oneTimePurchaseOfferDetailsList } doReturn offers.toList()
        on { oneTimePurchaseOfferDetails } doReturn null
    }

    private class FakeSupportBillingClient : SupportBillingClient {
        override var isReady = true
        lateinit var purchaseListener: PurchasesUpdatedListener
        val connectionListeners = mutableListOf<BillingClientStateListener>()
        val purchaseQueries = mutableListOf<PurchasesResponseListener>()
        val productQueries = mutableListOf<ProductDetailsResponseListener>()
        val consumeCalls = mutableListOf<ConsumeCall>()
        val launchCalls = mutableListOf<BillingFlowParams>()
        var launchResult: BillingResult = billingResult()

        override fun setPurchasesUpdatedListener(listener: PurchasesUpdatedListener) {
            purchaseListener = listener
        }

        override fun startConnection(listener: BillingClientStateListener) {
            connectionListeners += listener
        }

        override fun queryPurchasesAsync(
            params: QueryPurchasesParams,
            listener: PurchasesResponseListener,
        ) {
            purchaseQueries += listener
        }

        override fun queryProductDetailsAsync(
            params: QueryProductDetailsParams,
            listener: ProductDetailsResponseListener,
        ) {
            productQueries += listener
        }

        override fun launchBillingFlow(
            activity: Activity,
            params: BillingFlowParams,
        ): BillingResult {
            launchCalls += params
            return launchResult
        }

        override fun consumeAsync(
            params: ConsumeParams,
            listener: ConsumeResponseListener,
        ) {
            consumeCalls += ConsumeCall(params.purchaseToken, listener)
        }

        fun finishConnection(
            code: Int = BillingClient.BillingResponseCode.OK,
            index: Int = 0,
        ) {
            isReady = code == BillingClient.BillingResponseCode.OK
            connectionListeners[index].onBillingSetupFinished(billingResult(code))
        }

        fun disconnect(index: Int = 0) {
            connectionListeners[index].onBillingServiceDisconnected()
        }

        fun completePurchaseQuery(
            code: Int = BillingClient.BillingResponseCode.OK,
            index: Int = 0,
            purchases: List<Purchase> = emptyList(),
        ) {
            purchaseQueries[index].onQueryPurchasesResponse(billingResult(code), purchases)
        }

        fun completeProductQuery(productDetails: ProductDetails?, index: Int = 0) {
            val products = listOfNotNull(productDetails)
            productQueries[index].onProductDetailsResponse(
                billingResult(),
                QueryProductDetailsResult.create(products, emptyList()),
            )
        }

        fun updatePurchases(
            code: Int = BillingClient.BillingResponseCode.OK,
            purchases: List<Purchase>? = null,
        ) {
            purchaseListener.onPurchasesUpdated(billingResult(code), purchases)
        }

        fun completeConsume(
            token: String,
            code: Int = BillingClient.BillingResponseCode.OK,
        ) {
            consumeCalls.single { it.token == token }.listener
                .onConsumeResponse(billingResult(code), token)
        }

        data class ConsumeCall(
            val token: String,
            val listener: ConsumeResponseListener,
        )
    }

    private companion object {
        fun billingResult(code: Int = BillingClient.BillingResponseCode.OK): BillingResult =
            BillingResult.newBuilder()
                .setResponseCode(code)
                .setDebugMessage("test")
                .build()
    }
}
