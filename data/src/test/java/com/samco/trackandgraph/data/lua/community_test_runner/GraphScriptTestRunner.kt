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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

@RunWith(Parameterized::class)
internal class GraphScriptTestRunner : CommunityTestRunner() {

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
            return crawlTestDirectory("generated/lua-community/graphs")
        }
    }

    @Test
    fun `run community lua test`() {
        val vmProvider = daggerComponent.provideVMProvider()
        val vmLease = acquireTestVMLease(vmProvider)
        try {
            val test = vmLease.globals.load(testLuaText)
            val testSet = test.call().checktable()!!
            for (key in testSet.keys()) {
                runTest(
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

    private fun runTest(key: LuaValue, testStructure: LuaTable, vmLease: VMLease) {
        try {
            val testConfig = testStructure["config"].checktable()!!
            val overriddenScript = overrideScriptConfig(scriptLuaText, testConfig)
            val testDataSources = testStructure["sources"].checkfunction()!!.call()
            val resolvedScript = daggerComponent.provideLuaScriptResolver()
                .resolveLuaScript(overriddenScript, vmLease)

            // Use LuaGraphAdapter to execute the script (reuses the same logic)
            val scriptResult = daggerComponent.provideLuaGraphAdapter()
                .executeScript(resolvedScript, testDataSources.addDataSourceFunctions())
            testStructure["assertions"].checkfunction()!!.call(scriptResult)
            println("Test passed: $testName.$key")
        } catch (t: Throwable) {
            println("Test failed $testName.$key: ${t.message}")
            t.printStackTrace()
            throw t
        }
    }

    private fun LuaValue.addDataSourceFunctions(): LuaTable {
        val luaTable = this.checktable()!!
        var index = 0
        for (key in luaTable.keys()) {
            val dataPoints = luaTable[key].checktable()!!
            val iterator = dataPoints.keys()
                .map { dataPointParser.parseDataPoint(dataPoints[it].checktable()!!) }
                .iterator()
            luaTable[key] = luaDataSourceProvider
                .createLuaDataSource(index++, key.toString(), iterator)
        }
        return luaTable
    }

    private fun overrideScriptConfig(
        script: String,
        config: LuaTable,
    ): String {
        // For each key value pair in the config map, find the first line matching the pattern
        // key = something and replace it with key = value
        // if the pattern is not found throw an error
        return config.keys().fold(script) { acc, key ->
            val value = config[key].checkjstring()
            val pattern = Regex("local $key =.*")
            val match = pattern.find(acc) ?: error("Pattern $pattern not found in script")
            acc.replaceRange(match.range, "local $key = $value")
        }
    }
}
