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
package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.helpers.formatDayMonthYearHourMinuteWeekDayOneLine
import com.samco.trackandgraph.base.helpers.getDisplayValue


@Composable
fun DataPointInfoDialog(
    dataPoint: DataPoint,
    isDuration: Boolean,
    weekdayNames: List<String>,
    onDismissRequest: () -> Unit
) = CustomDialog(onDismissRequest) {
    Text(
        formatDayMonthYearHourMinuteWeekDayOneLine(
            LocalContext.current,
            weekdayNames,
            dataPoint.timestamp
        ),
        fontSize = MaterialTheme.typography.h5.fontSize,
        fontWeight = MaterialTheme.typography.h5.fontWeight
    )
    SpacingSmall()
    Text(dataPoint.getDisplayValue(isDuration))
    Text(dataPoint.note)
}

@Composable
fun DataPointValueAndDescription(
    modifier: Modifier = Modifier,
    dataPoint: DataPoint,
    isDuration: Boolean,
    restrictNoteText: Boolean = true
) = Column(modifier = modifier) {
    Text(
        text = dataPoint.getDisplayValue(isDuration),
        fontSize = MaterialTheme.typography.subtitle2.fontSize,
        fontWeight = MaterialTheme.typography.subtitle2.fontWeight,
    )
    if (dataPoint.note.isNotEmpty()) {
        SpacingSmall()
        if (restrictNoteText) {
            Text(
                text = dataPoint.note,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.body2
            )
        } else {
            Text(
                text = dataPoint.note,
                style = MaterialTheme.typography.body2
            )
        }
    }
}

@Composable
fun FeatureInfoDialog(
    feature: Feature,
    onDismissRequest: () -> Unit
) = CustomDialog(onDismissRequest) {
    Text(
        feature.name,
        fontSize = MaterialTheme.typography.h5.fontSize,
        fontWeight = MaterialTheme.typography.h5.fontWeight
    )
    SpacingSmall()
    Text(feature.description)
}

