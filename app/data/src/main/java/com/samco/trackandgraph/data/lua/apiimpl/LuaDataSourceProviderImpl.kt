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

    private class PeekableIterator<T>(private val iterator: Iterator<T>): Iterator<T> {
        private var peeked: T? = null

        override fun hasNext(): Boolean {
            return peeked != null || iterator.hasNext()
        }

        override fun next(): T {
            val stored = peeked
            peeked = null
            return stored ?: iterator.next()
        }

        fun peek(): T {
            return peeked ?: iterator.next().also { peeked = it }
        }
    }

    fun createDataSourceTable(dataSources: Map<String, RawDataSample>): LuaValue {
        val dataSourceTable = LuaTable()
        dataSources.entries.forEachIndexed { index, entry ->
            val iterator = PeekableIterator(entry.value.iterator())
            dataSourceTable[entry.key] = createLuaDataSource(index, entry.key, iterator)
        }
        return dataSourceTable
    }

    fun createLuaDataSource(index: Int, name: String, iterator: Iterator<DataPoint>): LuaValue {
        val wrappedIterator = iterator as? PeekableIterator ?: PeekableIterator(iterator)
        val luaTable = LuaTable()
        luaTable[NAME] = name
        luaTable[DP] = getDpLuaFunction(wrappedIterator)
        luaTable[DP_BATCH] = getDpBatchLuaFunction(wrappedIterator)
        luaTable[DP_ALL] = getDpAllLuaFunction(wrappedIterator)
        luaTable[DP_AFTER] = getDpAfterLuaFunction(wrappedIterator)
        luaTable[INDEX] = index + 1 // Lua is 1 indexed
        return luaTable
    }

    fun createLuaDataSource(iterator: Iterator<DataPoint>): LuaValue {
        val wrappedIterator = iterator as? PeekableIterator ?: PeekableIterator(iterator)
        val luaTable = LuaTable()
        luaTable[DP] = getDpLuaFunction(wrappedIterator)
        luaTable[DP_BATCH] = getDpBatchLuaFunction(wrappedIterator)
        luaTable[DP_ALL] = getDpAllLuaFunction(wrappedIterator)
        luaTable[DP_AFTER] = getDpAfterLuaFunction(wrappedIterator)
        return luaTable
    }

    private fun getDpAfterLuaFunction(dataSource: PeekableIterator<DataPoint>) =
        oneArgFunction { arg1 ->
            val zonedDateTime = dateTimeParser.parseDateTimeOrNull(arg1)
            val batch = mutableListOf<LuaValue>()
            if (zonedDateTime == null) {
                while (dataSource.hasNext()) {
                    batch.add(dataPointParser.toLuaValueNullable(dataSource.next()))
                }
            } else {
                while (dataSource.hasNext()) {
                    val dataPoint = dataSource.peek()
                    if (dataPoint.timestamp <= zonedDateTime.toOffsetDateTime()) break
                    dataSource.next()
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
