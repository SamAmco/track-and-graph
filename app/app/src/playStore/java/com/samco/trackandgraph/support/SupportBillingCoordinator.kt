/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

internal interface SupportBillingCoordinator {
    suspend fun load(): SupportLoadResult

    suspend fun reconcileIfNeeded()

    suspend fun purchase(
        host: SupportBillingFlowHost,
        optionId: String,
    ): SupportPurchaseResult
}

private class PurchaseContinuation(
    private val continuation: CancellableContinuation<SupportPurchaseResult>,
) {
    val isActive: Boolean get() = continuation.isActive

    fun complete(result: SupportPurchaseResult) {
        if (continuation.isActive) continuation.resume(result)
    }
}

private enum class OutstandingPurchaseRecovery {
    Settled,
    Pending,
    Deferred,
}

/**
 * Process-scoped billing orchestration. It owns Play connection and settlement mechanics, but no
 * screen state or presentation decisions.
 */
@Singleton
internal class SupportBillingCoordinatorImpl @Inject constructor(
    private val gateway: SupportBillingGateway,
    private val recoveryStore: SupportBillingRecoveryStore,
) : SupportBillingCoordinator {
    private val lock = Any()
    private val connectionWaiters = mutableListOf<(Boolean) -> Unit>()
    private val consumeWaiters = mutableMapOf<String, MutableList<(Boolean) -> Unit>>()
    private var connectionInProgress = false
    private var connectionAttempt = 0L
    private var purchaseStarting = false
    private var activePurchase: PurchaseContinuation? = null

    init {
        gateway.setPurchaseUpdateListener(::onPurchasesUpdated)
    }

    override suspend fun load(): SupportLoadResult = suspendCancellableCoroutine { continuation ->
        serialized {
            withConnection { connected ->
                if (!continuation.isActive) return@withConnection
                if (connected) {
                    recoverForLoad(
                        isActive = { continuation.isActive },
                        onResult = { result ->
                            if (continuation.isActive) continuation.resume(result)
                        },
                    )
                } else {
                    continuation.resume(SupportLoadResult.Unavailable(false))
                }
            }
        }
    }

    override suspend fun purchase(
        host: SupportBillingFlowHost,
        optionId: String,
    ): SupportPurchaseResult {
        val reserved = serialized {
            if (purchaseStarting || activePurchase != null) {
                false
            } else {
                purchaseStarting = true
                true
            }
        }
        if (!reserved) return SupportPurchaseResult.Failed

        try {
            recoveryStore.markReconciliationNeeded()
        } catch (cancellation: CancellationException) {
            serialized { purchaseStarting = false }
            throw cancellation
        } catch (throwable: Throwable) {
            serialized { purchaseStarting = false }
            Timber.w(throwable, "Unable to persist support purchase recovery marker")
            return SupportPurchaseResult.Failed
        }

        return purchaseReserved(host, optionId)
    }

    private suspend fun purchaseReserved(
        host: SupportBillingFlowHost,
        optionId: String,
    ): SupportPurchaseResult = suspendCancellableCoroutine { continuation ->
        val request = PurchaseContinuation(continuation)
        serialized {
            purchaseStarting = false
            if (!request.isActive) {
                return@serialized
            }

            activePurchase = request
            withConnection { connected ->
                if (activePurchase !== request) return@withConnection
                if (!request.isActive) {
                    activePurchase = null
                    return@withConnection
                }
                if (!connected) {
                    activePurchase = null
                    request.complete(SupportPurchaseResult.Failed)
                    return@withConnection
                }

                val result = gateway.launchBillingFlow(host, optionId)
                when (result.response) {
                    PlatformBillingResponse.Ok -> Unit
                    PlatformBillingResponse.ItemAlreadyOwned -> {
                        recoverOutstandingPurchases { recovery ->
                            if (activePurchase === request) activePurchase = null
                            request.complete(
                                recovery.toPurchaseResult()
                            )
                        }
                    }

                    PlatformBillingResponse.UserCanceled -> {
                        activePurchase = null
                        request.complete(SupportPurchaseResult.Canceled)
                    }

                    PlatformBillingResponse.Error -> {
                        Timber.w("Unable to launch support purchase: %s", result.debugMessage)
                        activePurchase = null
                        request.complete(SupportPurchaseResult.Failed)
                    }
                }
            }
        }
    }

    override suspend fun reconcileIfNeeded() {
        val generation = recoveryStore.reconciliationGeneration() ?: return
        val purchaseIsActive = serialized { purchaseStarting || activePurchase != null }
        if (purchaseIsActive) return

        val outcome = recoverOutstandingPurchasesWithConnection()
        if (outcome == OutstandingPurchaseRecovery.Settled) {
            recoveryStore.clearReconciliationNeeded(generation)
        }
    }

    private fun withConnection(onResult: (Boolean) -> Unit) {
        if (gateway.isReady) {
            onResult(true)
            return
        }

        connectionWaiters += onResult
        if (connectionInProgress) return

        connectionInProgress = true
        val attempt = ++connectionAttempt
        gateway.startConnection(
            onFinished = { result ->
                serialized {
                    if (attempt != connectionAttempt) return@serialized
                    connectionInProgress = false
                    if (result.response != PlatformBillingResponse.Ok) {
                        Timber.w("Billing setup failed: %s", result.debugMessage)
                    }
                    completeConnectionWaiters(result.response == PlatformBillingResponse.Ok)
                }
            },
            onDisconnected = {
                serialized {
                    if (attempt != connectionAttempt) return@serialized
                    connectionInProgress = false
                    connectionAttempt += 1
                    completeConnectionWaiters(false)
                }
            },
        )
    }

    private fun completeConnectionWaiters(connected: Boolean) {
        val waiters = connectionWaiters.toList()
        connectionWaiters.clear()
        waiters.forEach { it(connected) }
    }

    private fun recoverForLoad(
        isActive: () -> Boolean,
        onResult: (SupportLoadResult) -> Unit,
    ) {
        gateway.queryPurchases { result, purchases ->
            serialized {
                if (result.response != PlatformBillingResponse.Ok) {
                    Timber.w("Unable to query outstanding purchases: %s", result.debugMessage)
                    if (isActive()) queryProduct(onResult, hasPendingPurchase = false)
                    return@serialized
                }

                val hasPendingPurchase = purchases.any {
                    SUPPORT_PRODUCT_ID in it.productIds &&
                        it.state == PlatformPurchaseState.Pending
                }
                val completedPurchases = purchases
                    .filter {
                        SUPPORT_PRODUCT_ID in it.productIds &&
                            it.state == PlatformPurchaseState.Purchased
                    }
                    .distinctBy(PlatformPurchase::token)

                consumeAll(completedPurchases) { _ ->
                    if (isActive()) queryProduct(onResult, hasPendingPurchase)
                }
            }
        }
    }

    private fun queryProduct(
        onResult: (SupportLoadResult) -> Unit,
        hasPendingPurchase: Boolean,
    ) {
        gateway.queryProduct(SUPPORT_PRODUCT_ID) { result, product ->
            serialized {
                if (result.response != PlatformBillingResponse.Ok || product == null) {
                    Timber.w("Unable to query support product: %s", result.debugMessage)
                    onResult(SupportLoadResult.Unavailable(hasPendingPurchase))
                    return@serialized
                }

                val options = product.offers
                    .filter { !it.isRental && !it.isPreorder }
                    .sortedBy(PlatformOffer::priceMicros)
                    .map {
                        SupportPurchaseOption(
                            id = it.id,
                            formattedPrice = it.formattedPrice,
                            priceMicros = it.priceMicros,
                            highlighted = HIGHLIGHTED_TAG in it.tags,
                        )
                    }
                if (options.isEmpty()) {
                    onResult(SupportLoadResult.Unavailable(hasPendingPurchase))
                } else {
                    onResult(
                        SupportLoadResult.Available(
                            description = product.description,
                            options = options,
                            hasPendingPurchase = hasPendingPurchase,
                        )
                    )
                }
            }
        }
    }

    private fun onPurchasesUpdated(
        result: PlatformBillingResult,
        purchases: List<PlatformPurchase>?,
    ) = serialized {
        val purchaseRequest = activePurchase

        when (result.response) {
            PlatformBillingResponse.Ok -> {
                val purchase = purchases.orEmpty()
                    .firstOrNull { SUPPORT_PRODUCT_ID in it.productIds }
                when (purchase?.state) {
                    PlatformPurchaseState.Purchased -> consume(purchase.token) { consumed ->
                        serialized {
                            if (activePurchase === purchaseRequest) activePurchase = null
                            purchaseRequest?.complete(
                                if (consumed) SupportPurchaseResult.Completed
                                else SupportPurchaseResult.ConsumptionDeferred
                            )
                        }
                    }

                    PlatformPurchaseState.Pending -> {
                        if (activePurchase === purchaseRequest) activePurchase = null
                        purchaseRequest?.complete(SupportPurchaseResult.Pending)
                    }

                    else -> {
                        if (activePurchase === purchaseRequest) activePurchase = null
                        purchaseRequest?.complete(SupportPurchaseResult.Failed)
                    }
                }
            }

            PlatformBillingResponse.UserCanceled -> {
                if (activePurchase === purchaseRequest) activePurchase = null
                purchaseRequest?.complete(SupportPurchaseResult.Canceled)
            }

            PlatformBillingResponse.ItemAlreadyOwned -> {
                recoverOutstandingPurchases { recovery ->
                    if (activePurchase === purchaseRequest) activePurchase = null
                    purchaseRequest?.complete(
                        recovery.toPurchaseResult()
                    )
                }
            }

            PlatformBillingResponse.Error -> {
                Timber.w("Support purchase failed: %s", result.debugMessage)
                if (activePurchase === purchaseRequest) activePurchase = null
                purchaseRequest?.complete(SupportPurchaseResult.Failed)
            }
        }
    }

    private suspend fun recoverOutstandingPurchasesWithConnection(): OutstandingPurchaseRecovery =
        suspendCancellableCoroutine { continuation ->
            serialized {
                withConnection { connected ->
                    if (!continuation.isActive) return@withConnection
                    if (!connected) {
                        continuation.resume(OutstandingPurchaseRecovery.Deferred)
                        return@withConnection
                    }
                    recoverOutstandingPurchases { recovery ->
                        if (continuation.isActive) continuation.resume(recovery)
                    }
                }
            }
        }

    private fun recoverOutstandingPurchases(
        onComplete: (OutstandingPurchaseRecovery) -> Unit,
    ) {
        gateway.queryPurchases { result, purchases ->
            serialized {
                if (result.response != PlatformBillingResponse.Ok) {
                    Timber.w("Unable to query outstanding purchases: %s", result.debugMessage)
                    onComplete(OutstandingPurchaseRecovery.Deferred)
                    return@serialized
                }
                val hasPendingPurchase = purchases.any {
                    SUPPORT_PRODUCT_ID in it.productIds &&
                        it.state == PlatformPurchaseState.Pending
                }
                consumeAll(
                    purchases
                        .filter {
                            SUPPORT_PRODUCT_ID in it.productIds &&
                                it.state == PlatformPurchaseState.Purchased
                        }
                        .distinctBy(PlatformPurchase::token),
                ) { allConsumed ->
                    onComplete(
                        when {
                            hasPendingPurchase -> OutstandingPurchaseRecovery.Pending
                            allConsumed -> OutstandingPurchaseRecovery.Settled
                            else -> OutstandingPurchaseRecovery.Deferred
                        }
                    )
                }
            }
        }
    }

    private fun consumeAll(
        purchases: List<PlatformPurchase>,
        onComplete: (allConsumed: Boolean) -> Unit,
    ) {
        if (purchases.isEmpty()) {
            onComplete(true)
            return
        }

        var remaining = purchases.size
        var allConsumed = true
        purchases.forEach { purchase ->
            consume(purchase.token) { consumed ->
                allConsumed = allConsumed && consumed
                remaining -= 1
                if (remaining == 0) onComplete(allConsumed)
            }
        }
    }

    private fun OutstandingPurchaseRecovery.toPurchaseResult(): SupportPurchaseResult = when (this) {
        OutstandingPurchaseRecovery.Settled -> SupportPurchaseResult.Recovered
        OutstandingPurchaseRecovery.Pending -> SupportPurchaseResult.Pending
        OutstandingPurchaseRecovery.Deferred -> SupportPurchaseResult.ConsumptionDeferred
    }

    /** Coalesces duplicate consume attempts for the same Play purchase token. */
    private fun consume(
        purchaseToken: String,
        onComplete: (Boolean) -> Unit,
    ) {
        consumeWaiters[purchaseToken]?.let {
            it += onComplete
            return
        }
        consumeWaiters[purchaseToken] = mutableListOf(onComplete)

        gateway.consume(purchaseToken) { result ->
            serialized {
                val callbacks = consumeWaiters.remove(purchaseToken).orEmpty()
                val consumed = result.response == PlatformBillingResponse.Ok
                if (!consumed) {
                    Timber.w("Unable to consume support purchase: %s", result.debugMessage)
                }
                callbacks.forEach { it(consumed) }
            }
        }
    }

    private inline fun <T> serialized(block: () -> T): T = synchronized(lock, block)

    private companion object {
        const val SUPPORT_PRODUCT_ID = "developer_support"
        const val HIGHLIGHTED_TAG = "highlighted"
    }
}
