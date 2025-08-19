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
import android.net.Uri
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.settings.mockSettings
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.ui.ContinueDialog
import com.samco.trackandgraph.ui.compose.ui.CustomContinueCancelDialog
import com.samco.trackandgraph.ui.compose.ui.DateButton
import com.samco.trackandgraph.ui.compose.ui.DayMonthYearHourMinuteWeekDayOneLineText
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.MiniNumericTextField
import com.samco.trackandgraph.ui.compose.ui.SelectedTime
import com.samco.trackandgraph.ui.compose.ui.TextButton
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner
import com.samco.trackandgraph.ui.compose.ui.TimeButton
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.ChronoUnit
import kotlin.system.exitProcess

private const val SQLITE_MIME_TYPE = "application/vnd.sqlite3"

@Composable
fun BackupAndRestoreScreen(viewModel: BackupAndRestoreViewModel) {
    val context = LocalContext.current
    val backupSuccessText = stringResource(id = R.string.backup_successful)

    // Handle backup error toasts
    LaunchedEffect(context) {
        viewModel.backupError.filterNotNull().collect {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    // Handle backup success toasts
    LaunchedEffect(context) {
        viewModel.backupSuccessful.filter { it }.collect {
            Toast.makeText(context, backupSuccessText, Toast.LENGTH_SHORT).show()
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { viewModel.restoreDatabase(it) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(SQLITE_MIME_TYPE)
    ) { viewModel.exportDatabase(it) }

    val autoBackupInfo = viewModel.autoBackupInfo.collectAsStateWithLifecycle().value
    val autoBackupEnabled = viewModel.autoBackupEnabled.collectAsStateWithLifecycle().value
    val restoreError = viewModel.restoreError.collectAsStateWithLifecycle().value
    val restoreSuccessful = viewModel.restoreSuccessful.collectAsStateWithLifecycle().value

    BackupAndRestoreView(
        autoBackupInfo = autoBackupInfo,
        autoBackupEnabled = autoBackupEnabled,
        restoreError = restoreError,
        restoreSuccessful = restoreSuccessful,
        onBackupNow = { fileName -> backupLauncher.launch(fileName) },
        onRestoreData = { restoreLauncher.launch(arrayOf("*/*")) },
        onRestoreErrorConsumed = viewModel::onRestoreErrorConsumed,
        onAutoBackupEnabledChanged = { enabled ->
            if (!enabled) viewModel.disableAutoBackup()
        }
    )
}

@Composable
private fun BackupAndRestoreView(
    autoBackupInfo: BackupAndRestoreViewModel.AutoBackupInfo?,
    autoBackupEnabled: Boolean,
    restoreError: Int?,
    restoreSuccessful: Boolean,
    onBackupNow: (String) -> Unit,
    onRestoreData: () -> Unit,
    onRestoreErrorConsumed: () -> Unit,
    onAutoBackupEnabledChanged: (Boolean) -> Unit
) = Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(inputSpacingLarge),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
) {

    InputSpacingLarge()

    BackupCard(
        autoBackupInfo = autoBackupInfo,
        autoBackupEnabled = autoBackupEnabled,
        onBackupNow = onBackupNow,
        onAutoBackupEnabledChanged = onAutoBackupEnabledChanged
    )

    Spacer(modifier = Modifier.weight(1f))

    var showPreRestoreDialog by rememberSaveable { mutableStateOf(false) }

    TextButton(
        onClick = { showPreRestoreDialog = true },
        text = stringResource(id = R.string.restore_data).uppercase()
    )

    if (showPreRestoreDialog) {
        PreRestoreDialog(
            onConfirm = {
                showPreRestoreDialog = false
                onRestoreData()
            },
            onDismiss = { showPreRestoreDialog = false }
        )
    }

    if (restoreError != null) {
        RestoreErrorDialog(
            error = restoreError,
            onDismiss = onRestoreErrorConsumed,
        )
    }

    if (restoreSuccessful) {
        RestoreCompleteDialog()
    }

    Spacer(modifier = Modifier.weight(1f))
}

@Composable
fun RestoreErrorDialog(error: Int, onDismiss: () -> Unit) = ContinueDialog(
    body = error,
    continueText = R.string.ok,
    onConfirm = onDismiss,
    onDismissRequest = onDismiss,
)

@Composable
private fun BackupCard(
    autoBackupInfo: BackupAndRestoreViewModel.AutoBackupInfo?,
    autoBackupEnabled: Boolean,
    onBackupNow: (String) -> Unit,
    onAutoBackupEnabledChanged: (Boolean) -> Unit
) = Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
) {
    Column(
        modifier = Modifier.padding(inputSpacingLarge)
    ) {
        Text(
            text = stringResource(id = R.string.backup_hint_text),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        InputSpacingLarge()

        if (autoBackupInfo != null) AutoBackupInfo(autoBackupInfo)

        AutoBackupControls(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            autoBackupEnabled = autoBackupEnabled,
            onAutoBackupEnabledChanged = onAutoBackupEnabledChanged
        )

        DialogInputSpacing()

        BackupButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onBackupNow = onBackupNow
        )
    }
}

@Composable
private fun ColumnScope.AutoBackupInfo(autoBackupInfo: BackupAndRestoreViewModel.AutoBackupInfo) {
    CenterGradientDivider()
    InputSpacingLarge()

    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = stringResource(id = R.string.last_successful_backup),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )

    if (autoBackupInfo.lastSuccessful != null) {
        DayMonthYearHourMinuteWeekDayOneLineText(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            dateTime = autoBackupInfo.lastSuccessful,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
        )
    } else {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(id = R.string.none),
            style = MaterialTheme.typography.bodyLarge,
        )
    }

    InputSpacingLarge()

    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = stringResource(id = R.string.next_scheduled_backup),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )

    if (autoBackupInfo.nextScheduled != null) {
        DayMonthYearHourMinuteWeekDayOneLineText(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            dateTime = autoBackupInfo.nextScheduled,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
        )
    } else {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(id = R.string.none),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }

    InputSpacingLarge()
    CenterGradientDivider()
    InputSpacingLarge()
}

@Composable
private fun BackupButton(
    modifier: Modifier,
    onBackupNow: (String) -> Unit
) {
    val context = LocalContext.current

    TextButton(
        modifier = modifier,
        onClick = {
            val now = OffsetDateTime.now()
            val generatedName = context.getString(
                R.string.backup_file_name_suffix,
                "TrackAndGraphBackup", now.year, now.monthValue,
                now.dayOfMonth, now.hour, now.minute, now.second
            )
            onBackupNow(generatedName)
        },
        text = stringResource(id = R.string.backup_now).uppercase()
    )
}

@SuppressLint("InlinedApi")
@Composable
private fun AutoBackupControls(
    modifier: Modifier,
    autoBackupEnabled: Boolean,
    onAutoBackupEnabledChanged: (Boolean) -> Unit
) = Row(
    modifier = modifier
        .wrapContentWidth()
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            shape = MaterialTheme.shapes.medium
        )
        .padding(start = cardPadding),
    verticalAlignment = Alignment.CenterVertically
) {

    Text(
        text = stringResource(id = R.string.auto_backup).uppercase(),
        style = MaterialTheme.typography.labelLarge
    )

    DialogInputSpacing()

    var showConfigureAutoBackupDialog by rememberSaveable { mutableStateOf(false) }

    Checkbox(
        checked = autoBackupEnabled,
        onCheckedChange = {
            if (it) showConfigureAutoBackupDialog = true
            else onAutoBackupEnabledChanged(false)
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

    var showNotificationPermissionPromptDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (showConfigureAutoBackupDialog) {
        val autoBackupViewModel = hiltViewModel<AutoBackupViewModelImpl>()

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
) = ContinueDialog(
    body = R.string.notification_permission_prompt,
    onConfirm = onConfirm,
    onDismissRequest = onDismiss,
    continueText = R.string.ok
)

@Composable
private fun PreRestoreDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) = ContinueDialog(
    body = R.string.restore_hint_text,
    onConfirm = onConfirm,
    onDismissRequest = onDismiss
)

@Composable
private fun RestoreCompleteDialog() = ContinueDialog(
    body = R.string.restore_successful,
    onConfirm = { exitProcess(0) },
    dismissOnClickOutside = false,
    onDismissRequest = { },
    continueText = R.string.ok
)

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
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)
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

    val location = viewModel.autoBackupLocation.collectAsStateWithLifecycle().value
    val intervalTextFieldValue = viewModel.autoBackupIntervalTextFieldValue.value
    val backupUnit = viewModel.autoBackupUnit.collectAsStateWithLifecycle().value
    val firstDate = viewModel.autoBackupFirstDate.collectAsStateWithLifecycle().value
    val configValid = viewModel.autoBackupConfigValid.collectAsStateWithLifecycle().value

    CustomContinueCancelDialog(
        onDismissRequest = onDismiss,
        onConfirm = onConfirm,
        continueText = R.string.apply,
        continueEnabled = configValid,
    ) {
        AutoBackupInnerLayout(
            location = location,
            intervalTextFieldValue = intervalTextFieldValue,
            backupUnit = backupUnit,
            firstDate = firstDate,
            onBackupLocationChanged = viewModel::onBackupLocationChanged,
            onBackupIntervalChanged = viewModel::onBackupIntervalChanged,
            onBackupUnitChanged = viewModel::onBackupUnitChanged,
            onAutoBackupFirstDateChanged = viewModel::onAutoBackupFirstDateChanged,
            onAutoBackupFirstTimeChanged = viewModel::onAutoBackupFirstTimeChanged
        )
    }
}

@Composable
private fun AutoBackupInnerLayout(
    location: String?,
    intervalTextFieldValue: TextFieldValue,
    backupUnit: ChronoUnit,
    firstDate: OffsetDateTime,
    onBackupLocationChanged: (Uri?) -> Unit,
    onBackupIntervalChanged: (TextFieldValue) -> Unit,
    onBackupUnitChanged: (ChronoUnit) -> Unit,
    onAutoBackupFirstDateChanged: (OffsetDateTime) -> Unit,
    onAutoBackupFirstTimeChanged: (SelectedTime) -> Unit
) = Column {
    val selectFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(SQLITE_MIME_TYPE)
    ) { onBackupLocationChanged(it) }

    val defaultAutoBackupFileName = stringResource(id = R.string.default_auto_backup_file_name)

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(id = R.string.backup_location),
        style = MaterialTheme.typography.titleSmall.copy(textAlign = TextAlign.Center),
    )

    DialogInputSpacing()

    if (location != null) {
        Text(
            text = location,
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        DialogInputSpacing()
    }

    val buttonText = if (location == null)
        stringResource(id = R.string.select_location)
    else stringResource(id = R.string.select_new_location)

    TextButton(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        onClick = { selectFileLauncher.launch(defaultAutoBackupFileName) },
        text = buttonText
    )

    InputSpacingLarge()
    Divider()
    InputSpacingLarge()

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(id = R.string.backup_every),
        style = MaterialTheme.typography.titleSmall.copy(textAlign = TextAlign.Center),
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
            textFieldValue = intervalTextFieldValue,
            onValueChange = onBackupIntervalChanged,
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
            selectedItem = backupUnit,
            onItemSelected = onBackupUnitChanged
        )

        Spacer(modifier = Modifier.weight(1f))
    }

    InputSpacingLarge()
    Divider()
    InputSpacingLarge()

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(id = R.string.next_backup_at),
        style = MaterialTheme.typography.titleSmall.copy(textAlign = TextAlign.Center),
    )

    DialogInputSpacing()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        DateButton(
            dateTime = firstDate,
            onDateSelected = onAutoBackupFirstDateChanged,
            allowPastDates = false
        )

        DialogInputSpacing()

        TimeButton(
            dateTime = firstDate,
            onTimeSelected = onAutoBackupFirstTimeChanged,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BackupAndRestoreViewPreview() {
    val sampleAutoBackupInfo = BackupAndRestoreViewModel.AutoBackupInfo(
        lastSuccessful = null, //OffsetDateTime.parse("2023-12-15T14:30:00+01:00"),
        nextScheduled = OffsetDateTime.parse("2023-12-22T14:30:00+01:00")
    )

    TnGComposeTheme {
        BackupAndRestoreView(
            autoBackupInfo = sampleAutoBackupInfo,
            autoBackupEnabled = true,
            restoreError = null,
            restoreSuccessful = false,
            onBackupNow = { },
            onRestoreData = { },
            onRestoreErrorConsumed = { },
            onAutoBackupEnabledChanged = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AutoBackupInnerLayoutPreview() {
    TnGComposeTheme {
        CompositionLocalProvider(LocalSettings provides mockSettings) {
            AutoBackupInnerLayout(
                location = "/storage/emulated/0/Download/TrackAndGraphBackup_2023_12_15.db",
                intervalTextFieldValue = TextFieldValue("7"),
                backupUnit = ChronoUnit.DAYS,
                firstDate = OffsetDateTime.parse("2023-12-22T14:30:00+01:00"),
                onBackupLocationChanged = { },
                onBackupIntervalChanged = { },
                onBackupUnitChanged = { },
                onAutoBackupFirstDateChanged = { },
                onAutoBackupFirstTimeChanged = { }
            )
        }
    }
}