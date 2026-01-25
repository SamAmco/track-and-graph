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

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.csvreadwriter.ImportFeaturesException
import com.samco.trackandgraph.importexport.ImportExportFeatureUtils.getFileNameFromUri
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.ContinueCancelButtons
import com.samco.trackandgraph.ui.compose.ui.CustomDialog
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.HalfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.LoadingOverlay
import com.samco.trackandgraph.ui.compose.ui.SelectorButton
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge

@Composable
fun ImportExportDialog(
    trackGroupId: Long,
    trackGroupName: String?,
    onDismissRequest: () -> Unit,
) {
    val importViewModel: ImportFeaturesModuleViewModel = hiltViewModel<ImportFeaturesModuleViewModelImpl>()
    val exportViewModel: ExportFeaturesViewModel = hiltViewModel<ExportFeaturesViewModelImpl>()
    val context = LocalContext.current

    // Import state
    val selectedImportFileUri by importViewModel.selectedFileUri.collectAsStateWithLifecycle()
    val importState by importViewModel.importState.collectAsStateWithLifecycle()
    val importException by importViewModel.importException.collectAsStateWithLifecycle()

    val selectedImportFileName by remember {
        derivedStateOf {
            selectedImportFileUri?.let { uri ->
                getFileNameFromUri(context, uri)
            }
        }
    }

    // Export state
    val exportState by exportViewModel.exportState.collectAsStateWithLifecycle()
    val selectedExportFileUri by exportViewModel.selectedFileUri.collectAsStateWithLifecycle()
    val availableFeatures by exportViewModel.availableFeatures.collectAsStateWithLifecycle()
    val selectedFeatures by exportViewModel.selectedFeatures.collectAsStateWithLifecycle()

    val selectedExportFileName by remember {
        derivedStateOf {
            selectedExportFileUri?.let { uri ->
                getFileNameFromUri(context, uri)
            }
        }
    }

    // Handle import completion
    LaunchedEffect(importState) {
        when (importState) {
            ImportState.DONE -> {
                importViewModel.reset()
                onDismissRequest()
            }
            else -> {}
        }
    }

    // Handle import errors
    LaunchedEffect(importException) {
        importException?.let { exception ->
            Toast.makeText(
                context,
                getStringForImportException(context, exception),
                Toast.LENGTH_LONG
            ).show()
            importViewModel.clearException()
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
        exportViewModel.loadFeatures(trackGroupId)
    }

    // Handle export errors
    LaunchedEffect(Unit) {
        for (errorMessage in exportViewModel.errors) {
            Toast.makeText(
                context,
                context.getString(R.string.export_error_message, errorMessage),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // File pickers
    val importFilePickerLauncher = rememberLauncherForActivityResult(
        contract = GetCsvContentActivityResultContract()
    ) { uri: Uri? ->
        uri?.let { importViewModel.setSelectedFileUri(it) }
    }

    val exportFileCreationLauncher = rememberLauncherForActivityResult(
        contract = CreateCsvDocumentActivityResultContract(trackGroupName)
    ) { uri: Uri? ->
        uri?.let { exportViewModel.setSelectedFileUri(it) }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    ImportExportDialogContent(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        // Import state
        selectedImportFileUri = selectedImportFileUri,
        selectedImportFileName = selectedImportFileName,
        importState = importState,
        onSelectImportFile = { importFilePickerLauncher.launch(Unit) },
        onImportConfirm = { importViewModel.beginImport(trackGroupId) },
        // Export state
        exportState = exportState,
        selectedExportFileUri = selectedExportFileUri,
        selectedExportFileName = selectedExportFileName,
        availableFeatures = availableFeatures,
        selectedFeatures = selectedFeatures,
        onSelectExportFile = { exportFileCreationLauncher.launch(Unit) },
        onToggleFeature = exportViewModel::toggleFeatureSelection,
        onExportConfirm = { exportViewModel.beginExport() },
        // Common
        onDismissRequest = {
            importViewModel.reset()
            exportViewModel.reset()
            onDismissRequest()
        }
    )
}

@Composable
private fun ImportExportDialogContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    // Import state
    selectedImportFileUri: Uri?,
    selectedImportFileName: String?,
    importState: ImportState,
    onSelectImportFile: () -> Unit,
    onImportConfirm: () -> Unit,
    // Export state
    exportState: ExportState,
    selectedExportFileUri: Uri?,
    selectedExportFileName: String?,
    availableFeatures: List<FeatureDto>,
    selectedFeatures: List<FeatureDto>,
    onSelectExportFile: () -> Unit,
    onToggleFeature: (FeatureDto) -> Unit,
    onExportConfirm: () -> Unit,
    // Common
    onDismissRequest: () -> Unit,
) {
    CustomDialog(
        onDismissRequest = onDismissRequest,
        paddingValues = PaddingValues(
            start = inputSpacingLarge,
            end = inputSpacingLarge,
            bottom = cardPadding,
            top = 0.dp,
        )
    ) {
        // Tab Row
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                text = { Text(stringResource(R.string.importButton)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                text = { Text(stringResource(R.string.exportButton)) }
            )
        }

        InputSpacingLarge()

        // Content based on selected tab
        when (selectedTab) {
            0 -> ImportTabContent(
                selectedFileUri = selectedImportFileUri,
                selectedFileName = selectedImportFileName,
                importState = importState,
                onSelectFile = onSelectImportFile,
                onConfirm = onImportConfirm,
                onCancel = onDismissRequest
            )
            1 -> ExportTabContent(
                exportState = exportState,
                selectedFileUri = selectedExportFileUri,
                selectedFileName = selectedExportFileName,
                availableFeatures = availableFeatures,
                selectedFeatures = selectedFeatures,
                onCreateFile = onSelectExportFile,
                onToggleFeature = onToggleFeature,
                onConfirm = onExportConfirm,
                onCancel = onDismissRequest
            )
        }
    }
}

@Composable
private fun ImportTabContent(
    selectedFileUri: Uri?,
    selectedFileName: String?,
    importState: ImportState,
    onSelectFile: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing)
    ) {
        // Header text
        Text(
            text = stringResource(R.string.import_from),
            style = MaterialTheme.typography.bodyLarge
        )

        // File selection button
        SelectorButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = cardPadding),
            text = selectedFileName ?: stringResource(R.string.select_file),
            enabled = importState != ImportState.IMPORTING,
            textColor = if (selectedFileUri == null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            onClick = onSelectFile
        )

        DialogInputSpacing()

        // Warning section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dialogInputSpacing)
        ) {
            Icon(
                painter = painterResource(R.drawable.warning_icon),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )

            Text(
                text = stringResource(R.string.import_warning),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        // Progress indicator during import
        if (importState == ImportState.IMPORTING) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        DialogInputSpacing()

        ContinueCancelButtons(
            continueText = R.string.importButton,
            continueEnabled = selectedFileUri != null && importState != ImportState.IMPORTING,
            onContinue = onConfirm,
            onCancel = onCancel
        )
    }
}

@Composable
private fun ExportTabContent(
    exportState: ExportState,
    selectedFileUri: Uri?,
    selectedFileName: String?,
    availableFeatures: List<FeatureDto>,
    selectedFeatures: List<FeatureDto>,
    onCreateFile: () -> Unit,
    onToggleFeature: (FeatureDto) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) = Box {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = cardPadding),
            text = selectedFileName ?: stringResource(R.string.select_file),
            enabled = exportState == ExportState.WAITING,
            textColor = if (selectedFileUri == null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            onClick = onCreateFile
        )

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

        ContinueCancelButtons(
            continueText = R.string.exportButton,
            continueEnabled = selectedFileUri != null &&
                selectedFeatures.isNotEmpty() &&
                exportState != ExportState.LOADING &&
                exportState != ExportState.EXPORTING,
            onContinue = onConfirm,
            onCancel = onCancel
        )
    }

    // Loading/Progress indication
    if (exportState != ExportState.WAITING) {
        LoadingOverlay()
    }
}

private fun getStringForImportException(context: Context, exception: ImportFeaturesException): String {
    return when (exception) {
        is ImportFeaturesException.Unknown -> context.getString(
            R.string.import_exception_unknown
        )
        is ImportFeaturesException.InconsistentDataType -> context.getString(
            R.string.import_exception_inconsistent_data_type,
            exception.lineNumber.toString()
        )
        is ImportFeaturesException.InconsistentRecord -> context.getString(
            R.string.import_exception_inconsistent_record,
            exception.lineNumber.toString()
        )
        is ImportFeaturesException.BadTimestamp -> context.getString(
            R.string.import_exception_bad_timestamp,
            exception.lineNumber.toString()
        )
        is ImportFeaturesException.BadHeaders -> context.getString(
            R.string.import_exception_bad_headers,
            exception.requiredHeaders
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ImportExportDialogImportTabPreview() {
    TnGComposeTheme {
        ImportExportDialogContent(
            selectedTab = 0,
            onTabSelected = {},
            selectedImportFileUri = null,
            selectedImportFileName = null,
            importState = ImportState.WAITING,
            onSelectImportFile = {},
            onImportConfirm = {},
            exportState = ExportState.WAITING,
            selectedExportFileUri = null,
            selectedExportFileName = null,
            availableFeatures = emptyList(),
            selectedFeatures = emptyList(),
            onSelectExportFile = {},
            onToggleFeature = {},
            onExportConfirm = {},
            onDismissRequest = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ImportExportDialogExportTabPreview() {
    TnGComposeTheme {
        val sampleFeatures = listOf(
            FeatureDto(1L, "Weight"),
            FeatureDto(2L, "Sleep Hours"),
            FeatureDto(3L, "Exercise")
        )
        ImportExportDialogContent(
            selectedTab = 1,
            onTabSelected = {},
            selectedImportFileUri = null,
            selectedImportFileName = null,
            importState = ImportState.WAITING,
            onSelectImportFile = {},
            onImportConfirm = {},
            exportState = ExportState.WAITING,
            selectedExportFileUri = "file://test.csv".toUri(),
            selectedExportFileName = "test_export.csv",
            availableFeatures = sampleFeatures,
            selectedFeatures = sampleFeatures.take(2),
            onSelectExportFile = {},
            onToggleFeature = {},
            onExportConfirm = {},
            onDismissRequest = {}
        )
    }
}
