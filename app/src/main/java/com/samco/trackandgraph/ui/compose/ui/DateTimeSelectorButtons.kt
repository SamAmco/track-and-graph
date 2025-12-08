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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.helpers.formatDayMonthYear
import com.samco.trackandgraph.helpers.formatHourMinute
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

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
        time = selectedDateTime.toLocalTime(),
        onTimeSelected = { time ->
            onDateTimeSelected(
                selectedDateTime
                    .withHour(time.hour)
                    .withMinute(time.minute)
            )
        }
    )
}

@Composable
fun DateButton(
    modifier: Modifier = Modifier,
    dateTime: OffsetDateTime,
    enabled: Boolean = true,
    allowPastDates: Boolean = true,
    onDateSelected: (OffsetDateTime) -> Unit,
) {
    val context = LocalContext.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    SelectorButton(
        modifier = modifier,
        text = formatDayMonthYear(context, dateTime),
        enabled = enabled,
        onClick = { showDatePicker = true }
    )

    if (showDatePicker) {
        DatePickerDialogContent(
            initialDateTime = dateTime,
            onDismissRequest = { showDatePicker = false },
            allowPastDates = allowPastDates,
            onDateSelected = { selectedDate ->
                onDateSelected(selectedDate)
                showDatePicker = false
            }
        )
    }
}


@Composable
fun TimeButton(
    modifier: Modifier = Modifier,
    time: LocalTime,
    enabled: Boolean = true,
    onTimeSelected: (LocalTime) -> Unit
) {
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    SelectorButton(
        modifier = modifier,
        text = formatHourMinute(time),
        enabled = enabled,
        onClick = { showTimePicker = true }
    )

    if (showTimePicker) {
        TimePickerDialogContent(
            initialHour = time.hour,
            initialMinute = time.minute,
            onCancel = { showTimePicker = false },
            onConfirm = { selectedTime ->
                onTimeSelected(selectedTime)
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogContent(
    initialHour: Int = 14,
    initialMinute: Int = 30,
    onCancel: () -> Unit = {},
    onConfirm: (LocalTime) -> Unit = {}
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    CustomContinueCancelDialog(
        onDismissRequest = onCancel,
        onConfirm = { onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute)) },
        continueText = R.string.ok,
        cancelText = R.string.cancel,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
        content = {
            InputSpacingLarge()
            TimePicker(
                modifier = Modifier.fillMaxWidth(),
                state = timePickerState
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialogContent(
    initialDateTime: OffsetDateTime,
    allowPastDates: Boolean = true,
    onDismissRequest: () -> Unit = {},
    onDateSelected: (OffsetDateTime) -> Unit = {}
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateTime.toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (allowPastDates) return true
                return utcTimeMillis >= LocalDateTime.now()
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            }

            override fun isSelectableYear(year: Int): Boolean {
                if (allowPastDates) return true
                return year >= LocalDate.now().year
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val utcDate = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC)
                        val selected = utcDate.withOffsetSameLocal(initialDateTime.offset)
                        onDateSelected(selected)
                    }
                    onDismissRequest()
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(
                    text = stringResource(android.R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        content = { DatePicker(state = datePickerState) }
    )
}

@Preview
@Composable
fun DateTimeButtonRowPreview() {
    TnGComposeTheme {
        DateTimeButtonRow(
            selectedDateTime = OffsetDateTime.parse("2023-06-15T14:30:00+01:00"),
            onDateTimeSelected = { }
        )
    }
}

@Preview
@Composable
fun TimePickerDialogPreview() {
    TnGComposeTheme {
        TimePickerDialogContent()
    }
}

@Preview
@Composable
fun DatePickerDialogPreview() {
    TnGComposeTheme {
        DatePickerDialogContent(OffsetDateTime.parse("2023-06-15T14:30:00+01:00"))
    }
}