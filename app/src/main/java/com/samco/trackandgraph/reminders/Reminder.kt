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

package com.samco.trackandgraph.reminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.CheckedDays
import com.samco.trackandgraph.base.database.dto.CheckedDays.Companion.withSet
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.cardElevation
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.showTimePickerDialog
import com.samco.trackandgraph.ui.compose.ui.slimOutlinedTextField
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter

@Composable
fun Reminder(
    modifier: Modifier = Modifier,
    isElevated: Boolean = false,
    reminderViewData: ReminderViewData,
    onDeleteClick: () -> Unit
) = Card(
    modifier = modifier
        .fillMaxWidth()
        .padding(halfDialogInputSpacing),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isElevated) cardElevation * 3f else cardElevation),
    shape = MaterialTheme.shapes.small,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        ReminderTopRow(
            reminderViewData = reminderViewData,
            onDeleteClick = onDeleteClick
        )

        ReminderDayCheckboxes(
            reminderViewData = reminderViewData
        )

        ReminderDayLabels()
    }
}

@Composable
private fun ReminderTopRow(
    reminderViewData: ReminderViewData,
    onDeleteClick: () -> Unit
) {
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReminderNameInput(reminderViewData = reminderViewData)

        ReminderTimeDisplay(
            reminderViewData = reminderViewData,
            timeFormatter = timeFormatter,
        )

        ReminderDeleteButton(onDeleteClick = onDeleteClick)
    }
}

@Composable
private fun RowScope.ReminderNameInput(
    reminderViewData: ReminderViewData,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        modifier = Modifier
            .weight(1f)
            .slimOutlinedTextField()
            .padding(top = cardPadding),
        value = reminderViewData.name.value,
        onValueChange = { reminderViewData.name.value = it },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            capitalization = KeyboardCapitalization.Sentences
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        )
    )
}

@Composable
private fun ReminderTimeDisplay(
    modifier: Modifier = Modifier,
    reminderViewData: ReminderViewData,
    timeFormatter: DateTimeFormatter,
) {
    val context = LocalContext.current

    Text(
        modifier = modifier
            .clickable {
                showTimePickerDialog(
                    context = context,
                    hour = reminderViewData.time.value.hour,
                    minute = reminderViewData.time.value.minute,
                    onTimeSelected = {
                        reminderViewData.time.value = LocalTime.of(it.hour, it.minute)
                    }
                )
            }
            .padding(
                start = dialogInputSpacing,
                end = dialogInputSpacing,
                top = cardPadding
            ),
        text = reminderViewData.time.value.format(timeFormatter),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ReminderDeleteButton(
    modifier: Modifier = Modifier,
    onDeleteClick: () -> Unit
) {
    IconButton(
        modifier = modifier
            .size(buttonSize)
            .padding(
                top = cardPadding,
                end = cardPadding
            ),
        onClick = onDeleteClick
    ) {
        Icon(
            painter = painterResource(id = R.drawable.delete_icon),
            contentDescription = stringResource(id = R.string.delete_reminder_content_description),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReminderDayCheckboxes(
    reminderViewData: ReminderViewData
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = cardPadding,
                end = cardPadding,
                top = dialogInputSpacing,
            ),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val checkedDays = reminderViewData.checkedDays.value.toList()

        checkedDays.forEachIndexed { index, isChecked ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { newChecked ->
                        reminderViewData.checkedDays.value = reminderViewData.checkedDays.value.withSet(index, newChecked)
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderDayLabels() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = cardPadding,
                end = cardPadding,
                bottom = cardPadding
            ),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val dayLabels = listOf(
            stringResource(id = R.string.mon),
            stringResource(id = R.string.tue),
            stringResource(id = R.string.wed),
            stringResource(id = R.string.thu),
            stringResource(id = R.string.fri),
            stringResource(id = R.string.sat),
            stringResource(id = R.string.sun)
        )

        dayLabels.forEach { label ->
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview(
    showBackground = true)
@Composable
private fun ReminderPreview() = TnGComposeTheme {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Regular reminder
        Reminder(
            reminderViewData = ReminderViewData(
                id = 1L,
                displayIndex = 0,
                name = remember { mutableStateOf(TextFieldValue("Morning Workout")) },
                time = remember { mutableStateOf(LocalTime.of(7, 30)) },
                checkedDays = remember {
                    mutableStateOf(
                        CheckedDays(
                            monday = true,
                            tuesday = true,
                            wednesday = true,
                            thursday = true,
                            friday = true,
                            saturday = false,
                            sunday = false
                        )
                    )
                }
            ),
            onDeleteClick = {}
        )

        // Elevated reminder
        Reminder(
            isElevated = true,
            reminderViewData = ReminderViewData(
                id = 2L,
                displayIndex = 1,
                name = remember { mutableStateOf(TextFieldValue("Evening Meditation")) },
                time = remember { mutableStateOf(LocalTime.of(21, 0)) },
                checkedDays = remember { mutableStateOf(CheckedDays.all()) }
            ),
            onDeleteClick = {}
        )
    }
}
