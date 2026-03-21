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
import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.database.entity.Function
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.serialization.FunctionGraphSerializer
import com.samco.trackandgraph.data.validation.FunctionValidator
import com.samco.trackandgraph.time.FakeTimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import com.samco.trackandgraph.data.database.dto.ComponentDeleteRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FunctionHelperImplTest {

    private lateinit var fakeFunctionDao: FakeFunctionDao
    private lateinit var fakeGroupItemDao: FakeGroupItemDao
    private lateinit var serializer: FunctionGraphSerializer
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private lateinit var uut: FunctionHelperImpl

    private val minimalFunctionGraph = FunctionGraph(
        nodes = emptyList(),
        outputNode = FunctionGraphNode.OutputNode(
            x = 0f,
            y = 0f,
            id = 0,
            dependencies = emptyList()
        ),
        isDuration = false
    )

    @Before
    fun before() {
        fakeFunctionDao = FakeFunctionDao()
        fakeGroupItemDao = FakeGroupItemDao()

        val json = Json { ignoreUnknownKeys = true }
        serializer = FunctionGraphSerializer(json)

        val fakeTransactionHelper = object : DatabaseTransactionHelper {
            override suspend fun <R> withTransaction(block: suspend () -> R): R = block()
        }

        val fakeTimeProvider = FakeTimeProvider()
        val fakeValidator = object : FunctionValidator {
            override suspend fun validateFunction(function: com.samco.trackandgraph.data.database.dto.Function) {}
        }

        uut = FunctionHelperImpl(
            transactionHelper = fakeTransactionHelper,
            dao = fakeFunctionDao,
            groupItemDao = fakeGroupItemDao,
            functionGraphSerializer = serializer,
            functionValidator = fakeValidator,
            timeProvider = fakeTimeProvider,
            io = dispatcher
        )
    }

    /**
     * Inserts a function entity directly (bypassing insertFunction's validation),
     * returning the function ID.
     */
    private fun insertFunctionDirectly(groupId: Long): Long {
        val featureId = fakeFunctionDao.insertFeature(
            Feature(id = 0L, name = "Test Function", description = "")
        )
        val serializedGraph = serializer.serialize(minimalFunctionGraph)!!
        val functionId = fakeFunctionDao.insertFunction(
            Function(id = 0L, featureId = featureId, functionGraph = serializedGraph)
        )
        fakeGroupItemDao.insertGroupItem(
            GroupItem(
                groupId = groupId,
                displayIndex = 0,
                childId = functionId,
                type = GroupItemType.FUNCTION,
                createdAt = 1000L
            )
        )
        return functionId
    }

    // =========================================================================
    // Uniqueness tests
    // =========================================================================

    @Test
    fun `getFunctionsForGroupSync returns unique=true when function in only one group`() =
        runTest(dispatcher) {
            // PREPARE
            val groupId = 1L
            insertFunctionDirectly(groupId)

            // EXECUTE
            val result = uut.getFunctionsForGroupSync(groupId)

            // VERIFY
            assertEquals(1, result.size)
            assertEquals(true, result[0].unique)
        }

    @Test
    fun `getFunctionsForGroupSync returns unique=false when function in multiple groups`() =
        runTest(dispatcher) {
            // PREPARE
            val group1 = 1L
            val group2 = 2L
            val functionId = insertFunctionDirectly(group1)
            // Add symlink to group2
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = group2,
                    displayIndex = 0,
                    childId = functionId,
                    type = GroupItemType.FUNCTION,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            val result = uut.getFunctionsForGroupSync(group1)

            // VERIFY
            assertEquals(1, result.size)
            assertEquals(false, result[0].unique)
        }

    @Test
    fun `getFunctionsForGroupSync sets unique independently per function`() =
        runTest(dispatcher) {
            // PREPARE - one unique function, one non-unique function in the same group
            val group1 = 1L
            val group2 = 2L

            val uniqueFunctionId = insertFunctionDirectly(group1)
            val sharedFunctionId = insertFunctionDirectly(group1)
            // Add symlink for sharedFunction to group2
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = group2,
                    displayIndex = 0,
                    childId = sharedFunctionId,
                    type = GroupItemType.FUNCTION,
                    createdAt = 1000L
                )
            )

            // EXECUTE
            val result = uut.getFunctionsForGroupSync(group1)

            // VERIFY
            assertEquals(2, result.size)
            val uniqueResult = result.first { it.id == uniqueFunctionId }
            val sharedResult = result.first { it.id == sharedFunctionId }
            assertEquals(true, uniqueResult.unique)
            assertEquals(false, sharedResult.unique)
        }

    @Test
    fun `getFunctionById returns unique=true when function in only one group`() =
        runTest(dispatcher) {
            // PREPARE
            val functionId = insertFunctionDirectly(1L)
            // EXECUTE
            val result = uut.getFunctionById(functionId)
            // VERIFY
            assertNotNull(result)
            assertEquals(true, result!!.unique)
        }

    @Test
    fun `getFunctionById returns unique=false when function in multiple groups`() =
        runTest(dispatcher) {
            // PREPARE
            val functionId = insertFunctionDirectly(1L)
            fakeGroupItemDao.insertGroupItem(
                GroupItem(groupId = 2L, displayIndex = 0, childId = functionId, type = GroupItemType.FUNCTION, createdAt = 1000L)
            )
            // EXECUTE
            val result = uut.getFunctionById(functionId)
            // VERIFY
            assertNotNull(result)
            assertEquals(false, result!!.unique)
        }

    @Test
    fun `tryGetFunctionByFeatureId returns unique=true when function in only one group`() =
        runTest(dispatcher) {
            // PREPARE
            val functionId = insertFunctionDirectly(1L)
            val featureId = fakeFunctionDao.getFunctionById(functionId)!!.featureId
            // EXECUTE
            val result = uut.tryGetFunctionByFeatureId(featureId)
            // VERIFY
            assertNotNull(result)
            assertEquals(true, result!!.unique)
        }

    @Test
    fun `tryGetFunctionByFeatureId returns unique=false when function in multiple groups`() =
        runTest(dispatcher) {
            // PREPARE
            val functionId = insertFunctionDirectly(1L)
            val featureId = fakeFunctionDao.getFunctionById(functionId)!!.featureId
            fakeGroupItemDao.insertGroupItem(
                GroupItem(groupId = 2L, displayIndex = 0, childId = functionId, type = GroupItemType.FUNCTION, createdAt = 1000L)
            )
            // EXECUTE
            val result = uut.tryGetFunctionByFeatureId(featureId)
            // VERIFY
            assertNotNull(result)
            assertEquals(false, result!!.unique)
        }

    // =========================================================================
    // Delete tests
    // =========================================================================

    @Test
    fun `deleteFunction removes function when deleteEverywhere is true`() =
        runTest(dispatcher) {
            // PREPARE
            val group1 = 1L
            val group2 = 2L
            val functionId = insertFunctionDirectly(group1)
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = group2,
                    displayIndex = 0,
                    childId = functionId,
                    type = GroupItemType.FUNCTION,
                    createdAt = 1000L
                )
            )
            val groupItemId = fakeGroupItemDao.getGroupItemsForChild(
                functionId, GroupItemType.FUNCTION
            ).first().id

            // EXECUTE
            uut.deleteFunction(
                ComponentDeleteRequest(
                    groupItemId = groupItemId,
                    deleteEverywhere = true
                )
            )

            // VERIFY
            assertEquals(0, fakeFunctionDao.numFunctions())
            assertEquals(
                0,
                fakeGroupItemDao.getGroupItemsForChild(functionId, GroupItemType.FUNCTION).size
            )
        }

    @Test
    fun `deleteFunction removes only symlink when deleteEverywhere is false and in multiple groups`() =
        runTest(dispatcher) {
            // PREPARE
            val group1 = 1L
            val group2 = 2L
            val functionId = insertFunctionDirectly(group1)
            fakeGroupItemDao.insertGroupItem(
                GroupItem(
                    groupId = group2,
                    displayIndex = 0,
                    childId = functionId,
                    type = GroupItemType.FUNCTION,
                    createdAt = 1000L
                )
            )
            val group2ItemId = fakeGroupItemDao.getGroupItemsForChild(
                functionId, GroupItemType.FUNCTION
            ).first { it.groupId == group2 }.id

            // EXECUTE
            uut.deleteFunction(
                ComponentDeleteRequest(
                    groupItemId = group2ItemId,
                    deleteEverywhere = false
                )
            )

            // VERIFY
            assertEquals(1, fakeFunctionDao.numFunctions())
            val remainingItems = fakeGroupItemDao.getGroupItemsForChild(
                functionId, GroupItemType.FUNCTION
            )
            assertEquals(1, remainingItems.size)
            assertEquals(group1, remainingItems[0].groupId)
        }

    @Test
    fun `deleteFunction deletes function entirely when deleteEverywhere is false but only in one group`() =
        runTest(dispatcher) {
            // PREPARE
            val groupId = 1L
            val functionId = insertFunctionDirectly(groupId)
            val groupItemId = fakeGroupItemDao.getGroupItemsForChild(
                functionId, GroupItemType.FUNCTION
            ).first().id

            // EXECUTE
            uut.deleteFunction(
                ComponentDeleteRequest(
                    groupItemId = groupItemId,
                    deleteEverywhere = false
                )
            )

            // VERIFY
            assertEquals(0, fakeFunctionDao.numFunctions())
            assertEquals(
                0,
                fakeGroupItemDao.getGroupItemsForChild(functionId, GroupItemType.FUNCTION).size
            )
        }

    @Test
    fun `deleteFunction does nothing when function does not exist`() =
        runTest(dispatcher) {
            // PREPARE — no function inserted

            // EXECUTE — should not throw
            uut.deleteFunction(
                ComponentDeleteRequest(
                    groupItemId = 999L,
                    deleteEverywhere = true
                )
            )

            // VERIFY — nothing to assert beyond no exception thrown
        }

    // =========================================================================
    // Duplicate tests
    // =========================================================================

    @Test
    fun `duplicateFunction creates copy with new functionId`() =
        runTest(dispatcher) {
            // PREPARE
            val groupId = 1L
            val functionId = insertFunctionDirectly(groupId)
            val groupItemId = fakeGroupItemDao.getGroupItemsForChild(
                functionId, GroupItemType.FUNCTION
            ).first().id

            // EXECUTE
            val duplicateResult = uut.duplicateFunction(groupItemId)

            // VERIFY
            assertNotNull(duplicateResult)
            val newFunctionId = duplicateResult!!.componentId
            assertNotEquals(functionId, newFunctionId)
            assertEquals(2, fakeFunctionDao.numFunctions())
            val newGroupItems = fakeGroupItemDao.getGroupItemsForChild(
                newFunctionId, GroupItemType.FUNCTION
            )
            assertEquals(1, newGroupItems.size)
            assertEquals(groupId, newGroupItems[0].groupId)
        }

    @Test
    fun `duplicateFunction returns null when groupItem does not exist`() =
        runTest(dispatcher) {
            // PREPARE — no group item with this id

            // EXECUTE
            val result = uut.duplicateFunction(999L)

            // VERIFY
            assertNull(result)
        }

    @Test
    fun `duplicateFunction places copy immediately after original in display order`() =
        runTest(dispatcher) {
            // PREPARE
            val groupId = 1L
            val functionId = insertFunctionDirectly(groupId)
            val originalGroupItem = fakeGroupItemDao.getGroupItemsForChild(
                functionId, GroupItemType.FUNCTION
            ).first()
            assertEquals(0, originalGroupItem.displayIndex)

            // EXECUTE
            val duplicateResult = uut.duplicateFunction(originalGroupItem.id)

            // VERIFY
            assertNotNull(duplicateResult)
            val duplicateGroupItem = fakeGroupItemDao.getGroupItemsForChild(
                duplicateResult!!.componentId, GroupItemType.FUNCTION
            ).first()
            assertEquals(1, duplicateGroupItem.displayIndex)
        }
}
