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

package com.samco.trackandgraph.reminders.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.SmallTextButton
import com.samco.trackandgraph.ui.compose.ui.TimeButton
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

@Composable
fun WeekDayReminderConfigurationScreen(
    editReminder: Reminder? = null,
    editParams: ReminderParams.WeekDayParams? = null,
    onUpsertReminder: (Reminder) -> Unit,
    onDismiss: () -> Unit,
    viewModel: WeekDayReminderConfigurationViewModel = hiltViewModel<WeekDayReminderConfigurationViewModelImpl>()
) {
    val reminderName by viewModel.reminderName.collectAsState()
    val selectedTime by viewModel.selectedTime.collectAsState()
    val checkedDays by viewModel.checkedDays.collectAsState()
    
    LaunchedEffect(editReminder, editParams) {
        viewModel.initializeFromReminder(editReminder, editParams)
    }
    
    WeekDayReminderConfigurationContent(
        reminderName = reminderName,
        onReminderNameChanged = viewModel::updateReminderName,
        selectedTime = selectedTime,
        onTimeSelected = viewModel::updateSelectedTime,
        checkedDays = checkedDays,
        onCheckedDaysChanged = viewModel::updateCheckedDays,
        isEditMode = editReminder != null,
        onConfirm = {
            onUpsertReminder(viewModel.getReminder())
        },
        onDismiss = onDismiss
    )
}

@Composable
fun WeekDayReminderConfigurationContent(
    reminderName: String,
    onReminderNameChanged: (String) -> Unit,
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    checkedDays: CheckedDays,
    onCheckedDaysChanged: (CheckedDays) -> Unit,
    isEditMode: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = "Configure Reminder",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.tngColors.onSurface
        )
        
        DialogInputSpacing()
        
        // Name field
        OutlinedTextField(
            value = reminderName,
            onValueChange = onReminderNameChanged,
            label = { Text("Reminder Name") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true
        )
        
        DialogInputSpacing()
        
        // Time selector
        Text(
            text = "Time",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.tngColors.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        TimeButton(
            modifier = Modifier.fillMaxWidth(),
            time = selectedTime,
            onTimeSelected = onTimeSelected
        )
        
        DialogInputSpacing()
        
        // Days checkboxes
        Text(
            text = "Days",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.tngColors.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        DayCheckboxes(
            checkedDays = checkedDays,
            onCheckedDaysChanged = onCheckedDaysChanged
        )
        
        DialogInputSpacing()
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SmallTextButton(
                stringRes = R.string.cancel,
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.tngColors.onSurface
                )
            )
            SmallTextButton(
                stringRes = if (isEditMode) R.string.update else R.string.add,
                onClick = onConfirm
            )
        }
    }
}

@Composable
fun DayCheckboxes(
    checkedDays: CheckedDays,
    onCheckedDaysChanged: (CheckedDays) -> Unit
) {
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dayValues = checkedDays.toList()

    Column {
        // First row: Mon-Thu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayNames.take(4).forEachIndexed { index, dayName ->
                DayCheckbox(
                    dayName = dayName,
                    isChecked = dayValues[index],
                    onCheckedChange = { isChecked ->
                        val newDays = CheckedDays.fromList(
                            dayValues.toMutableList().apply { set(index, isChecked) }
                        )
                        onCheckedDaysChanged(newDays)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Second row: Fri-Sun
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            dayNames.drop(4).forEachIndexed { relativeIndex, dayName ->
                val index = relativeIndex + 4
                DayCheckbox(
                    dayName = dayName,
                    isChecked = dayValues[index],
                    onCheckedChange = { isChecked ->
                        val newDays = CheckedDays.fromList(
                            dayValues.toMutableList().apply { set(index, isChecked) }
                        )
                        onCheckedDaysChanged(newDays)
                    }
                )
                if (relativeIndex < 2) {
                    Spacer(modifier = Modifier.width(32.dp))
                }
            }
        }
    }
}

@Composable
fun DayCheckbox(
    dayName: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = dayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.tngColors.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WeekDayReminderConfigurationContentPreview() {
    TnGComposeTheme {
        WeekDayReminderConfigurationContent(
            reminderName = "Morning Exercise",
            onReminderNameChanged = {},
            selectedTime = LocalTime.of(9, 0),
            onTimeSelected = {},
            checkedDays = CheckedDays.all(),
            onCheckedDaysChanged = {},
            isEditMode = false,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
