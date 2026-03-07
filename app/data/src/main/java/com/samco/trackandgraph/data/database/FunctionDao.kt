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

package com.samco.trackandgraph.data.database

import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.database.entity.Function
import com.samco.trackandgraph.data.database.entity.FunctionInputFeature
import com.samco.trackandgraph.data.database.entity.queryresponse.FunctionWithFeature

/**
 * Data access interface for function-related operations.
 * This interface abstracts the database operations needed by FunctionHelper,
 * allowing for different implementations (Room, fake for testing, etc.)
 */
internal interface FunctionDao {

    // =========================================================================
    // Feature operations
    // =========================================================================

    fun insertFeature(feature: Feature): Long

    fun updateFeature(feature: Feature)

    fun deleteFeature(id: Long)

    fun getFeatureById(featureId: Long): Feature?

    // =========================================================================
    // Function CRUD operations
    // =========================================================================

    fun insertFunction(function: Function): Long

    fun updateFunction(function: Function)

    fun getFunctionById(functionId: Long): FunctionWithFeature?

    fun getFunctionByFeatureId(featureId: Long): FunctionWithFeature?

    fun getFunctionsForGroupSync(groupId: Long): List<FunctionWithFeature>

    fun hasAnyFunctions(): Boolean

    // =========================================================================
    // Function input feature operations
    // =========================================================================

    fun insertFunctionInputFeature(functionInputFeature: FunctionInputFeature): Long

    fun getFunctionInputFeaturesSync(functionId: Long): List<FunctionInputFeature>

    fun deleteFunctionInputFeatures(functionId: Long)
}
