/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

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
