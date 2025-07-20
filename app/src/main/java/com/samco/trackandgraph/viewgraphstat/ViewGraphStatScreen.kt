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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.PopupTabBackground
import com.samco.trackandgraph.ui.compose.ui.cardPadding

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(animatedGraphHeightRatio)
                .background(MaterialTheme.colors.surface)
        ) {
            GraphViewStub(
                graphStatViewData = graphStatViewData,
                modifier = Modifier.fillMaxSize()
            )
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

@Preview(showBackground = true)
@Composable
private fun ViewGraphStatScreenPreview() = ViewGraphStatView(
    graphStatViewData = null,
    showingNotes = true,
    markedNote = null,
    notes = listOf(

    ),
    showHideNotesClicked = {},
    noteClicked = {},
)