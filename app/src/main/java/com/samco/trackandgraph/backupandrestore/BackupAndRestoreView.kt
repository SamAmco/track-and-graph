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

package com.samco.trackandgraph.backupandrestore

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.helpers.formatDayMonthYearHourMinuteWeekDayOneLine
import com.samco.trackandgraph.helpers.getWeekDayNames
import com.samco.trackandgraph.ui.compose.ui.ConfirmDialog
import com.samco.trackandgraph.ui.compose.ui.CustomConfirmCancelDialog
import com.samco.trackandgraph.ui.compose.ui.DateButton
import com.samco.trackandgraph.ui.compose.ui.MiniNumericTextField
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.TimeButton
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.ChronoUnit
import kotlin.system.exitProcess

private const val SQLITE_MIME_TYPE = "application/vnd.sqlite3"

@Composable
fun BackupAndRestoreView(viewModel: BackupAndRestoreViewModel) = Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(dimensionResource(id = R.dimen.input_spacing_large)),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
) {

    InputSpacingLarge()

    BackupCard(viewModel = viewModel)

    val context = LocalContext.current

    val backupSuccessText = stringResource(id = R.string.backup_successful)

    LaunchedEffect(context) {
        viewModel.backupError.filterNotNull().collect {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(context) {
        viewModel.backupSuccessful.filter { it }.collect {
            Toast.makeText(context, backupSuccessText, Toast.LENGTH_SHORT).show()
        }
    }

    Spacer(modifier = Modifier.weight(1f))

    var showPreRestoreDialog by rememberSaveable { mutableStateOf(false) }

    Button(
        onClick = { showPreRestoreDialog = true },
    ) {
        Text(
            text = stringResource(id = R.string.restore_data).uppercase(),
            style = MaterialTheme.typography.button
        )
    }

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

    val restoreError = viewModel.restoreError.collectAsStateWithLifecycle().value

    if (restoreError != null) {
        RestoreErrorDialog(
            error = restoreError,
            onDismiss = { viewModel.onRestoreErrorConsumed() },
        )
    }

    if (viewModel.restoreSuccessful.collectAsStateWithLifecycle().value) {
        RestoreCompleteDialog()
    }

    Spacer(modifier = Modifier.weight(1f))
}

@Composable
fun RestoreErrorDialog(error: Int, onDismiss: () -> Unit) = ConfirmDialog(
    continueText = R.string.ok,
    onConfirm = onDismiss,
    onDismissRequest = onDismiss
) {
    Text(
        text = stringResource(id = error),
        style = MaterialTheme.typography.subtitle2,
        color = MaterialTheme.colors.onSurface
    )
}

@Composable
private fun BackupCard(viewModel: BackupAndRestoreViewModel) = Card(
    shape = MaterialTheme.shapes.medium
) {
    Column(
        modifier = Modifier.padding(dimensionResource(id = R.dimen.card_padding_large))
    ) {
        Text(
            text = stringResource(id = R.string.backup_hint_text),
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.onSurface
        )

        InputSpacingLarge()

        val autoBackupInfo = viewModel.autoBackupInfo.collectAsStateWithLifecycle().value

        if (autoBackupInfo != null) AutoBackupInfo(autoBackupInfo)

        AutoBackupControls(
            Modifier.align(Alignment.CenterHorizontally),
            viewModel
        )

        DialogInputSpacing()

        BackupButton(
            Modifier.align(Alignment.CenterHorizontally),
            viewModel
        )
    }
}

@Composable
private fun ColumnScope.AutoBackupInfo(autoBackupInfo: BackupAndRestoreViewModel.AutoBackupInfo) {

    val context = LocalContext.current

    CenterGradientDivider()
    InputSpacingLarge()
    val lastSuccessful = autoBackupInfo.lastSuccessful?.let {
        formatDayMonthYearHourMinuteWeekDayOneLine(context, getWeekDayNames(context), it)
    } ?: stringResource(id = R.string.none)

    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = stringResource(id = R.string.last_successful_backup),
        style = MaterialTheme.typography.subtitle2,
        color = MaterialTheme.colors.onSurface,
    )

    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = lastSuccessful,
        style = MaterialTheme.typography.body1,
        color = MaterialTheme.colors.onSurface,
    )

    InputSpacingLarge()

    val nextScheduled = autoBackupInfo.nextScheduled?.let {
        formatDayMonthYearHourMinuteWeekDayOneLine(context, getWeekDayNames(context), it)
    } ?: stringResource(id = R.string.none)

    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = stringResource(id = R.string.next_scheduled_backup),
        style = MaterialTheme.typography.subtitle2,
        color = MaterialTheme.colors.onSurface,
    )

    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = nextScheduled,
        style = MaterialTheme.typography.body1,
        color = MaterialTheme.colors.onSurface,
    )

    InputSpacingLarge()
    CenterGradientDivider()
    InputSpacingLarge()
}

@Composable
private fun BackupButton(
    modifier: Modifier,
    viewModel: BackupAndRestoreViewModel
) {
    val context = LocalContext.current

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(SQLITE_MIME_TYPE)
    ) { viewModel.exportDatabase(it) }

    Button(
        modifier = modifier,
        onClick = {
            val now = OffsetDateTime.now()
            val generatedName = context.getString(
                R.string.backup_file_name_suffix,
                "TrackAndGraphBackup", now.year, now.monthValue,
                now.dayOfMonth, now.hour, now.minute, now.second
            )
            backupLauncher.launch(generatedName)
        }) {
        Text(
            text = stringResource(id = R.string.backup_now).uppercase(),
            style = MaterialTheme.typography.button
        )
    }
}

@SuppressLint("InlinedApi")
@Composable
private fun AutoBackupControls(modifier: Modifier, viewModel: BackupAndRestoreViewModel) = Row(
    modifier = modifier
        .wrapContentWidth()
        .border(
            width = 1.dp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
            shape = MaterialTheme.shapes.medium
        )
        .padding(start = dimensionResource(id = R.dimen.card_padding)),
    verticalAlignment = Alignment.CenterVertically
) {

    Text(
        text = stringResource(id = R.string.auto_backup).uppercase(),
        style = MaterialTheme.typography.button
    )

    DialogInputSpacing()

    var showConfigureAutoBackupDialog by rememberSaveable { mutableStateOf(false) }

    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()

    Checkbox(
        checked = autoBackupEnabled,
        onCheckedChange = {
            if (it) showConfigureAutoBackupDialog = true
            else viewModel.disableAutoBackup()
        }
    )

    if (autoBackupEnabled) {
        IconButton(onClick = { showConfigureAutoBackupDialog = true }) {
            Icon(
                painter = painterResource(id = R.drawable.edit_icon),
                contentDescription = stringResource(id = R.string.edit)
            )
        }
    }

    val autoBackupViewModel = hiltViewModel<AutoBackupViewModelImpl>()

    var showNotificationPermissionPromptDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (showConfigureAutoBackupDialog) {
        ConfigureAutoBackupDialog(
            viewModel = autoBackupViewModel,
            onConfirm = {
                showConfigureAutoBackupDialog = false

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!hasNotificationPermission(context)) {
                        showNotificationPermissionPromptDialog = true
                    }
                }

                autoBackupViewModel.onConfirmAutoBackup()
            },
            onDismiss = {
                showConfigureAutoBackupDialog = false
                autoBackupViewModel.onCancelConfig()
            }
        )
    }

    val permissionRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    if (showNotificationPermissionPromptDialog) {
        NotificationPermissionPromptDialog(
            onConfirm = {
                showNotificationPermissionPromptDialog = false
                permissionRequestLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = {
                showNotificationPermissionPromptDialog = false
                permissionRequestLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun hasNotificationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun NotificationPermissionPromptDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) = ConfirmDialog(
    onConfirm = onConfirm,
    onDismissRequest = onDismiss,
    continueText = R.string.ok
) {
    Text(
        text = stringResource(id = R.string.notification_permission_prompt),
        style = MaterialTheme.typography.subtitle2,
        color = MaterialTheme.colors.onSurface
    )
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
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.autoBackupConfigFailures.collect {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    CustomConfirmCancelDialog(
        onDismissRequest = onDismiss,
        onConfirm = onConfirm,
        continueText = R.string.apply,
        continueEnabled = viewModel.autoBackupConfigValid.collectAsStateWithLifecycle().value,
    ) {
        AutoBackupInnerLayout(viewModel = viewModel)
    }
}

@Composable
private fun AutoBackupInnerLayout(viewModel: AutoBackupViewModel) = Box(
    modifier = Modifier
        .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.8f)
) {
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        val location = viewModel.autoBackupLocation.collectAsStateWithLifecycle().value

        val selectFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument(SQLITE_MIME_TYPE)
        ) { viewModel.onBackupLocationChanged(it) }

        val defaultAutoBackupFileName = stringResource(id = R.string.default_auto_backup_file_name)

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.backup_location),
            style = MaterialTheme.typography.subtitle2.copy(textAlign = TextAlign.Center),
        )

        DialogInputSpacing()

        if (location != null) {
            Text(
                text = location,
                style = MaterialTheme.typography.body2.copy(textAlign = TextAlign.Center),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            DialogInputSpacing()
        }

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { selectFileLauncher.launch(defaultAutoBackupFileName) },
        ) {

            val text = if (location == null)
                stringResource(id = R.string.select_location)
            else stringResource(id = R.string.select_new_location)

            Text(
                text = text,
                style = MaterialTheme.typography.button
            )
        }

        InputSpacingLarge()
        Divider()
        InputSpacingLarge()

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.backup_every),
            style = MaterialTheme.typography.subtitle2.copy(textAlign = TextAlign.Center),
        )

        DialogInputSpacing()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Spacer(modifier = Modifier.weight(1f))

            MiniNumericTextField(
                modifier = Modifier.width(64.dp),
                textFieldValue = viewModel.autoBackupIntervalTextFieldValue.value,
                onValueChange = viewModel::onBackupIntervalChanged,
                textAlign = TextAlign.Center
            )

            DialogInputSpacing()

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

        InputSpacingLarge()
        Divider()
        InputSpacingLarge()

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.next_backup_at),
            style = MaterialTheme.typography.subtitle2.copy(textAlign = TextAlign.Center),
        )

        DialogInputSpacing()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            DateButton(
                dateTime = viewModel.autoBackupFirstDate.collectAsStateWithLifecycle().value,
                onDateSelected = viewModel::onAutoBackupFirstDateChanged,
                allowPastDates = false
            )

            DialogInputSpacing()

            TimeButton(
                dateTime = viewModel.autoBackupFirstDate.collectAsStateWithLifecycle().value,
                onTimeSelected = viewModel::onAutoBackupFirstDateChanged,
            )
        }
    }
}