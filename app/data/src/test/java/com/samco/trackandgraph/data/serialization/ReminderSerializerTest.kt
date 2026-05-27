/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.data.serialization

import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.ReminderParams
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.LocalTime

class ReminderSerializerTest {

    private val serializer = ReminderSerializer(Json { ignoreUnknownKeys = true })

    @Test
    fun `deserializes reminders without enabled flag as enabled`() {
        val encoded = """
            {
                "type":"weekday",
                "time":"09:00",
                "checkedDays":{
                    "monday":true,
                    "tuesday":true,
                    "wednesday":true,
                    "thursday":true,
                    "friday":true,
                    "saturday":false,
                    "sunday":false
                }
            }
        """.trimIndent()

        val result = serializer.deserializeParams(encoded) as ReminderParams.WeekDayParams

        assertTrue(result.enabled)
    }

    @Test
    fun `serializes disabled reminders into params json`() {
        val params = ReminderParams.WeekDayParams(
            time = LocalTime.of(9, 0),
            checkedDays = CheckedDays.all(),
            enabled = false
        )

        val encoded = serializer.serializeParams(params)!!

        assertTrue(encoded.contains("\"enabled\":false"))
        assertFalse((serializer.deserializeParams(encoded) as ReminderParams.WeekDayParams).enabled)
    }
}
