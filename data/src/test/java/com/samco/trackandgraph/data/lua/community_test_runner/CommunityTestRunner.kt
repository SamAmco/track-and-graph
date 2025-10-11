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

import com.samco.trackandgraph.data.assetreader.AssetReader
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.lua.DaggerLuaEngineTestComponent
import com.samco.trackandgraph.data.time.TimeProviderImpl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class CommunityTestRunner {

    private fun readAssetToString(path: String): String? {
        return javaClass.getClassLoader()
            ?.getResourceAsStream(path)
            .use { it?.bufferedReader()?.readText() }
    }

    protected val dataInteractor: DataInteractor = mock()
    protected val assetReader: AssetReader = mock()
    protected val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    protected val daggerComponent by lazy {
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

    protected val dataPointParser by lazy {
        daggerComponent.provideDataPointParser()
    }

    protected val luaDataSourceProvider by lazy {
        daggerComponent.provideLuaDataSourceProvider()
    }

    companion object {
        /**
         * Crawls a directory tree to find Lua script files and their corresponding test files.
         * @param scriptPath The resource path to crawl (e.g., "generated/lua-community")
         * @return List of test case arrays containing [testName, scriptLuaText, testLuaText]
         */
        fun crawlTestDirectory(scriptPath: String): List<Array<Any>> {
            val classLoader = this::class.java.classLoader ?: return emptyList()
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
                files
                    .filter { it.isDirectory }
                    .forEach { directoryStack.add(it) }

                // Find the main script file (any .lua file that's not a test file)
                val scriptFile = files.find {
                    it.isFile
                            && it.name.endsWith(".lua")
                            && !it.name.startsWith("test_")
                }

                // If a script file exists, find and process all test files
                if (scriptFile != null) {
                    val scriptLuaText = scriptFile.readText()

                    // Add all test files in the same directory
                    files.filter { it.isFile && it.name.startsWith("test_") && it.name.endsWith(".lua") }
                        .forEach { testFile ->
                            val testName =
                                currentDir.relativeTo(basePath).path + File.pathSeparator + testFile.name
                            testCases.add(arrayOf(testName, scriptLuaText, testFile.readText()))
                        }
                }
            }

            return testCases
        }
    }
}