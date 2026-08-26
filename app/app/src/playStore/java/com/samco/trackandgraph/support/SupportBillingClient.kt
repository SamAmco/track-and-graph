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
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
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

/** Logic-free seam around the Google Play BillingClient API. */
internal interface SupportBillingClient {
    val isReady: Boolean

    fun setPurchasesUpdatedListener(listener: PurchasesUpdatedListener)

    fun startConnection(listener: BillingClientStateListener)

    fun queryPurchasesAsync(
        params: QueryPurchasesParams,
        listener: PurchasesResponseListener,
    )

    fun queryProductDetailsAsync(
        params: QueryProductDetailsParams,
        listener: ProductDetailsResponseListener,
    )

    fun launchBillingFlow(
        activity: Activity,
        params: BillingFlowParams,
    ): BillingResult

    fun consumeAsync(
        params: ConsumeParams,
        listener: ConsumeResponseListener,
    )
}

@Singleton
internal class GooglePlaySupportBillingClient @Inject constructor(
    @ApplicationContext context: Context,
) : SupportBillingClient {
    private val purchasesUpdatedListener = AtomicReference<PurchasesUpdatedListener?>()

    private val delegate = BillingClient.newBuilder(context)
        .setListener { result, purchases ->
            purchasesUpdatedListener.get()?.onPurchasesUpdated(result, purchases)
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    override val isReady: Boolean
        get() = delegate.isReady

    override fun setPurchasesUpdatedListener(listener: PurchasesUpdatedListener) {
        purchasesUpdatedListener.set(listener)
    }

    override fun startConnection(listener: BillingClientStateListener) =
        delegate.startConnection(listener)

    override fun queryPurchasesAsync(
        params: QueryPurchasesParams,
        listener: PurchasesResponseListener,
    ) = delegate.queryPurchasesAsync(params, listener)

    override fun queryProductDetailsAsync(
        params: QueryProductDetailsParams,
        listener: ProductDetailsResponseListener,
    ) = delegate.queryProductDetailsAsync(params, listener)

    override fun launchBillingFlow(
        activity: Activity,
        params: BillingFlowParams,
    ): BillingResult = delegate.launchBillingFlow(activity, params)

    override fun consumeAsync(
        params: ConsumeParams,
        listener: ConsumeResponseListener,
    ) = delegate.consumeAsync(params, listener)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SupportBillingClientModule {
    @Binds
    abstract fun bindSupportBillingClient(
        implementation: GooglePlaySupportBillingClient,
    ): SupportBillingClient
}
