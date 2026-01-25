package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.TemporalAdjusters

internal class TimeLuaApiTests : LuaEngineImplTest() {

    //1743202801000 is the timestamp for 2025-03-29T00:00:01 in the Europe/Berlin timezone. the UTC offset is +1.
    // This is one day before daylight savings time starts. It's a Saturday.
    //1743372001000 is the timestamp if you add 2 days. The UTC offset will now be +2

    @Test
    fun `shift timestamp forward by duration`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local timestamp = { timestamp = 1743202801000 }
            local shifted = core.shift(timestamp, core.DURATION.DAY)
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = shifted.timestamp
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = 1743202801000 + MILLIS_IN_DAY
        val actual = (result.data as LuaGraphResultData.TextData).text!!.toLong()
        assertEquals(expected, actual)
    }

    @Test
    fun `shift date forward by duration and amount`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local date = { year = 2025, month = 3, day = 29, sec = 1, zone = "Europe/Berlin" }
            local shifted = core.shift(date, core.DURATION.DAY, 2)
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = shifted.timestamp
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = (1743202801000 + 2 * MILLIS_IN_DAY).toString()
        val actual = (result.data as LuaGraphResultData.TextData).text
        assertEquals(expected, actual)
    }

    @Test
    fun `shift timestamp backward by duration and amount`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local timestamp = { timestamp = 1743202801000 }
            local shifted = core.shift(timestamp, core.DURATION.DAY, -2)
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = shifted.timestamp
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = (1743202801000 - 2 * MILLIS_IN_DAY).toString()
        val actual = (result.data as LuaGraphResultData.TextData).text
        assertEquals(expected, actual)
    }

    @Test
    fun `shift date backward by duration`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local date = { year = 2025, month = 3, day = 29, sec = 1, zone = "Europe/Berlin" }
            local shifted = core.shift(date, core.DURATION.DAY, -1)
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = table.concat({ shifted.timestamp, shifted.offset }, " ")
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = (1743202801000 - MILLIS_IN_DAY).toString() + " " + SECONDS_IN_HOUR
        val actual = (result.data as LuaGraphResultData.TextData).text
        assertEquals(expected, actual)
    }

    @Test
    fun `shift timestamp forward by period`() = testLuaGraph(
        """
            local graph = require("tng.graph")
            local core = require("tng.core")
            local timestamp = { timestamp = 1743202801000, offset = (60 * 60) }
            local shifted = core.shift(timestamp, core.PERIOD.DAY, 2)
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = table.concat({ shifted.timestamp, shifted.offset }, " ")
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        //When shifting by a period of 2 days we actually add an extra hour because
        // the timezone changes from +1 to +2 so to get to the same hour 2 days later we need to add an extra hour
        // of time.
        val expected = "1743375601000 3600"
        val actual = (result.data as LuaGraphResultData.TextData).text
        assertEquals(expected, actual)
    }

    @Test
    fun `shift date backward by period and amount`() = testLuaGraph(
        """
            local graph = require("tng.graph")
            local core = require("tng.core")
            local date = { year = 2025, month = 3, day = 29, sec = 1, zone = "Europe/Berlin" }
            local shifted = core.shift(date, core.PERIOD.DAY, -2)
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = table.concat({ shifted.timestamp, shifted.offset }, " ")
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = (1743202801000 - 2 * MILLIS_IN_DAY).toString() + " " + SECONDS_IN_HOUR
        val actual = (result.data as LuaGraphResultData.TextData).text
        assertEquals(expected, actual)
    }

    @Test
    fun `shift timestamp backward by period and amount`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local timestamp = { timestamp = 1743202801000 }
            local shifted = core.shift(timestamp, core.PERIOD.DAY, -2)
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = table.concat({ shifted.timestamp, shifted.offset }, " ")
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = (1743202801000 - 2 * MILLIS_IN_DAY).toString() + " 0"
        val actual = (result.data as LuaGraphResultData.TextData).text
        assertEquals(expected, actual)
    }

    @Test
    fun `shift date forward by period and amount`() = testLuaGraph(
        """
            local graph = require("tng.graph")
            local core = require("tng.core")
            local date = { year = 2025, month = 3, day = 29, sec = 1, zone = "Europe/Berlin" }
            local shifted = core.shift(date, core.PERIOD.DAY, 2)
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = table.concat({ shifted.timestamp, shifted.offset, shifted.zone }, " ")
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val twoHours = 2 * SECONDS_IN_HOUR
        val expected = "1743372001000 $twoHours Europe/Berlin"
        val actual = (result.data as LuaGraphResultData.TextData).text
        assertEquals(expected, actual)
    }

    @Test
    fun `shift using date, offset, and zone uses zone over offset`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local timestamp = { timestamp = 1743202801000, offset = 3600, zone = "Europe/Berlin", extra = "extra" }
            local shifted = core.shift(timestamp, core.PERIOD.DAY, 2)
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = table.concat({ shifted.timestamp, shifted.offset, shifted.zone, shifted.extra }, " ")
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        //Supplying a zone overrides the offset
        val twoHours = 2 * SECONDS_IN_HOUR
        val expected = "1743372001000 $twoHours Europe/Berlin extra"
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `shift using date and offset uses offset`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local timestamp = { timestamp = 1743202801000, offset = 3600, extra = "extra" }
            local shifted = core.shift(timestamp, core.PERIOD.DAY, 2)
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = table.concat({ shifted.timestamp, shifted.offset, shifted.zone, shifted.extra }, " ")
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        //Kinda odd that we've added an extra hour to the timestamp. Clearly java time thinks that if you had an offset
        // of 1 hour, you must have crossed daylight savings here. So offset doesn't change, but the timestamp gets an
        // extra hour.
        val expected = "1743375601000 $SECONDS_IN_HOUR +01:00 extra"
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }


    @Test
    fun `shift doesnt lose any fields from the input table`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local timestamp = { timestamp = 1743202801000, offset = 3600, zone = "Europe/Berlin", extra = "extra" }
            local shifted = core.shift(timestamp, core.PERIOD.DAY, 2)
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = table.concat({ shifted.timestamp, shifted.offset, shifted.zone, shifted.extra }, " ")
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        //Supplying a zone overrides the offset
        val twoHours = 2 * SECONDS_IN_HOUR
        val expected = "1743372001000 $twoHours Europe/Berlin extra"
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `time with no args returns current time`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local time = core.time()
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = time.timestamp
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = Instant.now().toEpochMilli()
        val actual = (result.data as LuaGraphResultData.TextData).text!!.toLong()
        val diff = actual - expected
        assert(diff < 10)
    }

    @Test
    fun `time with date returns time at that date`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local date = { year = 2025, month = 3, day = 29, sec = 1, zone = "Europe/Berlin" }
            local time = core.time(date)
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = table.concat({ time.timestamp, time.offset, time.zone }, " ")
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = "1743202801000 $SECONDS_IN_HOUR Europe/Berlin"
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `date with no args returns current date`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local date = core.date()
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = table.concat({ date.year, date.month, date.day, date.hour, date.min, date.sec, date.yday, date.wday }, " ")
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val now = ZonedDateTime.now()
        val expected = "${now.year} ${now.monthValue} ${now.dayOfMonth} ${now.hour} ${now.minute} ${now.second} ${now.dayOfYear} ${now.dayOfWeek.value}"
        val actual = (result.data as LuaGraphResultData.TextData).text
        assertEquals(expected, actual)
    }

    @Test
    fun `date with timestamp returns date at that timestamp`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local timestamp = { timestamp = 1743202801000, offset = 3600, zone = "Europe/Berlin" }
            local date = core.date(timestamp)
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = table.concat({ date.year, date.month, date.day, date.hour, date.min, date.sec, date.yday, date.wday }, " ")
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = "2025 3 29 0 0 1 88 6"
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `format given a timestamp number and a format string returns the formatted date`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local timestamp = 1743202801000
            local result = core.format(timestamp, "yyyy-MM-dd HH:mm:ss")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = result
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        //It's the night before in UTC
        val expected = "2025-03-28 23:00:01"
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `format given a timestamp and a format string returns the formatted date`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local timestamp = { timestamp = 1743202801000, offset = 3600, zone = "Europe/Berlin" }
            local result = core.format(timestamp, "yyyy-MM-dd HH:mm:ss")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = result
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = "2025-03-29 00:00:01"
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `format given a date and a format string returns the formatted date`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local date = { year = 2025, month = 3, day = 29, sec = 1, zone = "Europe/Berlin" }
            local result = core.format(date, "yyyy-MM-dd HH:mm:ss")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = result
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = "2025-03-29 00:00:01"
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `you can override the day of week on a date`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local date = { year = 2025, month = 3, day = 29, hour = 1, min = 2, sec = 3, zone = "Europe/London" }
            date.wday = 1
            local time = core.time(date)
            local result = core.format(time, "yyyy-MM-dd HH:mm:ss")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = result
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = "2025-03-24 01:02:03"
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `you can override the day of month on a date`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local date = { year = 2025, month = 3, day = 29, hour = 1, min = 2, sec = 3, zone = "Europe/London" }
            date.day = 1
            local time = core.time(date)
            local result = core.format(time, "yyyy-MM-dd HH:mm:ss")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = result
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = "2025-03-01 01:02:03"
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `you can override the day of year on a date`() = testLuaGraph(
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local date = { year = 2025, month = 3, day = 29, hour = 1, min = 2, sec = 3, zone = "Europe/London" }
            date.yday = 1
            local time = core.time(date)
            local result = core.format(time, "yyyy-MM-dd HH:mm:ss")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = result
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val expected = "2025-01-01 01:02:03"
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `realistic overriding day of month`() = testLuaGraph(
        // Realistically you have to remove yday and wday from the table to override the day of month
        // if you are trying to adjust the current date otherwise they will take precedence.
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local date = core.date()
            date.yday = NIL
            date.wday = NIL
            date.day = 1
            local time = core.time(date)
            local result = core.format(time, "yyyy-MM-dd HH:mm:ss")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = result
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val now = ZonedDateTime.now()
        val expected = DateTimeFormatter.ofPattern("yyyy-MM-01 HH:mm:ss").format(now)
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `realistic overriding day of year`() = testLuaGraph(
        // Realistically you have to remove wday from the table to override the day of year
        // if you are trying to adjust the current date otherwise it will take precedence.
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local date = core.date()
            date.yday = 1
            date.wday = NIL
            local time = core.time(date)
            local result = core.format(time, "yyyy-MM-dd HH:mm:ss")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = result
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val now = ZonedDateTime.now().withDayOfYear(1)
        val expected = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(now)
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }

    @Test
    fun `realistic overriding day of week`() = testLuaGraph(
        // wday takes highest precedence so you have to remove yday and day from the table to override the day of week
        """
            local core = require("tng.core")
            local graph = require("tng.graph")
            local date = core.date()
            date.wday = 1
            local time = core.time(date)
            local result = core.format(time, "yyyy-MM-dd HH:mm:ss")
            return {
                type = graph.GRAPH_TYPE.TEXT,
                text = result
            }
        """.trimIndent()
    ) {
        println(result)
        assert(result.data is LuaGraphResultData.TextData)
        val now = ZonedDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val expected = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(now)
        assertEquals(expected, (result.data as LuaGraphResultData.TextData).text)
    }


    companion object {
        const val MILLIS_IN_DAY = 24 * 60 * 60 * 1000
        const val SECONDS_IN_HOUR = 60 * 60
    }
}