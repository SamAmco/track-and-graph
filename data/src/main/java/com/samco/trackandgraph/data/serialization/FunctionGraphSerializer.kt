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
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles serialization and deserialization of FunctionGraph objects to/from JSON strings.
 * Uses the centralized Json instance for consistent configuration across the app.
 */
@Singleton
class FunctionGraphSerializer @Inject constructor(
    private val json: Json,
    private val throwOnFailure: Boolean = BuildConfig.DEBUG
) {
    
    /**
     * Serializes a FunctionGraph DTO to a JSON string for database storage.
     */
    fun serialize(functionGraph: FunctionGraph): String? {
        return try {
            json.encodeToString(FunctionGraph.serializer(), functionGraph)
        } catch (e: Exception) {
            Timber.e(e, "Failed to serialize FunctionGraph: $functionGraph")
            // Return empty string on serialization failure in production
            // In debug mode, let the exception propagate for debugging
            if (throwOnFailure) throw e
            else null
        }
    }
    
    /**
     * Deserializes a JSON string from the database back to a FunctionGraph DTO.
     */
    fun deserialize(jsonString: String): FunctionGraph? {
        return try {
            json.decodeFromString(FunctionGraph.serializer(), jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Failed to deserialize FunctionGraph: $jsonString")
            // Return default FunctionGraph on deserialization failure in production
            // In debug mode, let the exception propagate for debugging
            if (throwOnFailure) throw e
            else null
        }
    }
}
