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

package com.samco.trackandgraph

import com.samco.trackandgraph.data.database.FunctionDao
import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.database.entity.Function
import com.samco.trackandgraph.data.database.entity.FunctionInputFeature
import com.samco.trackandgraph.data.database.entity.queryresponse.FunctionWithFeature

/**
 * A fake in-memory implementation of [FunctionDao] for testing purposes.
 * This allows tests to run without a real database and without mocking frameworks.
 */
internal class FakeFunctionDao : FunctionDao {

    private var nextFeatureId = 1L
    private var nextFunctionId = 1L
    private var nextInputFeatureId = 1L

    private val features = mutableMapOf<Long, Feature>()
    private val functions = mutableMapOf<Long, Function>()
    private val inputFeatures = mutableMapOf<Long, FunctionInputFeature>()

    fun clear() {
        features.clear()
        functions.clear()
        inputFeatures.clear()
        nextFeatureId = 1L
        nextFunctionId = 1L
        nextInputFeatureId = 1L
    }

    fun numFunctions(): Int = functions.size

    // =========================================================================
    // Feature operations
    // =========================================================================

    override fun insertFeature(feature: Feature): Long {
        val id = if (feature.id == 0L) nextFeatureId++ else feature.id
        features[id] = feature.copy(id = id)
        return id
    }

    override fun updateFeature(feature: Feature) {
        features[feature.id] = feature
    }

    override fun deleteFeature(id: Long) {
        features.remove(id)
        functions.entries.removeIf { it.value.featureId == id }
    }

    override fun getFeatureById(featureId: Long): Feature? = features[featureId]

    // =========================================================================
    // Function CRUD operations
    // =========================================================================

    override fun insertFunction(function: Function): Long {
        val id = if (function.id == 0L) nextFunctionId++ else function.id
        functions[id] = function.copy(id = id)
        return id
    }

    override fun updateFunction(function: Function) {
        functions[function.id] = function
    }

    override fun getFunctionById(functionId: Long): FunctionWithFeature? {
        val function = functions[functionId] ?: return null
        val feature = features[function.featureId] ?: return null
        return FunctionWithFeature(
            id = function.id,
            featureId = function.featureId,
            functionGraph = function.functionGraph,
            name = feature.name,
            description = feature.description,
        )
    }

    override fun getFunctionByFeatureId(featureId: Long): FunctionWithFeature? {
        val function = functions.values.firstOrNull { it.featureId == featureId } ?: return null
        return getFunctionById(function.id)
    }

    override fun getFunctionsForGroupSync(groupId: Long): List<FunctionWithFeature> {
        return functions.values.mapNotNull { getFunctionById(it.id) }
    }

    override fun hasAnyFunctions(): Boolean = functions.isNotEmpty()

    // =========================================================================
    // Function input feature operations
    // =========================================================================

    override fun insertFunctionInputFeature(functionInputFeature: FunctionInputFeature): Long {
        val id = if (functionInputFeature.id == 0L) nextInputFeatureId++ else functionInputFeature.id
        inputFeatures[id] = functionInputFeature.copy(id = id)
        return id
    }

    override fun getFunctionInputFeaturesSync(functionId: Long): List<FunctionInputFeature> =
        inputFeatures.values.filter { it.functionId == functionId }

    override fun deleteFunctionInputFeatures(functionId: Long) {
        inputFeatures.entries.removeIf { it.value.functionId == functionId }
    }
}
