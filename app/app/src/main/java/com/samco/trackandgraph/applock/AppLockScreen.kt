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
package com.samco.trackandgraph.applock

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.R
import com.samco.trackandgraph.remoteconfig.UrlNavigator
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.ui.Divider
import com.samco.trackandgraph.ui.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.ui.PasswordTextField
import com.samco.trackandgraph.ui.ui.RowCheckbox
import com.samco.trackandgraph.ui.ui.RowCheckboxPosition
import com.samco.trackandgraph.ui.ui.SmallTextButton
import com.samco.trackandgraph.ui.ui.TextButton
import com.samco.trackandgraph.ui.ui.cardPadding
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.Serializable

@Serializable
data object AppLockNavKey : NavKey

@Composable
fun AppLockScreen(navArgs: AppLockNavKey, urlNavigator: UrlNavigator) {
    val viewModel: AppLockSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val passwordChangedText = stringResource(R.string.app_lock_password_changed)
    var passwordChangedEventCount by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(context, viewModel) {
        viewModel.passwordChangedEvents.receiveAsFlow().collect {
            Toast.makeText(context, passwordChangedText, Toast.LENGTH_SHORT).show()
            passwordChangedEventCount++
        }
    }

    LaunchedEffect(context, viewModel) {
        viewModel.errorEvents.receiveAsFlow().collect { messageRes ->
            Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_LONG).show()
        }
    }

    TopAppBarContent(navArgs, urlNavigator)

    AppLockContent(
        state = state,
        lockAfterMinutesText = viewModel.lockAfterMinutesText,
        passwordChangedEventCount = passwordChangedEventCount,
        onEnable = viewModel::enable,
        onDisable = viewModel::disable,
        onBiometricEnabledChanged = viewModel::setBiometricEnabled,
        onChangePassword = viewModel::changePassword,
        onClearPasswordError = viewModel::clearPasswordError,
    )
}

@Preview(showBackground = true)
@Composable
private fun AppLockContentDisabledPreview() {
    TnGComposeTheme {
        AppLockContent(
            state = AppLockSettingsState(
                enabled = false,
                canAuthenticateWithBiometrics = true,
            ),
            lockAfterMinutesText = TextFieldState(
                AppLockConfig.DEFAULT_LOCK_AFTER_MINUTES.toString()
            ),
            passwordChangedEventCount = 0,
            onEnable = { _, _ -> },
            onDisable = {},
            onBiometricEnabledChanged = {},
            onChangePassword = { _, _ -> },
            onClearPasswordError = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppLockContentDisabledWorkingPreview() {
    TnGComposeTheme {
        AppLockContent(
            state = AppLockSettingsState(
                enabled = false,
                isWorking = true,
            ),
            lockAfterMinutesText = TextFieldState(
                AppLockConfig.DEFAULT_LOCK_AFTER_MINUTES.toString()
            ),
            passwordChangedEventCount = 0,
            onEnable = { _, _ -> },
            onDisable = {},
            onBiometricEnabledChanged = {},
            onChangePassword = { _, _ -> },
            onClearPasswordError = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppLockContentEnabledPreview() {
    TnGComposeTheme {
        AppLockContent(
            state = AppLockSettingsState(
                enabled = true,
                hasPassword = true,
                biometricEnabled = true,
                canAuthenticateWithBiometrics = true,
                lockAfterMinutes = 5,
            ),
            lockAfterMinutesText = TextFieldState("5"),
            passwordChangedEventCount = 0,
            onEnable = { _, _ -> },
            onDisable = {},
            onBiometricEnabledChanged = {},
            onChangePassword = { _, _ -> },
            onClearPasswordError = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppLockContentEnabledErrorPreview() {
    TnGComposeTheme {
        AppLockContent(
            state = AppLockSettingsState(
                enabled = true,
                hasPassword = true,
                biometricEnabled = false,
                canAuthenticateWithBiometrics = false,
                lockAfterMinutes = 0,
                passwordError = R.string.app_lock_error_passwords_do_not_match,
            ),
            lockAfterMinutesText = TextFieldState("0"),
            passwordChangedEventCount = 0,
            onEnable = { _, _ -> },
            onDisable = {},
            onBiometricEnabledChanged = {},
            onChangePassword = { _, _ -> },
            onClearPasswordError = {},
        )
    }
}

@Composable
private fun TopAppBarContent(navArgs: AppLockNavKey, urlNavigator: UrlNavigator) {
    val context = LocalContext.current

    LocalTopBarController.current.Set(
        navArgs,
        AppBarConfig(
            title = stringResource(R.string.app_lock_title),
            appBarPinned = true,
            actions = {
                IconButton(
                    onClick = {
                        urlNavigator.triggerNavigation(
                            context,
                            UrlNavigator.Location.APP_LOCK_DOCS,
                        )
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.about_icon),
                        contentDescription = stringResource(R.string.info),
                    )
                }
            },
        )
    )
}

@Composable
private fun AppLockContent(
    state: AppLockSettingsState,
    lockAfterMinutesText: TextFieldState,
    passwordChangedEventCount: Int,
    onEnable: (String, String) -> Unit,
    onDisable: () -> Unit,
    onBiometricEnabledChanged: (Boolean) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onClearPasswordError: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    WindowInsets.safeDrawing
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        .asPaddingValues()
                )
                .verticalScroll(rememberScrollState())
                .padding(cardPadding),
        ) {
            if (state.enabled) {
                EnabledContent(
                    state = state,
                    lockAfterMinutesText = lockAfterMinutesText,
                    passwordChangedEventCount = passwordChangedEventCount,
                    onDisable = onDisable,
                    onBiometricEnabledChanged = onBiometricEnabledChanged,
                    onChangePassword = onChangePassword,
                    onClearPasswordError = onClearPasswordError,
                )
            } else {
                DisabledContent(
                    state = state,
                    onEnable = onEnable,
                    onClearPasswordError = onClearPasswordError,
                )
            }
        }
    }
}

@Composable
private fun DisabledContent(
    state: AppLockSettingsState,
    onEnable: (String, String) -> Unit,
    onClearPasswordError: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    PasswordFields(
        password = password,
        confirmPassword = confirmPassword,
        passwordError = state.passwordError,
        enabled = !state.isWorking,
        onPasswordChanged = {
            password = it
            onClearPasswordError()
        },
        onConfirmPasswordChanged = {
            confirmPassword = it
            onClearPasswordError()
        },
    )

    InputSpacingLarge()

    TextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onEnable(password, confirmPassword) },
        enabled = password.isNotEmpty() &&
                confirmPassword.isNotEmpty() &&
                password == confirmPassword &&
                !state.isWorking,
        loading = state.isWorking,
        text = stringResource(R.string.app_lock_enable),
    )
}

@Composable
private fun EnabledContent(
    state: AppLockSettingsState,
    lockAfterMinutesText: TextFieldState,
    passwordChangedEventCount: Int,
    onDisable: () -> Unit,
    onBiometricEnabledChanged: (Boolean) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onClearPasswordError: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(passwordChangedEventCount) {
        if (passwordChangedEventCount > 0) {
            password = ""
            confirmPassword = ""
        }
    }

    Text(
        text = stringResource(R.string.app_lock_enabled),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )


    if (state.canAuthenticateWithBiometrics) {
        DialogInputSpacing()
        RowCheckbox(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.app_lock_enable_biometric_unlock),
            checked = state.biometricEnabled,
            onCheckedChange = onBiometricEnabledChanged,
            checkboxPosition = RowCheckboxPosition.End,
        )
        DialogInputSpacing()
    } else {
        InputSpacingLarge()
        Text(
            text = stringResource(R.string.app_lock_no_strong_biometric_available),
            style = MaterialTheme.typography.bodyMedium,
        )
        InputSpacingLarge()
    }

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        state = lockAfterMinutesText,
        label = { Text(stringResource(R.string.app_lock_lock_after_background_minutes)) },
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = DigitsOnlyInputTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        supportingText = {
            Text(stringResource(R.string.app_lock_lock_after_background_minutes_hint))
        },
    )

    InputSpacingLarge()
    Divider()
    InputSpacingLarge()

    Text(
        text = stringResource(R.string.app_lock_change_password),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )

    DialogInputSpacing()

    PasswordFields(
        password = password,
        confirmPassword = confirmPassword,
        passwordError = state.passwordError,
        enabled = !state.isWorking,
        onPasswordChanged = {
            password = it
            onClearPasswordError()
        },
        onConfirmPasswordChanged = {
            confirmPassword = it
            onClearPasswordError()
        },
    )

    DialogInputSpacing()

    TextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onChangePassword(password, confirmPassword) },
        enabled = password.isNotEmpty() && confirmPassword.isNotEmpty() && !state.isWorking,
        loading = state.isWorking,
        text = stringResource(R.string.app_lock_change_password),
    )

    InputSpacingLarge()
    Divider()
    DialogInputSpacing()

    SmallTextButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.app_lock_disable),
        onClick = onDisable,
        enabled = !state.isWorking,
    )
}

private val DigitsOnlyInputTransformation = InputTransformation {
    var index = 0
    while (index < length) {
        if (charAt(index).isDigit()) {
            index++
        } else {
            replace(index, index + 1, "")
        }
    }
}

@Composable
private fun PasswordFields(
    password: String,
    confirmPassword: String,
    passwordError: Int?,
    enabled: Boolean,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
) {
    PasswordTextField(
        modifier = Modifier.fillMaxWidth(),
        value = password,
        onValueChange = onPasswordChanged,
        label = stringResource(R.string.app_lock_password_label),
        enabled = enabled,
        imeAction = ImeAction.Next,
    )

    DialogInputSpacing()

    PasswordTextField(
        modifier = Modifier.fillMaxWidth(),
        value = confirmPassword,
        onValueChange = onConfirmPasswordChanged,
        label = stringResource(R.string.app_lock_confirm_password_label),
        enabled = enabled,
        imeAction = ImeAction.Done,
        supportingText = {
            passwordError?.let { Text(stringResource(it)) }
        },
        isError = passwordError != null,
    )
}
