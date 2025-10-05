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

import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.NodeDependency
import com.samco.trackandgraph.data.lua.DaggerLuaEngineTestComponent
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.LuaVMLock
import com.samco.trackandgraph.data.sampling.DataSampler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.seconds

class DeadlockStressTest {

    private val luaEngineComponent = DaggerLuaEngineTestComponent.builder()
        .dataInteractor(mock())
        .assetReader(mock())
        .ioDispatcher(Dispatchers.IO)
        .timeProvider(mock())
        .build()

    private val luaEngine: LuaEngine = luaEngineComponent.provideLuaEngine()

    @Test
    fun `multi thread stress test should deadlock due to contention`() = runTest {
        // Setup 3-layer function graph chain
        val (layer1Function, layer2Function, layer3Function) = createThreeLayerFunctionChain()

        // Create a custom dispatcher with more threads to increase deadlock probability
        val aggressiveDispatcher = Executors.newFixedThreadPool(16).asCoroutineDispatcher()

        // Create a custom DataSampler implementation that handles suspend calls properly
        val recursiveDataSampler = object : DataSampler {
            override suspend fun getRawDataSampleForFeatureId(featureId: Long, vmLock: LuaVMLock?) =
                when (featureId) {
                    1001L -> FunctionGraphDataSample.create(vmLock, layer1Function, this, luaEngine)
                    2001L -> FunctionGraphDataSample.create(vmLock, layer2Function, this, luaEngine)
                    3001L -> FunctionGraphDataSample.create(vmLock, layer3Function, this, luaEngine)
                    else -> null
                }

            override suspend fun getDataSampleForFeatureId(featureId: Long, vmLock: LuaVMLock?) =
                error("Should not be called by this test")

            override suspend fun getLabelsForFeatureId(featureId: Long) =
                error("Should not be called by this test")

            override suspend fun getDataSamplePropertiesForFeatureId(featureId: Long) =
                error("Should not be called by this test")
        }

        // Run 100 iterations of the function graph in parallel
        val testFuture = async {
            repeat(100) { iteration ->
                launch(aggressiveDispatcher) {
                    try {
                        // Create top-level function graph and iterate through results
                        val topLevelSample = FunctionGraphDataSample.create(
                            // let the FunctionGraphDataSample create the VM lock
                            vmLock = null,
                            function = layer3Function,
                            dataSampler = recursiveDataSampler,
                            luaEngine = luaEngine
                        )

                        // Force full evaluation by collecting results
                        val results = topLevelSample.toList()

                        // Verify we got some results (basic sanity check)
                        assert(results.isNotEmpty()) { "Iteration $iteration: No results returned" }

                        // Cleanup
                        topLevelSample.dispose()

                        println("Iteration $iteration completed successfully with ${results.size} results")
                    } catch (e: Exception) {
                        println("Iteration $iteration failed: ${e.message}")
                        throw e
                    }
                }
            }
        }

        try {
            // Wait for completion with timeout - this will throw TimeoutException if deadlocked
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(10.seconds) {
                    testFuture.await()
                    println("Deadlock stress test completed successfully!")
                }
            }
        } catch (e: TimeoutException) {
            println("DEADLOCK DETECTED: Test timed out after 10 seconds")
            println("This confirms the VM lock deadlock issue exists")

            // Cancel the future and clean up
            testFuture.cancel()
            aggressiveDispatcher.close()

            // Fail the test with a clear message about the deadlock
            throw AssertionError(
                "Deadlock detected in recursive function graph evaluation. " +
                        "This test successfully reproduced the VM lock contention issue."
            )
        } catch (e: Exception) {
            aggressiveDispatcher.close()
            throw e
        }
    }

    private fun createThreeLayerFunctionChain(): Triple<Function, Function, Function> {
        // Layer 1: Simple leaf function with Lua script that generates data
        val layer1Function = createLeafFunction(
            functionId = 1L,
            featureId = 1001L,
            name = "Layer 1 Leaf Function"
        )

        // Layer 2: Function that depends on Layer 1
        val layer2Function = createPassThroughFunction(
            functionId = 2L,
            featureId = 2001L,
            name = "Layer 2 Pass-through Function",
            inputFeatureId = 1001L // depends on layer 1
        )

        // Layer 3: Function that depends on Layer 2
        val layer3Function = createPassThroughFunction(
            functionId = 3L,
            featureId = 3001L,
            name = "Layer 3 Top Function",
            inputFeatureId = 2001L // depends on layer 2
        )

        return Triple(layer1Function, layer2Function, layer3Function)
    }

    private fun createLeafFunction(functionId: Long, featureId: Long, name: String): Function {
        val luaScriptNode = FunctionGraphNode.LuaScriptNode(
            x = 100f,
            y = 100f,
            id = 1,
            script = """
                return function(data_sources, config)
                    local i = 0
                    
                    return function()
                        if i >= 500 then return nil end
                        
                        i = i + 1
                        local data_point = {
                            timestamp = 1000000000 + i * 1000,
                            offset = 0,
                            value = i * 10,
                            label = "leaf_" .. i,
                            note = ""
                        }
                        
                        return data_point
                    end
                end
            """.trimIndent(),
            inputConnectorCount = 0,
            dependencies = emptyList()
        )

        val outputNode = FunctionGraphNode.OutputNode(
            x = 200f,
            y = 100f,
            id = 2,
            dependencies = listOf(NodeDependency(connectorIndex = 0, nodeId = 1))
        )

        val functionGraph = FunctionGraph(
            nodes = listOf(luaScriptNode, outputNode),
            outputNode = outputNode,
            isDuration = false
        )

        return Function(
            id = functionId,
            featureId = featureId,
            name = name,
            groupId = 1L,
            displayIndex = 0,
            description = "Leaf function for deadlock test",
            functionGraph = functionGraph,
            inputFeatureIds = emptyList()
        )
    }

    private fun createPassThroughFunction(
        functionId: Long,
        featureId: Long,
        name: String,
        inputFeatureId: Long
    ): Function {
        // Feature node that references the input function
        val featureNode = FunctionGraphNode.FeatureNode(
            x = 100f,
            y = 100f,
            id = 1,
            featureId = inputFeatureId
        )

        // Lua script node that passes through data from the feature node
        val luaScriptNode = FunctionGraphNode.LuaScriptNode(
            x = 200f,
            y = 100f,
            id = 2,
            script = """
                return function(source, config)
                    return function()
                        local data_point = source.dp()
                        if not data_point then return nil end
                        
                        data_point.value = data_point.value + 0.1
                        data_point.label = data_point.label .. "_pass"
                        
                        return data_point
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

        return Function(
            id = functionId,
            featureId = featureId,
            name = name,
            groupId = 1L,
            displayIndex = 0,
            description = "Pass-through function for deadlock test",
            functionGraph = functionGraph,
            inputFeatureIds = listOf(inputFeatureId)
        )
    }
}
