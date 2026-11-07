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
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionOutput
import com.samco.trackandgraph.data.database.dto.FunctionOutputRequest
import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.database.entity.Function as FunctionEntity
import com.samco.trackandgraph.data.database.entity.FunctionInputFeature
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType
import com.samco.trackandgraph.data.database.entity.queryresponse.FunctionWithFeature
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
        validateAdditionalOutputs(function.functionGraph, request.additionalOutputs)

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
            replaceInputFeatures(functionId, function.inputFeatureIds)

            // Each additional output becomes its own function, listed right after this one
            request.additionalOutputs.forEachIndexed { index, output ->
                insertAdditionalOutput(
                    ownerFunctionId = functionId,
                    groupId = request.groupId,
                    afterDisplayIndex = index,
                    output = output,
                    inputFeatureIds = function.inputFeatureIds,
                )
            }

            CreatedComponent(componentId = functionId, groupItemId = groupItemId)
        }
    }

    override suspend fun updateFunction(request: FunctionUpdateRequest) = withContext(io) {
        val requested = getFunctionById(request.id) ?: return@withContext
        // The canvas is owned by the function that holds the graph, which may not be the one
        // the caller identified if it is an additional output
        val existingFunction = requested.parentFunctionId
            ?.let { getFunctionById(it) }
            ?: requested

        val updatedFunction = existingFunction.copy(
            name = request.name ?: existingFunction.name,
            description = request.description ?: existingFunction.description,
            functionGraph = request.functionGraph ?: existingFunction.functionGraph,
            inputFeatureIds = request.inputFeatureIds ?: existingFunction.inputFeatureIds
        )

        val additionalOutputs = request.additionalOutputs
            ?: existingFunction.additionalOutputs.map {
                FunctionOutputRequest(it.outputNodeId, it.name, it.description)
            }

        functionValidator.validateFunction(updatedFunction)
        validateAdditionalOutputs(updatedFunction.functionGraph, additionalOutputs)

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
            replaceInputFeatures(updatedFunction.id, updatedFunction.inputFeatureIds)

            reconcileAdditionalOutputs(
                owner = updatedFunction,
                existingOutputs = existingFunction.additionalOutputs,
                requestedOutputs = additionalOutputs,
            )
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

            // The additional outputs of a canvas cannot outlive the function that owns it
            dao.getAdditionalOutputFunctionsSync(function.id).forEach {
                deleteFunctionRow(it.id, it.featureId)
            }

            // Deleting an additional output removes its node from the owner's canvas
            val ownerId = function.parentFunctionId
            val outputNodeId = function.outputNodeId
            if (ownerId != null && outputNodeId != null) {
                removeOutputNodeFromOwner(ownerId, outputNodeId)
            }

            groupItems.forEach { groupItemDao.deleteGroupItem(it.id) }
            dao.deleteFeature(function.featureId)
        }
    }

    private fun isFunctionUnique(functionId: Long) =
        groupItemDao.getGroupItemsForChild(functionId, GroupItemType.FUNCTION).size == 1

    private fun toDto(functionWithFeature: FunctionWithFeature): Function? {
        val inputFeatures =
            dao.getFunctionInputFeaturesSync(functionWithFeature.id).map { it.featureId }
        val functionGraphDto =
            functionGraphSerializer.deserialize(functionWithFeature.functionGraph)
                ?: return null
        val additionalOutputs = if (functionWithFeature.parentFunctionId == null) {
            dao.getAdditionalOutputFunctionsSync(functionWithFeature.id).mapNotNull {
                val outputNodeId = it.outputNodeId ?: return@mapNotNull null
                FunctionOutput(
                    functionId = it.id,
                    featureId = it.featureId,
                    outputNodeId = outputNodeId,
                    name = it.name,
                    description = it.description,
                )
            }
        } else emptyList()
        return functionWithFeature.toDto(
            functionGraphDto,
            inputFeatures,
            unique = isFunctionUnique(functionWithFeature.id),
            additionalOutputs = additionalOutputs,
        )
    }

    override suspend fun getFunctionById(functionId: Long): Function? = withContext(io) {
        dao.getFunctionById(functionId)?.let(::toDto)
    }

    override suspend fun tryGetFunctionByFeatureId(featureId: Long): Function? = withContext(io) {
        dao.getFunctionByFeatureId(featureId)?.let(::toDto)
    }

    override suspend fun getFunctionsForGroupSync(groupId: Long): List<Function> = withContext(io) {
        dao.getFunctionsForGroupSync(groupId).mapNotNull(::toDto)
    }

    override suspend fun duplicateFunction(
        groupItemId: Long,
        newName: String,
    ): CreatedComponent? =
        withContext(io) {
            transactionHelper.withTransaction {
                val originalGroupItem = groupItemDao.getGroupItemById(groupItemId)
                    ?: return@withTransaction null
                val function = dao.getFunctionById(originalGroupItem.childId)
                    ?: return@withTransaction null

                // A duplicate is always a standalone function. When the original shares its
                // canvas with other outputs, the copy keeps only the output that was duplicated.
                val isMultiOutput = function.parentFunctionId != null
                        || dao.getAdditionalOutputFunctionsSync(function.id).isNotEmpty()
                val duplicatedGraph = if (!isMultiOutput) {
                    function.functionGraph
                } else {
                    val graph = functionGraphSerializer.deserialize(function.functionGraph)
                        ?: return@withTransaction null
                    functionGraphSerializer.serialize(
                        graph.copy(
                            outputNode = graph.outputNodeFor(function.outputNodeId),
                            additionalOutputNodes = emptyList(),
                        )
                    ) ?: return@withTransaction null
                }

                // Create a copy of the feature
                val feature = dao.getFeatureById(function.featureId) ?: return@withTransaction null
                val newFeatureId = dao.insertFeature(
                    feature.copy(
                        id = 0L,
                        name = newName,
                    )
                )

                // Create a copy of the function (functionGraph is already serialized)
                val newFunctionId = dao.insertFunction(
                    FunctionEntity(
                        id = 0L,
                        featureId = newFeatureId,
                        functionGraph = duplicatedGraph
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

    private fun replaceInputFeatures(functionId: Long, inputFeatureIds: List<Long>) {
        dao.deleteFunctionInputFeatures(functionId)
        inputFeatureIds.forEach { inputFeatureId ->
            dao.insertFunctionInputFeature(
                FunctionInputFeature(
                    id = 0L,
                    functionId = functionId,
                    featureId = inputFeatureId
                )
            )
        }
    }

    /** The requested outputs must match the additional output nodes on the canvas exactly. */
    private fun validateAdditionalOutputs(
        functionGraph: FunctionGraph,
        outputs: List<FunctionOutputRequest>,
    ) {
        val requestedIds = outputs.map { it.outputNodeId }
        val nodeIds = functionGraph.additionalOutputNodes.map { it.id }
        if (requestedIds.size != requestedIds.toSet().size || requestedIds.toSet() != nodeIds.toSet()) {
            error("Additional outputs do not match the output nodes in the function graph")
        }
    }

    /**
     * Creates the feature, function and group item for one additional output and places it
     * directly after [afterDisplayIndex] in [groupId].
     */
    private fun insertAdditionalOutput(
        ownerFunctionId: Long,
        groupId: Long,
        afterDisplayIndex: Int,
        output: FunctionOutputRequest,
        inputFeatureIds: List<Long>,
    ) {
        val featureId = dao.insertFeature(
            Feature(id = 0L, name = output.name, description = output.description)
        )
        // The owner holds the only copy of the graph, so this row's own graph stays empty
        val functionId = dao.insertFunction(
            FunctionEntity(
                id = 0L,
                featureId = featureId,
                functionGraph = "",
                parentFunctionId = ownerFunctionId,
                outputNodeId = output.outputNodeId,
            )
        )
        groupItemDao.shiftDisplayIndexesDownAfter(groupId, afterDisplayIndex)
        groupItemDao.insertGroupItem(
            GroupItem(
                groupId = groupId,
                displayIndex = afterDisplayIndex + 1,
                childId = functionId,
                type = GroupItemType.FUNCTION,
                createdAt = timeProvider.epochMilli()
            )
        )
        replaceInputFeatures(functionId, inputFeatureIds)
    }

    private fun reconcileAdditionalOutputs(
        owner: Function,
        existingOutputs: List<FunctionOutput>,
        requestedOutputs: List<FunctionOutputRequest>,
    ) {
        val existingByNode = existingOutputs.associateBy { it.outputNodeId }
        val requestedNodeIds = requestedOutputs.map { it.outputNodeId }.toSet()

        existingOutputs
            .filter { it.outputNodeId !in requestedNodeIds }
            .forEach { deleteFunctionRow(it.functionId, it.featureId) }

        val ownerGroupItem = groupItemDao
            .getGroupItemsForChild(owner.id, GroupItemType.FUNCTION)
            .firstOrNull()
        var insertedCount = 0

        requestedOutputs.forEach { output ->
            val current = existingByNode[output.outputNodeId]
            if (current != null) {
                dao.updateFeature(
                    Feature(
                        id = current.featureId,
                        name = output.name,
                        description = output.description
                    )
                )
                replaceInputFeatures(current.functionId, owner.inputFeatureIds)
            } else {
                val groupId = ownerGroupItem?.groupId ?: return@forEach
                insertAdditionalOutput(
                    ownerFunctionId = owner.id,
                    groupId = groupId,
                    afterDisplayIndex = ownerGroupItem.displayIndex + insertedCount,
                    output = output,
                    inputFeatureIds = owner.inputFeatureIds,
                )
                insertedCount++
            }
        }
    }

    private fun deleteFunctionRow(functionId: Long, featureId: Long) {
        groupItemDao.getGroupItemsForChild(functionId, GroupItemType.FUNCTION)
            .forEach { groupItemDao.deleteGroupItem(it.id) }
        dao.deleteFeature(featureId)
    }

    private fun removeOutputNodeFromOwner(ownerId: Long, outputNodeId: Int) {
        val owner = dao.getFunctionById(ownerId) ?: return
        val graph = functionGraphSerializer.deserialize(owner.functionGraph) ?: return
        val updatedGraph = graph.copy(
            additionalOutputNodes = graph.additionalOutputNodes.filter { it.id != outputNodeId }
        )
        val serialized = functionGraphSerializer.serialize(updatedGraph) ?: return
        dao.updateFunction(
            FunctionEntity(
                id = owner.id,
                featureId = owner.featureId,
                functionGraph = serialized,
            )
        )
    }
}
