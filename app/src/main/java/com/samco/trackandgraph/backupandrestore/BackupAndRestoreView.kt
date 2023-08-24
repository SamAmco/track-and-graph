package com.samco.trackandgraph.backupandrestore

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.ui.ConfirmDialog
import org.threeten.bp.OffsetDateTime
import kotlin.system.exitProcess

@Composable
fun BackupAndRestoreView(viewModel: BackupAndRestoreViewModel) = Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(dimensionResource(id = R.dimen.input_spacing_large)),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
) {

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.input_spacing_large)))

    Card(
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.card_padding_large)),
            text = stringResource(id = R.string.backup_hint_text),
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.onSurface
        )
    }

    Spacer(modifier = Modifier.weight(0.2f))

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.sqlite3")
    ) { viewModel.exportDatabase(it) }

    val context = LocalContext.current

    Button(onClick = {
        val now = OffsetDateTime.now()
        val generatedName = context.getString(
            R.string.backup_file_name_suffix,
            "TrackAndGraphBackup", now.year, now.monthValue,
            now.dayOfMonth, now.hour, now.minute, now.second
        )
        backupLauncher.launch(generatedName)
    }) {
        Text(
            text = stringResource(id = R.string.backup_data).uppercase(),
            style = MaterialTheme.typography.button
        )
    }

    when (val backupState = viewModel.backupState.collectAsStateWithLifecycle().value) {
        is BackupAndRestoreViewModel.OperationState.Error -> {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.input_spacing_large)))
            Text(
                text = stringResource(id = backupState.stringResource),
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.error
            )
        }

        BackupAndRestoreViewModel.OperationState.Success -> {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.input_spacing_large)))
            Text(
                text = stringResource(id = R.string.backup_successful),
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onError
            )
        }

        BackupAndRestoreViewModel.OperationState.Idle,
        BackupAndRestoreViewModel.OperationState.InProgress -> {
        }
    }

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.input_spacing_large)))

    CenterGradientDivider()

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.input_spacing_large)))

    var showPreRestoreDialog by rememberSaveable { mutableStateOf(false) }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { viewModel.restoreDatabase(it) }

    if (showPreRestoreDialog) {
        PreRestoreDialog(
            onConfirm = {
                showPreRestoreDialog = false
                restoreLauncher.launch(arrayOf("*/*"))
            },
            onDismiss = { showPreRestoreDialog = false }
        )
    }

    Button(onClick = {
        showPreRestoreDialog = true
    }) {
        Text(
            text = stringResource(id = R.string.restore_data).uppercase(),
            style = MaterialTheme.typography.button
        )
    }

    //TODO show a dialog to confirm restore

    when (val restoreState = viewModel.restoreState.collectAsStateWithLifecycle().value) {
        is BackupAndRestoreViewModel.OperationState.Error -> {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.input_spacing_large)))
            Text(
                text = stringResource(id = restoreState.stringResource),
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.error
            )
        }

        BackupAndRestoreViewModel.OperationState.Success -> RestoreCompleteDialog()

        BackupAndRestoreViewModel.OperationState.Idle,
        BackupAndRestoreViewModel.OperationState.InProgress -> {
        }
    }

    Spacer(modifier = Modifier.weight(1f))
}

@Composable
private fun PreRestoreDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) = ConfirmDialog(
    onConfirm = onConfirm,
    onDismissRequest = onDismiss) {
    Text(
        text = stringResource(id = R.string.restore_hint_text),
        style = MaterialTheme.typography.subtitle2,
        color = MaterialTheme.colors.onSurface,
    )
}

@Composable
private fun RestoreCompleteDialog() = ConfirmDialog(
    onConfirm = { exitProcess(0) },
    dismissOnClickOutside = false,
    onDismissRequest = { },
    continueText = R.string.ok
) {
    Text(
        text = stringResource(id = R.string.restore_successful),
        style = MaterialTheme.typography.subtitle2,
        color = MaterialTheme.colors.onSurface
    )
}

@Composable
private fun CenterGradientDivider() = BoxWithConstraints(
    Modifier
        .fillMaxWidth()
        .height(2.dp)
) {
    val aspectRatio = maxWidth / maxHeight
    Box(
        Modifier
            .fillMaxSize()
            .scale(maxOf(aspectRatio, 1f), maxOf(1 / aspectRatio, 1f))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colors.secondary,
                        MaterialTheme.colors.surface.copy(alpha = 0.0f)
                    ),
                ),
            )
    )
}

