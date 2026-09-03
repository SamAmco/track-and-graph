/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

internal class FakeSupportBillingRecoveryStore : SupportBillingRecoveryStore {
    var generation: Long? = null
    var failMark = false

    override suspend fun markReconciliationNeeded(): Long {
        if (failMark) error("write failed")
        val next = (generation ?: 0L) + 1L
        generation = next
        return next
    }

    override suspend fun reconciliationGeneration(): Long? = generation

    override suspend fun clearReconciliationNeeded(generation: Long) {
        if (this.generation == generation) this.generation = null
    }
}
