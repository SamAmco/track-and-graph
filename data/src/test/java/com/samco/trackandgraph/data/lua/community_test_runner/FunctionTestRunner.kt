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
package com.samco.trackandgraph.data.lua.community_test_runner

import com.samco.trackandgraph.data.lua.VMLease
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

@RunWith(Parameterized::class)
internal class FunctionTestRunner : CommunityTestRunner() {

    @Parameterized.Parameter(0)
    lateinit var testName: String

    @Parameterized.Parameter(1)
    lateinit var scriptLuaText: String

    @Parameterized.Parameter(2)
    lateinit var testLuaText: String

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testData(): List<Array<Any>> {
            return crawlTestDirectory("generated/lua-community/functions")
        }
    }

    @Test
    fun `run community function test`() {
        val vmProvider = daggerComponent.provideVMProvider()
        val vmLease = runBlocking { vmProvider.acquire() }
        try {
            val test = vmLease.globals.load(testLuaText)
            val testSet = test.call().checktable()!!
            for (key in testSet.keys()) {
                runFunctionTest(
                    key = key,
                    testStructure = testSet[key].checktable()!!,
                    vmLease = vmLease
                )
            }
        } catch (t: Throwable) {
            println("Failed to run $testName : ${t.message}")
            t.printStackTrace()
            throw t
        } finally {
            vmProvider.release(vmLease)
        }
    }

    private fun runFunctionTest(key: LuaValue, testStructure: LuaTable, vmLease: VMLease) {
        try {
            val testConfig = testStructure["config"].checktable()!!
            val testDataSources = testStructure["sources"].checkfunction()!!.call().checktable()!!
            val resolvedScript = daggerComponent.provideLuaScriptResolver()
                .resolveLuaScript(scriptLuaText, vmLease)
                .checktable()!!

            // For functions, we execute the script directly and call the generator
            val functionResult = executeFunctionScript(
                resolvedScript = resolvedScript,
                dataSources = testDataSources,
                config = testConfig,
            )
            testStructure["assertions"].checkfunction()!!.call(functionResult)
            println("Test passed: $testName.$key")
        } catch (t: Throwable) {
            println("Test failed $testName.$key: ${t.message}")
            t.printStackTrace()
            throw t
        }
    }

    private fun executeFunctionScript(
        resolvedScript: LuaTable,
        dataSources: LuaTable,
        config: LuaTable,
    ): LuaTable {
        // Get the generator function from the table
        val generator = resolvedScript["generator"].checkfunction()!!

        // Convert data sources to the format expected by functions based on inputCount
        val luaDataSources = createLuaDataSourcesList(resolvedScript, dataSources)

        // Call the generator with the data sources and config to get the iterator function
        val iteratorFunc = generator.call(luaDataSources, config)

        // Collect all results from the iterator
        val resultsTable = LuaTable()
        var index = 1

        while (true) {
            val dataPoint = iteratorFunc.call()
            if (dataPoint.isnil()) break

            resultsTable[index++] = dataPoint
        }

        return resultsTable
    }

    private fun createLuaDataSourcesList(functionDef: LuaTable, dataSources: LuaTable): LuaValue {
        // Check inputCount to determine how to pass data sources
        val inputCount = functionDef["inputCount"]
        val expectedInputs = if (inputCount.isnil()) 1 else inputCount.toint()

        if (expectedInputs == 1) {
            // Single data source - return it directly (not in a table)
            val sourceData = dataSources[1].checktable()!!
            val dataPoints = sourceData.keys()
                .map { dataPointParser.parseDataPoint(sourceData[it].checktable()!!) }
                .iterator()
            return luaDataSourceProvider.createLuaDataSource(dataPoints)
        } else {
            // Multiple data sources - return a Lua table with 1-based indexing
            val luaTable = LuaTable()
            for (i in 1..expectedInputs) {
                val sourceData = dataSources[i].checktable()
                if (sourceData != null) {
                    val dataPoints = sourceData.keys()
                        .map { dataPointParser.parseDataPoint(sourceData[it].checktable()!!) }
                        .iterator()
                    luaTable[i] = luaDataSourceProvider.createLuaDataSource(dataPoints)
                }
            }
            return luaTable
        }
    }

}
