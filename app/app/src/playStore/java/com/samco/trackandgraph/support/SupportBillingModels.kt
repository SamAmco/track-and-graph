/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

internal data class SupportPurchaseOption(
    val id: String,
    val formattedPrice: String,
    val priceMicros: Long,
    val highlighted: Boolean,
)

internal sealed interface SupportLoadResult {
    val hasPendingPurchase: Boolean

    data class Available(
        val description: String,
        val options: List<SupportPurchaseOption>,
        override val hasPendingPurchase: Boolean,
    ) : SupportLoadResult

    data class Unavailable(
        override val hasPendingPurchase: Boolean,
    ) : SupportLoadResult
}

internal enum class SupportPurchaseResult {
    Completed,
    Pending,
    Canceled,
    Failed,
    ConsumptionDeferred,
    Recovered,
}
