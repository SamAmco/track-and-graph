/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

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
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

private data class GooglePlayOffer(
    val productDetails: ProductDetails,
    val offerDetails: ProductDetails.OneTimePurchaseOfferDetails,
)

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
