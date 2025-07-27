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

package com.samco.trackandgraph.viewgraphstat

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.ui.FullScreenGraphStatView
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.DayMonthYearHourMinuteWeekDayOneLineText
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.PopupTabBackground
import org.threeten.bp.OffsetDateTime

@Composable
fun ViewGraphStatScreen(
    viewModel: ViewGraphStatViewModel,
) {
    val graphStatViewData by viewModel.graphStatViewData.collectAsStateWithLifecycle(null)
    val showingNotes by viewModel.showingNotes.collectAsStateWithLifecycle(false)
    val timeMarker by viewModel.timeMarker.collectAsStateWithLifecycle(null)
    val notes by viewModel.notes.collectAsStateWithLifecycle(emptyList())

    ViewGraphStatView(
        graphStatViewData = graphStatViewData,
        showingNotes = showingNotes,
        timeMarker = timeMarker,
        notes = notes,
        showHideNotesClicked = viewModel::showHideNotesClicked,
        noteClicked = viewModel::noteClicked
    )
}

@Composable
private fun ViewGraphStatView(
    graphStatViewData: IGraphStatViewData?,
    showingNotes: Boolean,
    timeMarker: OffsetDateTime?,
    notes: List<GraphNote>,
    showHideNotesClicked: () -> Unit,
    noteClicked: (note: GraphNote) -> Unit,
) = TnGComposeTheme {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        val configuration = LocalConfiguration.current
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        val targetGraphHeight = remember(showingNotes, isPortrait) {
            val minGraphHeight = if (isPortrait) 0.3f else 0f
            if (showingNotes) minGraphHeight else 1f
        }
        val animatedGraphHeightRatio by animateFloatAsState(
            targetValue = targetGraphHeight,
            animationSpec = tween(durationMillis = 300),
            label = "graphHeightRatio"
        )

        if (animatedGraphHeightRatio > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(animatedGraphHeightRatio)
                    .background(MaterialTheme.colors.surface)
            ) {
                GraphStatView(
                    modifier = Modifier.fillMaxSize(),
                    graphStatViewData = graphStatViewData,
                    timeMarker = timeMarker,
                )
            }
        }

        NotesToggleButton(
            showingNotes = showingNotes,
            onToggleClicked = showHideNotesClicked,
            modifier = Modifier.fillMaxWidth()
        )

        if (animatedGraphHeightRatio < 1f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f - animatedGraphHeightRatio)
            ) {
                NotesList(
                    notes = notes,
                    onNoteClicked = noteClicked,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun GraphStatView(
    modifier: Modifier = Modifier,
    graphStatViewData: IGraphStatViewData?,
    timeMarker: OffsetDateTime?
) {
    if (graphStatViewData == null) {
        CircularProgressIndicator(
            modifier = modifier
        )
    } else {
        FullScreenGraphStatView(
            modifier = modifier,
            graphStatViewData = graphStatViewData,
            timeMarker = timeMarker,
        )
    }
}

@Composable
private fun NotesToggleButton(
    showingNotes: Boolean,
    onToggleClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleClicked() },
    ) {
        PopupTabBackground(
            modifier = Modifier.matchParentSize()
        )
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = cardPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.notes),
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(end = 8.dp)
            )

            Icon(
                painter = painterResource(
                    id = if (showingNotes) R.drawable.down_arrow else R.drawable.up_arrow
                ),
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface
            )
        }
    }
}

@Composable
private fun NotesList(
    notes: List<GraphNote>,
    onNoteClicked: (GraphNote) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(cardPadding),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing)
    ) {
        items(notes) { note ->
            NoteCard(
                note = note,
                onNoteClicked = onNoteClicked,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: GraphNote,
    onNoteClicked: (GraphNote) -> Unit,
    modifier: Modifier = Modifier
) {
    //TODO extract elevation and shape
    Card(
        modifier = modifier
            .clickable { onNoteClicked(note) },
        elevation = 4.dp,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(cardPadding)
        ) {
            // Top row: timestamp and feature path (for data point notes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DayMonthYearHourMinuteWeekDayOneLineText(note.timestamp)

                when (note) {
                    is GraphNote.DataPointNote -> {
                        Text(
                            text = note.featurePath,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = dialogInputSpacing)
                        )
                    }

                    is GraphNote.GlobalNote -> {
                        // No feature path for global notes
                    }
                }
            }

            // Middle row: display value (only for data point notes)
            when (note) {
                is GraphNote.DataPointNote -> {
                    Text(
                        text = note.displayValue,
                        style = MaterialTheme.typography.body2,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.End)
                    )
                }

                is GraphNote.GlobalNote -> {
                    // No display value for global notes
                }
            }

            // Bottom row: note text
            Text(
                text = note.noteText,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dialogInputSpacing)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ViewGraphStatScreenPreview() = ViewGraphStatView(
    graphStatViewData = null,
    showingNotes = true,
    timeMarker = null,
    notes = listOf(
        GraphNote.DataPointNote(
            timestamp = OffsetDateTime.parse("2025-07-25T10:30:00Z"),
            noteText = "Felt great today after morning workout! Really pushed myself on the deadlifts.",
            displayValue = "85.2 : Good",
            featurePath = "Health/Weight"
        ),
        GraphNote.GlobalNote(
            timestamp = OffsetDateTime.parse("2025-07-24T08:15:00Z"),
            noteText = "Started new diet plan"
        ),
        GraphNote.DataPointNote(
            timestamp = OffsetDateTime.parse("2025-07-23T19:45:00Z"),
            noteText = "Long day at work, stress eating kicked in unfortunately. Need to work on better coping strategies when deadlines approach.",
            displayValue = "2847 : Bad",
            featurePath = "Nutrition/Daily Calories"
        ),
        GraphNote.DataPointNote(
            timestamp = OffsetDateTime.parse("2025-07-21T07:00:00Z"),
            noteText = "Perfect sleep!",
            displayValue = "8:23:00 : Refreshed",
            featurePath = "Sleep/Duration"
        ),
        GraphNote.GlobalNote(
            timestamp = OffsetDateTime.parse("2025-07-19T06:00:00Z"),
            noteText = "Switched to morning workouts instead of evening - let's see how this affects my energy levels throughout the week."
        )
    ),
    showHideNotesClicked = {},
    noteClicked = {},
)