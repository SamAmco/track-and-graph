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

package com.samco.trackandgraph.data.interactor

import com.samco.trackandgraph.data.database.DatabaseTransactionHelper
import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionCreateRequest
import com.samco.trackandgraph.data.database.dto.FunctionDeleteRequest
import com.samco.trackandgraph.data.database.dto.FunctionUpdateRequest
import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.database.entity.FunctionInputFeature
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.serialization.FunctionGraphSerializer
import com.samco.trackandgraph.data.validation.FunctionValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FunctionHelperImpl @Inject constructor(
    private val transactionHelper: DatabaseTransactionHelper,
    private val dao: TrackAndGraphDatabaseDao,
    private val functionGraphSerializer: FunctionGraphSerializer,
    private val functionValidator: FunctionValidator,
    @IODispatcher private val io: CoroutineDispatcher
) : FunctionHelper {

    override suspend fun insertFunction(request: FunctionCreateRequest): Long? = withContext(io) {
        transactionHelper.withTransaction {
            val function = Function(
                id = 0L,
                featureId = 0L,
                name = request.name,
                groupIds = setOf(request.groupId),
                displayIndex = 0,
                description = request.description,
                functionGraph = request.functionGraph,
                inputFeatureIds = request.inputFeatureIds
            )

            functionValidator.validateFunction(function)

            val serializedGraph = functionGraphSerializer
                .serialize(function.functionGraph)
                ?: return@withTransaction null

            // First, create the Feature entity that the Function will reference
            // TODO: When multi-group support is added, this will need to handle multiple groups
            val feature = Feature(
                id = 0L, // Let the database generate the ID
                name = function.name,
                groupId = request.groupId,
                displayIndex = function.displayIndex,
                description = function.description
            )
            val featureId = dao.insertFeature(feature)

            // Now create the Function entity with the correct featureId
            val functionId =
                dao.insertFunction(function.copy(featureId = featureId).toEntity(serializedGraph))

            // Now create the FunctionInputFeature entities
            function.inputFeatureIds.forEach { inputFeatureId ->
                dao.insertFunctionInputFeature(
                    FunctionInputFeature(
                        id = 0L,
                        functionId = functionId,
                        featureId = inputFeatureId
                    )
                )
            }

            featureId
        }
    }

    override suspend fun updateFunction(request: FunctionUpdateRequest) = withContext(io) {
        transactionHelper.withTransaction {
            val existingFunction = getFunctionById(request.id) ?: return@withTransaction

            val updatedFunction = existingFunction.copy(
                name = request.name ?: existingFunction.name,
                description = request.description ?: existingFunction.description,
                functionGraph = request.functionGraph ?: existingFunction.functionGraph,
                inputFeatureIds = request.inputFeatureIds ?: existingFunction.inputFeatureIds
            )

            functionValidator.validateFunction(updatedFunction)

            val serializedGraph = functionGraphSerializer.serialize(updatedFunction.functionGraph)
                ?: return@withTransaction

            // TODO: When multi-group support is added, this will need to handle multiple groups
            val feature = Feature(
                id = updatedFunction.featureId,
                name = updatedFunction.name,
                groupId = updatedFunction.groupIds.first(),
                displayIndex = updatedFunction.displayIndex,
                description = updatedFunction.description
            )
            dao.updateFeature(feature)
            dao.updateFunction(updatedFunction.toEntity(serializedGraph))

            // Now re-create the FunctionInputFeature entities
            dao.deleteFunctionInputFeatures(updatedFunction.id)
            updatedFunction.inputFeatureIds.forEach { inputFeatureId ->
                dao.insertFunctionInputFeature(
                    FunctionInputFeature(
                        id = 0L,
                        functionId = updatedFunction.id,
                        featureId = inputFeatureId
                    )
                )
            }
        }
    }

    override suspend fun deleteFunction(request: FunctionDeleteRequest) = withContext(io) {
        transactionHelper.withTransaction {
            // Get the function to find its feature ID
            val function = dao.getFunctionById(request.functionId)
            if (function != null) {
                // TODO: When multi-group support is added, check request.groupId
                // to determine if we should remove from one group or delete entirely
                // Delete the feature, which will cascade delete the function
                dao.deleteFeature(function.featureId)
            }
        }
    }

    override suspend fun getFunctionById(functionId: Long): Function? = withContext(io) {
        val functionWithFeature = dao.getFunctionById(functionId) ?: return@withContext null
        val inputFeatures = dao.getFunctionInputFeaturesSync(functionId).map { it.featureId }
        val functionGraphDto =
            functionGraphSerializer.deserialize(functionWithFeature.functionGraph)
                ?: return@withContext null
        functionWithFeature.toDto(functionGraphDto, inputFeatures)
    }

    override suspend fun tryGetFunctionByFeatureId(featureId: Long): Function? = withContext(io) {
        val functionWithFeature = dao.getFunctionByFeatureId(featureId) ?: return@withContext null
        val inputFeatures =
            dao.getFunctionInputFeaturesSync(functionWithFeature.id).map { it.featureId }
        val functionGraphDto =
            functionGraphSerializer.deserialize(functionWithFeature.functionGraph)
                ?: return@withContext null
        functionWithFeature.toDto(functionGraphDto, inputFeatures)
    }

    override suspend fun getAllFunctionsSync(): List<Function> = withContext(io) {
        dao.getAllFunctionsSync().mapNotNull { functionWithFeature ->
            val inputFeatures =
                dao.getFunctionInputFeaturesSync(functionWithFeature.id).map { it.featureId }
            val functionGraphDto =
                functionGraphSerializer.deserialize(functionWithFeature.functionGraph)
                    ?: return@mapNotNull null
            functionWithFeature.toDto(functionGraphDto, inputFeatures)
        }
    }

    override suspend fun getFunctionsForGroupSync(groupId: Long): List<Function> = withContext(io) {
        dao.getFunctionsForGroupSync(groupId).mapNotNull { functionWithFeature ->
            val inputFeatures =
                dao.getFunctionInputFeaturesSync(functionWithFeature.id).map { it.featureId }
            val functionGraphDto =
                functionGraphSerializer.deserialize(functionWithFeature.functionGraph)
                    ?: return@mapNotNull null
            functionWithFeature.toDto(functionGraphDto, inputFeatures)
        }
    }

    override suspend fun duplicateFunction(function: Function, groupId: Long): Long? = withContext(io) {
        transactionHelper.withTransaction {
            val serializedGraph = functionGraphSerializer.serialize(function.functionGraph)
                ?: return@withTransaction null

            //Create a copy of the feature
            val feature = dao.getFeatureById(function.featureId) ?: return@withTransaction null
            val newFeatureId = dao.insertFeature(feature.copy(id = 0L))

            // Create a copy of the function with id = 0 to generate new id
            val duplicatedFunction = function.copy(id = 0L, featureId = newFeatureId)
            val newFunctionId = dao.insertFunction(duplicatedFunction.toEntity(serializedGraph))

            // Duplicate input features
            function.inputFeatureIds.forEach { inputFeature ->
                dao.insertFunctionInputFeature(
                    FunctionInputFeature(
                        id = 0L,
                        functionId = newFunctionId,
                        featureId = inputFeature
                    )
                )
            }

            newFunctionId
        }
    }

    override suspend fun hasAnyFunctions(): Boolean = withContext(io) {
        dao.hasAnyFunctions()
    }
}
