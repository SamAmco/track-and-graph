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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.model.ImportFeaturesException
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.CustomContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import androidx.core.net.toUri
import com.samco.trackandgraph.importexport.ImportExportFeatureUtils.getFileNameFromUri
import com.samco.trackandgraph.ui.compose.ui.SelectorButton
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing

@Composable
fun ImportFeaturesDialog(
    trackGroupId: Long,
    onDismissRequest: () -> Unit,
) {
    val viewModel: ImportFeaturesModuleViewModel = hiltViewModel<ImportFeaturesModuleViewModelImpl>()
    val context = LocalContext.current
    val selectedFileUri by viewModel.selectedFileUri.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val importException by viewModel.importException.collectAsStateWithLifecycle()

    // Derive file name from selected URI
    val selectedFileName by remember {
        derivedStateOf {
            selectedFileUri?.let { uri ->
                getFileNameFromUri(context, uri)
            }
        }
    }

    // Handle import completion and errors
    LaunchedEffect(importState) {
        when (importState) {
            ImportState.DONE -> {
                viewModel.reset()
                onDismissRequest()
            }
            else -> {}
        }
    }

    LaunchedEffect(importException) {
        importException?.let { exception ->
            Toast.makeText(
                context,
                getStringForImportException(context, exception),
                Toast.LENGTH_LONG
            ).show()
            viewModel.clearException()
        }
    }

    // File picker launcher with support for multiple CSV MIME types
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = GetCsvContentActivityResultContract()
    ) { uri: Uri? ->
        uri?.let { viewModel.setSelectedFileUri(it) }
    }

    CustomContinueCancelDialog(
        onDismissRequest = {
            viewModel.reset()
            onDismissRequest()
        },
        onConfirm = { viewModel.beginImport(trackGroupId) },
        continueText = R.string.importButton,
        continueEnabled = selectedFileUri != null && importState != ImportState.IMPORTING
    ) {
        ImportFeaturesDialogContent(
            selectedFileUri = selectedFileUri,
            selectedFileName = selectedFileName,
            importState = importState,
            onSelectFile = { filePickerLauncher.launch(Unit) }
        )
    }
}

@Composable
private fun ImportFeaturesDialogContent(
    selectedFileUri: Uri?,
    selectedFileName: String?,
    importState: ImportState,
    onSelectFile: () -> Unit
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
private fun ImportFeaturesDialogPreview() {
    TnGComposeTheme {
        ImportFeaturesDialogContent(
            selectedFileUri = null,
            selectedFileName = null,
            importState = ImportState.WAITING,
            onSelectFile = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ImportFeaturesDialogWithFilePreview() {
    TnGComposeTheme {
        ImportFeaturesDialogContent(
            selectedFileUri = "content://com.android.providers.downloads.documents/document/sample.csv".toUri(),
            selectedFileName = "sample.csv",
            importState = ImportState.WAITING,
            onSelectFile = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ImportFeaturesDialogImportingPreview() {
    TnGComposeTheme {
        ImportFeaturesDialogContent(
            selectedFileUri = "content://com.android.providers.downloads.documents/document/sample.csv".toUri(),
            selectedFileName = "sample.csv",
            importState = ImportState.IMPORTING,
            onSelectFile = {}
        )
    }
}
