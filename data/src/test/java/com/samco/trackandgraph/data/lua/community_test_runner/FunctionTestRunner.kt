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

import com.samco.trackandgraph.data.lua.apiimpl.ModuleLoadInterceptor
import io.github.z4kn4fein.semver.toVersion
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import java.io.File

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

        private fun loadLuaFile(globals: Globals, path: String): LuaValue {
            val classLoader = FunctionTestRunner::class.java.classLoader!!
            val scriptText = File(classLoader.getResource(path).toURI()).readText()
            return globals.load(scriptText).call()
        }
    }

    override fun getModuleLoadInterceptor(): ModuleLoadInterceptor {
        return object : ModuleLoadInterceptor {
            override fun onModuleLoad(
                globals: Globals,
                moduleName: String,
                module: LuaTable
            ): LuaTable {
                if (!moduleName.startsWith("tng.")) return module

                val gateModule = loadLuaFile(
                    globals = globals,
                    path = "generated/lua-community/test-utils/api-gate.lua"
                )

                val moduleFileName = moduleName.removePrefix("tng.")
                val apiSpecModule = loadLuaFile(
                    globals = globals,
                    path = "generated/lua-api/$moduleFileName.apispec.lua"
                )

                return gateModule["gate"].checkfunction()!!
                    .call(module, apiSpecModule)
                    .checktable()!!
            }
        }
    }

    @Test
    fun `run community function test`() {
        val vmProvider = daggerComponent.provideVMProvider()
        val vmLease = acquireTestVMLease(vmProvider)
        try {
            // Reset API usage tracking
            val gateModule = loadLuaFile(vmLease.globals, "generated/lua-community/test-utils/api-gate.lua")
            gateModule["reset"].checkfunction()!!.call()

            // Read the script to get the version
            val resolvedScript = daggerComponent.provideLuaScriptResolver()
                .resolveLuaScript(scriptLuaText, vmLease)
                .checktable()!!

            val version = resolvedScript["version"].checkjstring()!!.toVersion().major

            // Run all tests
            val test = vmLease.globals.load(testLuaText)
            val testSet = test.call().checktable()!!
            for (key in testSet.keys()) {
                runFunctionTest(
                    resolvedScript = resolvedScript,
                    key = key,
                    testStructure = testSet[key].checktable()!!,
                )
            }

            // Validate API usage at the end
            gateModule["validate"].checkfunction()!!
                .call(LuaValue.valueOf(version))

            // Clear usage table
            gateModule["reset"].checkfunction()!!.call()
        } catch (t: Throwable) {
            println("Failed to run $testName : ${t.message}")
            t.printStackTrace()
            throw t
        } finally {
            vmProvider.release(vmLease)
        }
    }

    private fun runFunctionTest(
        resolvedScript: LuaTable,
        key: LuaValue,
        testStructure: LuaTable,
    ) {
        try {
            val testConfig = testStructure["config"].takeIf { !it.isnil() }?.checktable()
            val testDataSources = testStructure["sources"].checkfunction()!!.call().checktable()!!

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
        config: LuaTable?,
    ): LuaTable {
        // Get the generator function from the table
        val generator = resolvedScript["generator"].checkfunction()!!

        // Convert data sources to the format expected by functions based on inputCount
        val luaDataSources = createLuaDataSourcesList(resolvedScript, dataSources)

        // Call the generator with the data sources and config to get the iterator function
        val iteratorFunc = if (config == null) {
            generator.call(luaDataSources)
        } else {
            generator.call(luaDataSources, config)
        }

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
