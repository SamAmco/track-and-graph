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

package com.samco.trackandgraph.functions.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.database.dto.CreatedComponent
import com.samco.trackandgraph.data.database.dto.FunctionCreateRequest
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.FunctionOutput
import com.samco.trackandgraph.data.database.dto.FunctionOutputRequest
import com.samco.trackandgraph.data.database.dto.FunctionUpdateRequest
import com.samco.trackandgraph.data.database.dto.NodeDependency
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.fixtures.testFunction
import com.samco.trackandgraph.functions.node_editor.viewmodel.AddNodeData
import com.samco.trackandgraph.functions.node_editor.viewmodel.Connector
import com.samco.trackandgraph.functions.node_editor.viewmodel.ConnectorType
import com.samco.trackandgraph.functions.node_editor.viewmodel.FunctionGraphBuilder
import com.samco.trackandgraph.functions.node_editor.viewmodel.FunctionGraphDecoder
import com.samco.trackandgraph.functions.node_editor.viewmodel.FunctionsScreenViewModelImpl
import com.samco.trackandgraph.functions.node_editor.viewmodel.LuaScriptConfigurationEncoder
import com.samco.trackandgraph.functions.node_editor.viewmodel.LuaScriptNodeProvider
import com.samco.trackandgraph.functions.node_editor.viewmodel.Node
import com.samco.trackandgraph.functions.node_editor.viewmodel.ValidationError
import com.samco.trackandgraph.remoteconfig.UrlNavigator
import com.samco.trackandgraph.util.buildGroupGraph
import com.samco.trackandgraph.util.feature
import com.samco.trackandgraph.util.group
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class FunctionsScreenViewModelMultiOutputTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dataInteractor: DataInteractor = mock()

    private lateinit var uut: FunctionsScreenViewModelImpl

    @Before
    fun before() = runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        whenever(dataInteractor.getGroupGraphSync()).thenReturn(
            buildGroupGraph(
                groups = listOf(group("root", 0)),
                features = listOf(feature(100L, "Source", 0L)),
            )
        )
        whenever(dataInteractor.hasAnyFunctions()).thenReturn(true)
        whenever(dataInteractor.insertFunction(any())).thenReturn(CreatedComponent(1L, 1L))
        whenever(dataInteractor.getFeatureIdsDependingOn(any())).thenReturn(emptySet())

        uut = FunctionsScreenViewModelImpl(
            context = mock<Context>(),
            ioDispatcher = dispatcher,
            dataInteractor = dataInteractor,
            functionGraphBuilder = FunctionGraphBuilder(mock<LuaScriptConfigurationEncoder>()),
            functionGraphDecoder = FunctionGraphDecoder(mock<LuaScriptNodeProvider>()),
            luaScriptNodeProvider = mock(),
            urlNavigator = mock<UrlNavigator>(),
        )
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    private fun outputs() = uut.nodes.value.filterIsInstance<Node.Output>()

    private fun connect(from: Int, to: Int) {
        uut.onDownOnConnector(Connector(from, ConnectorType.OUTPUT, 0))
        uut.onDropConnector(Connector(to, ConnectorType.INPUT, 0))
    }

    /** New function with a connected primary output (id 1) named "Main" and a data source (id 2). */
    private fun initNewFunctionWithSource() {
        uut.init(groupId = 5L, functionId = null)
        uut.onAddNode(AddNodeData.DataSourceNode, Offset.Zero)
        outputs().single().name.value = TextFieldValue("Main")
        connect(from = 2, to = 1)
    }

    // ---------------------------------------------------------------- adding / deleting

    @Test
    fun `adding an output node adds an additional output`() = runTest(dispatcher) {
        uut.init(groupId = 5L, functionId = null)

        uut.onAddNode(AddNodeData.OutputNode, Offset(10f, 20f))

        val added = outputs().single { !it.isPrimary }
        assertEquals(2, added.id)
        assertFalse(added.isUpdateMode)
        assertEquals(Offset(10f, 20f), uut.getWorldPosition(added))
        assertTrue(outputs().single { it.isPrimary }.isPrimary)
    }

    @Test
    fun `an output added while editing an existing function is in update mode`() = runTest(dispatcher) {
        whenever(dataInteractor.getFunctionById(1L)).thenReturn(testFunction(id = 1L))
        uut.init(groupId = 5L, functionId = 1L)

        uut.onAddNode(AddNodeData.OutputNode, Offset.Zero)

        assertTrue(outputs().single { !it.isPrimary }.isUpdateMode)
    }

    @Test
    fun `the primary output cannot be deleted`() = runTest(dispatcher) {
        uut.init(groupId = 5L, functionId = null)

        uut.onDeleteNode(outputs().single())

        assertEquals(1, outputs().size)
    }

    @Test
    fun `an additional output can be deleted along with its edges`() = runTest(dispatcher) {
        initNewFunctionWithSource()
        uut.onAddNode(AddNodeData.OutputNode, Offset.Zero)
        val added = outputs().single { !it.isPrimary }
        // The UI registers each connector as it is laid out
        uut.onUpsertConnector(Connector(added.id, ConnectorType.INPUT, 0), Offset.Zero)
        connect(from = 2, to = added.id)
        assertEquals(2, uut.edges.value.size)

        uut.onDeleteNode(added)

        assertEquals(1, outputs().size)
        assertEquals(listOf(1), uut.edges.value.map { it.to.nodeId })
    }

    // ---------------------------------------------------------------- saving

    @Test
    fun `saving validates every output node`() = runTest(dispatcher) {
        initNewFunctionWithSource()
        uut.onAddNode(AddNodeData.OutputNode, Offset.Zero) // blank name and not connected

        uut.onCreateOrUpdateFunction()

        verify(dataInteractor, never()).insertFunction(any())
        assertTrue(outputs().single { it.isPrimary }.validationErrors.isEmpty())
        assertEquals(
            listOf(ValidationError.MISSING_NAME, ValidationError.NO_INPUTS),
            outputs().single { !it.isPrimary }.validationErrors
        )
    }

    @Test
    fun `saving a new function sends the additional outputs`() = runTest(dispatcher) {
        initNewFunctionWithSource()
        uut.onAddNode(AddNodeData.OutputNode, Offset.Zero)
        val added = outputs().single { !it.isPrimary }
        added.name.value = TextFieldValue("Second")
        added.description.value = TextFieldValue("second description")
        connect(from = 2, to = added.id)

        uut.onCreateOrUpdateFunction()

        val captor = argumentCaptor<FunctionCreateRequest>()
        verify(dataInteractor).insertFunction(captor.capture())
        val request = captor.firstValue
        assertEquals("Main", request.name)
        assertEquals(5L, request.groupId)
        assertEquals(
            listOf(FunctionOutputRequest(added.id, "Second", "second description")),
            request.additionalOutputs
        )
        assertEquals(listOf(added.id), request.functionGraph.additionalOutputNodes.map { it.id })
        assertTrue(request.inputFeatureIds.isEmpty()) // the data source has no feature selected
    }

    @Test
    fun `saving a function without additional outputs sends none`() = runTest(dispatcher) {
        initNewFunctionWithSource()

        uut.onCreateOrUpdateFunction()

        val captor = argumentCaptor<FunctionCreateRequest>()
        verify(dataInteractor).insertFunction(captor.capture())
        assertTrue(captor.firstValue.additionalOutputs.isEmpty())
    }

    @Test
    fun `a failed save shows a generic error on the primary output only`() = runTest(dispatcher) {
        whenever(dataInteractor.insertFunction(any())).thenThrow(IllegalStateException("boom"))
        initNewFunctionWithSource()
        uut.onAddNode(AddNodeData.OutputNode, Offset.Zero)
        val added = outputs().single { !it.isPrimary }
        added.name.value = TextFieldValue("Second")
        connect(from = 2, to = added.id)

        uut.onCreateOrUpdateFunction()

        assertEquals(
            listOf(ValidationError.GENERIC_ERROR),
            outputs().single { it.isPrimary }.validationErrors
        )
        assertTrue(outputs().single { !it.isPrimary }.validationErrors.isEmpty())
    }

    // ---------------------------------------------------------------- editing

    private fun existingMultiOutputFunction() = testFunction(
        id = 1L,
        name = "Main",
        featureId = 11L,
        functionGraph = FunctionGraph(
            nodes = listOf(FunctionGraphNode.FeatureNode(0f, 0f, 1, featureId = 100L)),
            outputNode = FunctionGraphNode.OutputNode(0f, 0f, 2, listOf(NodeDependency(0, 1))),
            isDuration = false,
            additionalOutputNodes = listOf(
                FunctionGraphNode.OutputNode(0f, 0f, 3, listOf(NodeDependency(0, 1)))
            ),
        ),
    ).copy(
        additionalOutputs = listOf(FunctionOutput(2L, 12L, 3, "Second", ""))
    )

    @Test
    fun `opening an additional output edits the canvas of its owner`() = runTest(dispatcher) {
        val owner = existingMultiOutputFunction()
        val additional = owner.copy(
            id = 2L, featureId = 12L, name = "Second", parentFunctionId = 1L, outputNodeId = 3,
            additionalOutputs = emptyList(),
        )
        whenever(dataInteractor.getFunctionById(2L)).thenReturn(additional)
        whenever(dataInteractor.getFunctionById(1L)).thenReturn(owner)

        uut.init(groupId = 5L, functionId = 2L)
        uut.onCreateOrUpdateFunction()

        val captor = argumentCaptor<FunctionUpdateRequest>()
        verify(dataInteractor).updateFunction(captor.capture())
        assertEquals(1L, captor.firstValue.id)
        assertEquals("Main", captor.firstValue.name)
        assertEquals(
            listOf(FunctionOutputRequest(3, "Second", "")),
            captor.firstValue.additionalOutputs
        )
    }

    @Test
    fun `removing an additional output while editing drops it from the update`() = runTest(dispatcher) {
        whenever(dataInteractor.getFunctionById(1L)).thenReturn(existingMultiOutputFunction())
        uut.init(groupId = 5L, functionId = 1L)
        uut.onDeleteNode(outputs().single { !it.isPrimary })

        uut.onCreateOrUpdateFunction()

        val captor = argumentCaptor<FunctionUpdateRequest>()
        verify(dataInteractor).updateFunction(captor.capture())
        assertTrue(captor.firstValue.additionalOutputs!!.isEmpty())
        assertTrue(captor.firstValue.functionGraph!!.additionalOutputNodes.isEmpty())
    }

    @Test
    fun `data sources cannot read any output of the function being edited`() = runTest(dispatcher) {
        whenever(dataInteractor.getFunctionById(1L)).thenReturn(existingMultiOutputFunction())
        whenever(dataInteractor.getFeatureIdsDependingOn(12L)).thenReturn(setOf(99L))

        uut.init(groupId = 5L, functionId = 1L)

        val dataSource = uut.nodes.value.filterIsInstance<Node.DataSource>().single()
        // Own feature, the sibling output's feature and something depending on the sibling
        assertEquals(setOf(11L, 12L, 99L), dataSource.dependentFeatureIds)
    }
}
