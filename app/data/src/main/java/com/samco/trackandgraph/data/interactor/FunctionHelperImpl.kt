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
import com.samco.trackandgraph.data.database.FunctionDao
import com.samco.trackandgraph.data.database.GroupItemDao
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionCreateRequest
import com.samco.trackandgraph.data.database.dto.ComponentDeleteRequest
import com.samco.trackandgraph.data.database.dto.CreatedComponent
import com.samco.trackandgraph.data.database.dto.FunctionUpdateRequest
import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.database.entity.Function as FunctionEntity
import com.samco.trackandgraph.data.database.entity.FunctionInputFeature
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.serialization.FunctionGraphSerializer
import com.samco.trackandgraph.data.time.TimeProvider
import com.samco.trackandgraph.data.validation.FunctionValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FunctionHelperImpl @Inject constructor(
    private val transactionHelper: DatabaseTransactionHelper,
    private val dao: FunctionDao,
    private val groupItemDao: GroupItemDao,
    private val functionGraphSerializer: FunctionGraphSerializer,
    private val functionValidator: FunctionValidator,
    private val timeProvider: TimeProvider,
    @IODispatcher private val io: CoroutineDispatcher
) : FunctionHelper {

    override suspend fun insertFunction(request: FunctionCreateRequest): CreatedComponent? = withContext(io) {
        val function = Function(
            id = 0L,
            featureId = 0L,
            name = request.name,
            description = request.description,
            functionGraph = request.functionGraph,
            inputFeatureIds = request.inputFeatureIds,
            unique = true,
        )

        functionValidator.validateFunction(function)

        val serializedGraph = functionGraphSerializer
            .serialize(function.functionGraph)
            ?: return@withContext null

        transactionHelper.withTransaction {
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
            val groupItemId = groupItemDao.insertGroupItem(groupItem)

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

            CreatedComponent(componentId = functionId, groupItemId = groupItemId)
        }
    }

    override suspend fun updateFunction(request: FunctionUpdateRequest) = withContext(io) {
        val existingFunction = getFunctionById(request.id) ?: return@withContext

        val updatedFunction = existingFunction.copy(
            name = request.name ?: existingFunction.name,
            description = request.description ?: existingFunction.description,
            functionGraph = request.functionGraph ?: existingFunction.functionGraph,
            inputFeatureIds = request.inputFeatureIds ?: existingFunction.inputFeatureIds
        )

        functionValidator.validateFunction(updatedFunction)

        val serializedGraph = functionGraphSerializer.serialize(updatedFunction.functionGraph)
            ?: return@withContext

        transactionHelper.withTransaction {
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

    override suspend fun deleteFunction(request: ComponentDeleteRequest) = withContext(io) {
        transactionHelper.withTransaction {
            val groupItem = groupItemDao.getGroupItemById(request.groupItemId)
                ?: return@withTransaction
            val function = dao.getFunctionById(groupItem.childId) ?: return@withTransaction

            val groupItems = groupItemDao.getGroupItemsForChild(
                function.id,
                GroupItemType.FUNCTION
            )

            if (!request.deleteEverywhere && groupItems.size > 1) {
                groupItemDao.deleteGroupItem(request.groupItemId)
                return@withTransaction
            }

            groupItems.forEach { groupItemDao.deleteGroupItem(it.id) }
            dao.deleteFeature(function.featureId)
        }
    }

    private fun isFunctionUnique(functionId: Long) =
        groupItemDao.getGroupItemsForChild(functionId, GroupItemType.FUNCTION).size == 1

    override suspend fun getFunctionById(functionId: Long): Function? = withContext(io) {
        val functionWithFeature = dao.getFunctionById(functionId) ?: return@withContext null
        val inputFeatures = dao.getFunctionInputFeaturesSync(functionId).map { it.featureId }
        val functionGraphDto =
            functionGraphSerializer.deserialize(functionWithFeature.functionGraph)
                ?: return@withContext null
        functionWithFeature.toDto(functionGraphDto, inputFeatures, unique = isFunctionUnique(functionWithFeature.id))
    }

    override suspend fun tryGetFunctionByFeatureId(featureId: Long): Function? = withContext(io) {
        val functionWithFeature = dao.getFunctionByFeatureId(featureId) ?: return@withContext null
        val inputFeatures =
            dao.getFunctionInputFeaturesSync(functionWithFeature.id).map { it.featureId }
        val functionGraphDto =
            functionGraphSerializer.deserialize(functionWithFeature.functionGraph)
                ?: return@withContext null
        functionWithFeature.toDto(functionGraphDto, inputFeatures, unique = isFunctionUnique(functionWithFeature.id))
    }

    override suspend fun getFunctionsForGroupSync(groupId: Long): List<Function> = withContext(io) {
        dao.getFunctionsForGroupSync(groupId).mapNotNull { functionWithFeature ->
            val inputFeatures =
                dao.getFunctionInputFeaturesSync(functionWithFeature.id).map { it.featureId }
            val functionGraphDto =
                functionGraphSerializer.deserialize(functionWithFeature.functionGraph)
                    ?: return@mapNotNull null
            functionWithFeature.toDto(functionGraphDto, inputFeatures, unique = isFunctionUnique(functionWithFeature.id))
        }
    }

    override suspend fun duplicateFunction(groupItemId: Long): CreatedComponent? =
        withContext(io) {
            transactionHelper.withTransaction {
                val originalGroupItem = groupItemDao.getGroupItemById(groupItemId)
                    ?: return@withTransaction null
                val function = dao.getFunctionById(originalGroupItem.childId)
                    ?: return@withTransaction null

                // Create a copy of the feature
                val feature = dao.getFeatureById(function.featureId) ?: return@withTransaction null
                val newFeatureId = dao.insertFeature(feature.copy(id = 0L))

                // Create a copy of the function (functionGraph is already serialized)
                val newFunctionId = dao.insertFunction(
                    FunctionEntity(
                        id = 0L,
                        featureId = newFeatureId,
                        functionGraph = function.functionGraph
                    )
                )

                // Place duplicate immediately after the original
                val groupId = originalGroupItem.groupId!!
                groupItemDao.shiftDisplayIndexesDownAfter(
                    groupId,
                    originalGroupItem.displayIndex
                )
                val groupItem = GroupItem(
                    groupId = groupId,
                    displayIndex = originalGroupItem.displayIndex + 1,
                    childId = newFunctionId,
                    type = GroupItemType.FUNCTION,
                    createdAt = timeProvider.epochMilli()
                )
                val newGroupItemId = groupItemDao.insertGroupItem(groupItem)

                // Duplicate input features
                dao.getFunctionInputFeaturesSync(function.id).forEach { inputFeature ->
                    dao.insertFunctionInputFeature(
                        inputFeature.copy(id = 0L, functionId = newFunctionId)
                    )
                }

                CreatedComponent(componentId = newFunctionId, groupItemId = newGroupItemId)
            }
        }

    override suspend fun hasAnyFunctions(): Boolean = withContext(io) {
        dao.hasAnyFunctions()
    }
}
