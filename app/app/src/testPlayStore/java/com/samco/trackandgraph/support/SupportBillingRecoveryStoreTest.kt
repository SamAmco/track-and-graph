/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

import com.samco.trackandgraph.storage.FakePrefsPersistenceProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SupportBillingRecoveryStoreTest {
    private val store = SupportBillingRecoveryStoreImpl(FakePrefsPersistenceProvider())

    @Test
    fun `store has no reconciliation marker initially`() = runTest {
        assertEquals(null, store.reconciliationGeneration())
    }

    @Test
    fun `marking reconciliation increments persisted generation`() = runTest {
        assertEquals(1L, store.markReconciliationNeeded())
        assertEquals(2L, store.markReconciliationNeeded())
        assertEquals(2L, store.reconciliationGeneration())
    }

    @Test
    fun `matching reconciliation generation clears marker`() = runTest {
        val generation = store.markReconciliationNeeded()

        store.clearReconciliationNeeded(generation)

        assertEquals(null, store.reconciliationGeneration())
    }

    @Test
    fun `stale reconciliation generation cannot clear newer marker`() = runTest {
        val oldGeneration = store.markReconciliationNeeded()
        val currentGeneration = store.markReconciliationNeeded()

        store.clearReconciliationNeeded(oldGeneration)

        assertEquals(currentGeneration, store.reconciliationGeneration())
    }
}
