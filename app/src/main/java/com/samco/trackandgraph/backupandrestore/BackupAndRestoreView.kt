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

@file:OptIn(ExperimentalLayoutApi::class)

package com.samco.trackandgraph.backupandrestore

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.ui.ConfirmDialog
import com.samco.trackandgraph.ui.compose.ui.CustomConfirmCancelDialog
import com.samco.trackandgraph.ui.compose.ui.DateButton
import com.samco.trackandgraph.ui.compose.ui.SpacingLarge
import com.samco.trackandgraph.ui.compose.ui.SpacingSmall
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.TimeButton
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.ChronoUnit
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

    var showConfigureAutoBackupDialog by rememberSaveable { mutableStateOf(false) }

    Button(onClick = {
        showConfigureAutoBackupDialog = true
    }) {
        Text(
            text = stringResource(id = R.string.configure_auto_backup).uppercase(),
            style = MaterialTheme.typography.button
        )
    }

    val autoBackupViewModel = hiltViewModel<AutoBackupViewModelImpl>()

    if (showConfigureAutoBackupDialog) {
        ConfigureAutoBackupDialog(
            viewModel = autoBackupViewModel,
            onConfirm = {
                showConfigureAutoBackupDialog = false
                //TODO show notification permission if on high enough API
                autoBackupViewModel.onConfirmAutoBackup()
            },
            onDismiss = { showConfigureAutoBackupDialog = false }
        )
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
    onDismissRequest = onDismiss
) {
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

@Composable
private fun ConfigureAutoBackupDialog(
    viewModel: AutoBackupViewModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    CustomConfirmCancelDialog(
        onDismissRequest = onDismiss,
        customWidthPercentage = 0.9f,
        onConfirm = onConfirm,
        continueText = R.string.apply,
        continueEnabled = viewModel.autoBackupConfigValid.collectAsStateWithLifecycle().value,
    ) {

        val focusRequester = remember { FocusRequester() }

        Text(
            text = stringResource(id = R.string.backup_every),
            style = MaterialTheme.typography.h6,
        )

        SpacingSmall()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Spacer(modifier = Modifier.weight(1f))

            OutlinedTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .width(64.dp),
                value = viewModel.autoBackupIntervalTextFieldValue.value,
                onValueChange = viewModel::onBackupIntervalChanged,
                textStyle = MaterialTheme.typography.h6
                    .copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            SpacingSmall()

            val strings = mapOf(
                ChronoUnit.HOURS to stringResource(id = R.string.hours_generic),
                ChronoUnit.DAYS to stringResource(id = R.string.days_generic),
                ChronoUnit.WEEKS to stringResource(id = R.string.weeks_generic),
            )

            TextMapSpinner(
                modifier = Modifier.width(128.dp),
                strings = strings,
                selectedItem = viewModel.autoBackupUnit.collectAsStateWithLifecycle().value,
                onItemSelected = { viewModel.onBackupUnitChanged(it) }
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        SpacingLarge()

        Text(
            text = stringResource(id = R.string.next_backup_at),
            style = MaterialTheme.typography.h6,
        )

        SpacingSmall()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            DateButton(
                dateTime = viewModel.autoBackupFirstDate.collectAsStateWithLifecycle().value,
                onDateSelected = viewModel::onAutoBackupFirstDateChanged
            )

            SpacingSmall()

            TimeButton(
                dateTime = viewModel.autoBackupFirstDate.collectAsStateWithLifecycle().value,
                onTimeSelected = viewModel::onAutoBackupFirstDateChanged
            )
        }

        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }
    }
}