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
package com.samco.trackandgraph.lua.community

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.samco.trackandgraph.assetreader.AssetReader
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.lua.DaggerLuaEngineTestComponent
import com.samco.trackandgraph.lua.apiimpl.oneArgFunction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.Companion.NIL
import org.mockito.ArgumentMatchers.anyString
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityTestRunner {

    private fun readAssetToString(path: String): String? {
        return javaClass.getClassLoader()
            ?.getResourceAsStream(path)
            .use { it?.bufferedReader()?.readText() }
    }

    private val dataInteractor: DataInteractor = mock()
    private val assetReader: AssetReader = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private val daggerComponent by lazy {
        whenever(assetReader.readAssetToString(anyString())).thenAnswer {
            val path = it.getArgument<String>(0)
            readAssetToString(path)
        }

        DaggerLuaEngineTestComponent.builder()
            .dataInteractor(dataInteractor)
            .assetReader(assetReader)
            .ioDispatcher(ioDispatcher)
            .build()
    }

    private val dataPointParser by lazy {
        daggerComponent.provideDataPointParser()
    }

    private val luaDataSourceProvider by lazy {
        daggerComponent.provideLuaDataSourceProvider()
    }

    @Before
    fun setup() {
        val globals = daggerComponent.provideGlobalsProvider().globals.value
        globals["print"] = oneArgFunction {
            println(it.checkjstring())
            return@oneArgFunction NIL
        }
    }

    private var testFilter: String? = null//"line-graphs/difference"

    @Test
    fun `run all community lua tests`() {
        val scriptPath = "generated/lua-community/"
        val classLoader = javaClass.classLoader ?: return
        val baseUrl = classLoader.getResource(scriptPath) ?: return
        val basePath = File(baseUrl.toURI())

        // Use a stack for iterative traversal
        val directoryStack = ArrayDeque<File>()
        directoryStack.add(basePath)

        while (directoryStack.isNotEmpty()) {
            val currentDir = directoryStack.removeLast()
            val files = currentDir.listFiles() ?: continue
            files.filter { it.isDirectory }.forEach { directoryStack.add(it) }

            if (testFilter != null && !currentDir.path.contains(testFilter!!)) continue

            // Find script.lua in the current directory
            val scriptFile = files.find { it.name == "script.lua" }

            // If script.lua exists, find and process all test files
            if (scriptFile != null) {
                val scriptLua = scriptFile.readText()

                // Process all test files in the same directory
                files.filter { it.isFile && it.name.startsWith("test") && it.name.endsWith(".lua") }
                    .forEach { testFile ->
                        runTest(
                            testFileName = currentDir.relativeTo(basePath).path + "/" + testFile.name,
                            script = scriptLua,
                            testFile = testFile,
                        )
                    }
            }
        }
    }

    private fun runTest(testFileName: String, script: String, testFile: File) {
        try {
            runTest(testFileName, script, testFile.readText())
        } catch (t: Throwable) {
            println("Failed to run $testFileName : ${t.message}")
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

    private fun runTest(testFileName: String, scriptLua: String, testLua: String) {
        val globals = daggerComponent.provideGlobalsProvider().globals.value
        val test = globals.load(testLua)
        val testSet = test.call().checktable()!!
        for (key in testSet.keys()) {
            try {
                val testStructure = testSet[key].checktable()!!
                val testConfig = testStructure["config"].checktable()!!
                val overriddenScript = overrideScriptConfig(scriptLua, testConfig)
                val testDataSources = testStructure["sources"].checkfunction()!!.call()
                val scriptResult = daggerComponent.provideLuaScriptResolver()
                    .resolveLuaGraphScriptResult(
                        script = overriddenScript,
                        dataSources = testDataSources.addDataSourceFunctions(),
                    )
                testStructure["assertions"].checkfunction()!!.call(scriptResult)
                println("Test passed: $testFileName.$key")
            } catch (t: Throwable) {
                println("Test failed $testFileName.$key: ${t.message}")
                t.printStackTrace()
                throw t
            }
        }
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
