package com.samco.trackandgraph.lua.apiimpl

import com.samco.trackandgraph.base.database.dto.DataPoint
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.Companion.tableOf
import org.luaj.vm2.LuaValue.Companion.valueOf
import javax.inject.Inject

class DataPointParser @Inject constructor(
    private val dateTimeParser: DateTimeParser
) {
    companion object {
        const val VALUE = "value"
        const val LABEL = "label"
        const val NOTE = "note"
    }

    fun toLuaValueNullable(dataPoint: DataPoint?): LuaValue {
        if (dataPoint == null) return LuaValue.NIL
        val luaDataPoint = tableOf()
        luaDataPoint[VALUE] = valueOf(dataPoint.value)
        luaDataPoint[LABEL] = valueOf(dataPoint.label)
        luaDataPoint[NOTE] = valueOf(dataPoint.note)
        dateTimeParser.overrideOffsetDateTime(luaDataPoint, dataPoint.timestamp)
        return luaDataPoint
    }

    fun parseDataPoint(data: LuaValue): DataPoint = DataPoint(
        timestamp = dateTimeParser.parseOffsetDateTimeOrThrow(data),
        featureId = -1L,
        value = data[VALUE].optdouble(0.0),
        label = data[LABEL].optjstring("") ?: "",
        note = data[NOTE].optjstring("") ?: ""
    )
}