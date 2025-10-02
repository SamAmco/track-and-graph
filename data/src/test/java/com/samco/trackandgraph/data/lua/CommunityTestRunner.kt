package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.assetreader.AssetReader
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.time.TimeProviderImpl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.concurrent.withLock

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
            .timeProvider(TimeProviderImpl())
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
                            val testName =
                                currentDir.relativeTo(basePath).path + "/" + testFile.name
                            testCases.add(arrayOf(testName, scriptLuaText, testFile.readText()))
                        }
                }
            }

            return testCases
        }
    }

    @Test
    fun `run community lua test`() {
        val vmProvider = daggerComponent.provideVMProvider()
        val vmLease = runBlocking { vmProvider.acquire() }
        try {
            val test = vmLease.globals.load(testLuaText)
            val testSet = test.call().checktable()!!
            for (key in testSet.keys()) {
                try {
                    val testStructure = testSet[key].checktable()!!
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
        } catch (t: Throwable) {
            println("Failed to run $testName : ${t.message}")
            t.printStackTrace()
            throw t
        } finally {
            vmProvider.release(vmLease)
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
