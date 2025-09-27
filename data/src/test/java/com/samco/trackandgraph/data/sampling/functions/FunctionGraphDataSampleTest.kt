/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.data.sampling.functions

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.NodeDependency
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.DaggerLuaEngineTestComponent
import com.samco.trackandgraph.data.sampling.RawDataSample
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.threeten.bp.OffsetDateTime

class FunctionGraphDataSampleTest {

    private val luaEngineComponent = DaggerLuaEngineTestComponent.builder()
        .dataInteractor(mock())
        .assetReader(mock())
        .ioDispatcher(Dispatchers.IO)
        .timeProvider(mock())
        .build()
    
    private val luaEngine: LuaEngine = luaEngineComponent.provideLuaEngine()

    @Test
    fun `single data source reflects input data`() {
        // Arrange
        val featureId = 1L
        val inputDataPoints = listOf(
            DataPoint(
                timestamp = OffsetDateTime.parse("2023-01-03T10:00:00Z"),
                featureId = featureId,
                value = 30.0,
                label = "Point 3",
                note = ""
            ),
            DataPoint(
                timestamp = OffsetDateTime.parse("2023-01-02T10:00:00Z"),
                featureId = featureId,
                value = 20.0,
                label = "Point 2",
                note = ""
            ),
            DataPoint(
                timestamp = OffsetDateTime.parse("2023-01-01T10:00:00Z"),
                featureId = featureId,
                value = 10.0,
                label = "Point 1",
                note = ""
            )
        )

        val rawDataSample = RawDataSample.fromSequence(
            data = inputDataPoints.asSequence(),
            getRawDataPoints = { inputDataPoints },
            onDispose = {}
        )

        val featureNode = FunctionGraphNode.FeatureNode(
            x = 100f,
            y = 100f,
            id = 1,
            featureId = featureId
        )
        
        val outputNode = FunctionGraphNode.OutputNode(
            x = 200f,
            y = 100f,
            id = 2,
            dependencies = listOf(NodeDependency(connectorIndex = 0, nodeId = 1))
        )

        val functionGraph = FunctionGraph(
            nodes = listOf(featureNode, outputNode),
            outputNode = outputNode,
            isDuration = false
        )

        val function = Function(
            id = 1L,
            featureId = 2L,
            name = "Test Function",
            groupId = 1L,
            displayIndex = 0,
            description = "Test",
            functionGraph = functionGraph,
            inputFeatureIds = listOf(featureId)
        )

        val dataSources = mapOf(featureId to rawDataSample)

        // Act
        val functionGraphDataSample = FunctionGraphDataSample(function, dataSources, luaEngine)
        val result = functionGraphDataSample.toList()

        // Assert
        assertEquals("Should have same number of data points", inputDataPoints.size, result.size)
        assertEquals("Should have same data points in same order", inputDataPoints, result)
        assertEquals("getRawDataPoints should match iterated data", inputDataPoints, functionGraphDataSample.getRawDataPoints())
        
        // Cleanup
        functionGraphDataSample.dispose()
    }

    @Test
    fun `two data sources merge in timestamp order`() {
        // Arrange
        val featureId1 = 1L
        val featureId2 = 2L
        
        val dataPoints1 = listOf(
            DataPoint(
                timestamp = OffsetDateTime.parse("2023-01-05T10:00:00Z"),
                featureId = featureId1,
                value = 50.0,
                label = "Feature1 Point 2",
                note = ""
            ),
            DataPoint(
                timestamp = OffsetDateTime.parse("2023-01-02T10:00:00Z"),
                featureId = featureId1,
                value = 20.0,
                label = "Feature1 Point 1",
                note = ""
            )
        )
        
        val dataPoints2 = listOf(
            DataPoint(
                timestamp = OffsetDateTime.parse("2023-01-04T10:00:00Z"),
                featureId = featureId2,
                value = 40.0,
                label = "Feature2 Point 2",
                note = ""
            ),
            DataPoint(
                timestamp = OffsetDateTime.parse("2023-01-01T10:00:00Z"),
                featureId = featureId2,
                value = 10.0,
                label = "Feature2 Point 1",
                note = ""
            )
        )

        val rawDataSample1 = RawDataSample.fromSequence(
            data = dataPoints1.asSequence(),
            getRawDataPoints = { dataPoints1 },
            onDispose = {}
        )
        
        val rawDataSample2 = RawDataSample.fromSequence(
            data = dataPoints2.asSequence(),
            getRawDataPoints = { dataPoints2 },
            onDispose = {}
        )

        val featureNode1 = FunctionGraphNode.FeatureNode(
            x = 100f,
            y = 100f,
            id = 1,
            featureId = featureId1
        )
        
        val featureNode2 = FunctionGraphNode.FeatureNode(
            x = 100f,
            y = 200f,
            id = 2,
            featureId = featureId2
        )
        
        val outputNode = FunctionGraphNode.OutputNode(
            x = 300f,
            y = 150f,
            id = 3,
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 1),
                NodeDependency(connectorIndex = 1, nodeId = 2)
            )
        )

        val functionGraph = FunctionGraph(
            nodes = listOf(featureNode1, featureNode2, outputNode),
            outputNode = outputNode,
            isDuration = false
        )

        val function = Function(
            id = 1L,
            featureId = 3L,
            name = "Test Merge Function",
            groupId = 1L,
            displayIndex = 0,
            description = "Test merge",
            functionGraph = functionGraph,
            inputFeatureIds = listOf(featureId1, featureId2)
        )

        val dataSources = mapOf(
            featureId1 to rawDataSample1,
            featureId2 to rawDataSample2
        )

        // Act
        val functionGraphDataSample = FunctionGraphDataSample(function, dataSources, luaEngine)
        val result = functionGraphDataSample.toList()

        // Assert - should be merged in descending timestamp order
        val expectedOrder = listOf(
            dataPoints1[0], // 2023-01-05T10:00:00Z - Feature1 Point 2
            dataPoints2[0], // 2023-01-04T10:00:00Z - Feature2 Point 2
            dataPoints1[1], // 2023-01-02T10:00:00Z - Feature1 Point 1
            dataPoints2[1]  // 2023-01-01T10:00:00Z - Feature2 Point 1
        )
        
        assertEquals("Should have combined data points from both sources", 4, result.size)
        assertEquals("Should be merged in descending timestamp order", expectedOrder, result)
        assertEquals("getRawDataPoints should match iterated data", expectedOrder, functionGraphDataSample.getRawDataPoints())
        
        // Cleanup
        functionGraphDataSample.dispose()
    }

    @Test
    fun `duplicate feature dependencies only include data points once in getRawDataPoints`() {
        // Arrange
        val featureId = 1L
        val inputDataPoints = listOf(
            DataPoint(
                timestamp = OffsetDateTime.parse("2023-01-02T10:00:00Z"),
                featureId = featureId,
                value = 20.0,
                label = "Point 2",
                note = ""
            ),
            DataPoint(
                timestamp = OffsetDateTime.parse("2023-01-01T10:00:00Z"),
                featureId = featureId,
                value = 10.0,
                label = "Point 1",
                note = ""
            )
        )

        val rawDataSample = RawDataSample.fromSequence(
            data = inputDataPoints.asSequence(),
            getRawDataPoints = { inputDataPoints },
            onDispose = {}
        )

        // Create two feature nodes that reference the same feature
        val featureNode1 = FunctionGraphNode.FeatureNode(
            x = 100f,
            y = 100f,
            id = 1,
            featureId = featureId
        )
        
        val featureNode2 = FunctionGraphNode.FeatureNode(
            x = 100f,
            y = 200f,
            id = 2,
            featureId = featureId
        )
        
        val outputNode = FunctionGraphNode.OutputNode(
            x = 300f,
            y = 150f,
            id = 3,
            dependencies = listOf(
                NodeDependency(connectorIndex = 0, nodeId = 1),
                NodeDependency(connectorIndex = 1, nodeId = 2)
            )
        )

        val functionGraph = FunctionGraph(
            nodes = listOf(featureNode1, featureNode2, outputNode),
            outputNode = outputNode,
            isDuration = false
        )

        val function = Function(
            id = 1L,
            featureId = 3L,
            name = "Test Duplicate Function",
            groupId = 1L,
            displayIndex = 0,
            description = "Test duplicate dependencies",
            functionGraph = functionGraph,
            inputFeatureIds = listOf(featureId)
        )

        val dataSources = mapOf(featureId to rawDataSample)

        // Act
        val functionGraphDataSample = FunctionGraphDataSample(function, dataSources, luaEngine)
        val result = functionGraphDataSample.toList()

        // Assert - should only get each data point once, even though it's referenced by two nodes
        assertEquals("Should have same number of unique data points", inputDataPoints.size * 2, result.size)
        assertEquals("Should have same data points in same order", inputDataPoints.flatMap { listOf(it, it) }, result)
        assertEquals("getRawDataPoints should contain unique data points only", inputDataPoints, functionGraphDataSample.getRawDataPoints())
        
        // Cleanup
        functionGraphDataSample.dispose()
    }

    @Test
    fun `lua script function processes data points correctly`() {
        // Arrange
        val featureId = 1L
        val inputDataPoints = listOf(
            DataPoint(
                timestamp = OffsetDateTime.parse("2023-01-03T10:00:00Z"),
                featureId = featureId,
                value = 10.0,
                label = "input1",
                note = ""
            ),
            DataPoint(
                timestamp = OffsetDateTime.parse("2023-01-02T10:00:00Z"),
                featureId = featureId,
                value = 20.0,
                label = "input2",
                note = ""
            ),
            DataPoint(
                timestamp = OffsetDateTime.parse("2023-01-01T10:00:00Z"),
                featureId = featureId,
                value = 30.0,
                label = "input3",
                note = ""
            )
        )

        val rawDataSample = RawDataSample.fromSequence(
            data = inputDataPoints.asSequence(),
            getRawDataPoints = { inputDataPoints },
            onDispose = {}
        )

        val featureNode = FunctionGraphNode.FeatureNode(
            x = 100f,
            y = 100f,
            id = 1,
            featureId = featureId
        )
        
        // Create a Lua script node that doubles each value and modifies the label
        val luaScriptNode = FunctionGraphNode.LuaScriptNode(
            x = 200f,
            y = 100f,
            id = 2,
            script = """
                return function(data_sources)
                    local source = data_sources[1]
                    local data_point = source.dp()
                    while data_point do
                        -- Double the value and modify the label
                        data_point.value = data_point.value * 2
                        data_point.label = data_point.label .. "_doubled"
                        coroutine.yield(data_point)
                        data_point = source.dp()
                    end
                end
            """.trimIndent(),
            inputConnectorCount = 1,
            dependencies = listOf(NodeDependency(connectorIndex = 0, nodeId = 1))
        )
        
        val outputNode = FunctionGraphNode.OutputNode(
            x = 300f,
            y = 100f,
            id = 3,
            dependencies = listOf(NodeDependency(connectorIndex = 0, nodeId = 2))
        )

        val functionGraph = FunctionGraph(
            nodes = listOf(featureNode, luaScriptNode, outputNode),
            outputNode = outputNode,
            isDuration = false
        )

        val function = Function(
            id = 1L,
            featureId = 2L,
            name = "Lua Script Test Function",
            groupId = 1L,
            displayIndex = 0,
            description = "Test Lua script processing",
            functionGraph = functionGraph,
            inputFeatureIds = listOf(featureId)
        )

        val dataSources = mapOf(featureId to rawDataSample)

        // Act
        val functionGraphDataSample = FunctionGraphDataSample(function, dataSources, luaEngine)
        val result = functionGraphDataSample.toList()

        // Assert
        assertEquals("Should have same number of data points", inputDataPoints.size, result.size)
        
        // Verify each data point was processed correctly
        assertEquals(20.0, result[0].value, 0.001)  // 10.0 * 2
        assertEquals("input1_doubled", result[0].label)
        assertEquals(40.0, result[1].value, 0.001)  // 20.0 * 2
        assertEquals("input2_doubled", result[1].label)
        assertEquals(60.0, result[2].value, 0.001)  // 30.0 * 2
        assertEquals("input3_doubled", result[2].label)
        
        // Verify timestamps are preserved
        assertEquals(inputDataPoints[0].timestamp, result[0].timestamp)
        assertEquals(inputDataPoints[1].timestamp, result[1].timestamp)
        assertEquals(inputDataPoints[2].timestamp, result[2].timestamp)
        
        // Cleanup
        functionGraphDataSample.dispose()
    }
}
