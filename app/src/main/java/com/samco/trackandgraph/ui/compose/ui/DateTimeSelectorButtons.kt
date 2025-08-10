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
package com.samco.trackandgraph.ui.compose.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat.CLOCK_24H
import com.samco.trackandgraph.helpers.formatDayMonthYear
import com.samco.trackandgraph.helpers.formatHourMinute
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import java.util.Calendar

//TODO these can be transitioned to compose once we transition to material 3

@Composable
fun DateTimeButtonRow(
    modifier: Modifier = Modifier,
    selectedDateTime: OffsetDateTime,
    onDateTimeSelected: (OffsetDateTime) -> Unit,
) = Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    DateButton(
        modifier = Modifier.widthIn(min = 104.dp),
        dateTime = selectedDateTime,
        onDateSelected = { odt ->
            onDateTimeSelected(
                odt
                    .withHour(selectedDateTime.hour)
                    .withMinute(selectedDateTime.minute)
                    .withSecond(selectedDateTime.second)
                    .withNano(selectedDateTime.nano)
            )
        }
    )
    TimeButton(
        modifier = Modifier.widthIn(min = 104.dp),
        dateTime = selectedDateTime,
        onTimeSelected = { time ->
            onDateTimeSelected(
                selectedDateTime
                    .withHour(time.hour)
                    .withMinute(time.minute)
            )
        }
    )
}

data class SelectedTime(
    val hour: Int,
    val minute: Int
)

@Composable
fun TimeButton(
    modifier: Modifier = Modifier,
    dateTime: OffsetDateTime,
    enabled: Boolean = true,
    onTimeSelected: (SelectedTime) -> Unit
) = Box {
    val context = LocalContext.current

    SelectorButton(
        modifier = modifier,
        text = formatHourMinute(dateTime),
        enabled = enabled,
        onClick = {
            showTimePickerDialog(
                context = context,
                onTimeSelected = onTimeSelected,
                hour = dateTime.hour,
                minute = dateTime.minute
            )
        }
    )
}

fun showTimePickerDialog(
    context: Context,
    onTimeSelected: (SelectedTime) -> Unit,
    hour: Int? = null,
    minute: Int? = null,
) {
    val tag = "TimePicker"
    val fragmentManager = findFragmentManager(context) ?: return
    val fragment = fragmentManager.findFragmentByTag(tag)
    val existingPicker = fragment as? MaterialTimePicker
    val picker = existingPicker ?: MaterialTimePicker.Builder()
        .apply {
            if (hour != null) setHour(hour)
            if (minute != null) setMinute(minute)
        }
        .setTimeFormat(CLOCK_24H)
        .setInputMode(INPUT_MODE_CLOCK)
        .build()
    picker.apply {
        addOnPositiveButtonClickListener {
            onTimeSelected(SelectedTime(this.hour, this.minute))
        }
        show(fragmentManager, tag)
    }
}

@Composable
fun DateButton(
    modifier: Modifier = Modifier,
    dateTime: OffsetDateTime,
    enabled: Boolean = true,
    allowPastDates: Boolean = true,
    onDateSelected: (OffsetDateTime) -> Unit,
) = Box {
    val context = LocalContext.current
    val firstDayOfWeek = LocalSettings.current.firstDayOfWeek
    SelectorButton(
        modifier = modifier,
        text = formatDayMonthYear(context, dateTime),
        enabled = enabled,
        onClick = {
            showDatePickerDialog(context, onDateSelected, firstDayOfWeek, allowPastDates, dateTime)
        }
    )
}

fun showDatePickerDialog(
    context: Context,
    onDateSelected: (OffsetDateTime) -> Unit,
    firstDayOfWeek: DayOfWeek,
    allowPastDates: Boolean = true,
    dateTime: OffsetDateTime = OffsetDateTime.now()
) {
    val tag = "DatePicker"
    val fragmentManager = findFragmentManager(context) ?: return
    val fragment = fragmentManager.findFragmentByTag(tag)
    val existingPicker = fragment as? MaterialDatePicker<*>
    val dowMap = mapOf(
        DayOfWeek.MONDAY to Calendar.MONDAY,
        DayOfWeek.TUESDAY to Calendar.TUESDAY,
        DayOfWeek.WEDNESDAY to Calendar.WEDNESDAY,
        DayOfWeek.THURSDAY to Calendar.THURSDAY,
        DayOfWeek.FRIDAY to Calendar.FRIDAY,
        DayOfWeek.SATURDAY to Calendar.SATURDAY,
        DayOfWeek.SUNDAY to Calendar.SUNDAY
    )
    val picker = existingPicker ?: MaterialDatePicker.Builder
        .datePicker()
        .setSelection(dateTime.toInstant().toEpochMilli())
        .setCalendarConstraints(
            CalendarConstraints.Builder()
                .let {
                    if (allowPastDates) it
                    else it.setValidator(DateValidatorPointForward.now())
                }
                .setFirstDayOfWeek(dowMap[firstDayOfWeek] ?: Calendar.MONDAY)
                .build()
        )
        .build()

    picker.apply {
        addOnPositiveButtonClickListener { obj ->
            val epochMillis = (obj as? Long) ?: return@addOnPositiveButtonClickListener
            //epochMillis is 00:00 of a date in UTC, we want to return the same date in the local timezone
            val utcDate = Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC)
            val selected = utcDate.withOffsetSameLocal(OffsetDateTime.now().offset)
            onDateSelected(selected)
        }
        show(fragmentManager, tag)
    }
}

private fun findFragmentManager(context: Context): FragmentManager? {
    var currentContext = context
    while (currentContext !is FragmentActivity) {
        if (currentContext is ContextWrapper) {
            currentContext = currentContext.baseContext
        } else break
    }
    return (currentContext as? FragmentActivity)?.supportFragmentManager
}
