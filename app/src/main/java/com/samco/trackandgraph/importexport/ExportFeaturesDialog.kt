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
package com.samco.trackandgraph.importexport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.CustomContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.HalfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.LoadingOverlay
import com.samco.trackandgraph.ui.compose.ui.SelectorButton
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing

@Composable
fun ExportFeaturesDialog(
    trackGroupId: Long,
    trackGroupName: String?,
    onDismissRequest: () -> Unit,
) {
    val viewModel: ExportFeaturesViewModel = hiltViewModel<ExportFeaturesViewModelImpl>()
    val context = LocalContext.current

    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val selectedFileUri by viewModel.selectedFileUri.collectAsStateWithLifecycle()
    val availableFeatures by viewModel.availableFeatures.collectAsStateWithLifecycle()
    val selectedFeatures by viewModel.selectedFeatures.collectAsStateWithLifecycle()

    // Derive file name from selected URI
    val selectedFileName by remember {
        derivedStateOf {
            selectedFileUri?.let { uri ->
                ImportExportFeatureUtils.getFileNameFromUri(context, uri)
            }
        }
    }

    // Handle export completion
    LaunchedEffect(exportState) {
        when (exportState) {
            ExportState.DONE -> onDismissRequest()
            else -> {}
        }
    }

    // Load features when dialog opens
    LaunchedEffect(trackGroupId) {
        viewModel.loadFeatures(trackGroupId)
    }

    // File creation launcher
    val fileCreationLauncher = rememberLauncherForActivityResult(
        contract = CreateCsvDocumentActivityResultContract(trackGroupName)
    ) { uri: Uri? ->
        uri?.let { viewModel.setSelectedFileUri(it) }
    }

    CustomContinueCancelDialog(
        onDismissRequest = {
            viewModel.reset()
            onDismissRequest()
        },
        onConfirm = { viewModel.beginExport() },
        continueText = R.string.exportButton,
        continueEnabled = selectedFileUri != null &&
            selectedFeatures.isNotEmpty() &&
            exportState != ExportState.LOADING &&
            exportState != ExportState.EXPORTING
    ) {
        ExportFeaturesDialogContent(
            exportState = exportState,
            selectedFileUri = selectedFileUri,
            selectedFileName = selectedFileName,
            availableFeatures = availableFeatures,
            selectedFeatures = selectedFeatures,
            onCreateFile = { fileCreationLauncher.launch(Unit) },
            onToggleFeature = viewModel::toggleFeatureSelection,
        )
    }
}

@Composable
private fun ExportFeaturesDialogContent(
    exportState: ExportState,
    selectedFileUri: Uri?,
    selectedFileName: String?,
    availableFeatures: List<FeatureDto>,
    selectedFeatures: List<FeatureDto>,
    onCreateFile: () -> Unit,
    onToggleFeature: (FeatureDto) -> Unit,
) = BoxWithConstraints {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing)
    ) {
        // File selection section
        Text(
            text = stringResource(R.string.export_to),
            style = MaterialTheme.typography.bodyMedium
        )

        SelectorButton(
            onClick = onCreateFile,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = cardPadding),
            enabled = exportState == ExportState.WAITING,
        ) {
            Text(
                text = selectedFileName ?: stringResource(R.string.select_file),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selectedFileUri == null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        for (feature in availableFeatures) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleFeature(feature) }
                    .padding(cardPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedFeatures.contains(feature),
                    onCheckedChange = null,
                )
                HalfDialogInputSpacing()
                Text(
                    text = feature.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // Loading/Progress indication
    if (exportState != ExportState.WAITING) {
        LoadingOverlay()
    }
}

@Preview(showBackground = true)
@Composable
private fun ExportFeaturesDialogContentPreview() {
    TnGComposeTheme {
        val sampleFeatures = listOf(
            FeatureDto(1L, "Weight"),
            FeatureDto(2L, "Sleep Hours"),
            FeatureDto(3L, "Exercise")
        )
        ExportFeaturesDialogContent(
            exportState = ExportState.WAITING,
            selectedFileUri = "file://test.csv".toUri(),
            selectedFileName = "test_export.csv",
            availableFeatures = sampleFeatures,
            selectedFeatures = sampleFeatures.take(2),
            onCreateFile = {},
            onToggleFeature = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExportFeaturesDialogContentLoadingPreview() {
    TnGComposeTheme {
        ExportFeaturesDialogContent(
            exportState = ExportState.LOADING,
            selectedFileUri = null,
            selectedFileName = null,
            availableFeatures = emptyList(),
            selectedFeatures = emptyList(),
            onCreateFile = {},
            onToggleFeature = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExportFeaturesDialogContentNoFileSelectedPreview() {
    TnGComposeTheme {
        val sampleFeatures = listOf(
            FeatureDto(1L, "Weight"),
            FeatureDto(2L, "Sleep Hours"),
            FeatureDto(3L, "Exercise"),
            FeatureDto(4L, "Mood Rating"),
            FeatureDto(5L, "Steps")
        )
        ExportFeaturesDialogContent(
            exportState = ExportState.WAITING,
            selectedFileUri = null,
            selectedFileName = null,
            availableFeatures = sampleFeatures,
            selectedFeatures = sampleFeatures,
            onCreateFile = {},
            onToggleFeature = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExportFeaturesDialogContentExportingPreview() {
    TnGComposeTheme {
        val sampleFeatures = listOf(
            FeatureDto(1L, "Weight"),
            FeatureDto(2L, "Sleep Hours"),
            FeatureDto(3L, "Exercise")
        )
        ExportFeaturesDialogContent(
            exportState = ExportState.EXPORTING,
            selectedFileUri = "file://export_2024_01_15.csv".toUri(),
            selectedFileName = "export_2024_01_15.csv",
            availableFeatures = sampleFeatures,
            selectedFeatures = listOf(sampleFeatures[0], sampleFeatures[2]),
            onCreateFile = {},
            onToggleFeature = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExportFeaturesDialogContentNoFeaturesPreview() {
    TnGComposeTheme {
        ExportFeaturesDialogContent(
            exportState = ExportState.WAITING,
            selectedFileUri = "file://empty_group_export.csv".toUri(),
            selectedFileName = "empty_group_export.csv",
            availableFeatures = emptyList(),
            selectedFeatures = emptyList(),
            onCreateFile = {},
            onToggleFeature = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExportFeaturesDialogContentPartialSelectionPreview() {
    TnGComposeTheme {
        val sampleFeatures = listOf(
            FeatureDto(1L, "Daily Weight"),
            FeatureDto(2L, "Sleep Quality (1-10)"),
            FeatureDto(3L, "Morning Exercise"),
            FeatureDto(4L, "Water Intake (L)"),
            FeatureDto(5L, "Mood Rating"),
            FeatureDto(6L, "Work Productivity")
        )
        ExportFeaturesDialogContent(
            exportState = ExportState.WAITING,
            selectedFileUri = "file://partial_export.csv".toUri(),
            selectedFileName = "partial_export.csv",
            availableFeatures = sampleFeatures,
            selectedFeatures = listOf(sampleFeatures[0], sampleFeatures[2], sampleFeatures[4]),
            onCreateFile = {},
            onToggleFeature = {},
        )
    }
}
