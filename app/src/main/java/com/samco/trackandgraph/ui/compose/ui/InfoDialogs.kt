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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.helpers.getDisplayValue
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
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
    Text(
        text = dataPoint.getDisplayValue(isDuration),
        style = MaterialTheme.typography.body1,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = dataPoint.note,
        style = MaterialTheme.typography.body1
    )
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
    featureName: String,
    featureDescription: String,
    onDismissRequest: () -> Unit
) = CustomDialog(onDismissRequest) {
    Text(
        text = featureName,
        style = MaterialTheme.typography.h5,
        fontWeight = FontWeight.Bold
    )
    DialogInputSpacing()
    Text(
        text = featureDescription.ifEmpty { stringResource(R.string.no_description) },
        style = MaterialTheme.typography.body1
    )
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
        style = MaterialTheme.typography.h5,
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
    displayValue: String? = null,
    note: String,
    featureDisplayName: String,
    onDismissRequest: () -> Unit
) = CustomDialog(onDismissRequest) {
    // Header with timestamp
    DayMonthYearHourMinuteWeekDayOneLineText(
        dateTime = timestamp,
        style = MaterialTheme.typography.h5,
        fontWeight = FontWeight.Bold
    )

    HalfDialogInputSpacing()

    // Feature display name
    Text(
        text = featureDisplayName,
        style = MaterialTheme.typography.body1,
        fontStyle = FontStyle.Italic,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    // Display value
    if (displayValue != null) {
        HalfDialogInputSpacing()
        Text(
            text = displayValue,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    DialogInputSpacing()

    // Note text (scrollable body)
    Text(
        text = note,
        style = MaterialTheme.typography.body1
    )
}

@Preview
@Composable
private fun DataPointInfoDialogPreview() = TnGComposeTheme {
    val sampleDataPoint = DataPoint(
        timestamp = OffsetDateTime.parse("2024-01-15T14:30:00+00:00"),
        featureId = 1,
        value = 75.5,
        label = "",
        note = "Felt great after morning run! Weather was perfect and I maintained a good pace throughout."
    )

    DataPointInfoDialog(
        dataPoint = sampleDataPoint,
        isDuration = false,
        onDismissRequest = {}
    )
}

@Preview
@Composable
private fun DataPointInfoDialogDurationPreview() = TnGComposeTheme {
    val sampleDataPoint = DataPoint(
        timestamp = OffsetDateTime.parse("2024-01-15T09:15:00+00:00"),
        featureId = 2,
        value = 3725.0, // 1 hour 2 minutes 5 seconds
        label = "",
        note = "Morning workout session with strength training and cardio"
    )

    DataPointInfoDialog(
        dataPoint = sampleDataPoint,
        isDuration = true,
        onDismissRequest = {}
    )
}

@Preview
@Composable
private fun DataPointValueAndDescriptionPreview() = TnGComposeTheme {
    val sampleDataPoint = DataPoint(
        timestamp = OffsetDateTime.parse("2024-02-10T16:45:00+00:00"),
        featureId = 1,
        value = 8.5,
        label = "Good",
        note = "Good session today, focused on technique and form. Made significant progress on challenging routes."
    )

    DataPointValueAndDescription(
        dataPoint = sampleDataPoint,
        isDuration = false,
        restrictNoteText = true
    )
}

@Preview
@Composable
private fun DataPointValueAndDescriptionUnrestrictedPreview() = TnGComposeTheme {
    val sampleDataPoint = DataPoint(
        timestamp = OffsetDateTime.parse("2024-03-05T11:20:00+00:00"),
        featureId = 1,
        value = 42.5,
        label = "Excellent",
        note = "This is a very long note that would normally be truncated with restrictNoteText=true, but since we're showing the unrestricted version, all of this text should be visible. This is useful for full dialogs where we want to show the complete note content without ellipsis truncation."
    )

    DataPointValueAndDescription(
        dataPoint = sampleDataPoint,
        isDuration = false,
        restrictNoteText = false
    )
}

@Preview
@Composable
private fun DataPointValueAndDescriptionEmptyNotePreview() = TnGComposeTheme {
    val sampleDataPoint = DataPoint(
        timestamp = OffsetDateTime.parse("2024-04-12T08:00:00+00:00"),
        featureId = 1,
        value = 15.2,
        label = "Average",
        note = ""
    )

    DataPointValueAndDescription(
        dataPoint = sampleDataPoint,
        isDuration = false
    )
}

@Preview
@Composable
private fun FeatureInfoDialogPreview() = TnGComposeTheme {
    FeatureInfoDialog(
        featureName = "Daily Exercise",
        featureDescription = "Track daily exercise duration and intensity. This helps monitor fitness progress and maintain consistency in workout routines.",
        onDismissRequest = {}
    )
}

@Preview
@Composable
private fun FeatureInfoDialogEmptyDescriptionPreview() = TnGComposeTheme {
    FeatureInfoDialog(
        featureName = "Weight",
        featureDescription = "",
        onDismissRequest = {}
    )
}

@Preview
@Composable
private fun GlobalNoteDescriptionDialogPreview() = TnGComposeTheme {
    GlobalNoteDescriptionDialog(
        timestamp = OffsetDateTime.parse("2024-06-18T19:30:00+00:00"),
        note = "Had a great day today! Started with meditation, then went for a long hike in the mountains. The weather was perfect and I felt really connected with nature. Planning to make this a regular weekend activity.",
        onDismissRequest = {}
    )
}

@Preview
@Composable
private fun GlobalNoteDescriptionDialogShortNotePreview() = TnGComposeTheme {
    GlobalNoteDescriptionDialog(
        timestamp = OffsetDateTime.parse("2024-07-22T12:15:00+00:00"),
        note = "Quick note",
        onDismissRequest = {}
    )
}

@Preview
@Composable
private fun DataPointNoteDescriptionDialogPreview() = TnGComposeTheme {
    DataPointNoteDescriptionDialog(
        timestamp = OffsetDateTime.parse("2024-05-10T17:45:00+00:00"),
        displayValue = "85.3 : Kilos",
        note = "Weighed myself after dinner, so probably a bit higher than usual. Been focusing on strength training lately and feeling stronger overall.",
        featureDisplayName = "/Health/Body Weight",
        onDismissRequest = {}
    )
}

@Preview
@Composable
private fun DataPointNoteDescriptionDialogNoValuePreview() = TnGComposeTheme {
    DataPointNoteDescriptionDialog(
        timestamp = OffsetDateTime.parse("2024-08-03T21:00:00+00:00"),
        displayValue = null,
        note = "Completed today's meditation session. Felt very centered and calm afterwards. The breathing techniques are really helping with stress management.",
        featureDisplayName = "/Habits/Daily Meditation",
        onDismissRequest = {}
    )
}
