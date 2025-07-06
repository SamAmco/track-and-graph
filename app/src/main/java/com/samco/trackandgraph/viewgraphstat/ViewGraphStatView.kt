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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.theming.tngTypography

@Composable
fun ViewGraphStatScreen(
    viewModel: ViewGraphStatViewModel,
) {
    val graphStatViewData by viewModel.graphStatViewData.collectAsStateWithLifecycle(null)
    val showingNotes by viewModel.showingNotes.collectAsStateWithLifecycle(false)
    val markedNote by viewModel.markedNote.collectAsStateWithLifecycle(null)
    val notes by viewModel.notes.collectAsStateWithLifecycle(emptyList())

    ViewGraphStatView(
        graphStatViewData = graphStatViewData,
        showingNotes = showingNotes,
        markedNote = markedNote,
        notes = notes,
        showHideNotesClicked = viewModel::showHideNotesClicked,
        noteClicked = viewModel::noteClicked
    )
}

@Composable
private fun ViewGraphStatView(
    graphStatViewData: IGraphStatViewData?,
    showingNotes: Boolean,
    markedNote: GraphNote?,
    notes: List<GraphNote>,
    showHideNotesClicked: () -> Unit,
    noteClicked: (note: GraphNote) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        //TODO this code is wrong but it's something close

        // Use actual available height from parent container
        val availableHeight = maxHeight
        val buttonHeight = 56.dp // Approximate toggle button height
        val contentHeight = availableHeight - buttonHeight

        // Define proportions based on orientation
        val maxGraphHeightRatio = if (isPortrait) 0.77f else 0.7f
        val minGraphHeightRatio = if (isPortrait) 0.3f else 0f

        // Calculate target heights with min/max constraints based on available space
        val maxGraphHeight = (contentHeight * maxGraphHeightRatio).coerceAtLeast(200.dp)
        val minGraphHeight = if (isPortrait) {
            (contentHeight * minGraphHeightRatio).coerceAtLeast(150.dp)
        } else {
            0.dp // Can be 0 in landscape
        }

        val targetGraphHeight = if (showingNotes) minGraphHeight else maxGraphHeight
        val maxNotesHeight = contentHeight - minGraphHeight
        val targetNotesHeight = if (showingNotes) maxNotesHeight else 0.dp

        // Animate the heights
        val animatedGraphHeight by animateDpAsState(
            targetValue = targetGraphHeight,
            animationSpec = tween(durationMillis = 300),
            label = "graphHeight"
        )
        val animatedNotesHeight by animateDpAsState(
            targetValue = targetNotesHeight,
            animationSpec = tween(durationMillis = 300),
            label = "notesHeight"
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Graph Section - constrained height for readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedGraphHeight)
                    .background(MaterialTheme.colors.surface)
            ) {
                GraphViewStub(
                    graphStatViewData = graphStatViewData,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Notes Toggle Button - fixed height
            NotesToggleButton(
                showingNotes = showingNotes,
                onToggleClicked = showHideNotesClicked,
                modifier = Modifier.fillMaxWidth()
            )

            // Notes Section - remaining space with max constraint
            if (animatedNotesHeight > 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(animatedNotesHeight)
                        .background(MaterialTheme.colors.background)
                ) {
                    NotesListStub(
                        notes = notes,
                        markedNote = markedNote,
                        onNoteClicked = noteClicked,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun GraphViewStub(
    graphStatViewData: IGraphStatViewData?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Graph View Placeholder\n${graphStatViewData?.let { "Data: ${it.javaClass.simpleName}" } ?: "Loading..."}",
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface
        )
    }
}

@Composable
private fun NotesToggleButton(
    showingNotes: Boolean,
    onToggleClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onToggleClicked() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                contentDescription = if (showingNotes) "Hide notes" else "Show notes",
                tint = MaterialTheme.colors.onSurface
            )
        }
    }
}

@Composable
private fun NotesListStub(
    notes: List<GraphNote>,
    markedNote: GraphNote?,
    onNoteClicked: (GraphNote) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notes) { note ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNoteClicked(note) },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Note: ${note.timestamp}",
                        style = MaterialTheme.typography.body1
                    )
                    if (note.isDataPoint()) {
                        Text(
                            text = "Feature: ${note.featurePath ?: "Unknown"}",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface
                        )
                    }
                    if (markedNote == note) {
                        Text(
                            text = "★ Marked",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}