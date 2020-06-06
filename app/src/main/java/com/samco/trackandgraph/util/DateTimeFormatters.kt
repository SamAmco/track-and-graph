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

package com.samco.trackandgraph.util

import android.content.Context
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.Temporal


enum class DateFormatSetting { DMY, MDY, YMD }

private fun getDateTimePref(context: Context): DateFormatSetting {
    val datePrefIndex = getPrefs(context).getInt(
        DATE_FORMAT_SETTING_PREF_KEY, DateFormatSetting.DMY.ordinal
    )
    return DateFormatSetting.values()[datePrefIndex]
}

fun formatDate(format: String, date: Temporal) = DateTimeFormatter
    .ofPattern(format)
    .withZone(ZoneId.systemDefault())
    .format(date)

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
