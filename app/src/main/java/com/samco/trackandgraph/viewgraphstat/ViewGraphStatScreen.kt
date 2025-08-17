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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.DataPointNoteDescriptionDialog
import com.samco.trackandgraph.ui.compose.ui.DayMonthYearHourMinuteWeekDayOneLineText
import com.samco.trackandgraph.ui.compose.ui.GlobalNoteDescriptionDialog
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.cardElevation
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.shapeLarge
import org.threeten.bp.OffsetDateTime

@Composable
fun ViewGraphStatScreen(
    viewModel: ViewGraphStatViewModel,
) {
    val graphStatViewData by viewModel.graphStatViewData.collectAsStateWithLifecycle(null)
    val showingNotes by viewModel.showingNotes.collectAsStateWithLifecycle(false)
    val timeMarker by viewModel.timeMarker.collectAsStateWithLifecycle(null)
    val notes by viewModel.notes.collectAsStateWithLifecycle(emptyList())
    val selectedNoteForDialog by viewModel.selectedNoteForDialog.collectAsStateWithLifecycle(null)

    ViewGraphStatView(
        graphStatViewData = graphStatViewData,
        showingNotes = showingNotes,
        timeMarker = timeMarker,
        notes = notes,
        selectedNoteForDialog = selectedNoteForDialog,
        showHideNotesClicked = viewModel::showHideNotesClicked,
        setNotesVisibility = viewModel::setNotesVisibility,
        noteClicked = viewModel::noteClicked,
        dismissNoteDialog = viewModel::dismissNoteDialog
    )
}

@Composable
private fun ViewGraphStatView(
    graphStatViewData: IGraphStatViewData?,
    showingNotes: Boolean,
    timeMarker: OffsetDateTime?,
    notes: List<GraphNote>,
    selectedNoteForDialog: GraphNote?,
    showHideNotesClicked: () -> Unit,
    setNotesVisibility: (Boolean) -> Unit,
    noteClicked: (note: GraphNote) -> Unit,
    dismissNoteDialog: () -> Unit,
) = TnGComposeTheme {
    BoxWithConstraints {
        val configuration = LocalConfiguration.current
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val density = LocalDensity.current
        val containerHeight = maxHeight

        // Calculate container height in pixels once and remember it
        val containerHeightPx = remember(containerHeight, density) {
            with(density) { containerHeight.toPx() }
        }

        // Store the user's preferred notes position (when notes are visible)
        val defaultNotesPosition = if (isPortrait) 0.35f else 0f
        var savedGraphWeight by rememberSaveable { mutableFloatStateOf(defaultNotesPosition) }
        var currentGraphWeight by remember { mutableFloatStateOf(if (showingNotes) savedGraphWeight else 1f) }

        // Track whether we're currently dragging
        var isDraggingNotesButton by remember { mutableStateOf(false) }
        var justTappedNotesButton by remember { mutableStateOf(false) }

        LaunchedEffect(showingNotes, isDraggingNotesButton, justTappedNotesButton) {
            // If the user is dragging, or the current graph weight and the saved graph weight are very close
            // (meaning the user just finished dragging), don't animate
            if (isDraggingNotesButton || !justTappedNotesButton) return@LaunchedEffect

            val duration = 300
            val animationSpec = tween<Float>(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
            val initialValue = if (showingNotes) 1f else currentGraphWeight
            val targetValue = if (showingNotes) savedGraphWeight else 1f

            animate(
                initialValue = initialValue,
                targetValue = targetValue,
                animationSpec = animationSpec
            ) { value, _ ->
                currentGraphWeight = value
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (currentGraphWeight > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(currentGraphWeight)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    GraphStatView(
                        modifier = Modifier.fillMaxSize(),
                        graphStatViewData = graphStatViewData,
                        timeMarker = timeMarker,
                    )
                }
            }

            if (notes.isNotEmpty()) {
                NotesToggleButton(
                    modifier = Modifier.fillMaxWidth(),
                    showingNotes = showingNotes,
                    onToggleClicked = {
                        justTappedNotesButton = true
                        showHideNotesClicked()
                    },
                    onDrag = { dragOffset ->
                        justTappedNotesButton = false
                        val weightChange = -dragOffset.y / containerHeightPx
                        val newWeight = (currentGraphWeight - weightChange)

                        // Update saved position and current weight
                        savedGraphWeight = newWeight
                        currentGraphWeight = newWeight
                        setNotesVisibility(true)
                    },
                    onDraggingChanged = { isDraggingNotesButton = it },
                )
            }

            if (currentGraphWeight < 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f - currentGraphWeight)
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

    // Handle dialog display
    selectedNoteForDialog?.let { note ->
        if (note is GraphNote.GlobalNote) {
            GlobalNoteDescriptionDialog(
                timestamp = note.timestamp,
                note = note.noteText,
                onDismissRequest = dismissNoteDialog
            )
        } else if (note is GraphNote.DataPointNote) {
            DataPointNoteDescriptionDialog(
                timestamp = note.timestamp,
                displayValue = note.displayValue,
                note = note.noteText,
                featureDisplayName = note.featurePath,
                onDismissRequest = dismissNoteDialog
            )
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
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        FullScreenGraphStatView(
            modifier = modifier,
            graphStatViewData = graphStatViewData,
            timeMarker = timeMarker,
        )
    }
}

suspend fun PointerInputScope.detectTapAndDragGestures(
    onTap: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDraggingChanged: (Boolean) -> Unit = {}
) {
    awaitEachGesture {
        val down = awaitFirstDown()
        var dragStarted = false

        val drag = awaitTouchSlopOrCancellation(down.id) { change, _ ->
            dragStarted = true
            onDraggingChanged(true)
            change.consume()
        }

        if (drag != null && dragStarted) {
            // Drag started
            drag(drag.id) { change ->
                onDrag(change.positionChange())
                change.consume()
            }
            // Drag ended
            onDraggingChanged(false)
        } else if (!dragStarted) {
            onTap()
        }
    }
}

@Composable
private fun PopupTabBackground(
    modifier: Modifier = Modifier,
    showingNotes: Boolean,
    content: @Composable BoxScope.() -> Unit
) = Box(
    modifier = modifier
        .background(
            color = MaterialTheme.tngColors.selectorButtonColor,
            shape = RoundedCornerShape(
                topStart = shapeLarge,
                topEnd = shapeLarge,
                bottomStart = if (showingNotes) shapeLarge else 0.dp,
                bottomEnd = if (showingNotes) shapeLarge else 0.dp
            )
        ),
    content = content
)

@Composable
private fun NotesToggleButton(
    showingNotes: Boolean,
    onToggleClicked: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDraggingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) = PopupTabBackground(
    showingNotes = showingNotes,
    modifier = modifier
        .fillMaxWidth()
        .pointerInput(Unit) {
            detectTapAndDragGestures(
                onTap = onToggleClicked,
                onDrag = onDrag,
                onDraggingChanged = onDraggingChanged
            )
        },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = halfDialogInputSpacing, bottom = halfDialogInputSpacing),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.notes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(end = 8.dp)
        )

        Icon(
            painter = painterResource(
                id = if (showingNotes) R.drawable.down_arrow else R.drawable.up_arrow
            ),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NotesList(
    notes: List<GraphNote>,
    onNoteClicked: (GraphNote) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(cardPadding),
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
    Card(
        modifier = modifier
            .clickable { onNoteClicked(note) },
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.small,
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
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
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface,
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
    setNotesVisibility = {},
    noteClicked = {},
    selectedNoteForDialog = null,
    dismissNoteDialog = {},
)
