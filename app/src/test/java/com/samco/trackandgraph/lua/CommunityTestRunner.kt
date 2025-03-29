package com.samco.trackandgraph.lua

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.samco.trackandgraph.assetreader.AssetReader
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.lua.apiimpl.oneArgFunction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.mockito.ArgumentMatchers
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
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
        whenever(assetReader.readAssetToString(ArgumentMatchers.anyString())).thenAnswer {
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
            return@oneArgFunction LuaValue.Companion.NIL
        }
    }

    @Parameter(0)
    lateinit var testName: String

    @Parameter(1)
    lateinit var scriptLuaText: String

    @Parameter(2)
    lateinit var testLuaText: String

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testData(): List<Array<Any>> {
            val scriptPath = "generated/lua-community"
            val classLoader = CommunityTestRunner::class.java.classLoader ?: return emptyList()
            val baseUrl = classLoader.getResource(scriptPath) ?: return emptyList()
            val basePath = File(baseUrl.toURI())

            // Find all test files paired with their script files
            val testCases = mutableListOf<Array<Any>>()

            // Use a stack for iterative traversal
            val directoryStack = ArrayDeque<File>()
            directoryStack.add(basePath)

            while (directoryStack.isNotEmpty()) {
                val currentDir = directoryStack.removeLast()
                val files = currentDir.listFiles() ?: continue
                files.filter { it.isDirectory }.forEach { directoryStack.add(it) }

                // Find script.lua in the current directory
                val scriptFile = files.find { it.name == "script.lua" }

                // If script.lua exists, find and process all test files
                if (scriptFile != null) {
                    val scriptLuaText = scriptFile.readText()

                    // Add all test files in the same directory
                    files.filter { it.isFile && it.name.startsWith("test") && it.name.endsWith(".lua") }
                        .forEach { testFile ->
                            val testName = currentDir.relativeTo(basePath).path + "/" + testFile.name
                            testCases.add(arrayOf(testName, scriptLuaText, testFile.readText()))
                        }
                }
            }

            return testCases
        }
    }

    @Test
    fun `run community lua test`() {
        try {
            val globals = daggerComponent.provideGlobalsProvider().globals.value
            val test = globals.load(testLuaText)
            val testSet = test.call().checktable()!!
            for (key in testSet.keys()) {
                try {
                    val testStructure = testSet[key].checktable()!!
                    val testConfig = testStructure["config"].checktable()!!
                    val overriddenScript = overrideScriptConfig(scriptLuaText, testConfig)
                    val testDataSources = testStructure["sources"].checkfunction()!!.call()
                    val scriptResult = daggerComponent.provideLuaScriptResolver()
                        .resolveLuaGraphScriptResult(
                            script = overriddenScript,
                            dataSources = testDataSources.addDataSourceFunctions(),
                        )
                    testStructure["assertions"].checkfunction()!!.call(scriptResult)
                    println("Test passed: $testName.$key")
                } catch (t: Throwable) {
                    println("Test failed $testName.$key: ${t.message}")
                    t.printStackTrace()
                    throw t
                }
            }
        } catch (t: Throwable) {
            println("Failed to run $testName : ${t.message}")
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
