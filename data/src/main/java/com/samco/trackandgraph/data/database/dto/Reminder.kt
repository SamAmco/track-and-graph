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

package com.samco.trackandgraph.data.database.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.samco.trackandgraph.data.serialization.LocalTimeSerializer
import com.samco.trackandgraph.data.serialization.LocalDateTimeSerializer
import org.threeten.bp.LocalTime
import org.threeten.bp.LocalDateTime

data class Reminder(
    val id: Long,
    val displayIndex: Int,
    val reminderName: String,
    val groupId: Long?,
    val featureId: Long?,
    val params: ReminderParams
)

@Serializable
sealed class ReminderParams {
    @Serializable
    @SerialName("weekday")
    data class WeekDayParams(
        @Serializable(with = LocalTimeSerializer::class)
        val time: LocalTime,
        val checkedDays: CheckedDays
    ) : ReminderParams()

    @Serializable
    @SerialName("periodic")
    data class PeriodicParams(
        @Serializable(with = LocalDateTimeSerializer::class)
        val starts: LocalDateTime,
        @Serializable(with = LocalDateTimeSerializer::class)
        val ends: LocalDateTime?,
        val interval: Int,
        val period: Period
    ) : ReminderParams()

    @Serializable
    @SerialName("monthday")
    data class MonthDayParams(
        @Serializable(with = LocalTimeSerializer::class)
        val time: LocalTime,
        val occurrence: MonthDayOccurrence,
        val dayType: MonthDayType,
        @Serializable(with = LocalDateTimeSerializer::class)
        val ends: LocalDateTime?
    ) : ReminderParams()
}

@Serializable
enum class Period {
    @SerialName("minutes")
    MINUTES,
    @SerialName("hours")
    HOURS,
    @SerialName("days")
    DAYS,
    @SerialName("weeks")
    WEEKS,
    @SerialName("months")
    MONTHS,
    @SerialName("years")
    YEARS
}

@Serializable
enum class MonthDayOccurrence {
    @SerialName("first")
    FIRST,
    @SerialName("second")
    SECOND,
    @SerialName("third")
    THIRD,
    @SerialName("fourth")
    FOURTH,
    @SerialName("last")
    LAST
}

@Serializable
enum class MonthDayType {
    @SerialName("monday")
    MONDAY,
    @SerialName("tuesday")
    TUESDAY,
    @SerialName("wednesday")
    WEDNESDAY,
    @SerialName("thursday")
    THURSDAY,
    @SerialName("friday")
    FRIDAY,
    @SerialName("saturday")
    SATURDAY,
    @SerialName("sunday")
    SUNDAY,
    @SerialName("day")
    DAY
}
