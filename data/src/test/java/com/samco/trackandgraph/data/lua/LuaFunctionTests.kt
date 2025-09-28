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
package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.sampling.RawDataSample
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class LuaFunctionTests : LuaEngineImplTest() {

    @Test
    fun `Basic generator function yields input data points`() = testLuaFunction(
        dataSources = listOf(
            sequenceOf(
                TestDP(timestamp = 1000, value = 1.0),
                TestDP(timestamp = 2000, value = 2.0),
                TestDP(timestamp = 3000, value = 3.0)
            )
        ),
        script = """
            return function(data_sources)
                local source = data_sources[1]
                local data_point = source.dp()
                while data_point do
                    coroutine.yield(data_point)
                    data_point = source.dp()
                end
            end
        """.trimIndent()
    ) {
        assertEquals(3, resultList.size)
        assertEquals(1.0, resultList[0].value, 0.001)
        assertEquals(2.0, resultList[1].value, 0.001)
        assertEquals(3.0, resultList[2].value, 0.001)
    }

    @Test
    fun `Generator function adds 1 to each input data point`() = testLuaFunction(
        dataSources = listOf(
            sequenceOf(
                TestDP(timestamp = 1000, value = 1.0, label = "first"),
                TestDP(timestamp = 2000, value = 2.5, label = "second"),
                TestDP(timestamp = 3000, value = 10.0, label = "third")
            )
        ),
        script = """
            
            return function(data_sources)
                local source = data_sources[1]
                local all_data = source.dpall()
                for _, data_point in ipairs(all_data) do
                    -- Add 1 to the value
                    data_point.value = data_point.value + 1
                    coroutine.yield(data_point)
                end
            end

        """.trimIndent()
    ) {
        assertEquals(3, resultList.size)
        assertEquals(2.0, resultList[0].value, 0.001)
        assertEquals("first", resultList[0].label)
        assertEquals(3.5, resultList[1].value, 0.001)
        assertEquals("second", resultList[1].label)
        assertEquals(11.0, resultList[2].value, 0.001)
        assertEquals("third", resultList[2].label)
    }

    @Test
    fun `Generator function maintains local state across yields`() = testLuaFunction(
        dataSources = listOf(
            sequenceOf(
                TestDP(timestamp = 1000, value = 100.0, label = "base"),
                TestDP(timestamp = 2000, value = 200.0, label = "base"),
                TestDP(timestamp = 3000, value = 300.0, label = "base")
            )
        ),
        script = """
            return function(data_sources)
                local source = data_sources[1]
                local counter = 0  -- Local variable that persists across yields
                
                local data_point = source.dp()
                while data_point do
                    counter = counter + 1
                    -- Create new data point with counter value
                    local new_point = {
                        timestamp = data_point.timestamp,
                        offset = data_point.offset,
                        value = counter,  -- Use the incrementing counter
                        label = "count_" .. counter,
                        note = data_point.note
                    }
                    coroutine.yield(new_point)
                    data_point = source.dp()
                end
            end
        """.trimIndent()
    ) {
        assertEquals(3, resultList.size)
        assertEquals(1.0, resultList[0].value, 0.001)
        assertEquals("count_1", resultList[0].label)
        assertEquals(2.0, resultList[1].value, 0.001)
        assertEquals("count_2", resultList[1].label)
        assertEquals(3.0, resultList[2].value, 0.001)
        assertEquals("count_3", resultList[2].label)
    }

    @Test
    fun `Function only consumes needed upstream data points (lazy evaluation)`() {
        var upstreamConsumed = 0
        val totalAvailable = 1000

        testLuaFunction(
            dataSources = listOf(
                sequence {
                    repeat(totalAvailable) { i ->
                        upstreamConsumed++
                        yield(TestDP(timestamp = (i + 1) * 1000L, value = (i + 1).toDouble(), label = "item_${i + 1}"))
                    }
                }
            ),
            script = """
                return function(data_sources)
                    local source = data_sources[1]
                    local count = 0
                    local max_items = 3  -- Only process first 3 items
                    
                    local data_point = source.dp()
                    while data_point and count < max_items do
                        count = count + 1
                        data_point.label = "processed_" .. count
                        coroutine.yield(data_point)
                        data_point = source.dp()
                    end
                    -- Exit early, don't consume remaining data points
                end
            """.trimIndent()
        ) {
            // Verify we only got 3 results
            assertEquals(3, resultList.size)
            assertEquals("processed_1", resultList[0].label)
            assertEquals("processed_2", resultList[1].label)
            assertEquals("processed_3", resultList[2].label)

            // Most importantly: verify we only consumed 4 upstream data points (3 processed + 1 look-ahead), not all 1000
            assertEquals("Should only consume 4 upstream data points (3 processed + 1 look-ahead), not all $totalAvailable", 4, upstreamConsumed)
        }
    }

    @Test
    fun `Generic stateful counter test with 10 data points`() {
        testStatefulCounter(10)
    }

    @Test
    fun `Function returns table with generator key`() = testLuaFunction(
        dataSources = listOf(
            sequenceOf(
                TestDP(timestamp = 1000, value = 10.0, label = "first"),
                TestDP(timestamp = 2000, value = 20.0, label = "second"),
                TestDP(timestamp = 3000, value = 30.0, label = "third")
            )
        ),
        script = """
            return {
                generator = function(data_sources)
                    local source = data_sources[1]
                    local multiplier = 2  -- Local state for the generator
                    
                    local data_point = source.dp()
                    while data_point do
                        -- Create new data point with doubled value
                        local new_point = {
                            timestamp = data_point.timestamp,
                            offset = data_point.offset,
                            value = data_point.value * multiplier,
                            label = data_point.label .. "_x" .. multiplier,
                            note = data_point.note
                        }
                        coroutine.yield(new_point)
                        data_point = source.dp()
                    end
                end
            }
        """.trimIndent()
    ) {
        assertEquals(3, resultList.size)
        assertEquals(20.0, resultList[0].value, 0.001)  // 10.0 * 2
        assertEquals("first_x2", resultList[0].label)
        assertEquals(40.0, resultList[1].value, 0.001)  // 20.0 * 2
        assertEquals("second_x2", resultList[1].label)
        assertEquals(60.0, resultList[2].value, 0.001)  // 30.0 * 2
        assertEquals("third_x2", resultList[2].label)
    }

    /**
     * Generic helper to test stateful counter behavior with configurable data point count
     */
    private fun testStatefulCounter(dataPointCount: Int) {
        testLuaFunction(
            dataSources = listOf(
                sequence {
                    repeat(dataPointCount) { i ->
                        yield(TestDP(timestamp = (i + 1) * 1000L, value = 100.0, label = "base"))
                    }
                }
            ),
            script = """
                return function(data_sources)
                    local source = data_sources[1]
                    local counter = 0  -- Local variable that persists across yields
                    
                    local data_point = source.dp()
                    while data_point do
                        counter = counter + 1
                        -- Mutate the input data point directly
                        data_point.value = counter  -- Use the incrementing counter
                        data_point.label = "count_" .. counter
                        coroutine.yield(data_point)
                        data_point = source.dp()
                    end
                end
            """.trimIndent()
        ) {
            assertEquals(dataPointCount, resultList.size)
            // Verify each counter value is correct
            for (i in 0 until dataPointCount) {
                assertEquals((i + 1).toDouble(), resultList[i].value, 0.001)
                assertEquals("count_${i + 1}", resultList[i].label)
            }
        }
    }

    /**
     * Helper to test stateful counter behavior with a shared Lua engine instance
     * This version uses a provided engine instead of creating a new one
     */
    private fun testStatefulCounterWithSharedEngine(luaEngine: LuaEngineImpl, dataPointCount: Int) {
        val dataSources = listOf(
            sequence {
                repeat(dataPointCount) { i ->
                    yield(TestDP(timestamp = (i + 1) * 1000L, value = 100.0, label = "base"))
                }
            }
        )
        
        val rawDataSources = dataSources.map { source ->
            val asDataPoints = source.map { it.toDataPoint() }
            // Use the same rawDataSampleFromSequence method from the base class
            object : RawDataSample() {
                private val visited = mutableListOf<DataPoint>()
                override fun getRawDataPoints() = visited
                override fun iterator() = asDataPoints.onEach { visited.add(it) }.iterator()
                override fun dispose() {}
            }
        }

        val script = """
            return function(data_sources)
                local source = data_sources[1]
                local counter = 0  -- Local variable that persists across yields
                
                local data_point = source.dp()
                while data_point do
                    counter = counter + 1
                    -- Mutate the input data point directly
                    data_point.value = counter  -- Use the incrementing counter
                    data_point.label = "count_" .. counter
                    coroutine.yield(data_point)
                    data_point = source.dp()
                end
            end
        """.trimIndent()

        val result = luaEngine.runLuaFunctionGenerator(script, rawDataSources, emptyList())
        val resultList = result.toList()

        assertEquals(dataPointCount, resultList.size)
        // Verify each counter value is correct
        for (i in 0 until dataPointCount) {
            assertEquals((i + 1).toDouble(), resultList[i].value, 0.001)
            assertEquals("count_${i + 1}", resultList[i].label)
        }
    }

    @Test
    fun `Concurrent execution with shared Lua globals is thread-safe`() {
        val threadCount = 50
        val dataPointsPerThread = 500
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val exceptions = mutableListOf<Exception>()
        
        // Create a single shared Lua engine instance for all threads
        val sharedLuaEngine = uut()

        repeat(threadCount) {
            executor.submit {
                try {
                    // Each thread runs the stateful counter test with the shared engine
                    testStatefulCounterWithSharedEngine(sharedLuaEngine, dataPointsPerThread)
                } catch (e: Exception) {
                    synchronized(exceptions) {
                        exceptions.add(e)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        // Wait for all threads to complete (with timeout)
        assertTrue("Concurrent test timed out", latch.await(3, TimeUnit.SECONDS))

        executor.shutdown()
        assertTrue("Executor failed to shutdown", executor.awaitTermination(1, TimeUnit.SECONDS))

        // Check that no exceptions occurred
        if (exceptions.isNotEmpty()) {
            throw AssertionError("Concurrent execution failed with ${exceptions.size} exceptions. First: ${exceptions.first()}")
        }
    }

    @Test
    fun `Generator function uses configuration parameters correctly`() = testLuaFunction(
        dataSources = listOf(
            sequenceOf(
                TestDP(timestamp = 1000, value = 10.0, label = "first"),
                TestDP(timestamp = 2000, value = 20.0, label = "second"),
                TestDP(timestamp = 3000, value = 30.0, label = "third")
            )
        ),
        script = """
            return function(data_sources, config)
                local source = data_sources[1]
                local multiplier = config.multiplier
                local prefix = config.prefix
                local offset = config.offset
                
                local data_point = source.dp()
                while data_point do
                    -- Use configuration to transform the data point
                    data_point.value = (data_point.value * multiplier) + offset
                    data_point.label = prefix .. data_point.label
                    coroutine.yield(data_point)
                    data_point = source.dp()
                end
            end
        """.trimIndent(),
        config = listOf(
            LuaScriptConfigurationValue.Number(id = "multiplier", value = 2.5),
            LuaScriptConfigurationValue.Text(id = "prefix", value = "processed_"),
            LuaScriptConfigurationValue.Number(id = "offset", value = 100.0)
        )
    ) {
        assertEquals("Should have 3 results", 3, resultList.size)
        
        // Verify first data point: (10.0 * 2.5) + 100.0 = 125.0
        assertEquals("First value should be transformed", 125.0, resultList[0].value, 0.001)
        assertEquals("First label should be prefixed", "processed_first", resultList[0].label)
        
        // Verify second data point: (20.0 * 2.5) + 100.0 = 150.0
        assertEquals("Second value should be transformed", 150.0, resultList[1].value, 0.001)
        assertEquals("Second label should be prefixed", "processed_second", resultList[1].label)
        
        // Verify third data point: (30.0 * 2.5) + 100.0 = 175.0
        assertEquals("Third value should be transformed", 175.0, resultList[2].value, 0.001)
        assertEquals("Third label should be prefixed", "processed_third", resultList[2].label)
    }
}

