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

import com.samco.trackandgraph.data.assetreader.AssetReader
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.lua.apiimpl.NoOpModuleLoadInterceptorImpl
import com.samco.trackandgraph.data.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ApiLevelCalculatorTest {

    // Hard-coded apispec files with varying API levels and different key definition styles
    private val coreApiSpec = """
        -- API specification for core.lua module
        -- Maps each exported symbol to the API level where it was introduced
        return {
            time = 2,
            PERIOD = 1,
            format_duration = 3,
            data_point = 1,
            calculate_average = 4
        }
    """.trimIndent()

    private val graphApiSpec = """
        -- API specification for graph.lua module
        return {
            GRAPH_TYPE = 1,
            text = 2,
            line_graph = 5,
            bar_chart = 3,
            scatter_plot = 6
        }
    """.trimIndent()

    private val extensionsApiSpec = """
        -- API specification for extensions.lua module
        return {
            ["custom_function"] = 7,
            ["advanced_filter.nested"] = 8,
            ["export_data"] = 2,
            ["import_config"] = 4
        }
    """.trimIndent()

    private val assetReader = mock<AssetReader>()
    private val apiLevelCalculator = ApiLevelCalculator(assetReader)

    private suspend fun createTestComponent(): LuaEngineTestComponent {
        val dataInteractor = mock<DataInteractor>()
        val timeProvider = mock<TimeProvider>()

        // Mock the findFilesWithSuffix method to return our test files
        whenever(assetReader.findFilesWithSuffix("generated/lua-api", ".apispec.lua"))
            .thenReturn(
                listOf(
                    "generated/lua-api/core.apispec.lua",
                    "generated/lua-api/graph.apispec.lua",
                    "generated/lua-api/extensions.apispec.lua"
                )
            )

        // Mock the readAssetToString method to return our hard-coded content
        whenever(assetReader.readAssetToString("generated/lua-api/core.apispec.lua"))
            .thenReturn(coreApiSpec)
        whenever(assetReader.readAssetToString("generated/lua-api/graph.apispec.lua"))
            .thenReturn(graphApiSpec)
        whenever(assetReader.readAssetToString("generated/lua-api/extensions.apispec.lua"))
            .thenReturn(extensionsApiSpec)

        return DaggerLuaEngineTestComponent.builder()
            .dataInteractor(dataInteractor)
            .assetReader(assetReader)
            .ioDispatcher(Dispatchers.IO)
            .timeProvider(timeProvider)
            .moduleLoadInterceptor(NoOpModuleLoadInterceptorImpl())
            .apiLevelCalculator(apiLevelCalculator)
            .build()
    }

    @Test
    fun `calculates maximum API level across multiple apispec files with varying levels`() =
        runTest {
            val component = createTestComponent()
            val vmProvider = component.provideVMProvider()

            // Acquire a VM lease to use for parsing
            val vmLease = vmProvider.acquire()

            try {
                // Call getMaxApiLevel - should return 8 (highest level from extensions.apispec.lua)
                val maxApiLevel = apiLevelCalculator.getMaxApiLevel(vmLease)

                // Verify the maximum API level is correctly calculated
                // Expected: 8 from advanced_filter in extensions.apispec.lua
                assertEquals(8, maxApiLevel)

            } catch (t: Throwable) {
                error("Test threw an exception")
            } finally {
                vmProvider.release(vmLease)
            }
        }
}
