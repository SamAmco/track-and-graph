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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class FakeSupportBillingCoordinator : SupportBillingCoordinator {
    val loadCalls = mutableListOf<Request<SupportLoadResult>>()
    val purchaseCalls = mutableListOf<PurchaseCall>()

    override suspend fun load(): SupportLoadResult = suspendCancellableCoroutine { continuation ->
        loadCalls += Request(continuation)
    }

    override suspend fun reconcileIfNeeded() = Unit

    override suspend fun purchase(
        host: SupportBillingFlowHost,
        optionId: String,
    ): SupportPurchaseResult = suspendCancellableCoroutine { continuation ->
        val request = Request(continuation)
        purchaseCalls += PurchaseCall(host, optionId, request)
    }

    fun completeLoad(result: SupportLoadResult, index: Int = 0) {
        loadCalls[index].complete(result)
    }

    fun completePurchase(result: SupportPurchaseResult, index: Int = 0) {
        purchaseCalls[index].request.complete(result)
    }

    internal class Request<T>(
        private val continuation: CancellableContinuation<T>,
    ) {
        val canceled: Boolean get() = continuation.isCancelled

        fun complete(result: T) {
            if (continuation.isActive) continuation.resume(result)
        }
    }

    internal data class PurchaseCall(
        val host: SupportBillingFlowHost,
        val optionId: String,
        val request: Request<SupportPurchaseResult>,
    )
}
