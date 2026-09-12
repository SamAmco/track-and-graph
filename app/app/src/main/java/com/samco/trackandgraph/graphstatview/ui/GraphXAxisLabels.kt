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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph. If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.graphstatview.ui

import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private val graphXAxisSecondFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val graphXAxisMinuteFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal data class GraphXAxisLabelText(
    val months: List<String>,
    val weekdays: List<String>,
    val weekdayDayFormat: String,
    val dayMonthFormat: String,
    val monthYearFormat: String,
)

internal enum class GraphXAxisLabelFormat { SECOND, MINUTE, WEEKDAY, DAY, MONTH }

internal fun graphXAxisLabelFormatForDuration(durationMillis: Long): GraphXAxisLabelFormat = when {
    Duration.ofMillis(durationMillis).toMinutes() < 5L -> GraphXAxisLabelFormat.SECOND
    Duration.ofMillis(durationMillis).toDays() < 1L -> GraphXAxisLabelFormat.MINUTE
    Duration.ofMillis(durationMillis).toDays() < 14L -> GraphXAxisLabelFormat.WEEKDAY
    Duration.ofMillis(durationMillis).toDays() < 304L -> GraphXAxisLabelFormat.DAY
    else -> GraphXAxisLabelFormat.MONTH
}

internal fun graphXAxisContextualStartLabelFormat(
    durationMillis: Long,
): GraphXAxisLabelFormat = graphXAxisContextualStartLabelFormat(
    graphXAxisLabelFormatForDuration(durationMillis),
)

internal fun graphXAxisContextualStartLabelFormat(
    format: GraphXAxisLabelFormat,
): GraphXAxisLabelFormat = when (format) {
    GraphXAxisLabelFormat.SECOND,
    GraphXAxisLabelFormat.MINUTE,
    GraphXAxisLabelFormat.WEEKDAY -> GraphXAxisLabelFormat.DAY
    GraphXAxisLabelFormat.DAY,
    GraphXAxisLabelFormat.MONTH -> GraphXAxisLabelFormat.MONTH
}

internal fun graphXAxisLabelCandidates(
    format: GraphXAxisLabelFormat,
    labelText: GraphXAxisLabelText,
): List<String> = when (format) {
    GraphXAxisLabelFormat.SECOND -> listOf("88:88:88")
    GraphXAxisLabelFormat.MINUTE -> listOf("88:88")
    GraphXAxisLabelFormat.WEEKDAY -> labelText.weekdays.map {
        formatGraphXAxisLabel(labelText.weekdayDayFormat, it, "88")
    }
    GraphXAxisLabelFormat.DAY -> labelText.months.map {
        formatGraphXAxisLabel(labelText.dayMonthFormat, "88", it)
    }
    GraphXAxisLabelFormat.MONTH -> labelText.months.map {
        formatGraphXAxisLabel(labelText.monthYearFormat, it, "88")
    }
}

internal fun formatGraphXAxisTimestamp(
    epochMillis: Long,
    format: GraphXAxisLabelFormat,
    zoneId: ZoneId,
    labelText: GraphXAxisLabelText,
): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
    return when (format) {
        GraphXAxisLabelFormat.SECOND -> dateTime.format(graphXAxisSecondFormatter)
        GraphXAxisLabelFormat.MINUTE -> dateTime.format(graphXAxisMinuteFormatter)
        GraphXAxisLabelFormat.WEEKDAY -> formatGraphXAxisLabel(
            labelText.weekdayDayFormat,
            labelText.weekdays[dateTime.dayOfWeek.value - 1],
            twoDigitString(dateTime.dayOfMonth),
        )
        GraphXAxisLabelFormat.DAY -> formatGraphXAxisLabel(
            labelText.dayMonthFormat,
            twoDigitString(dateTime.dayOfMonth),
            labelText.months[dateTime.monthValue - 1],
        )
        GraphXAxisLabelFormat.MONTH -> formatGraphXAxisLabel(
            labelText.monthYearFormat,
            labelText.months[dateTime.monthValue - 1],
            twoDigitString(Math.floorMod(dateTime.year, 100)),
        )
    }
}

internal fun formatGraphXAxisTimestamp(
    epochMillis: Long,
    durationMillis: Long,
    zoneId: ZoneId,
    labelText: GraphXAxisLabelText,
): String = formatGraphXAxisTimestamp(
    epochMillis,
    graphXAxisLabelFormatForDuration(durationMillis),
    zoneId,
    labelText,
)

internal fun formatGraphXAxisStartTimestamp(
    epochMillis: Long,
    durationMillis: Long,
    zoneId: ZoneId,
    labelText: GraphXAxisLabelText,
): String = formatGraphXAxisTimestamp(
    epochMillis,
    graphXAxisContextualStartLabelFormat(durationMillis),
    zoneId,
    labelText,
)

private fun formatGraphXAxisLabel(format: String, first: String, second: String): String =
    String.format(format, first, second)

private fun twoDigitString(value: Int): String = value.toString().padStart(2, '0')
