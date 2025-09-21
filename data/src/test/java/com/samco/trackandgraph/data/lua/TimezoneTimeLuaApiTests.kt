package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.time.TimeProvider
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

internal class TimezoneTimeLuaApiTests : LuaEngineImplTest() {

    override val timeProvider: TimeProvider = mock()

    @Test
    fun `get_end_of_period with DAY period for NYC user at 8pm should return end of current day not next day`() {
        // NYC user at 2025-06-16 8pm Eastern Time (UTC-4)
        val nycZone = ZoneId.of("America/New_York")
        val nycTime = ZonedDateTime.of(2025, 6, 16, 20, 0, 0, 0, nycZone) // 8pm on June 16th
        
        // Configure mock to return the NYC time
        whenever(timeProvider.now()).thenReturn(nycTime)
        whenever(timeProvider.defaultZone()).thenReturn(nycZone)
        
        testLuaGraph(
            """
                local core = require("tng.core")
                local graph = require("tng.graph")
                
                -- Test the get_end_of_period logic manually (similar to the function)
                local zone_override = "America/New_York"
                local zone = zone_override or core.date().zone
                local date = core.date(core.time())
                date = core.date(core.shift(date, core.PERIOD.DAY, 1))
                date.hour = 0
                date.min = 0
                date.sec = 0
                date.zone = zone
                
                local result_timestamp = core.time(date)
                local formatted_result = core.format(result_timestamp, "yyyy-MM-dd HH:mm:ss")
                
                return graph.text(formatted_result)
            """.trimIndent()
        ) {
            println("NYC 8pm get_end_of_period result: $result")
            assert(result.data is LuaGraphResultData.TextData)
            val actualResult = (result.data as LuaGraphResultData.TextData).text
            
            // The user expects this to be the end of 2025-06-16 (start of 2025-06-17)
            // But they claim it's giving them the end of 2025-06-17 (start of 2025-06-18)
            val expectedEndOfCurrentDay = "2025-06-17 00:00:00" // Start of next day = end of current day
            val incorrectEndOfNextDay = "2025-06-18 00:00:00"   // Start of day after next = end of next day
            
            println("Input: NYC user at 2025-06-16 8pm Eastern Time")
            println("Expected (end of current day): $expectedEndOfCurrentDay")
            println("Actual result: $actualResult")
            println("Incorrect (end of next day): $incorrectEndOfNextDay")
            
            // This test will help us see what the function actually returns
            // If the user's complaint is correct, actualResult will equal incorrectEndOfNextDay
            assertEquals(expectedEndOfCurrentDay, actualResult)
        }
    }
    
    @Test
    fun `get_end_of_period function directly with DAY period for NYC user at 8pm should return end of current day`() {
        // NYC user at 2025-06-16 8pm Eastern Time (UTC-4)
        val nycZone = ZoneId.of("America/New_York")
        val nycTime = ZonedDateTime.of(2025, 6, 16, 20, 0, 0, 0, nycZone) // 8pm on June 16th
        
        // Configure mock to return the NYC time
        whenever(timeProvider.now()).thenReturn(nycTime)
        whenever(timeProvider.defaultZone()).thenReturn(nycZone)
        
        testLuaGraph(
            """
                local core = require("tng.core")
                local graph = require("tng.graph")
                
                -- Use get_end_of_period function directly with core.time() (preserves timezone)
                local current_time = core.time()
                local end_of_day = core.get_end_of_period(core.PERIOD.DAY, current_time, "America/New_York")
                local result_timestamp = core.time(end_of_day)
                local formatted_result = core.format(result_timestamp, "yyyy-MM-dd HH:mm:ss")
                
                return graph.text(formatted_result)
            """.trimIndent()
        ) {
            println("get_end_of_period direct call result: $result")
            assert(result.data is LuaGraphResultData.TextData)
            val actualResult = (result.data as LuaGraphResultData.TextData).text
            
            val expectedEndOfCurrentDay = "2025-06-17 00:00:00" // Start of next day = end of current day
            
            println("Input: NYC user at 2025-06-16 8pm Eastern Time")
            println("Expected (end of current day): $expectedEndOfCurrentDay")
            println("Actual result: $actualResult")
            
            assertEquals(expectedEndOfCurrentDay, actualResult)
        }
    }


}
