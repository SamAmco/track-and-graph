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
package com.samco.trackandgraph.lua.apiimpl

import com.samco.trackandgraph.time.TimeProvider
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.Companion.tableOf
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.TemporalAmount
import javax.inject.Inject

class DateTimeParser @Inject constructor(
    private val timeProvider: TimeProvider
) {

    companion object {
        const val TIMESTAMP = "timestamp"
        const val OFFSET = "offset"
        const val ZONE = "zone"
        const val YEAR = "year"
        const val MONTH = "month"
        const val DAY = "day"
        const val HOUR = "hour"
        const val MINUTE = "min"
        const val SECOND = "sec"
        const val W_DAY = "wday"
        const val Y_DAY = "yday"
    }

    /**
     * Parses a datetime
     *
     * A datetime can mean either
     * -- @class date
     * -- @class timestamp
     * in tng.lua
     */
    fun parseDateTimeOrNow(dateTime: LuaValue): ZonedDateTime = parseDateTimeOrNull(dateTime)
        ?: timeProvider.now()

    fun parseDateTimeOrNull(dateTime: LuaValue): ZonedDateTime? = when {
        dateTime.isnumber() -> ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(dateTime.tolong()),
            ZoneOffset.UTC,
        )

        dateTime.istable() -> parseDateTimeTable(dateTime)

        else -> null
    }

    fun parseOffsetDateTimeOrThrow(dateTime: LuaValue): OffsetDateTime {
        val timestamp = dateTime[TIMESTAMP].checklong()
        if (dateTime[OFFSET].isnil()) {
            return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC)
        }
        val offset = dateTime[OFFSET].checklong()
        return OffsetDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneOffset.ofTotalSeconds(offset.toInt()).normalized()
        )
    }

    fun parseDateOrNow(date: LuaValue): ZonedDateTime = when {
        date.istable() -> parseDateOrThrow(date)
        else -> timeProvider.now()
    }

    fun parseTimestampOrNow(arg: LuaValue): ZonedDateTime = parseTimestampOrNull(arg)
        ?: timeProvider.now()

    fun parseTimestampOrThrow(arg: LuaValue): ZonedDateTime = parseTimestampOrNull(arg)
        ?: throw IllegalArgumentException("Expected a timestamp or a table, got ${arg.typename()}")

    private fun parseTimestampOrNull(arg: LuaValue): ZonedDateTime? = when {
        arg.isnumber() -> ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(arg.tolong()),
            ZoneOffset.UTC,
        )

        arg.istable() -> parseTimestamp(arg)

        else -> null
    }

    fun overrideOffsetDateTime(table: LuaValue, offsetDateTime: OffsetDateTime) {
        table[TIMESTAMP] = offsetDateTime.toInstant().toEpochMilli().toDouble()
        table[OFFSET] = offsetDateTime.offset.totalSeconds
    }

    fun zonedDateTimeToLuaTimestamp(zonedDateTime: ZonedDateTime): LuaTable {
        val table = tableOf()
        table[TIMESTAMP] = zonedDateTime.toInstant().toEpochMilli().toDouble()
        table[OFFSET] = zonedDateTime.offset.totalSeconds
        table[ZONE] = zonedDateTime.zone.id
        return table
    }

    fun zonedDateTimeToLuaDate(zonedDateTime: ZonedDateTime): LuaTable {
        val table = tableOf()
        table[YEAR] = zonedDateTime.year
        table[MONTH] = zonedDateTime.monthValue
        table[DAY] = zonedDateTime.dayOfMonth
        table[HOUR] = zonedDateTime.hour
        table[MINUTE] = zonedDateTime.minute
        table[SECOND] = zonedDateTime.second
        table[W_DAY] = zonedDateTime.dayOfWeek.value
        table[Y_DAY] = zonedDateTime.dayOfYear
        table[ZONE] = zonedDateTime.zone.id
        return table
    }

    fun parseDurationOrPeriod(unit: LuaValue, multiple: LuaValue): TemporalAmount = when {
        unit.isnumber() -> Duration.ofMillis(unit.tolong()).multipliedBy(multiple.optlong(1))
        unit.isstring() -> Period.parse(unit.tojstring()).multipliedBy(multiple.optint(1))
        else -> throw IllegalArgumentException("Expected a number or string, got ${unit.typename()}")
    }

    private fun parseDateTimeTable(dateTime: LuaValue): ZonedDateTime {
        val timestamp = dateTime[TIMESTAMP]
        return when {
            timestamp.isnumber() -> parseTimestamp(dateTime, timestamp.tolong())
            else -> parseDateOrThrow(dateTime)
        }
    }

    private fun parseTimestamp(dateTime: LuaValue): ZonedDateTime =
        parseTimestamp(dateTime, dateTime[TIMESTAMP].checklong())

    private fun parseTimestamp(dateTime: LuaValue, timestamp: Long): ZonedDateTime {
        val zone = dateTime[ZONE]

        if (zone.isstring()) {
            val instant = Instant.ofEpochMilli(timestamp)
            return ZonedDateTime.ofInstant(instant, ZoneId.of(zone.tojstring()))
        }

        val offset = dateTime[OFFSET]

        if (offset.isnumber()) {
            val instant = Instant.ofEpochMilli(timestamp)
            return ZonedDateTime.ofInstant(
                instant,
                ZoneOffset.ofTotalSeconds(offset.toint()).normalized()
            )
        }

        val instant = Instant.ofEpochMilli(timestamp)
        return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
    }

    private fun parseDateOrThrow(dateTime: LuaValue): ZonedDateTime {
        val year = dateTime[YEAR].checkint()
        val month = dateTime[MONTH].checkint()
        val day = dateTime[DAY].checkint()
        val hour = dateTime[HOUR].optint(0)
        val minute = dateTime[MINUTE].optint(0)
        val second = dateTime[SECOND].optint(0)
        val dayOfWeek = dateTime[W_DAY].optinteger(null)?.v
        val dayOfYear = dateTime[Y_DAY].optinteger(null)?.v
        val zone = parseZone(dateTime) ?: timeProvider.defaultZone()
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0, zone).let {
            if (dayOfWeek != null) it.with(TemporalAdjusters.previousOrSame(dayOfWeek.toDayOfWeek()))
            else if (dayOfYear != null) it.withDayOfYear(dayOfYear)
            else it
        }
    }

    private fun Int.toDayOfWeek(): DayOfWeek = when (this) {
        1 -> DayOfWeek.MONDAY
        2 -> DayOfWeek.TUESDAY
        3 -> DayOfWeek.WEDNESDAY
        4 -> DayOfWeek.THURSDAY
        5 -> DayOfWeek.FRIDAY
        6 -> DayOfWeek.SATURDAY
        7 -> DayOfWeek.SUNDAY
        else -> throw LuaError("Invalid day of week: $this")
    }

    private fun parseZone(dateTime: LuaValue): ZoneId? = ZoneId.of(dateTime[ZONE].tojstring())
}