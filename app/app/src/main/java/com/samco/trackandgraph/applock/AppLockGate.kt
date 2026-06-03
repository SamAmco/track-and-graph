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

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.ui.PasswordTextField
import com.samco.trackandgraph.ui.ui.SmallTextButton
import com.samco.trackandgraph.ui.ui.TextButton
import com.samco.trackandgraph.ui.ui.inputSpacingXLarge

@Composable
fun AppLockGate(
    viewModel: AppLockGateViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TnGComposeTheme {
        when {
            !state.initialized -> AppLockGateLoading()
            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    content()

                    if (state.enabled && !state.unlocked) {
                        AppUnlockDialog(state = state, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLockGateLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.safeDrawing.asPaddingValues()),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun AppLockGateLoadingPreview() {
    TnGComposeTheme {
        AppLockGateLoading()
    }
}

@Composable
private fun AppUnlockDialog(
    state: AppLockGateState,
    viewModel: AppLockGateViewModel,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            decorFitsSystemWindows = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        AppUnlockScreen(
            state = state,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun AppUnlockScreen(
    state: AppLockGateState,
    viewModel: AppLockGateViewModel,
) {
    val context = LocalContext.current
    val passwordError by viewModel.passwordError.collectAsStateWithLifecycle()
    var password by rememberSaveable { mutableStateOf("") }
    var biometricError by remember { mutableStateOf<String?>(null) }
    var biometricPromptShown by rememberSaveable { mutableStateOf(false) }

    fun showBiometricPrompt() {
        biometricError = null
        val activity = context.findFragmentActivity() ?: return
        activity.showAppLockBiometricPrompt(
            onSuccess = viewModel::unlockAfterBiometric,
            onError = { biometricError = it },
            onUsePassword = {},
        )
    }

    LaunchedEffect(state.biometricEnabled, state.canAuthenticateWithBiometrics) {
        if (state.biometricEnabled &&
            state.canAuthenticateWithBiometrics &&
            !biometricPromptShown
        ) {
            biometricPromptShown = true
            showBiometricPrompt()
        }
    }

    AppUnlockContent(
        password = password,
        onPasswordChanged = {
            password = it
            viewModel.clearPasswordError()
        },
        passwordError = passwordError,
        biometricError = biometricError,
        showBiometricUnlock = state.biometricEnabled && state.canAuthenticateWithBiometrics,
        isWorking = state.isWorking,
        onUnlock = { viewModel.unlockWithPassword(password) },
        onBiometricUnlock = {
            biometricPromptShown = true
            showBiometricPrompt()
        },
    )
}

@Composable
private fun AppUnlockContent(
    password: String,
    onPasswordChanged: (String) -> Unit,
    passwordError: Int?,
    biometricError: String?,
    showBiometricUnlock: Boolean,
    isWorking: Boolean,
    onUnlock: () -> Unit,
    onBiometricUnlock: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .consumePointerInput()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.safeDrawing.asPaddingValues())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(inputSpacingXLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )

            InputSpacingLarge()

            Text(
                text = stringResource(R.string.app_lock_locked_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            InputSpacingLarge()

            PasswordTextField(
                modifier = Modifier.fillMaxWidth(),
                value = password,
                onValueChange = onPasswordChanged,
                label = stringResource(R.string.app_lock_password_label),
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (password.isNotEmpty()) onUnlock()
                    }
                ),
                supportingText = {
                    passwordError?.let { Text(stringResource(it)) }
                },
                isError = passwordError != null,
            )

            DialogInputSpacing()

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onUnlock,
                enabled = password.isNotEmpty() && !isWorking,
                loading = isWorking,
                text = stringResource(R.string.app_lock_unlock),
            )

            if (showBiometricUnlock) {
                DialogInputSpacing()

                SmallTextButton(
                    text = stringResource(R.string.app_lock_use_biometrics),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isWorking,
                    onClick = onBiometricUnlock,
                )
            }

            biometricError?.let {
                DialogInputSpacing()
                Text(
                    text = it,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun Modifier.consumePointerInput() = pointerInput(Unit) {
    awaitEachGesture {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppUnlockContentPreview() {
    TnGComposeTheme {
        AppUnlockContent(
            password = "",
            onPasswordChanged = {},
            passwordError = null,
            biometricError = null,
            showBiometricUnlock = true,
            isWorking = false,
            onUnlock = {},
            onBiometricUnlock = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppUnlockContentErrorPreview() {
    TnGComposeTheme {
        AppUnlockContent(
            password = "wrong",
            onPasswordChanged = {},
            passwordError = R.string.app_lock_error_incorrect_password,
            biometricError = stringResource(R.string.app_lock_biometric_not_recognized),
            showBiometricUnlock = true,
            isWorking = false,
            onUnlock = {},
            onBiometricUnlock = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppUnlockContentWorkingPreview() {
    TnGComposeTheme {
        AppUnlockContent(
            password = "password",
            onPasswordChanged = {},
            passwordError = null,
            biometricError = null,
            showBiometricUnlock = true,
            isWorking = true,
            onUnlock = {},
            onBiometricUnlock = {},
        )
    }
}
