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
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
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

private data class ConsumeRequest(
    val thankYouSession: Long?,
    val onComplete: () -> Unit,
)

@Singleton
internal class SupportBillingManager @Inject constructor(
    private val billingClient: SupportBillingClient,
) : PurchasesUpdatedListener {

    private val lock = Any()
    private val _state = MutableStateFlow(SupportBillingState())
    val state: StateFlow<SupportBillingState> = _state.asStateFlow()

    /** All fields below are read and written only while [lock] is held. */
    private val purchasableOptions = mutableMapOf<String, PurchasableOption>()
    private val consumeRequests = mutableMapOf<String, MutableList<ConsumeRequest>>()
    private var connectionInProgress = false
    private var connectionSession: Long? = null
    private var connectionAttempt = 0L
    private var currentSession = 0L
    private var sessionActive = false
    private var activePurchaseSession: Long? = null

    init {
        billingClient.setPurchasesUpdatedListener(this)
    }

    /** Starts all Play calls lazily when the support dialog is opened. */
    fun load() = serialized {
        val session = startSession()
        _state.value = SupportBillingState(products = SupportProductsState.Loading)
        purchasableOptions.clear()

        if (billingClient.isReady) {
            recoverOutstandingPurchases(session) { queryProductDetails(session) }
        } else {
            connect(session)
        }
    }

    fun purchase(activity: Activity, optionId: String) = serialized {
        val session = currentSession.takeIf { isCurrentSession(it) } ?: return@serialized
        if (_state.value.purchaseInProgress) return@serialized

        val option = purchasableOptions[optionId] ?: run {
            showPaymentFailure(session)
            return@serialized
        }

        activePurchaseSession = session
        _state.value = _state.value.copy(
            purchaseInProgress = true,
            message = null,
            showThankYou = false,
        )

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(option.productDetails)
            .setOfferToken(option.offerDetails.offerToken ?: optionId)
            .build()
        val result = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()
        )

        if (
            result.responseCode != BillingResponseCode.OK &&
            activePurchaseSession == session
        ) {
            Timber.w("Unable to launch support purchase: %s", result.debugMessage)
            activePurchaseSession = null
            showPaymentFailure(session)
        }
    }

    /** Invalidates callbacks belonging to the dialog session that just closed. */
    fun clearTransientState() = serialized {
        currentSession += 1
        sessionActive = false
        activePurchaseSession = null
        purchasableOptions.clear()
        _state.value = _state.value.copy(
            purchaseInProgress = false,
            message = null,
            showThankYou = false,
        )
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) = serialized {
        val purchaseSession = activePurchaseSession?.takeIf(::isCurrentSession)

        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                val purchase = purchases.orEmpty()
                    .firstOrNull { SUPPORT_PRODUCT_ID in it.products }
                when (purchase?.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> consume(
                        purchase = purchase,
                        thankYouSession = purchaseSession,
                    )

                    Purchase.PurchaseState.PENDING -> {
                        activePurchaseSession = null
                        if (purchaseSession != null) {
                            _state.value = _state.value.copy(
                                purchaseInProgress = false,
                                message = SupportBillingMessage.PaymentPending,
                            )
                        }
                    }

                    else -> finishPurchaseWithFailure(purchaseSession)
                }
            }

            BillingResponseCode.USER_CANCELED -> {
                activePurchaseSession = null
                if (purchaseSession != null) {
                    _state.value = _state.value.copy(
                        purchaseInProgress = false,
                        message = null,
                    )
                }
            }

            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                activePurchaseSession = null
                val session = purchaseSession ?: currentSession.takeIf(::isCurrentSession)
                if (session != null) {
                    recoverOutstandingPurchases(session) {
                        if (isCurrentSession(session)) {
                            _state.value = _state.value.copy(purchaseInProgress = false)
                            queryProductDetails(session)
                        }
                    }
                }
            }

            else -> {
                Timber.w("Support purchase failed: %s", billingResult.debugMessage)
                finishPurchaseWithFailure(purchaseSession)
            }
        }
    }

    private fun connect(session: Long) {
        connectionSession = session
        if (connectionInProgress) return

        connectionInProgress = true
        val attempt = ++connectionAttempt
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                serialized {
                    if (attempt != connectionAttempt) return@serialized
                    connectionInProgress = false
                    val requestedSession = connectionSession
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        if (requestedSession != null && isCurrentSession(requestedSession)) {
                            recoverOutstandingPurchases(requestedSession) {
                                queryProductDetails(requestedSession)
                            }
                        }
                    } else {
                        Timber.w("Billing setup failed: %s", billingResult.debugMessage)
                        requestedSession?.let(::showProductsUnavailable)
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                serialized {
                    if (attempt != connectionAttempt) return@serialized
                    connectionInProgress = false
                    val requestedSession = connectionSession
                    if (
                        requestedSession != null &&
                        isCurrentSession(requestedSession) &&
                        _state.value.products == SupportProductsState.Loading
                    ) {
                        showProductsUnavailable(requestedSession)
                    }
                }
            }
        })
    }

    /** Play retains non-consumed purchases, so failed consumes need no local persistence. */
    private fun recoverOutstandingPurchases(
        session: Long,
        onComplete: () -> Unit,
    ) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            serialized {
                if (result.responseCode != BillingResponseCode.OK) {
                    Timber.w(
                        "Unable to query outstanding purchases: %s",
                        result.debugMessage,
                    )
                    if (isCurrentSession(session)) onComplete()
                    return@serialized
                }

                if (
                    isCurrentSession(session) &&
                    purchases.any {
                        SUPPORT_PRODUCT_ID in it.products &&
                            it.purchaseState == Purchase.PurchaseState.PENDING
                    }
                ) {
                    _state.value = _state.value.copy(
                        message = SupportBillingMessage.PaymentPending,
                    )
                }

                val completedPurchases = purchases
                    .filter {
                        SUPPORT_PRODUCT_ID in it.products &&
                            it.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    .distinctBy(Purchase::getPurchaseToken)

                if (completedPurchases.isEmpty()) {
                    if (isCurrentSession(session)) onComplete()
                    return@serialized
                }

                var remaining = completedPurchases.size
                completedPurchases.forEach { purchase ->
                    consume(purchase, thankYouSession = null) {
                        remaining -= 1
                        if (remaining == 0 && isCurrentSession(session)) onComplete()
                    }
                }
            }
        }
    }

    private fun queryProductDetails(session: Long) {
        if (!isCurrentSession(session)) return

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(SUPPORT_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(product))
                .build()
        ) { result, detailsResult ->
            serialized {
                if (!isCurrentSession(session)) return@serialized

                if (result.responseCode != BillingResponseCode.OK) {
                    Timber.w("Unable to query support product: %s", result.debugMessage)
                    showProductsUnavailable(session)
                    return@serialized
                }

                val details = detailsResult.productDetailsList
                    .firstOrNull { it.productId == SUPPORT_PRODUCT_ID }
                if (details == null) {
                    showProductsUnavailable(session)
                    return@serialized
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
                    showProductsUnavailable(session)
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
    }

    /** Coalesces duplicate consume attempts for the same Play purchase token. */
    private fun consume(
        purchase: Purchase,
        thankYouSession: Long?,
        onComplete: () -> Unit = {},
    ) {
        val token = purchase.purchaseToken
        val request = ConsumeRequest(thankYouSession, onComplete)
        consumeRequests[token]?.let {
            it += request
            return
        }
        consumeRequests[token] = mutableListOf(request)

        billingClient.consumeAsync(
            ConsumeParams.newBuilder()
                .setPurchaseToken(token)
                .build()
        ) { result, _ ->
            serialized {
                val completedRequests = consumeRequests.remove(token).orEmpty()
                val currentThankYouSession = completedRequests
                    .mapNotNull(ConsumeRequest::thankYouSession)
                    .firstOrNull(::isCurrentSession)

                completedRequests
                    .mapNotNull(ConsumeRequest::thankYouSession)
                    .firstOrNull { it == activePurchaseSession }
                    ?.let { activePurchaseSession = null }

                if (result.responseCode == BillingResponseCode.OK) {
                    if (currentThankYouSession != null) {
                        _state.value = _state.value.copy(
                            purchaseInProgress = false,
                            message = null,
                            showThankYou = true,
                        )
                    }
                } else {
                    Timber.w("Unable to consume support purchase: %s", result.debugMessage)
                    if (currentThankYouSession != null) {
                        // The payment itself succeeded. Play retains this purchase so a later
                        // dialog load can retry consumption; don't report a payment failure.
                        _state.value = _state.value.copy(purchaseInProgress = false)
                    }
                }

                completedRequests.forEach { it.onComplete() }
            }
        }
    }

    private fun startSession(): Long {
        currentSession += 1
        sessionActive = true
        activePurchaseSession = null
        connectionSession = currentSession
        return currentSession
    }

    private fun isCurrentSession(session: Long): Boolean =
        sessionActive && session == currentSession

    private fun showProductsUnavailable(session: Long) {
        if (!isCurrentSession(session)) return
        purchasableOptions.clear()
        _state.value = _state.value.copy(
            products = SupportProductsState.Unavailable,
            purchaseInProgress = false,
            showThankYou = false,
        )
    }

    private fun finishPurchaseWithFailure(session: Long?) {
        activePurchaseSession = null
        session?.let(::showPaymentFailure)
    }

    private fun showPaymentFailure(session: Long) {
        if (!isCurrentSession(session)) return
        _state.value = _state.value.copy(
            purchaseInProgress = false,
            message = SupportBillingMessage.PaymentFailed,
        )
    }

    private inline fun <T> serialized(block: () -> T): T = synchronized(lock, block)

    private companion object {
        const val SUPPORT_PRODUCT_ID = "developer_support"
        const val HIGHLIGHTED_TAG = "highlighted"
    }
}
