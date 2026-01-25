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

@file:OptIn(ExperimentalFoundationApi::class)

package com.samco.trackandgraph.reminders.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.ui.ScaledStaticChip
import com.samco.trackandgraph.ui.compose.ui.cardPadding

@Composable
fun WeekDayReminderDetails(
    reminderViewData: ReminderViewData.WeekDayReminderViewData,
    modifier: Modifier = Modifier,
    chipScale: Float = 0.65f
) = Column(
    modifier = modifier,
    horizontalAlignment = Alignment.Start,
) {
    val dayNames = listOf(
        stringResource(id = R.string.mon),
        stringResource(id = R.string.tue),
        stringResource(id = R.string.wed),
        stringResource(id = R.string.thu),
        stringResource(id = R.string.fri),
        stringResource(id = R.string.sat),
        stringResource(id = R.string.sun)
    )

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((cardPadding * chipScale), Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy((cardPadding * chipScale)),
    ) {
        for (i in 0..6) {
            ScaledStaticChip(
                text = dayNames[i].uppercase(),
                isSelected = reminderViewData.checkedDays[i],
                scale = chipScale
            )
        }
    }
}

