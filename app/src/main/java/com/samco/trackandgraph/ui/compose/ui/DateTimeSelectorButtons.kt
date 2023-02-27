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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat.CLOCK_24H
import com.samco.trackandgraph.base.helpers.formatDayMonthYear
import com.samco.trackandgraph.base.helpers.formatHourMinute
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId

@Composable
fun DateTimeButtonRow(
    modifier: Modifier = Modifier,
    selectedDateTime: OffsetDateTime,
    onDateTimeSelected: (OffsetDateTime) -> Unit,
) = Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.SpaceEvenly
) {

    val context = LocalContext.current
    val selected = remember(selectedDateTime) { selectedDateTime }
    val dateString = remember(selectedDateTime) {
        formatDayMonthYear(context, selected)
    }
    val timeString = remember(selectedDateTime) {
        formatHourMinute(selected)
    }

    DateButton(
        modifier = Modifier.widthIn(min = 104.dp),
        context = context,
        dateString = dateString,
        onDateSelected = { odt ->
            onDateTimeSelected(
                odt
                    .withHour(selected.hour)
                    .withMinute(selected.minute)
            )
        }
    )
    TimeButton(
        modifier = Modifier.widthIn(min = 104.dp),
        context = context,
        timeString = timeString,
        onTimeSelected = { time ->
            onDateTimeSelected(
                selected
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
    context: Context,
    timeString: String,
    onTimeSelected: (SelectedTime) -> Unit
) = Box {

    val tag = "TimePicker"

    SelectorTextButton(
        modifier = modifier,
        text = timeString,
        onClick = {
            val fragmentManager = findFragmentManager(context) ?: return@SelectorTextButton
            val fragment = fragmentManager.findFragmentByTag(tag)
            val existingPicker = fragment as? MaterialTimePicker
            val picker = existingPicker ?: MaterialTimePicker.Builder()
                .setTimeFormat(CLOCK_24H)
                .build()
            picker.apply {
                addOnPositiveButtonClickListener {
                    onTimeSelected(SelectedTime(this.hour, this.minute))
                }
                show(fragmentManager, tag)
            }
        }
    )
}

@Composable
fun DateButton(
    modifier: Modifier = Modifier,
    context: Context,
    dateString: String,
    onDateSelected: (OffsetDateTime) -> Unit
) = Box {
    SelectorTextButton(
        modifier = modifier,
        text = dateString,
        onClick = {
            showDateDialog(context, onDateSelected)
        }
    )
}

fun showDateDialog(
    context: Context,
    onDateSelected: (OffsetDateTime) -> Unit
) {
    val tag = "DatePicker"
    val fragmentManager = findFragmentManager(context) ?: return
    val fragment = fragmentManager.findFragmentByTag(tag)
    val existingPicker = fragment as? MaterialDatePicker<*>
    val picker = existingPicker ?: MaterialDatePicker.Builder.datePicker().build()
    picker.apply {
        addOnPositiveButtonClickListener { obj ->
            val epochMillis = (obj as? Long) ?: return@addOnPositiveButtonClickListener
            val instant = Instant.ofEpochMilli(epochMillis)
            val dateTime = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault())
            onDateSelected(dateTime)
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
