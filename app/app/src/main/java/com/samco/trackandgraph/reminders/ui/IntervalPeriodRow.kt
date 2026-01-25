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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing

@Composable
fun IntervalPeriodRow(
    interval: String,
    onIntervalChanged: (String) -> Unit,
    period: Period,
    onPeriodChanged: (Period) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dialogInputSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Interval number input
        OutlinedTextField(
            value = interval,
            onValueChange = onIntervalChanged,
            modifier = Modifier
                .weight(0.4f)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        // If the user enters a decimal floor it to the nearest int
                        val numericValue = interval.toDoubleOrNull()?.toInt() ?: 1
                        val coercedValue = numericValue.coerceAtLeast(1)
                        if (coercedValue.toString() != interval) {
                            onIntervalChanged(coercedValue.toString())
                        }
                    }
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        // Period selector
        TextMapSpinner(
            strings = mapOf(
                Period.MINUTES to stringResource(id = R.string.minutes_generic),
                Period.HOURS to stringResource(id = R.string.hours_generic),
                Period.DAYS to stringResource(id = R.string.days_generic),
                Period.WEEKS to stringResource(id = R.string.weeks_generic),
                Period.MONTHS to stringResource(id = R.string.months_generic),
                Period.YEARS to stringResource(id = R.string.years_generic)
            ),
            selectedItem = period,
            onItemSelected = onPeriodChanged,
            modifier = Modifier.weight(1f)
        )
    }
}
