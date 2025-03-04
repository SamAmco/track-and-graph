package com.samco.trackandgraph.lua.apiimpl

import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.sampling.RawDataSample
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.Companion.NIL
import javax.inject.Inject

class LuaDataSourceProviderImpl @Inject constructor(
    private val dateTimeParser: DateTimeParser,
    private val dataPointParser: DataPointParser,
) {

    companion object {
        const val NAME = "name"
        const val DP = "dp"
        const val DP_BATCH = "dpbatch"
        const val DP_ALL = "dpall"
        const val DP_AFTER = "dpafter"
    }

    fun createDataSourceTable(dataSources: Map<String, RawDataSample>): LuaValue {
        val dataSourceTable = LuaTable()
        dataSources.forEach { (key, value) ->
            dataSourceTable[key] = createLuaDataSource(key, value)
        }
        return dataSourceTable
    }

    private fun createLuaDataSource(name: String, dataSample: RawDataSample): LuaValue {
        val luaTable = LuaTable()
        val iterator = dataSample.iterator()
        luaTable[NAME] = name
        luaTable[DP] = getDpLuaFunction(iterator)
        luaTable[DP_BATCH] = getDpBatchLuaFunction(iterator)
        luaTable[DP_ALL] = getDpAllLuaFunction(iterator)
        luaTable[DP_AFTER] = getDpAfterLuaFunction(iterator)
        return luaTable
    }

    private fun getDpAfterLuaFunction(dataSource: Iterator<DataPoint>) = oneArgFunction { arg1 ->
        val zonedDateTime = dateTimeParser.parseDateTimeOrNow(arg1)
        val batch = mutableListOf<LuaValue>()
        while (dataSource.hasNext()) {
            val dataPoint = dataSource.next()
            if (dataPoint.timestamp <= zonedDateTime.toOffsetDateTime()) break
            batch.add(dataPointParser.toLuaValueNullable(dataPoint))
        }
        return@oneArgFunction LuaValue.listOf(batch.toTypedArray())
    }

    private fun getDpAllLuaFunction(dataSource: Iterator<DataPoint>): LuaValue = zeroArgFunction {
        val batch = mutableListOf<LuaValue>()
        while (dataSource.hasNext()) {
            batch.add(dataPointParser.toLuaValueNullable(dataSource.next()))
        }
        return@zeroArgFunction LuaValue.listOf(batch.toTypedArray())
    }

    private fun getDpBatchLuaFunction(dataSource: Iterator<DataPoint>): LuaValue = oneArgFunction { arg1 ->
        val count = arg1.optint(1)
        val batch = mutableListOf<LuaValue>()
        while (dataSource.hasNext() && batch.size < count) {
            batch.add(dataPointParser.toLuaValueNullable(dataSource.next()))
        }
        return@oneArgFunction LuaValue.listOf(batch.toTypedArray())
    }

    private fun getDpLuaFunction(dataSource: Iterator<DataPoint>): LuaValue = zeroArgFunction {
        if (!dataSource.hasNext()) return@zeroArgFunction NIL
        return@zeroArgFunction dataPointParser.toLuaValueNullable(dataSource.next())
    }

}
