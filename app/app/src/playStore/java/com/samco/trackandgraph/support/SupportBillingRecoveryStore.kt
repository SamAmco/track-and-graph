/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.samco.trackandgraph.storage.PrefsPersistenceProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

internal interface SupportBillingRecoveryStore {
    /** Marks a new checkout attempt and returns its monotonically increasing generation. */
    suspend fun markReconciliationNeeded(): Long

    suspend fun reconciliationGeneration(): Long?

    /** Clears only the attempt that was reconciled, never a newer checkout marker. */
    suspend fun clearReconciliationNeeded(generation: Long)
}

@Singleton
internal class SupportBillingRecoveryStoreImpl @Inject constructor(
    prefsPersistenceProvider: PrefsPersistenceProvider,
) : SupportBillingRecoveryStore {
    private val dataStore = prefsPersistenceProvider.getDataStore(DATA_STORE_NAME)

    override suspend fun markReconciliationNeeded(): Long {
        var generation = 1L
        dataStore.edit { preferences ->
            generation = (preferences[RECONCILIATION_GENERATION] ?: 0L)
                .takeUnless { it == Long.MAX_VALUE }
                ?.plus(1L)
                ?: 1L
            preferences[RECONCILIATION_GENERATION] = generation
        }
        return generation
    }

    override suspend fun reconciliationGeneration(): Long? =
        dataStore.data.first()[RECONCILIATION_GENERATION]

    override suspend fun clearReconciliationNeeded(generation: Long) {
        dataStore.edit { preferences ->
            if (preferences[RECONCILIATION_GENERATION] == generation) {
                preferences.remove(RECONCILIATION_GENERATION)
            }
        }
    }

    private companion object {
        const val DATA_STORE_NAME = "support_billing_recovery"
        val RECONCILIATION_GENERATION = longPreferencesKey("reconciliation_generation")
    }
}
