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
package com.samco.trackandgraph.data.lua.functionadapters

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.lua.apiimpl.ConfigurationValueParser
import com.samco.trackandgraph.data.lua.apiimpl.DataPointParser
import com.samco.trackandgraph.data.lua.apiimpl.LuaDataSourceProviderImpl
import com.samco.trackandgraph.data.sampling.RawDataSample
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import timber.log.Timber
import javax.inject.Inject

/**
 * Adapter responsible for executing Lua function scripts and returning Sequence<DataPoint> results.
 * This adapter handles the data processing aspect of Lua functions, finding the generator function
 * that returns an iterator function and wrapping it in a Sequence.
 *
 * The Lua function should return an iterator function (closure) that can be called repeatedly
 * to get the next data point, returning nil when no more data is available.
 *
 * Note: This is separate from the future metadata adapter which will handle parsing
 * configurable parameters, input connector count, name, description etc.
 */
internal class LuaFunctionDataSourceAdapter @Inject constructor(
    private val luaDataSourceProvider: LuaDataSourceProviderImpl,
    private val dataPointParser: DataPointParser,
    private val configurationValueParser: ConfigurationValueParser,
) {

    /**
     * Creates a Sequence from a resolved Lua script with the provided data sources.
     *
     * @param resolvedScript The resolved LuaValue from executing the Lua script
     * @param dataSources List of input data sources to pass to the Lua function
     * @return Sequence<DataPoint> wrapping the Lua iterator function result
     * @throws Exception if script execution fails
     */
    fun createDataPointSequence(
        resolvedScript: LuaValue,
        dataSources: List<RawDataSample>,
        configuration: List<LuaScriptConfigurationValue>,
    ): Sequence<DataPoint> {
        val generatorFunction = findGeneratorFunction(resolvedScript)
        val config = configurationValueParser.parseConfigurationValues(configuration)

        // Return a Sequence that lazily processes the iterator function
        return createLuaGeneratorSequence(
            generatorFunction = generatorFunction,
            dataSources = dataSources,
            config = config
        )
    }

    private fun createLuaDataSourcesList(dataSources: List<RawDataSample>): LuaValue {
        if (dataSources.size == 1) {
            return luaDataSourceProvider.createLuaDataSource(dataSources[0].iterator())
        }
        val luaTable = LuaTable()
        dataSources.forEachIndexed { index, rawDataSample ->
            // Create a Lua data source for each RawDataSample
            luaTable[index + 1] = luaDataSourceProvider
                .createLuaDataSource(rawDataSample.iterator())
        }
        return luaTable
    }

    private fun createLuaGeneratorSequence(
        generatorFunction: LuaFunction,
        dataSources: List<RawDataSample>,
        config: LuaTable,
    ): Sequence<DataPoint> {
        return Sequence {
            object : Iterator<DataPoint> {
                private var nextDataPoint: DataPoint? = null
                private val iteratorFunction: LuaFunction

                init {
                    try {
                        // Convert List<RawDataSample> to Lua-compatible format
                        val luaDataSources = createLuaDataSourcesList(dataSources)
                        // Call the generator function to get the iterator function
                        val result = generatorFunction.call(luaDataSources, config)
                        if (!result.isfunction()) {
                            throw RuntimeException("Lua generator function must return an iterator function")
                        }
                        iteratorFunction = result.checkfunction()!!

                        // Load the first data point
                        loadNextDataPoint()
                    } catch (e: Throwable) {
                        Timber.e(e)
                        throw e
                    }
                }

                override fun hasNext(): Boolean {
                    return nextDataPoint != null
                }

                override fun next(): DataPoint {
                    return try {
                        val current = nextDataPoint
                            ?: throw NoSuchElementException("No more elements in Lua iterator")

                        // Load the next data point for the next call
                        loadNextDataPoint()
                        current
                    } catch (e: Throwable) {
                        Timber.e(e)
                        throw e
                    }
                }

                private fun loadNextDataPoint() {
                    val result = iteratorFunction.call()
                    nextDataPoint =
                        if (result.isnil()) null
                        else dataPointParser.parseDataPoint(result)
                }
            }
        }
    }

    private fun findGeneratorFunction(resolvedScript: LuaValue): LuaFunction {
        return when {
            resolvedScript.isfunction() -> {
                // Direct function - this is our generator
                resolvedScript.checkfunction()!!
            }

            resolvedScript.istable() -> {
                // Table - look for "generator" key
                val generatorValue = resolvedScript["generator"]
                if (generatorValue.isnil()) {
                    throw IllegalArgumentException("Lua script returned a table but no 'generator' key found")
                }
                if (!generatorValue.isfunction()) {
                    throw IllegalArgumentException("Lua script 'generator' key is not a function")
                }
                generatorValue.checkfunction()!!
            }

            else -> {
                throw IllegalArgumentException("Lua script must return either a function or a table with a 'generator' key")
            }
        }
    }
}
