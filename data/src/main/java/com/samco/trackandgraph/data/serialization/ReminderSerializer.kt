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

package com.samco.trackandgraph.data.serialization

import com.samco.trackandgraph.data.BuildConfig
import com.samco.trackandgraph.data.database.dto.ReminderParams
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles serialization and deserialization of Reminder objects to/from JSON strings.
 * Uses the centralized Json instance for consistent configuration across the app.
 */
@Singleton
class ReminderSerializer @Inject constructor(
    private val json: Json,
) {

    /**
     * Serializes ReminderParams to a JSON string for database storage.
     */
    fun serializeParams(
        params: ReminderParams,
        throwOnFailure: Boolean = BuildConfig.DEBUG
    ): String? {
        return try {
            json.encodeToString(ReminderParams.serializer(), params)
        } catch (e: Exception) {
            Timber.e(e, "Failed to serialize ReminderParams: $params")
            if (throwOnFailure) throw e
            else null
        }
    }

    /**
     * Deserializes a JSON string from the database back to ReminderParams.
     */
    fun deserializeParams(
        jsonString: String,
        throwOnFailure: Boolean = BuildConfig.DEBUG
    ): ReminderParams? {
        return try {
            json.decodeFromString(ReminderParams.serializer(), jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Failed to deserialize ReminderParams: $jsonString")
            if (throwOnFailure) throw e
            else null
        }
    }
}
