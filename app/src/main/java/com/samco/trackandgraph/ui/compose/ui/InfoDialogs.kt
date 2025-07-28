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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.helpers.getDisplayValue
import org.threeten.bp.OffsetDateTime

@Composable
fun DataPointInfoDialog(
    dataPoint: DataPoint,
    isDuration: Boolean,
    onDismissRequest: () -> Unit
) = CustomDialog(onDismissRequest) {
    DayMonthYearHourMinuteWeekDayOneLineText(
        dateTime = dataPoint.timestamp,
        style = MaterialTheme.typography.h5,
        fontWeight = FontWeight.Bold
    )
    DialogInputSpacing()
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
        DialogInputSpacing()
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
    DialogInputSpacing()
    Text(feature.description)
}

@Composable
fun GlobalNoteDescriptionDialog(
    timestamp: OffsetDateTime,
    note: String,
    onDismissRequest: () -> Unit
) = CustomDialog(onDismissRequest) {
    // Header with timestamp
    DayMonthYearHourMinuteWeekDayOneLineText(
        dateTime = timestamp,
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Bold
    )
    
    DialogInputSpacing()
    
    // Note text (scrollable body)
    Text(
        text = note,
        style = MaterialTheme.typography.body1
    )
}

@Composable
fun DataPointNoteDescriptionDialog(
    timestamp: OffsetDateTime,
    displayValue: String,
    note: String,
    featureDisplayName: String,
    onDismissRequest: () -> Unit
) = CustomDialog(onDismissRequest) {
    // Header with timestamp
    DayMonthYearHourMinuteWeekDayOneLineText(
        dateTime = timestamp,
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Bold
    )
    
    // Feature display name
    Text(
        text = featureDisplayName,
        style = MaterialTheme.typography.body1,
        fontStyle = FontStyle.Italic,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    
    // Display value
    Text(
        text = displayValue,
        style = MaterialTheme.typography.body1,
        fontStyle = FontStyle.Italic,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    
    DialogInputSpacing()
    
    // Note text (scrollable body)
    Text(
        text = note,
        style = MaterialTheme.typography.body1
    )
}
