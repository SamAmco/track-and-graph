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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.map
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.R
import com.samco.trackandgraph.adddatapoint.AddDataPointsDialog
import com.samco.trackandgraph.adddatapoint.AddDataPointsNavigationViewModel
import com.samco.trackandgraph.adddatapoint.AddDataPointsViewModelImpl
import com.samco.trackandgraph.data.database.dto.Tracker
import com.samco.trackandgraph.data.lua.dto.LuaEngineDisabledException
import com.samco.trackandgraph.helpers.formatDayMonthYearHourMinuteWeekDayTwoLines
import com.samco.trackandgraph.helpers.getWeekDayNames
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.CheckboxLabeledExpandingSection
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.CustomContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.DataPointInfoDialog
import com.samco.trackandgraph.ui.compose.ui.DataPointValueAndDescription
import com.samco.trackandgraph.ui.compose.ui.DateScrollData
import com.samco.trackandgraph.ui.compose.ui.DateScrollLazyColumn
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.DurationInput
import com.samco.trackandgraph.ui.compose.ui.EmptyScreenText
import com.samco.trackandgraph.ui.compose.ui.FeatureInfoDialog
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.LabelInputTextField
import com.samco.trackandgraph.ui.compose.ui.LoadingOverlay
import com.samco.trackandgraph.ui.compose.ui.ValueInputTextField
import com.samco.trackandgraph.ui.compose.ui.cardMarginSmall
import kotlinx.serialization.Serializable

@Serializable
data class FeatureHistoryNavKey(
    val featureId: Long,
    val featureName: String
) : NavKey

@Composable
fun FeatureHistoryScreen(navArgs: FeatureHistoryNavKey) {
    val viewModel: FeatureHistoryViewModel = hiltViewModel<FeatureHistoryViewModelImpl>()

    // Initialize ViewModel with the featureId from NavKey
    LaunchedEffect(navArgs.featureId) {
        viewModel.initViewModel(navArgs.featureId)
    }

    TopAppBarContent(
        navArgs = navArgs,
        featureName = navArgs.featureName,
        viewModel = viewModel
    )

    FeatureHistoryView(viewModel = viewModel)
}

@Composable
private fun TopAppBarContent(
    navArgs: FeatureHistoryNavKey,
    featureName: String,
    viewModel: FeatureHistoryViewModel
) {
    val topBarController = LocalTopBarController.current

    // Observe data points count for subtitle
    val dataPointsCount by viewModel.dateScrollData.map { it.items.size }.observeAsState(0)
    val tracker by viewModel.tracker.observeAsState(null)

    val subtitle = if (dataPointsCount > 0) {
        stringResource(R.string.data_points, dataPointsCount)
    } else {
        null
    }

    val actions: @Composable RowScope.() -> Unit = remember(viewModel, tracker) {
        {
            // Info action
            IconButton(onClick = { viewModel.onShowFeatureInfo() }) {
                Icon(
                    painter = painterResource(id = R.drawable.about_icon),
                    contentDescription = stringResource(id = R.string.info)
                )
            }
            if (tracker != null) {
                // Update action
                IconButton(onClick = { viewModel.showUpdateAllDialog() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.edit_icon),
                        contentDescription = stringResource(id = R.string.update)
                    )
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
fun FeatureHistoryView(viewModel: FeatureHistoryViewModel) {
    val dateScrollData = viewModel.dateScrollData.observeAsState().value
    val isDuration by viewModel.isDuration.observeAsState(false)
    val tracker by viewModel.tracker.observeAsState(null)
    val featureInfo by viewModel.showFeatureInfo.observeAsState()
    val dataPointInfo by viewModel.showDataPointInfo.observeAsState()
    val dataPointDialogViewModel = hiltViewModel<AddDataPointsViewModelImpl>()
    val error by viewModel.error.collectAsStateWithLifecycle()

    when {
        dateScrollData != null && dateScrollData.items.isEmpty() -> {
            EmptyScreenText(textId = R.string.no_data_points_history_fragment_hint)
        }

        dateScrollData != null -> {
            DateScrollData(
                dateScrollData = dateScrollData,
                dataPointDialogViewModel = dataPointDialogViewModel,
                viewModel = viewModel,
                isDuration = isDuration,
                tracker = tracker
            )
        }

        error != null -> {
            val message =
                if (error is LuaEngineDisabledException) stringResource(R.string.lua_engine_disabled)
                else error?.message ?: ""
            EmptyScreenText(
                text = stringResource(R.string.data_resolution_error, message),
                color = MaterialTheme.colorScheme.error,
                alpha = 1f
            )
        }
    }

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

    if (viewModel.showDeleteConfirmDialog.observeAsState(false).value) {
        ContinueCancelDialog(
            body = R.string.ru_sure_del_data_point,
            onDismissRequest = viewModel::onDeleteDismissed,
            onConfirm = viewModel::onDeleteConfirmed
        )
    }

    if (viewModel.showUpdateDialog.observeAsState(false).value) {
        UpdateDialog(viewModel = viewModel)
    }

    if (viewModel.showUpdateWarning.observeAsState(false).value) {
        UpdateWarningDialog(
            viewModel::onCancelUpdateWarning,
            viewModel::onConfirmUpdateWarning
        )
    }

    if (viewModel.isUpdating.observeAsState(false).value) {
        LoadingOverlay()
    }

    if (!dataPointDialogViewModel.hidden.observeAsState(true).value) {
        AddDataPointsDialog(
            viewModel = dataPointDialogViewModel,
            onDismissRequest = { dataPointDialogViewModel.reset() }
        )
    }
}

@Composable
private fun DateScrollData(
    dateScrollData: DateScrollData<DataPointInfo>,
    dataPointDialogViewModel: AddDataPointsViewModelImpl,
    viewModel: FeatureHistoryViewModel,
    isDuration: Boolean,
    tracker: Tracker?
) {
    DateScrollLazyColumn(
        modifier = Modifier.padding(cardMarginSmall),
        contentPadding = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            .asPaddingValues(),
        data = dateScrollData
    ) {
        DataPoint(
            dataPoint = it,
            addDataPointsViewModel = dataPointDialogViewModel,
            viewModel = viewModel,
            weekdayNames = getWeekDayNames(LocalContext.current),
            isDuration = isDuration,
            tracker = tracker
        )
        Spacer(modifier = Modifier.height(cardMarginSmall))
    }
}

@Composable
private fun UpdateWarningDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) = ContinueCancelDialog(
    body = R.string.ru_sure_update_data,
    onDismissRequest = onDismissRequest,
    onConfirm = onConfirm
)

@Composable
private fun UpdateDialog(
    viewModel: UpdateDialogViewModel
) = CustomContinueCancelDialog(
    onDismissRequest = viewModel::onCancelUpdate,
    onConfirm = viewModel::onUpdateClicked,
    continueText = R.string.update,
    continueEnabled = viewModel.updateButtonEnabled.observeAsState(false).value
) {

    Text(
        stringResource(R.string.update_all_data_points),
        fontSize = MaterialTheme.typography.titleLarge.fontSize,
        fontWeight = MaterialTheme.typography.titleLarge.fontWeight,
    )
    InputSpacingLarge()

    Text(
        stringResource(R.string.where_colon),
        fontSize = MaterialTheme.typography.titleMedium.fontSize,
        fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
    )
    DialogInputSpacing()
    WhereValueInput(viewModel)
    DialogInputSpacing()
    WhereLabelInput(viewModel)

    InputSpacingLarge()

    Text(
        stringResource(R.string.to_colon),
        fontSize = MaterialTheme.typography.titleMedium.fontSize,
        fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
    )

    DialogInputSpacing()
    ToValueInput(viewModel)
    DialogInputSpacing()
    ToLabelInput(viewModel)
}

@Composable
private fun ToLabelInput(viewModel: UpdateDialogViewModel) {
    val focusRequester = remember { FocusRequester() }

    CheckboxLabeledExpandingSection(
        checked = viewModel.toLabelEnabled.observeAsState(false).value,
        onCheckedChanged = viewModel::setToLabelEnabled,
        label = stringResource(R.string.label_equals),
        focusRequester = focusRequester
    ) {
        LabelInputTextField(
            modifier = it,
            textFieldValue = viewModel.toLabel,
            onValueChange = viewModel::setToTextLabel
        )
    }
}

@Composable
private fun ToValueInput(viewModel: UpdateDialogViewModel) {
    val focusRequester = remember { FocusRequester() }
    val isDuration by viewModel.isDuration.observeAsState(false)

    CheckboxLabeledExpandingSection(
        checked = viewModel.toValueEnabled.observeAsState(false).value,
        onCheckedChanged = viewModel::setToValueEnabled,
        label = stringResource(R.string.value_equals),
        focusRequester = focusRequester
    ) {
        if (isDuration) {
            DurationInput(
                modifier = it,
                viewModel = viewModel.toDurationViewModel
            )
        } else {
            ValueInputTextField(
                modifier = it,
                textFieldValue = viewModel.toValue,
                onValueChange = viewModel::setToTextValue
            )
        }
    }
}

@Composable
private fun WhereLabelInput(viewModel: UpdateDialogViewModel) {
    val focusRequester = remember { FocusRequester() }

    CheckboxLabeledExpandingSection(
        checked = viewModel.whereLabelEnabled.observeAsState(false).value,
        onCheckedChanged = viewModel::setWhereLabelEnabled,
        label = stringResource(R.string.label_equals),
        focusRequester = focusRequester
    ) {
        LabelInputTextField(
            modifier = it,
            textFieldValue = viewModel.whereLabel,
            onValueChange = viewModel::setWhereTextLabel,
        )
    }
}

@Composable
private fun WhereValueInput(
    viewModel: UpdateDialogViewModel
) {
    val focusRequester = remember { FocusRequester() }
    val isDuration by viewModel.isDuration.observeAsState(false)

    CheckboxLabeledExpandingSection(
        checked = viewModel.whereValueEnabled.observeAsState(false).value,
        onCheckedChanged = viewModel::setWhereValueEnabled,
        label = stringResource(R.string.value_equals),
        focusRequester = focusRequester
    ) {
        if (isDuration) {
            DurationInput(
                modifier = it,
                viewModel = viewModel.whereDurationViewModel
            )
        } else {
            ValueInputTextField(
                modifier = it,
                textFieldValue = viewModel.whereValue,
                onValueChange = viewModel::setWhereTextValue,
            )
        }
    }
}

@Composable
private fun DataPoint(
    dataPoint: DataPointInfo,
    addDataPointsViewModel: AddDataPointsNavigationViewModel,
    viewModel: FeatureHistoryViewModel,
    weekdayNames: List<String>,
    isDuration: Boolean,
    tracker: Tracker?
) = Card(
    modifier = Modifier
        .clickable { viewModel.onDataPointClicked(dataPoint) },
    elevation = CardDefaults.cardElevation(defaultElevation = cardMarginSmall),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(cardMarginSmall)
    ) {
        Text(
            text = formatDayMonthYearHourMinuteWeekDayTwoLines(
                LocalContext.current,
                weekdayNames,
                dataPoint.date
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
        if (tracker != null) {
            IconButton(onClick = {
                addDataPointsViewModel.showAddDataPointDialog(
                    trackerId = tracker.id,
                    dataPointTimestamp = dataPoint.date
                )
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.edit_icon),
                    contentDescription = stringResource(id = R.string.edit_data_point_button_content_description),
                    tint = MaterialTheme.tngColors.secondary
                )
            }
            IconButton(onClick = { viewModel.onDeleteClicked(dataPoint) }) {
                Icon(
                    painter = painterResource(id = R.drawable.delete_icon),
                    contentDescription = stringResource(id = R.string.delete_data_point_button_content_description),
                    tint = MaterialTheme.tngColors.primary
                )
            }
        }
    }
}
