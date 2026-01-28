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
package com.samco.trackandgraph.featurehistory

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.map
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.R
import com.samco.trackandgraph.adddatapoint.AddDataPointsDialog
import com.samco.trackandgraph.adddatapoint.AddDataPointsNavigationViewModel
import com.samco.trackandgraph.adddatapoint.AddDataPointsViewModelImpl
import com.samco.trackandgraph.data.lua.dto.LuaEngineDisabledException
import com.samco.trackandgraph.helpers.formatDayMonthYearHourMinuteWeekDayTwoLines
import com.samco.trackandgraph.helpers.getWeekDayNames
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.DataPointInfoDialog
import com.samco.trackandgraph.ui.compose.ui.DataPointValueAndDescription
import com.samco.trackandgraph.ui.compose.ui.DateDisplayResolution
import com.samco.trackandgraph.ui.compose.ui.DateScrollData
import com.samco.trackandgraph.ui.compose.ui.DateScrollLazyColumn
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.EmptyScreenText
import com.samco.trackandgraph.ui.compose.ui.FeatureInfoDialog
import com.samco.trackandgraph.ui.compose.ui.LoadingOverlay
import com.samco.trackandgraph.ui.compose.ui.cardMarginSmall
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import kotlinx.serialization.Serializable
import org.threeten.bp.OffsetDateTime

@Serializable
data class FeatureHistoryNavKey(
    val featureId: Long,
    val featureName: String
) : NavKey

@Composable
fun FeatureHistoryScreen(navArgs: FeatureHistoryNavKey) {
    val viewModel: FeatureHistoryViewModel = hiltViewModel<FeatureHistoryViewModelImpl>()
    val addDataPointsDialogViewModel: AddDataPointsNavigationViewModel =
        hiltViewModel<AddDataPointsViewModelImpl>()

    // Initialize ViewModel with the featureId from NavKey
    LaunchedEffect(navArgs.featureId) {
        viewModel.initViewModel(navArgs.featureId)
    }

    // Collect all state from ViewModel
    val dateScrollData by viewModel.dateScrollData.observeAsState()
    val isDuration by viewModel.isDuration.observeAsState(false)
    val isTracker by viewModel.tracker.map { it != null }.observeAsState(false)
    val featureInfo by viewModel.showFeatureInfo.observeAsState()
    val dataPointInfo by viewModel.showDataPointInfo.observeAsState()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedDataPoints by viewModel.selectedDataPoints.collectAsStateWithLifecycle()
    val showDeleteConfirmDialog by viewModel.showDeleteConfirmDialog.observeAsState(false)
    val showDeleteSelectedConfirmDialog by viewModel.showDeleteSelectedConfirmDialog.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.observeAsState(false)
    val showUpdateWarning by viewModel.showUpdateWarning.observeAsState(false)
    val isUpdating by viewModel.isUpdating.observeAsState(false)

    TopAppBarContent(
        navArgs = navArgs,
        featureName = navArgs.featureName,
        dataPointsCount = dateScrollData?.items?.size ?: 0,
        isTracker = isTracker,
        isMultiSelectMode = isMultiSelectMode,
        selectedCount = selectedDataPoints.size,
        onInfoClick = viewModel::onShowFeatureInfo,
        onUpdateClick = viewModel::showUpdateAllDialog,
        onExitMultiSelect = viewModel::exitMultiSelectMode
    )

    FeatureHistoryView(
        dateScrollData = dateScrollData,
        isDuration = isDuration,
        isTracker = isTracker,
        isMultiSelectMode = isMultiSelectMode,
        selectedDataPoints = selectedDataPoints,
        errorMessage = when {
            error is LuaEngineDisabledException -> stringResource(R.string.lua_engine_disabled)
            error != null -> error?.message ?: ""
            else -> null
        },
        onDataPointClick = viewModel::onDataPointClicked,
        onDataPointLongPress = viewModel::onDataPointLongPressed,
        onDataPointSelected = viewModel::onDataPointSelected,
        onEditClick = { dataPoint ->
            viewModel.tracker.value?.let { tracker ->
                addDataPointsDialogViewModel.showAddDataPointDialog(
                    trackerId = tracker.id,
                    dataPointTimestamp = dataPoint.date
                )
            }
        },
        onDeleteClick = viewModel::onDeleteClicked,
        onDeleteSelectedClick = viewModel::onDeleteSelectedClicked
    )

    // Dialogs
    featureInfo?.let {
        FeatureInfoDialog(
            featureName = it.name,
            featureDescription = it.description,
            onDismissRequest = viewModel::onHideFeatureInfo
        )
    }

    dataPointInfo?.let {
        DataPointInfoDialog(
            dataPoint = it.toDataPoint(),
            isDuration = isDuration,
            onDismissRequest = viewModel::onDismissDataPoint
        )
    }

    if (showDeleteConfirmDialog) {
        ContinueCancelDialog(
            body = R.string.ru_sure_del_data_point,
            onDismissRequest = viewModel::onDeleteDismissed,
            onConfirm = viewModel::onDeleteConfirmed
        )
    }

    if (showDeleteSelectedConfirmDialog) {
        ContinueCancelDialog(
            body = R.string.ru_sure_del_data_points,
            onDismissRequest = viewModel::onDeleteSelectedDismissed,
            onConfirm = viewModel::onDeleteSelectedConfirmed
        )
    }

    if (showUpdateDialog) {
        UpdateDialog(viewModel = viewModel)
    }

    if (showUpdateWarning) {
        UpdateWarningDialog(
            onDismissRequest = viewModel::onCancelUpdateWarning,
            onConfirm = viewModel::onConfirmUpdateWarning
        )
    }

    if (isUpdating) {
        LoadingOverlay()
    }

    if (!addDataPointsDialogViewModel.hidden.observeAsState(true).value) {
        AddDataPointsDialog(
            viewModel = addDataPointsDialogViewModel,
            onDismissRequest = { addDataPointsDialogViewModel.reset() }
        )
    }
}

@Composable
internal fun UpdateWarningDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) = ContinueCancelDialog(
    body = R.string.ru_sure_update_data,
    onDismissRequest = onDismissRequest,
    onConfirm = onConfirm
)

@Composable
private fun TopAppBarContent(
    navArgs: FeatureHistoryNavKey,
    featureName: String,
    dataPointsCount: Int,
    isTracker: Boolean,
    isMultiSelectMode: Boolean,
    selectedCount: Int,
    onInfoClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onExitMultiSelect: () -> Unit
) {
    val topBarController = LocalTopBarController.current

    val subtitle = when {
        isMultiSelectMode -> stringResource(R.string.items_selected, selectedCount)
        dataPointsCount > 0 -> stringResource(R.string.data_points, dataPointsCount)
        else -> null
    }

    val actions: @Composable RowScope.() -> Unit = remember(
        isTracker,
        isMultiSelectMode,
        onInfoClick,
        onUpdateClick,
        onExitMultiSelect
    ) {
        {
            if (isMultiSelectMode) {
                IconButton(onClick = onExitMultiSelect) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = stringResource(id = R.string.cancel)
                    )
                }
            } else {
                IconButton(onClick = onInfoClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.about_icon),
                        contentDescription = stringResource(id = R.string.info)
                    )
                }
                if (isTracker) {
                    IconButton(onClick = onUpdateClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.edit_icon),
                            contentDescription = stringResource(id = R.string.update)
                        )
                    }
                }
            }
        }
    }

    topBarController.Set(
        navArgs,
        AppBarConfig(
            title = featureName,
            backNavigationAction = true,
            subtitle = subtitle,
            actions = actions
        )
    )
}

@Composable
private fun FeatureHistoryView(
    dateScrollData: DateScrollData<DataPointInfo>?,
    isDuration: Boolean = false,
    isTracker: Boolean = true,
    isMultiSelectMode: Boolean = false,
    selectedDataPoints: Set<DataPointInfo> = emptySet(),
    errorMessage: String? = null,
    offsetDiffHours: Int? = null,
    onDataPointClick: (DataPointInfo) -> Unit = {},
    onDataPointLongPress: (DataPointInfo) -> Unit = {},
    onDataPointSelected: (DataPointInfo, Boolean) -> Unit = { _, _ -> },
    onEditClick: (DataPointInfo) -> Unit = {},
    onDeleteClick: (DataPointInfo) -> Unit = {},
    onDeleteSelectedClick: () -> Unit = {}
) = TnGComposeTheme {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            dateScrollData != null && dateScrollData.items.isEmpty() -> {
                EmptyScreenText(textId = R.string.no_data_points_history_fragment_hint)
            }

            dateScrollData != null -> {
                DataPointList(
                    dateScrollData = dateScrollData,
                    isDuration = isDuration,
                    isTracker = isTracker,
                    isMultiSelectMode = isMultiSelectMode,
                    selectedDataPoints = selectedDataPoints,
                    offsetDiffHours = offsetDiffHours,
                    onDataPointClick = onDataPointClick,
                    onDataPointLongPress = onDataPointLongPress,
                    onDataPointSelected = onDataPointSelected,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick
                )
            }

            errorMessage != null -> {
                EmptyScreenText(
                    text = stringResource(R.string.data_resolution_error, errorMessage),
                    color = MaterialTheme.colorScheme.error,
                    alpha = 1f
                )
            }
        }

        if (isMultiSelectMode && selectedDataPoints.isNotEmpty()) {
            FloatingActionButton(
                onClick = onDeleteSelectedClick,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(inputSpacingLarge)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.delete_icon),
                    contentDescription = stringResource(id = R.string.delete_selected_content_description)
                )
            }
        }
    }
}

@Composable
private fun DataPointList(
    dateScrollData: DateScrollData<DataPointInfo>,
    isDuration: Boolean,
    isTracker: Boolean,
    isMultiSelectMode: Boolean,
    selectedDataPoints: Set<DataPointInfo>,
    offsetDiffHours: Int?,
    onDataPointClick: (DataPointInfo) -> Unit,
    onDataPointLongPress: (DataPointInfo) -> Unit,
    onDataPointSelected: (DataPointInfo, Boolean) -> Unit,
    onEditClick: (DataPointInfo) -> Unit,
    onDeleteClick: (DataPointInfo) -> Unit
) {
    val weekdayNames = getWeekDayNames(LocalContext.current)

    DateScrollLazyColumn(
        modifier = Modifier.padding(cardMarginSmall),
        contentPadding = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            .asPaddingValues(),
        data = dateScrollData
    ) { dataPoint ->
        DataPointCard(
            dataPoint = dataPoint,
            weekdayNames = weekdayNames,
            isDuration = isDuration,
            isTracker = isTracker,
            isMultiSelectMode = isMultiSelectMode,
            isSelected = dataPoint in selectedDataPoints,
            offsetDiffHours = offsetDiffHours,
            onClick = { onDataPointClick(dataPoint) },
            onLongClick = { onDataPointLongPress(dataPoint) },
            onSelectedChange = { selected -> onDataPointSelected(dataPoint, selected) },
            onEditClick = { onEditClick(dataPoint) },
            onDeleteClick = { onDeleteClick(dataPoint) }
        )
        Spacer(modifier = Modifier.height(cardMarginSmall))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DataPointCard(
    dataPoint: DataPointInfo,
    weekdayNames: List<String>,
    isDuration: Boolean,
    isTracker: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    offsetDiffHours: Int?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSelectedChange: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) = Card(
    modifier = Modifier
        .combinedClickable(
            onClick = {
                if (isMultiSelectMode) {
                    onSelectedChange(!isSelected)
                } else {
                    onClick()
                }
            },
            onLongClick = {
                if (isTracker && !isMultiSelectMode) {
                    onLongClick()
                }
            }
        ),
    elevation = CardDefaults.cardElevation(defaultElevation = cardMarginSmall),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(cardMarginSmall)
    ) {
        Text(
            text = formatDayMonthYearHourMinuteWeekDayTwoLines(
                LocalContext.current,
                weekdayNames,
                dataPoint.date,
                offsetDiffHours
            ),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        DialogInputSpacing()
        DataPointValueAndDescription(
            modifier = Modifier.weight(1f),
            dataPoint = dataPoint.toDataPoint(),
            isDuration = isDuration
        )
        if (isMultiSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectedChange
            )
        } else if (isTracker) {
            IconButton(onClick = onEditClick) {
                Icon(
                    painter = painterResource(id = R.drawable.edit_icon),
                    contentDescription = stringResource(id = R.string.edit_data_point_button_content_description),
                    tint = MaterialTheme.tngColors.secondary
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(id = R.drawable.delete_icon),
                    contentDescription = stringResource(id = R.string.delete_data_point_button_content_description),
                    tint = MaterialTheme.tngColors.primary
                )
            }
        }
    }
}

// region Previews

private val sampleDataPoints = listOf(
    DataPointInfo(
        date = OffsetDateTime.parse("2024-01-15T10:30:00Z"),
        featureId = 1L,
        value = 75.5,
        label = "Morning",
        note = "Feeling good today"
    ),
    DataPointInfo(
        date = OffsetDateTime.parse("2024-01-14T18:45:00Z"),
        featureId = 1L,
        value = 82.0,
        label = "Evening",
        note = ""
    ),
    DataPointInfo(
        date = OffsetDateTime.parse("2024-01-13T09:00:00Z"),
        featureId = 1L,
        value = 70.0,
        label = "",
        note = "After workout"
    ),
    DataPointInfo(
        date = OffsetDateTime.parse("2024-01-12T14:20:00Z"),
        featureId = 1L,
        value = 78.5,
        label = "Afternoon",
        note = ""
    ),
)

private val sampleDateScrollData = DateScrollData(
    dateDisplayResolution = DateDisplayResolution.MONTH_DAY,
    items = sampleDataPoints
)

@Preview(showBackground = true)
@Composable
private fun TrackerHistoryPreview() {
    FeatureHistoryView(
        dateScrollData = sampleDateScrollData,
        offsetDiffHours = 0
    )
}

@Preview(showBackground = true)
@Composable
private fun TrackerHistoryMultiSelectPreview() {
    FeatureHistoryView(
        dateScrollData = sampleDateScrollData,
        isMultiSelectMode = true,
        selectedDataPoints = setOf(sampleDataPoints[0], sampleDataPoints[2]),
        offsetDiffHours = 0
    )
}

@Preview(showBackground = true)
@Composable
private fun FunctionHistoryPreview() {
    FeatureHistoryView(
        dateScrollData = sampleDateScrollData,
        isTracker = false,
        offsetDiffHours = 0
    )
}

@Preview(showBackground = true)
@Composable
private fun EmptyHistoryPreview() {
    FeatureHistoryView(
        dateScrollData = DateScrollData(
            dateDisplayResolution = DateDisplayResolution.MONTH_DAY,
            items = emptyList()
        ),
        offsetDiffHours = 0
    )
}

// endregion
