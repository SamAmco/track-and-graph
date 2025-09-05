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

package com.samco.trackandgraph.data.model

import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.model.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FunctionHelperImpl @Inject constructor(
    private val transactionHelper: DatabaseTransactionHelper,
    private val dao: TrackAndGraphDatabaseDao,
    @IODispatcher private val io: CoroutineDispatcher
) : FunctionHelper {

    override suspend fun insertFunction(function: Function): Long = withContext(io) {
        transactionHelper.withTransaction {
            // First, create the Feature entity that the Function will reference
            val feature = Feature(
                id = 0L, // Let the database generate the ID
                name = function.name,
                groupId = function.groupId,
                displayIndex = function.displayIndex,
                description = function.description
            )
            val featureId = dao.insertFeature(feature)
            
            // Now create the Function entity with the correct featureId
            dao.insertFunction(function.copy(featureId = featureId).toEntity())
        }
    }

    override suspend fun updateFunction(function: Function) = withContext(io) {
        transactionHelper.withTransaction {
            val feature = Feature(
                id = function.featureId,
                name = function.name,
                groupId = function.groupId,
                displayIndex = function.displayIndex,
                description = function.description
            )
            dao.updateFeature(feature)
            dao.updateFunction(function.toEntity())
        }
    }

    override suspend fun deleteFunction(functionId: Long) = withContext(io) {
        transactionHelper.withTransaction {
            // Get the function to find its feature ID
            val function = dao.getFunctionById(functionId)
            if (function != null) {
                // Delete the feature, which will cascade delete the function
                dao.deleteFeature(function.featureId)
            }
        }
    }

    override suspend fun getFunctionById(functionId: Long): Function? = withContext(io) {
        val functionWithFeature = dao.getFunctionById(functionId) ?: return@withContext null
        val inputFeatures = dao.getFunctionInputFeaturesSync(functionId).map { it.toDto() }
        functionWithFeature.toDto(inputFeatures)
    }

    override suspend fun getFunctionByFeatureId(featureId: Long): Function? = withContext(io) {
        val functionWithFeature = dao.getFunctionByFeatureId(featureId) ?: return@withContext null
        val inputFeatures = dao.getFunctionInputFeaturesSync(functionWithFeature.id).map { it.toDto() }
        functionWithFeature.toDto(inputFeatures)
    }

    override suspend fun getAllFunctionsSync(): List<Function> = withContext(io) {
        dao.getAllFunctionsSync().map { functionWithFeature ->
            val inputFeatures = dao.getFunctionInputFeaturesSync(functionWithFeature.id).map { it.toDto() }
            functionWithFeature.toDto(inputFeatures)
        }
    }

    override suspend fun getFunctionsForGroupSync(groupId: Long): List<Function> = withContext(io) {
        dao.getFunctionsForGroupSync(groupId).map { functionWithFeature ->
            val inputFeatures = dao.getFunctionInputFeaturesSync(functionWithFeature.id).map { it.toDto() }
            functionWithFeature.toDto(inputFeatures)
        }
    }

    override suspend fun duplicateFunction(function: Function): Long? = withContext(io) {
        transactionHelper.withTransaction {
            //Create a copy of the feature
            val feature = dao.getFeatureById(function.featureId) ?: return@withTransaction null
            val newFeatureId = dao.insertFeature(feature.copy(id = 0L))

            // Create a copy of the function with id = 0 to generate new id
            val duplicatedFunction = function.copy(id = 0L, featureId = newFeatureId)
            val newFunctionId = dao.insertFunction(duplicatedFunction.toEntity())

            // Duplicate input features
            function.inputFeatures.forEach { inputFeature ->
                dao.insertFunctionInputFeature(
                    inputFeature.copy(
                        id = 0L,
                        functionId = newFunctionId
                    ).toEntity()
                )
            }

            newFunctionId
        }
    }

    override suspend fun hasAnyFunctions(): Boolean = withContext(io) {
        dao.hasAnyFunctions()
    }
}
