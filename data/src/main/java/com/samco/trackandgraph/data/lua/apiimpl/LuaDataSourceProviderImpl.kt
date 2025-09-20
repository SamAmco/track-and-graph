package com.samco.trackandgraph.data.lua.apiimpl

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.sampling.RawDataSample
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.Companion.NIL
import javax.inject.Inject

internal class LuaDataSourceProviderImpl @Inject constructor(
    private val dateTimeParser: DateTimeParser,
    private val dataPointParser: DataPointParser,
) {

    companion object {
        const val NAME = "name"
        const val DP = "dp"
        const val DP_BATCH = "dpbatch"
        const val DP_ALL = "dpall"
        const val DP_AFTER = "dpafter"
        const val INDEX = "index"
    }

    fun createDataSourceTable(dataSources: Map<String, RawDataSample>): LuaValue {
        val dataSourceTable = LuaTable()
        dataSources.entries.forEachIndexed { index, entry ->
            dataSourceTable[entry.key] = createLuaDataSource(index, entry.key, entry.value.iterator())
        }
        return dataSourceTable
    }

    fun createLuaDataSource(index: Int, name: String, iterator: Iterator<DataPoint>): LuaValue {
        val luaTable = LuaTable()
        luaTable[NAME] = name
        luaTable[DP] = getDpLuaFunction(iterator)
        luaTable[DP_BATCH] = getDpBatchLuaFunction(iterator)
        luaTable[DP_ALL] = getDpAllLuaFunction(iterator)
        luaTable[DP_AFTER] = getDpAfterLuaFunction(iterator)
        luaTable[INDEX] = index + 1 // Lua is 1 indexed
        return luaTable
    }

    private fun getDpAfterLuaFunction(dataSource: Iterator<DataPoint>) =
        oneArgFunction { arg1 ->
            val zonedDateTime = dateTimeParser.parseDateTimeOrNull(arg1)
            val batch = mutableListOf<LuaValue>()
            if (zonedDateTime == null) {
                while (dataSource.hasNext()) {
                    batch.add(dataPointParser.toLuaValueNullable(dataSource.next()))
                }
            } else {
                while (dataSource.hasNext()) {
                    val dataPoint = dataSource.next()
                    if (dataPoint.timestamp <= zonedDateTime.toOffsetDateTime()) break
                    batch.add(dataPointParser.toLuaValueNullable(dataPoint))
                }
            }
            return@oneArgFunction LuaValue.listOf(batch.toTypedArray())
        }

    private fun getDpAllLuaFunction(dataSource: Iterator<DataPoint>): LuaValue =
        zeroArgFunction {
            val batch = mutableListOf<LuaValue>()
            while (dataSource.hasNext()) {
                batch.add(dataPointParser.toLuaValueNullable(dataSource.next()))
            }
            return@zeroArgFunction LuaValue.listOf(batch.toTypedArray())
        }

    private fun getDpBatchLuaFunction(dataSource: Iterator<DataPoint>): LuaValue =
        oneArgFunction { arg1 ->
            val count = arg1.optint(1)
            val batch = mutableListOf<LuaValue>()
            while (dataSource.hasNext() && batch.size < count) {
                batch.add(dataPointParser.toLuaValueNullable(dataSource.next()))
            }
            return@oneArgFunction LuaValue.listOf(batch.toTypedArray())
        }

    private fun getDpLuaFunction(dataSource: Iterator<DataPoint>): LuaValue =
        zeroArgFunction {
            if (!dataSource.hasNext()) return@zeroArgFunction NIL
            return@zeroArgFunction dataPointParser.toLuaValueNullable(dataSource.next())
        }

}
