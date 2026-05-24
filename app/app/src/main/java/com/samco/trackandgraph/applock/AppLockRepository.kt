/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.applock

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.samco.trackandgraph.storage.PrefsPersistenceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface AppLockRepository {
    val config: Flow<AppLockConfig>
    suspend fun getConfig(): AppLockConfig
    suspend fun setConfig(config: AppLockConfig)
}

@Singleton
class AppLockRepositoryImpl @Inject constructor(
    prefsPersistenceProvider: PrefsPersistenceProvider,
    private val json: Json,
) : AppLockRepository {

    private val dataStore = prefsPersistenceProvider.getDataStore(DATA_STORE_NAME)

    override val config: Flow<AppLockConfig> = dataStore.data
        .map { preferences -> preferences[APP_LOCK_CONFIG_KEY].toAppLockConfig() }

    override suspend fun getConfig(): AppLockConfig =
        dataStore.data.first()[APP_LOCK_CONFIG_KEY].toAppLockConfig()

    override suspend fun setConfig(config: AppLockConfig) {
        dataStore.edit { preferences ->
            preferences[APP_LOCK_CONFIG_KEY] = json.encodeToString(config)
        }
    }

    private fun String?.toAppLockConfig(): AppLockConfig =
        this?.let { runCatching { json.decodeFromString<AppLockConfig>(it) }.getOrNull() }
            ?: AppLockConfig()

    private companion object {
        const val DATA_STORE_NAME = "app_lock_config"
        val APP_LOCK_CONFIG_KEY = stringPreferencesKey("config")
    }
}
