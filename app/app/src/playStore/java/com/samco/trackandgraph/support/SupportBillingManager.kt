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
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

internal data class SupportPurchaseOption(
    val id: String,
    val formattedPrice: String,
    val priceMicros: Long,
    val highlighted: Boolean,
)

internal sealed interface SupportProductsState {
    data object NotLoaded : SupportProductsState
    data object Loading : SupportProductsState
    data object Unavailable : SupportProductsState
    data class Available(
        val description: String,
        val options: List<SupportPurchaseOption>,
    ) : SupportProductsState
}

internal enum class SupportBillingMessage {
    PaymentFailed,
    PaymentPending,
}

internal data class SupportBillingState(
    val products: SupportProductsState = SupportProductsState.NotLoaded,
    val purchaseInProgress: Boolean = false,
    val message: SupportBillingMessage? = null,
    val showThankYou: Boolean = false,
)

private data class PurchasableOption(
    val productDetails: ProductDetails,
    val offerDetails: ProductDetails.OneTimePurchaseOfferDetails,
)

@Singleton
internal class SupportBillingManager @Inject constructor(
    @ApplicationContext context: Context,
) : PurchasesUpdatedListener {

    private val _state = MutableStateFlow(SupportBillingState())
    val state: StateFlow<SupportBillingState> = _state.asStateFlow()

    private val purchasableOptions = mutableMapOf<String, PurchasableOption>()
    private var connectionInProgress = false

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    /** Starts all Play calls lazily when the support dialog is opened. */
    fun load() {
        _state.value = SupportBillingState(products = SupportProductsState.Loading)
        purchasableOptions.clear()

        if (billingClient.isReady) {
            recoverOutstandingPurchases(::queryProductDetails)
        } else {
            connect()
        }
    }

    fun purchase(activity: Activity, optionId: String) {
        val option = purchasableOptions[optionId] ?: run {
            showPaymentFailure()
            return
        }

        _state.value = _state.value.copy(
            purchaseInProgress = true,
            message = null,
            showThankYou = false,
        )

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(option.productDetails)
            .setOfferToken(optionId)
            .build()
        val result = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()
        )

        if (result.responseCode != BillingResponseCode.OK) {
            Timber.w("Unable to launch support purchase: %s", result.debugMessage)
            showPaymentFailure()
        }
    }

    fun clearTransientState() {
        _state.value = _state.value.copy(message = null, showThankYou = false)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                val purchase = purchases.orEmpty()
                    .firstOrNull { SUPPORT_PRODUCT_ID in it.products }
                when (purchase?.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> consume(purchase, showThankYou = true)
                    Purchase.PurchaseState.PENDING -> {
                        _state.value = _state.value.copy(
                            purchaseInProgress = false,
                            message = SupportBillingMessage.PaymentPending,
                        )
                    }
                    else -> showPaymentFailure()
                }
            }
            BillingResponseCode.USER_CANCELED -> {
                _state.value = _state.value.copy(purchaseInProgress = false, message = null)
            }
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                recoverOutstandingPurchases {
                    _state.value = _state.value.copy(purchaseInProgress = false)
                    queryProductDetails()
                }
            }
            else -> {
                Timber.w("Support purchase failed: %s", billingResult.debugMessage)
                showPaymentFailure()
            }
        }
    }

    private fun connect() {
        if (connectionInProgress) return
        connectionInProgress = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                connectionInProgress = false
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    recoverOutstandingPurchases(::queryProductDetails)
                } else {
                    Timber.w("Billing setup failed: %s", billingResult.debugMessage)
                    showProductsUnavailable()
                }
            }

            override fun onBillingServiceDisconnected() {
                connectionInProgress = false
            }
        })
    }

    /** Play retains non-consumed purchases, so failed consumes need no local persistence. */
    private fun recoverOutstandingPurchases(onComplete: () -> Unit) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) {
                Timber.w("Unable to query outstanding purchases: %s", result.debugMessage)
                onComplete()
                return@queryPurchasesAsync
            }

            val completedPurchases = purchases.filter {
                SUPPORT_PRODUCT_ID in it.products &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (purchases.any {
                    SUPPORT_PRODUCT_ID in it.products &&
                        it.purchaseState == Purchase.PurchaseState.PENDING
                }) {
                _state.value = _state.value.copy(message = SupportBillingMessage.PaymentPending)
            }
            if (completedPurchases.isEmpty()) {
                onComplete()
                return@queryPurchasesAsync
            }

            var remaining = completedPurchases.size
            completedPurchases.forEach { purchase ->
                consume(purchase, showThankYou = false) {
                    remaining -= 1
                    if (remaining == 0) onComplete()
                }
            }
        }
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(SUPPORT_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(product))
                .build()
        ) { result, detailsResult ->
            if (result.responseCode != BillingResponseCode.OK) {
                Timber.w("Unable to query support product: %s", result.debugMessage)
                showProductsUnavailable()
                return@queryProductDetailsAsync
            }

            val details = detailsResult.productDetailsList
                .firstOrNull { it.productId == SUPPORT_PRODUCT_ID }
            if (details == null) {
                showProductsUnavailable()
                return@queryProductDetailsAsync
            }

            val offers = details.oneTimePurchaseOfferDetailsList
                .orEmpty()
                .ifEmpty { listOfNotNull(details.oneTimePurchaseOfferDetails) }
                .filter { it.rentalDetails == null && it.preorderDetails == null }
                .sortedBy { it.priceAmountMicros }
                .mapNotNull { offer -> offer.offerToken?.let { token -> token to offer } }

            purchasableOptions.clear()
            offers.forEach { (token, offer) ->
                purchasableOptions[token] = PurchasableOption(details, offer)
            }
            if (offers.isEmpty()) {
                showProductsUnavailable()
            } else {
                _state.value = _state.value.copy(
                    products = SupportProductsState.Available(
                        description = details.description,
                        options = offers.map { (token, offer) ->
                            SupportPurchaseOption(
                                id = token,
                                formattedPrice = offer.formattedPrice,
                                priceMicros = offer.priceAmountMicros,
                                highlighted = HIGHLIGHTED_TAG in offer.offerTags.orEmpty(),
                            )
                        },
                    ),
                    purchaseInProgress = false,
                    showThankYou = false,
                )
            }
        }
    }

    private fun consume(
        purchase: Purchase,
        showThankYou: Boolean,
        onComplete: () -> Unit = {},
    ) {
        billingClient.consumeAsync(
            ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        ) { result, _ ->
            if (result.responseCode == BillingResponseCode.OK) {
                if (showThankYou) {
                    _state.value = _state.value.copy(
                        purchaseInProgress = false,
                        message = null,
                        showThankYou = true,
                    )
                }
            } else {
                Timber.w("Unable to consume support purchase: %s", result.debugMessage)
                if (showThankYou) {
                    // The payment itself succeeded. Play retains this purchase so a later dialog
                    // load can retry consumption; don't misleadingly report a payment failure.
                    _state.value = _state.value.copy(purchaseInProgress = false)
                }
            }
            onComplete()
        }
    }

    private fun showProductsUnavailable() {
        purchasableOptions.clear()
        _state.value = _state.value.copy(
            products = SupportProductsState.Unavailable,
            purchaseInProgress = false,
            showThankYou = false,
        )
    }

    private fun showPaymentFailure() {
        _state.value = _state.value.copy(
            purchaseInProgress = false,
            message = SupportBillingMessage.PaymentFailed,
        )
    }

    private companion object {
        const val SUPPORT_PRODUCT_ID = "developer_support"
        const val HIGHLIGHTED_TAG = "highlighted"
    }
}
