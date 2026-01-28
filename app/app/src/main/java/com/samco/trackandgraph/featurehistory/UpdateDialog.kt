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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.ui.CheckboxLabeledExpandingSection
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.CustomContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.DurationInput
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.LabelInputTextField
import com.samco.trackandgraph.ui.compose.ui.ValueInputTextField

@Composable
internal fun UpdateDialog(
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
