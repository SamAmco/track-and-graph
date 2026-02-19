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
import com.samco.trackandgraph.data.database.GroupItemDao
import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionCreateRequest
import com.samco.trackandgraph.data.database.dto.FunctionDeleteRequest
import com.samco.trackandgraph.data.database.dto.FunctionUpdateRequest
import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.database.entity.FunctionInputFeature
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.serialization.FunctionGraphSerializer
import com.samco.trackandgraph.data.time.TimeProviderImpl
import com.samco.trackandgraph.data.validation.FunctionValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FunctionHelperImpl @Inject constructor(
    private val transactionHelper: DatabaseTransactionHelper,
    private val dao: TrackAndGraphDatabaseDao,
    private val groupItemDao: GroupItemDao,
    private val functionGraphSerializer: FunctionGraphSerializer,
    private val functionValidator: FunctionValidator,
    private val timeProvider: TimeProviderImpl,
    @IODispatcher private val io: CoroutineDispatcher
) : FunctionHelper {

    override suspend fun insertFunction(request: FunctionCreateRequest): Long? = withContext(io) {
        transactionHelper.withTransaction {
            val function = Function(
                id = 0L,
                featureId = 0L,
                name = request.name,
                description = request.description,
                functionGraph = request.functionGraph,
                inputFeatureIds = request.inputFeatureIds
            )

            functionValidator.validateFunction(function)

            val serializedGraph = functionGraphSerializer
                .serialize(function.functionGraph)
                ?: return@withTransaction null

            // First, create the Feature entity that the Function will reference
            val feature = Feature(
                id = 0L, // Let the database generate the ID
                name = function.name,
                description = function.description
            )
            val featureId = dao.insertFeature(feature)

            // Create the Function entity
            val functionId = dao.insertFunction(
                function.copy(featureId = featureId)
                    .toEntity(serializedGraph)
            )

            // Create GroupItem entry with function ID
            groupItemDao.shiftDisplayIndexesDown(request.groupId)
            val groupItem = GroupItem(
                groupId = request.groupId,
                displayIndex = 0,
                childId = functionId,
                type = GroupItemType.FUNCTION,
                createdAt = timeProvider.epochMilli()
            )
            groupItemDao.insertGroupItem(groupItem)

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

            functionId
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

            val feature = Feature(
                id = updatedFunction.featureId,
                name = updatedFunction.name,
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
            val function = dao.getFunctionById(request.functionId) ?: return@withTransaction

            val groupItems = groupItemDao.getGroupItemsForChild(
                function.id,
                GroupItemType.FUNCTION
            )

            if (request.groupId != null && groupItems.size > 1) {
                groupItems
                    .filter { it.groupId == request.groupId }
                    .forEach { groupItemDao.deleteGroupItem(it.id) }
                return@withTransaction
            }

            groupItems.forEach { groupItemDao.deleteGroupItem(it.id) }
            dao.deleteFeature(function.featureId)
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

    override suspend fun duplicateFunction(function: Function, groupId: Long): Long? =
        withContext(io) {
            transactionHelper.withTransaction {
                val serializedGraph = functionGraphSerializer.serialize(function.functionGraph)
                    ?: return@withTransaction null

                //Create a copy of the feature
                val feature = dao.getFeatureById(function.featureId) ?: return@withTransaction null
                val newFeatureId = dao.insertFeature(feature.copy(id = 0L))

                // Create a copy of the function with id = 0 to generate new id
                val duplicatedFunction = function.copy(id = 0L, featureId = newFeatureId)
                val newFunctionId = dao.insertFunction(duplicatedFunction.toEntity(serializedGraph))

                // Find the original's position in this group to place duplicate after it
                val originalGroupItem = groupItemDao.getGroupItem(
                    groupId,
                    function.id,
                    GroupItemType.FUNCTION
                )
                val newDisplayIndex = if (originalGroupItem != null) {
                    groupItemDao.shiftDisplayIndexesDownAfter(groupId, originalGroupItem.displayIndex)
                    originalGroupItem.displayIndex + 1
                } else {
                    groupItemDao.shiftDisplayIndexesDown(groupId)
                    0
                }

                val groupItem = GroupItem(
                    groupId = groupId,
                    displayIndex = newDisplayIndex,
                    childId = newFunctionId,
                    type = GroupItemType.FUNCTION,
                    createdAt = timeProvider.epochMilli()
                )
                groupItemDao.insertGroupItem(groupItem)

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
