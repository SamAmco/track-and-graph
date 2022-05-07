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

package com.samco.trackandgraph.ui

import android.content.Context
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.util.DATE_FORMAT_SETTING_PREF_KEY
import com.samco.trackandgraph.util.getPrefs
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.Temporal
import java.text.DecimalFormat

val doubleFormatter = DecimalFormat("#.##################")

enum class DateFormatSetting { DMY, MDY, YMD }

private fun getDateTimePref(context: Context): DateFormatSetting {
    val datePrefIndex = getPrefs(context).getInt(
        DATE_FORMAT_SETTING_PREF_KEY, DateFormatSetting.DMY.ordinal
    )
    return DateFormatSetting.values()[datePrefIndex]
}

private fun formatDate(format: String, date: Temporal): String = DateTimeFormatter
    .ofPattern(format)
    .format(date)

fun getWeekDayNames(context: Context) = listOf(
    context.getString(R.string.mon),
    context.getString(R.string.tue),
    context.getString(R.string.wed),
    context.getString(R.string.thu),
    context.getString(R.string.fri),
    context.getString(R.string.sat),
    context.getString(R.string.sun)
)

/**
 * Should return the day of the week locally at the time the data point was tracked
 */
private fun weekDayPart(dateTime: OffsetDateTime, weekDayNames: List<String>) =
    " (${weekDayNames[dateTime.dayOfWeek.value - 1]}) "

fun formatDayWeekDayMonthYearHourMinuteOneLine(
    context: Context,
    weekDayNames: List<String>,
    dateTime: OffsetDateTime
) = formatDayMonthYear(context, dateTime) +
        weekDayPart(dateTime, weekDayNames) +
        formatHourMinute(dateTime)

fun DataPoint.getDisplayValue(dataType: DataType): String {
    val time = this.timestamp
    val value = this.value
    val label = this.label
    return object : IDataPoint() {
        override val timestamp = time
        override val value = value
        override val label = label
    }.getDisplayValue(dataType)
}

fun IDataPoint.getDisplayValue(dataType: DataType): String {
    return when (dataType) {
        DataType.DISCRETE -> doubleFormatter.format(this.value) + " : ${this.label}"
        DataType.CONTINUOUS -> doubleFormatter.format(this.value)
        DataType.DURATION -> formatTimeDuration(this.value.toLong())
    }
}

fun formatDayMonthYearHourMinuteWeekDayTwoLines(
    context: Context,
    weekDayNames: List<String>,
    dateTime: OffsetDateTime
) = formatDayMonthYear(context, dateTime) + " " +
        formatHourMinute(dateTime) +
        "\n" +
        weekDayPart(dateTime, weekDayNames)

fun formatHourMinute(temporal: Temporal) = formatDate("HH:mm", temporal)

fun formatDayMonthYearHourMinute(context: Context, temporal: Temporal): String {
    val format = when (getDateTimePref(context)) {
        DateFormatSetting.DMY -> "dd/MM/yy  HH:mm"
        DateFormatSetting.MDY -> "MM/dd/yy  HH:mm"
        DateFormatSetting.YMD -> "yy/MM/dd  HH:mm"
    }
    return formatDate(format, temporal)
}

fun formatDayMonthYear(context: Context, temporal: Temporal): String {
    val format = when (getDateTimePref(context)) {
        DateFormatSetting.DMY -> "dd/MM/yy"
        DateFormatSetting.MDY -> "MM/dd/yy"
        DateFormatSetting.YMD -> "yy/MM/dd"
    }
    return formatDate(format, temporal)
}

fun formatDayMonth(context: Context, temporal: Temporal): String {
    val format = when (getDateTimePref(context)) {
        DateFormatSetting.DMY -> "dd/MM"
        DateFormatSetting.MDY, DateFormatSetting.YMD -> "MM/dd"
    }
    return formatDate(format, temporal)
}

fun formatMonthYear(context: Context, temporal: Temporal): String {
    val format = when (getDateTimePref(context)) {
        DateFormatSetting.DMY, DateFormatSetting.MDY -> "MM/yy"
        DateFormatSetting.YMD -> "yy/MM"
    }
    return formatDate(format, temporal)
}

fun formatTimeDuration(seconds: Long): String {
    val absSecs = kotlin.math.abs(seconds)
    return String.format("%d:%02d:%02d", seconds / 3600, (absSecs % 3600) / 60, (absSecs % 60))
}