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
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

internal enum class PlatformBillingResponse {
    Ok,
    UserCanceled,
    ItemAlreadyOwned,
    Error,
}

internal data class PlatformBillingResult(
    val response: PlatformBillingResponse,
    val debugMessage: String = "",
)

internal enum class PlatformPurchaseState {
    Purchased,
    Pending,
    Other,
}

internal data class PlatformPurchase(
    val token: String,
    val productIds: List<String>,
    val state: PlatformPurchaseState,
)

internal data class PlatformProduct(
    val productId: String,
    val description: String,
    val offers: List<PlatformOffer>,
)

internal data class PlatformOffer(
    val id: String,
    val formattedPrice: String,
    val priceMicros: Long,
    val tags: List<String>,
    val isRental: Boolean,
    val isPreorder: Boolean,
)

internal interface SupportBillingFlowHost

private data class AndroidSupportBillingFlowHost(
    val activity: Activity,
) : SupportBillingFlowHost

internal fun Activity.asSupportBillingFlowHost(): SupportBillingFlowHost =
    AndroidSupportBillingFlowHost(this)

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

private data class GooglePlayOffer(
    val productDetails: ProductDetails,
    val offerDetails: ProductDetails.OneTimePurchaseOfferDetails,
)

internal class LatestQueryCache<K, V> {
    private val lock = Any()
    private var generation = 0L
    private var values = emptyMap<K, V>()

    fun beginQuery(): Long = synchronized(lock) { ++generation }

    fun replaceIfLatest(queryGeneration: Long, newValues: Map<K, V>): Boolean =
        synchronized(lock) {
            if (queryGeneration != generation) return@synchronized false
            values = newValues
            true
        }

    operator fun get(key: K): V? = synchronized(lock) { values[key] }
}

@Singleton
internal class GooglePlaySupportBillingGateway @Inject constructor(
    @ApplicationContext context: Context,
) : SupportBillingGateway {
    private val purchaseUpdateListener =
        AtomicReference<((PlatformBillingResult, List<PlatformPurchase>?) -> Unit)?>(null)
    private val offers = LatestQueryCache<String, GooglePlayOffer>()
    private val billingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases ->
            purchaseUpdateListener.get()?.invoke(
                result.toPlatformResult(),
                purchases?.map(Purchase::toPlatformPurchase),
            )
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    override val isReady: Boolean
        get() = billingClient.isReady

    override fun setPurchaseUpdateListener(
        listener: (PlatformBillingResult, List<PlatformPurchase>?) -> Unit,
    ) {
        purchaseUpdateListener.set(listener)
    }

    override fun startConnection(
        onFinished: (PlatformBillingResult) -> Unit,
        onDisconnected: () -> Unit,
    ) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                onFinished(billingResult.toPlatformResult())
            }

            override fun onBillingServiceDisconnected() {
                onDisconnected()
            }
        })
    }

    override fun queryPurchases(
        callback: (PlatformBillingResult, List<PlatformPurchase>) -> Unit,
    ) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            callback(result.toPlatformResult(), purchases.map(Purchase::toPlatformPurchase))
        }
    }

    override fun queryProduct(
        productId: String,
        callback: (PlatformBillingResult, PlatformProduct?) -> Unit,
    ) {
        val queryGeneration = offers.beginQuery()
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(product))
                .build()
        ) { result, detailsResult ->
            val details = detailsResult.productDetailsList
                .firstOrNull { it.productId == productId }
            val offerDetails = details?.oneTimePurchaseOfferDetailsList
                .orEmpty()
                .ifEmpty { listOfNotNull(details?.oneTimePurchaseOfferDetails) }
                .mapNotNull { offer -> offer.offerToken?.let { it to offer } }

            val queryOffers = if (details == null) emptyMap() else offerDetails.associate {
                (offerId, offer) -> offerId to GooglePlayOffer(details, offer)
            }
            if (!offers.replaceIfLatest(queryGeneration, queryOffers)) {
                callback(
                    PlatformBillingResult(
                        response = PlatformBillingResponse.Error,
                        debugMessage = "Support product query was superseded",
                    ),
                    null,
                )
                return@queryProductDetailsAsync
            }

            callback(
                result.toPlatformResult(),
                details?.let {
                    PlatformProduct(
                        productId = it.productId,
                        description = it.description,
                        offers = offerDetails.map { (offerId, offer) ->
                            PlatformOffer(
                                id = offerId,
                                formattedPrice = offer.formattedPrice,
                                priceMicros = offer.priceAmountMicros,
                                tags = offer.offerTags.orEmpty(),
                                isRental = offer.rentalDetails != null,
                                isPreorder = offer.preorderDetails != null,
                            )
                        },
                    )
                },
            )
        }
    }

    override fun launchBillingFlow(
        host: SupportBillingFlowHost,
        offerId: String,
    ): PlatformBillingResult {
        val activity = (host as? AndroidSupportBillingFlowHost)?.activity
            ?: return PlatformBillingResult(
                response = PlatformBillingResponse.Error,
                debugMessage = "Invalid billing flow host",
            )
        val offer = offers[offerId]
            ?: return PlatformBillingResult(
                response = PlatformBillingResponse.Error,
                debugMessage = "Unknown support offer",
            )
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(offer.productDetails)
            .setOfferToken(offer.offerDetails.offerToken ?: offerId)
            .build()
        return billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()
        ).toPlatformResult()
    }

    override fun consume(
        purchaseToken: String,
        callback: (PlatformBillingResult) -> Unit,
    ) {
        billingClient.consumeAsync(
            ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
        ) { result, _ -> callback(result.toPlatformResult()) }
    }
}

private fun BillingResult.toPlatformResult() = PlatformBillingResult(
    response = when (responseCode) {
        BillingClient.BillingResponseCode.OK -> PlatformBillingResponse.Ok
        BillingClient.BillingResponseCode.USER_CANCELED -> PlatformBillingResponse.UserCanceled
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
            PlatformBillingResponse.ItemAlreadyOwned

        else -> PlatformBillingResponse.Error
    },
    debugMessage = debugMessage,
)

private fun Purchase.toPlatformPurchase() = PlatformPurchase(
    token = purchaseToken,
    productIds = products,
    state = when (purchaseState) {
        Purchase.PurchaseState.PURCHASED -> PlatformPurchaseState.Purchased
        Purchase.PurchaseState.PENDING -> PlatformPurchaseState.Pending
        else -> PlatformPurchaseState.Other
    },
)

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SupportBillingGatewayModule {
    @Binds
    abstract fun bindSupportBillingGateway(
        implementation: GooglePlaySupportBillingGateway,
    ): SupportBillingGateway
}
