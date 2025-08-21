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
package com.samco.trackandgraph.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import com.samco.trackandgraph.R
import com.samco.trackandgraph.adddatapoint.AddDataPointsDialog
import com.samco.trackandgraph.adddatapoint.AddDataPointsNavigationViewModel
import com.samco.trackandgraph.adddatapoint.AddDataPointsViewModelImpl
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.DataPointNoteDescriptionDialog
import com.samco.trackandgraph.ui.compose.ui.DateDisplayResolution
import com.samco.trackandgraph.ui.compose.ui.DateScrollData
import com.samco.trackandgraph.ui.compose.ui.DateScrollLazyColumn
import com.samco.trackandgraph.ui.compose.ui.DayMonthYearHourMinuteWeekDayOneLineText
import com.samco.trackandgraph.ui.compose.ui.EmptyScreenText
import com.samco.trackandgraph.ui.compose.ui.GlobalNoteDescriptionDialog
import com.samco.trackandgraph.ui.compose.ui.HalfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.cardElevation
import com.samco.trackandgraph.ui.compose.ui.cardMarginSmall
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import kotlinx.serialization.Serializable
import org.threeten.bp.OffsetDateTime

@Serializable
data object NotesNavKey : NavKey

@Composable
fun NotesScreen(navArgs: NotesNavKey) {
    val notesViewModel: NotesViewModel = hiltViewModel()
    val addDataPointsDialogViewModel: AddDataPointsNavigationViewModel = hiltViewModel<AddDataPointsViewModelImpl>()
    val globalNoteDialogViewModel: GlobalNoteInputViewModel = hiltViewModel<GlobalNoteInputViewModelImpl>()

    val dateScrollData = notesViewModel.dateScrollData.observeAsState().value
    val showGlobalNoteDialog = globalNoteDialogViewModel.show.observeAsState(false).value
    val selectedNoteForDialog by notesViewModel.selectedNoteForDialog.collectAsState()

    TopAppBarContent()

    NotesView(
        dateScrollData = dateScrollData,
        onNoteClick = { note -> notesViewModel.onNoteClicked(note) },
        onEditClick = { note ->
            if (note.trackerId != null) {
                addDataPointsDialogViewModel.showAddDataPointDialog(
                    trackerId = note.trackerId,
                    dataPointTimestamp = note.date
                )
            } else {
                globalNoteDialogViewModel.openDialog(note.date)
            }
        },
        onDeleteClick = { note ->
            notesViewModel.deleteNote(note)
        }
    )

    selectedNoteForDialog?.let { note ->
        if (note.featureId == null) {
            GlobalNoteDescriptionDialog(
                timestamp = note.date,
                note = note.note,
                onDismissRequest = { notesViewModel.onDialogDismissed() }
            )
        } else {
            DataPointNoteDescriptionDialog(
                timestamp = note.date,
                displayValue = null,
                note = note.note,
                featureDisplayName = note.featurePath,
                onDismissRequest = { notesViewModel.onDialogDismissed() }
            )
        }
    }

    AddDataPointsDialog(
        viewModel = addDataPointsDialogViewModel,
        onDismissRequest = { addDataPointsDialogViewModel.reset() }
    )

    if (showGlobalNoteDialog) {
        GlobalNoteInputDialogView(viewModel = globalNoteDialogViewModel)
    }
}

@Composable
private fun TopAppBarContent() {
    val globalNoteDialogViewModel: GlobalNoteInputViewModel = hiltViewModel<GlobalNoteInputViewModelImpl>()
    val topBarController = LocalTopBarController.current
    val title = stringResource(R.string.notes)
    LaunchedEffect(title) {
        topBarController.set(
            AppBarConfig(
                title = title,
                actions = {
                    IconButton(onClick = { globalNoteDialogViewModel.openDialog(null) }) {
                        Icon(
                            painter = painterResource(R.drawable.add_icon),
                            contentDescription = null,
                            tint = MaterialTheme.tngColors.onSurface
                        )
                    }
                }
            )
        )
    }
}

@Composable
private fun NotesView(
    dateScrollData: DateScrollData<NoteInfo>?,
    onNoteClick: (NoteInfo) -> Unit,
    onEditClick: (NoteInfo) -> Unit,
    onDeleteClick: (NoteInfo) -> Unit
) = TnGComposeTheme {
    if (dateScrollData == null || dateScrollData.items.isEmpty()) {
        EmptyScreenText(textId = R.string.no_data_points_history_fragment_hint)
    } else {
        DateScrollLazyColumn(
            modifier = Modifier.padding(cardMarginSmall),
            contentPadding = WindowInsets.safeDrawing
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues(),
            data = dateScrollData
        ) { note ->
            Note(
                noteInfo = note,
                onNoteClick = { onNoteClick(note) },
                onEditClick = { onEditClick(note) },
                onDeleteClick = { onDeleteClick(note) }
            )
        }
    }
}

@Composable
private fun Note(
    noteInfo: NoteInfo,
    onNoteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) = Card(
    modifier = Modifier
        .padding(cardMarginSmall)
        .clickable { onNoteClick() },
    elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.small
) {

    var boxEnd by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { boxEnd = it.size.width }
    ) {
        var menuExpanded by remember { mutableStateOf(false) }

        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            IconButton(
                onClick = { menuExpanded = true }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.list_menu_icon),
                    contentDescription = stringResource(id = R.string.edit),
                    tint = MaterialTheme.tngColors.onSurface
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                offset = DpOffset(boxEnd.dp, 0.dp),
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.edit)) },
                    onClick = {
                        menuExpanded = false
                        onEditClick()
                    }
                )

                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.delete)) },
                    onClick = {
                        menuExpanded = false
                        onDeleteClick()
                    }
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(cardPadding)
        ) {
            DayMonthYearHourMinuteWeekDayOneLineText(
                dateTime = noteInfo.date,
                style = MaterialTheme.typography.titleSmall,
            )
            HalfDialogInputSpacing()
            if (noteInfo.featurePath.isNotBlank()) {
                Text(
                    style = MaterialTheme.typography.bodyLarge,
                    text = noteInfo.featurePath,
                    color = MaterialTheme.tngColors.onSurface.copy(alpha = 0.5f)
                )
                HalfDialogInputSpacing()
            }
            Text(
                style = MaterialTheme.typography.bodyLarge,
                text = noteInfo.note,
                maxLines = 3
            )
        }
    }
}

@Preview
@Composable
private fun NotesViewPreview() {
    val notes = listOf(
        NoteInfo(
            date = OffsetDateTime.parse("2023-01-01T10:00:00Z"),
            note = "This is a global note. It doesn't have a feature path.",
            featurePath = "",
            featureId = null,
            featureName = null,
            groupId = null,
            trackerId = null,
        ),
        NoteInfo(
            date = OffsetDateTime.parse("2023-01-02T12:30:00Z"),
            note = "This is a note for a data point. It has a feature path so we can see what it is for.",
            featurePath = "Health/Blood Pressure/Systolic",
            trackerId = 1,
            featureId = 2,
            featureName = "Systolic",
            groupId = 1,
        ),
        NoteInfo(
            date = OffsetDateTime.parse("2023-01-02T18:45:00Z"),
            note = "This is another note for a data point.",
            featurePath = "Finance/Spending/Groceries",
            trackerId = 2,
            featureId = null,
            featureName = null,
            groupId = null,
        ),
    )

    TnGComposeTheme {
        NotesView(
            dateScrollData = DateScrollData(
                dateDisplayResolution = DateDisplayResolution.MONTH_DAY,
                items = notes.reversed()
            ),
            onNoteClick = {},
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}