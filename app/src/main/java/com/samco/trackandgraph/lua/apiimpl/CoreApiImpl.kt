package com.samco.trackandgraph.lua.apiimpl

import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

class CoreApiImpl @Inject constructor(
    private val dateTimeParser: DateTimeParser
) {
    companion object {
        const val TIME = "time"
        const val DATE = "date"
        const val SHIFT = "shift"
        const val FORMAT = "format"
    }

    fun installIn(table: LuaTable) = table.apply {
        overrideOrThrow(TIME, getTimeLuaFunction())
        overrideOrThrow(DATE, getDateLuaFunction())
        overrideOrThrow(SHIFT, getShiftLuaFunction())
        overrideOrThrow(FORMAT, getFormatLuaFunction())
    }

    private fun getTimeLuaFunction(): LuaValue = oneArgFunction { arg: LuaValue ->
        val date = dateTimeParser.parseDateOrNow(arg)
        return@oneArgFunction dateTimeParser.zonedDateTimeToLuaTimestamp(date)
    }

    private fun getDateLuaFunction(): LuaValue = oneArgFunction { arg: LuaValue ->
        val timestamp = dateTimeParser.parseTimestampOrNow(arg)
        return@oneArgFunction dateTimeParser.zonedDateTimeToLuaDate(timestamp)
    }

    private fun getShiftLuaFunction(): LuaValue = threeArgFunction { arg1, arg2, arg3 ->
        val dateTime = dateTimeParser.parseDateTimeOrNow(arg1)
        val shift = dateTimeParser.parseDurationOrPeriod(arg2, arg3)
        val shiftedDateTime = dateTime + shift
        val shiftedTimeTable = dateTimeParser.zonedDateTimeToLuaTimestamp(shiftedDateTime)
        return@threeArgFunction when {
            arg1.istable() -> merge(arg1.checktable()!!, shiftedTimeTable)
            else -> shiftedTimeTable
        }
    }

    private fun getFormatLuaFunction(): LuaValue = twoArgFunction { arg1, arg2 ->
        val dateTime = dateTimeParser.parseDateTimeOrNow(arg1)
        val format = arg2.tojstring()
        val formatter = DateTimeFormatter.ofPattern(format)
        return@twoArgFunction LuaValue.valueOf(dateTime.format(formatter))
    }
}