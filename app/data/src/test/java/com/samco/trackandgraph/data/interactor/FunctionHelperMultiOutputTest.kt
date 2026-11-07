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

import com.samco.trackandgraph.FakeFunctionDao
import com.samco.trackandgraph.FakeGroupItemDao
import com.samco.trackandgraph.data.database.DatabaseTransactionHelper
import com.samco.trackandgraph.data.database.dto.ComponentDeleteRequest
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionCreateRequest
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.FunctionOutputRequest
import com.samco.trackandgraph.data.database.dto.FunctionUpdateRequest
import com.samco.trackandgraph.data.database.dto.NodeDependency
import com.samco.trackandgraph.data.database.entity.GroupItemType
import com.samco.trackandgraph.data.serialization.FunctionGraphSerializer
import com.samco.trackandgraph.data.validation.FunctionValidator
import com.samco.trackandgraph.time.FakeTimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Covers functions whose canvas has more than one output node. Each extra output is stored as a
 * function of its own (so it is a feature that can be graphed) which points at the owning
 * function through parentFunctionId and shares its graph.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FunctionHelperMultiOutputTest {

    private val groupId = 1L
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeFunctionDao: FakeFunctionDao
    private lateinit var fakeGroupItemDao: FakeGroupItemDao
    private lateinit var serializer: FunctionGraphSerializer
    private lateinit var uut: FunctionHelperImpl

    private fun output(id: Int, vararg sources: Int) = FunctionGraphNode.OutputNode(
        x = 0f, y = 0f, id = id,
        dependencies = sources.map { NodeDependency(connectorIndex = 0, nodeId = it) },
    )

    private fun graph(vararg additionalOutputIds: Int) = FunctionGraph(
        nodes = listOf(FunctionGraphNode.FeatureNode(x = 0f, y = 0f, id = 1, featureId = 100L)),
        outputNode = output(10, 1),
        isDuration = false,
        additionalOutputNodes = additionalOutputIds.map { output(it, 1) },
    )

    private fun createRequest(vararg additional: FunctionOutputRequest) = FunctionCreateRequest(
        name = "Main",
        groupId = groupId,
        description = "main description",
        functionGraph = graph(*additional.map { it.outputNodeId }.toIntArray()),
        inputFeatureIds = listOf(100L),
        additionalOutputs = additional.toList(),
    )

    @Before
    fun before() {
        fakeFunctionDao = FakeFunctionDao()
        fakeGroupItemDao = FakeGroupItemDao()
        serializer = FunctionGraphSerializer(Json { ignoreUnknownKeys = true })

        uut = FunctionHelperImpl(
            transactionHelper = object : DatabaseTransactionHelper {
                override suspend fun <R> withTransaction(block: suspend () -> R): R = block()
            },
            dao = fakeFunctionDao,
            groupItemDao = fakeGroupItemDao,
            functionGraphSerializer = serializer,
            functionValidator = object : FunctionValidator {
                override suspend fun validateFunction(function: Function) {}
            },
            timeProvider = FakeTimeProvider(),
            io = dispatcher,
        )
    }

    private fun groupItemFor(functionId: Long) =
        fakeGroupItemDao.getGroupItemsForChild(functionId, GroupItemType.FUNCTION).single()

    /** Creates "Main" with outputs "Second" (node 11) and "Third" (node 12). */
    private suspend fun createThreeOutputFunction(): Triple<Function, Function, Function> {
        val created = uut.insertFunction(
            createRequest(
                FunctionOutputRequest(11, "Second", "second description"),
                FunctionOutputRequest(12, "Third"),
            )
        )!!
        val main = uut.getFunctionById(created.componentId)!!
        val second = uut.getFunctionById(main.additionalOutputs[0].functionId)!!
        val third = uut.getFunctionById(main.additionalOutputs[1].functionId)!!
        return Triple(main, second, third)
    }

    // =========================================================================
    // Create
    // =========================================================================

    @Test
    fun `insertFunction creates one function and feature per output`() = runTest(dispatcher) {
        val (main, second, third) = createThreeOutputFunction()

        assertEquals(3, fakeFunctionDao.numFunctions())
        assertEquals(listOf("Main", "Second", "Third"), listOf(main, second, third).map { it.name })
        assertEquals(3, listOf(main, second, third).map { it.featureId }.toSet().size)
        assertEquals("second description", second.description)
    }

    @Test
    fun `insertFunction links additional outputs to the owner and their output node`() = runTest(dispatcher) {
        val (main, second, third) = createThreeOutputFunction()

        assertNull(main.parentFunctionId)
        assertNull(main.outputNodeId)
        assertEquals(main.id, second.parentFunctionId)
        assertEquals(11, second.outputNodeId)
        assertEquals(main.id, third.parentFunctionId)
        assertEquals(12, third.outputNodeId)
    }

    @Test
    fun `insertFunction lists the additional outputs right after the owner in the group`() = runTest(dispatcher) {
        val (main, second, third) = createThreeOutputFunction()

        assertEquals(0, groupItemFor(main.id).displayIndex)
        assertEquals(1, groupItemFor(second.id).displayIndex)
        assertEquals(2, groupItemFor(third.id).displayIndex)
        assertTrue(listOf(main, second, third).all { groupItemFor(it.id).groupId == groupId })
    }

    @Test
    fun `insertFunction gives every output the same input features`() = runTest(dispatcher) {
        val (main, second, third) = createThreeOutputFunction()

        listOf(main, second, third).forEach { assertEquals(listOf(100L), it.inputFeatureIds) }
    }

    @Test
    fun `additional outputs read the graph of the function that owns them`() = runTest(dispatcher) {
        val (main, second, _) = createThreeOutputFunction()

        assertEquals(main.functionGraph, second.functionGraph)
        assertEquals(listOf(11, 12), second.functionGraph.additionalOutputNodes.map { it.id })
    }

    @Test
    fun `getFunctionById only lists additional outputs on the owner`() = runTest(dispatcher) {
        val (main, second, _) = createThreeOutputFunction()

        assertEquals(listOf(11, 12), main.additionalOutputs.map { it.outputNodeId })
        assertEquals(listOf("Second", "Third"), main.additionalOutputs.map { it.name })
        assertEquals(second.featureId, main.additionalOutputs[0].featureId)
        assertTrue(second.additionalOutputs.isEmpty())
    }

    @Test
    fun `tryGetFunctionByFeatureId resolves an additional output`() = runTest(dispatcher) {
        val (main, second, _) = createThreeOutputFunction()

        val result = uut.tryGetFunctionByFeatureId(second.featureId)

        assertEquals(second.id, result?.id)
        assertEquals(main.id, result?.parentFunctionId)
    }

    @Test
    fun `insertFunction rejects outputs that do not match the graph`() = runTest(dispatcher) {
        val request = createRequest(FunctionOutputRequest(11, "Second"))
            .copy(additionalOutputs = listOf(FunctionOutputRequest(99, "Wrong")))

        try {
            uut.insertFunction(request)
            fail("Expected an IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals(0, fakeFunctionDao.numFunctions())
        }
    }

    @Test
    fun `insertFunction without additional outputs behaves as before`() = runTest(dispatcher) {
        val created = uut.insertFunction(createRequest())!!

        val function = uut.getFunctionById(created.componentId)!!
        assertEquals(1, fakeFunctionDao.numFunctions())
        assertTrue(function.additionalOutputs.isEmpty())
        assertNull(function.parentFunctionId)
    }

    // =========================================================================
    // Update
    // =========================================================================

    @Test
    fun `updateFunction renames an additional output and keeps its function`() = runTest(dispatcher) {
        val (main, second, _) = createThreeOutputFunction()

        uut.updateFunction(
            FunctionUpdateRequest(
                id = main.id,
                additionalOutputs = listOf(
                    FunctionOutputRequest(11, "Renamed", "new description"),
                    FunctionOutputRequest(12, "Third"),
                ),
            )
        )

        val updated = uut.getFunctionById(second.id)!!
        assertEquals("Renamed", updated.name)
        assertEquals("new description", updated.description)
        assertEquals(second.featureId, updated.featureId)
        assertEquals(3, fakeFunctionDao.numFunctions())
    }

    @Test
    fun `updateFunction adds a new additional output next to the owner`() = runTest(dispatcher) {
        val (main, _, _) = createThreeOutputFunction()

        uut.updateFunction(
            FunctionUpdateRequest(
                id = main.id,
                functionGraph = graph(11, 12, 13),
                additionalOutputs = listOf(
                    FunctionOutputRequest(11, "Second"),
                    FunctionOutputRequest(12, "Third"),
                    FunctionOutputRequest(13, "Fourth"),
                ),
            )
        )

        val updated = uut.getFunctionById(main.id)!!
        assertEquals(listOf("Second", "Third", "Fourth"), updated.additionalOutputs.map { it.name })
        val fourth = updated.additionalOutputs.single { it.outputNodeId == 13 }
        assertEquals(1, groupItemFor(fourth.functionId).displayIndex)
        assertEquals(listOf(100L), uut.getFunctionById(fourth.functionId)!!.inputFeatureIds)
    }

    @Test
    fun `updateFunction removes an additional output that is no longer requested`() = runTest(dispatcher) {
        val (main, second, third) = createThreeOutputFunction()

        uut.updateFunction(
            FunctionUpdateRequest(
                id = main.id,
                functionGraph = graph(12),
                additionalOutputs = listOf(FunctionOutputRequest(12, "Third")),
            )
        )

        assertNull(uut.getFunctionById(second.id))
        assertNull(uut.tryGetFunctionByFeatureId(second.featureId))
        assertTrue(fakeGroupItemDao.getGroupItemsForChild(second.id, GroupItemType.FUNCTION).isEmpty())
        assertNotNull(uut.getFunctionById(third.id))
        assertEquals(2, fakeFunctionDao.numFunctions())
    }

    @Test
    fun `updateFunction refreshes the input features of every output`() = runTest(dispatcher) {
        val (main, second, third) = createThreeOutputFunction()

        uut.updateFunction(FunctionUpdateRequest(id = main.id, inputFeatureIds = listOf(100L, 200L)))

        listOf(main, second, third).forEach {
            assertEquals(listOf(100L, 200L), uut.getFunctionById(it.id)!!.inputFeatureIds)
        }
    }

    @Test
    fun `updateFunction keeps additional outputs when none are requested`() = runTest(dispatcher) {
        val (main, _, _) = createThreeOutputFunction()

        uut.updateFunction(FunctionUpdateRequest(id = main.id, name = "New main name"))

        val updated = uut.getFunctionById(main.id)!!
        assertEquals("New main name", updated.name)
        assertEquals(listOf("Second", "Third"), updated.additionalOutputs.map { it.name })
        assertEquals(3, fakeFunctionDao.numFunctions())
    }

    @Test
    fun `updateFunction on an additional output updates the owner canvas`() = runTest(dispatcher) {
        val (main, second, _) = createThreeOutputFunction()

        uut.updateFunction(FunctionUpdateRequest(id = second.id, name = "Owner renamed"))

        assertEquals("Owner renamed", uut.getFunctionById(main.id)!!.name)
        assertEquals("Second", uut.getFunctionById(second.id)!!.name)
    }

    @Test
    fun `updateFunction rejects a graph that disagrees with the requested outputs`() = runTest(dispatcher) {
        val (main, _, _) = createThreeOutputFunction()

        try {
            uut.updateFunction(FunctionUpdateRequest(id = main.id, functionGraph = graph(11)))
            fail("Expected an IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals(3, fakeFunctionDao.numFunctions())
        }
    }

    // =========================================================================
    // Delete
    // =========================================================================

    @Test
    fun `deleteFunction on the owner deletes every additional output`() = runTest(dispatcher) {
        val (main, second, third) = createThreeOutputFunction()

        uut.deleteFunction(ComponentDeleteRequest(groupItemId = groupItemFor(main.id).id, deleteEverywhere = true))

        assertEquals(0, fakeFunctionDao.numFunctions())
        assertTrue(fakeGroupItemDao.getAll().isEmpty())
        assertNull(uut.tryGetFunctionByFeatureId(second.featureId))
        assertNull(uut.tryGetFunctionByFeatureId(third.featureId))
    }

    @Test
    fun `deleteFunction on an additional output removes its node from the owner canvas`() = runTest(dispatcher) {
        val (main, second, third) = createThreeOutputFunction()

        uut.deleteFunction(ComponentDeleteRequest(groupItemId = groupItemFor(second.id).id, deleteEverywhere = true))

        assertNull(uut.getFunctionById(second.id))
        val owner = uut.getFunctionById(main.id)!!
        assertEquals(listOf(12), owner.functionGraph.additionalOutputNodes.map { it.id })
        assertEquals(listOf(third.id), owner.additionalOutputs.map { it.functionId })
        assertEquals(2, fakeFunctionDao.numFunctions())
    }

    @Test
    fun `deleteFunction on a symlink of the owner keeps the additional outputs`() = runTest(dispatcher) {
        val (main, second, _) = createThreeOutputFunction()
        val extraItem = fakeGroupItemDao.insertGroupItem(
            groupItemFor(main.id).copy(id = 0L, groupId = 2L)
        )

        uut.deleteFunction(ComponentDeleteRequest(groupItemId = extraItem, deleteEverywhere = false))

        assertNotNull(uut.getFunctionById(main.id))
        assertNotNull(uut.getFunctionById(second.id))
        assertEquals(3, fakeFunctionDao.numFunctions())
    }

    // =========================================================================
    // Duplicate
    // =========================================================================

    @Test
    fun `duplicateFunction of an additional output creates a standalone single output copy`() = runTest(dispatcher) {
        val (main, second, _) = createThreeOutputFunction()

        val copy = uut.duplicateFunction(groupItemFor(second.id).id, "Copy")!!

        val duplicated = uut.getFunctionById(copy.componentId)!!
        assertEquals("Copy", duplicated.name)
        assertNull(duplicated.parentFunctionId)
        assertNull(duplicated.outputNodeId)
        assertTrue(duplicated.functionGraph.additionalOutputNodes.isEmpty())
        // The copy evaluates what the duplicated output evaluated
        assertEquals(second.functionGraph.outputNodeFor(11), duplicated.functionGraph.outputNode)
        assertEquals(listOf(100L), duplicated.inputFeatureIds)
        // The original canvas is untouched
        assertEquals(2, uut.getFunctionById(main.id)!!.additionalOutputs.size)
    }

    @Test
    fun `duplicateFunction of the owner does not copy the additional outputs`() = runTest(dispatcher) {
        val (main, _, _) = createThreeOutputFunction()

        val copy = uut.duplicateFunction(groupItemFor(main.id).id, "Copy")!!

        val duplicated = uut.getFunctionById(copy.componentId)!!
        assertTrue(duplicated.additionalOutputs.isEmpty())
        assertTrue(duplicated.functionGraph.additionalOutputNodes.isEmpty())
        assertEquals(10, duplicated.functionGraph.outputNode.id)
        assertEquals(4, fakeFunctionDao.numFunctions())
        assertFalse(duplicated.id == main.id)
    }
}
