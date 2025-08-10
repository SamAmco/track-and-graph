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

package com.samco.trackandgraph.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner

@Composable
fun TrackWidgetConfigureDialog(
    onCreateWidget: (Long?) -> Unit,
    onNoFeatures: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: TrackWidgetConfigureDialogViewModel = hiltViewModel()
) {
    val featureMap by viewModel.featureMap.collectAsStateWithLifecycle()

    // Handle create widget event
    LaunchedEffect(viewModel.onCreateWidget) {
        for (featureId in viewModel.onCreateWidget) {
            onCreateWidget(featureId)
        }
    }

    // Check if we have features and handle no features case
    LaunchedEffect(featureMap) {
        if (featureMap?.isEmpty() == true) {
            onNoFeatures()
            return@LaunchedEffect
        }
    }

    TrackWidgetConfigureDialogContent(
        featureMap = featureMap ?: emptyMap(),
        onFeatureSelected = viewModel::onFeatureSelected,
        onConfirm = viewModel::onCreateClicked,
        onDismiss = onDismiss
    )
}

@Composable
private fun TrackWidgetConfigureDialogContent(
    featureMap: Map<Long, String>,
    onFeatureSelected: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFeatureId by remember(featureMap) {
        mutableStateOf(featureMap.keys.firstOrNull())
    }

    // Update the view model when selection changes
    LaunchedEffect(selectedFeatureId) {
        selectedFeatureId?.let { onFeatureSelected(it) }
    }

    ContinueCancelDialog(
        onDismissRequest = onDismiss,
        onConfirm = onConfirm,
        continueText = R.string.create,
        dismissText = R.string.cancel,
        continueEnabled = selectedFeatureId != null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.select_a_feature),
                style = MaterialTheme.typography.subtitle2,
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.card_padding))
            )

            DialogInputSpacing()

            selectedFeatureId?.let { selected ->
                TextMapSpinner(
                    modifier = Modifier.fillMaxWidth(),
                    strings = featureMap,
                    selectedItem = selected,
                    onItemSelected = { featureId ->
                        selectedFeatureId = featureId
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackWidgetConfigureDialogContentPreview() {
    TrackWidgetConfigureDialogContent(
        featureMap = mapOf(
            1L to "Group 1/Feature 1",
            2L to "Group 1/Feature 2",
            3L to "Group 2/Feature 3"
        ),
        onFeatureSelected = {},
        onConfirm = {},
        onDismiss = {}
    )
}