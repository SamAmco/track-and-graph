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

import androidx.annotation.StringRes
import androidx.biometric.BiometricManager
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.di.IODispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppLockSettingsState(
    val enabled: Boolean = false,
    val hasPassword: Boolean = false,
    val biometricEnabled: Boolean = false,
    val canAuthenticateWithBiometrics: Boolean = false,
    val lockAfterMinutes: Int = AppLockConfig.DEFAULT_LOCK_AFTER_MINUTES,
    @StringRes val passwordError: Int? = null,
    val isWorking: Boolean = false,
)

@HiltViewModel
class AppLockSettingsViewModel @Inject constructor(
    private val repository: AppLockRepository,
    private val session: AppLockSession,
    private val passwordHasher: AppLockPasswordHasher,
    private val biometricManager: BiometricManager,
    @IODispatcher private val io: CoroutineDispatcher,
) : ViewModel() {

    private val _passwordError = MutableStateFlow<Int?>(null)
    private val _isWorking = MutableStateFlow(false)
    private val _passwordChangedEvents = Channel<Unit>(1)
    private val _errorEvents = Channel<Int>(1)
    val passwordChangedEvents: ReceiveChannel<Unit> = _passwordChangedEvents
    val errorEvents: ReceiveChannel<Int> = _errorEvents
    val lockAfterMinutesText = TextFieldState(
        AppLockConfig.DEFAULT_LOCK_AFTER_MINUTES.toString()
    )

    val state: StateFlow<AppLockSettingsState> = combine(
        repository.config,
        _passwordError,
        _isWorking,
    ) { config, passwordError, isWorking ->
        AppLockSettingsState(
            enabled = config.enabled,
            hasPassword = config.hasPassword,
            biometricEnabled = config.biometricEnabled,
            canAuthenticateWithBiometrics = canAuthenticateWithBiometrics(),
            lockAfterMinutes = config.lockAfterMinutes,
            passwordError = passwordError,
            isWorking = isWorking,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppLockSettingsState(
            canAuthenticateWithBiometrics = canAuthenticateWithBiometrics()
        )
    )

    init {
        viewModelScope.launch {
            val initialConfig = repository.config.first()
            lockAfterMinutesText.setTextAndPlaceCursorAtEnd(
                initialConfig.lockAfterMinutes.toString()
            )

            snapshotFlow { lockAfterMinutesText.text.toString() }
                .distinctUntilChanged()
                .collect { text ->
                    if (_isWorking.value) return@collect
                    val minutes = text.toIntOrNull() ?: return@collect
                    setLockAfterMinutes(minutes)
                }
        }
    }

    fun enable(password: String, confirmPassword: String) {
        if (_isWorking.value) return

        when {
            password.isEmpty() -> {
                _passwordError.value = R.string.app_lock_error_enter_password
                return
            }

            password != confirmPassword -> {
                _passwordError.value = R.string.app_lock_error_passwords_do_not_match
                return
            }
        }

        viewModelScope.launch {
            _isWorking.value = true
            try {
                val biometricsAvailable = canAuthenticateWithBiometrics()
                withContext(io) {
                    val verifier = passwordHasher.createVerifier(password)
                    repository.setConfig(
                        AppLockConfig(
                            enabled = true,
                            passwordSalt = verifier.salt,
                            passwordHash = verifier.hash,
                            passwordHashIterations = verifier.iterations,
                            passwordHashAlgorithm = verifier.algorithm,
                            biometricEnabled = biometricsAvailable,
                            lockAfterMinutes = repository.getConfig().lockAfterMinutes,
                        )
                    )
                }
                _passwordError.value = null
                session.unlock()
            } catch (e: AppLockPasswordHasher.AppLockPasswordHashingException) {
                _errorEvents.send(R.string.app_lock_error_password_hashing_unavailable)
            } finally {
                _isWorking.value = false
            }
        }
    }

    fun disable() {
        if (_isWorking.value) return

        viewModelScope.launch {
            _isWorking.value = true
            withContext(io) {
                repository.setConfig(AppLockConfig())
            }
            _passwordError.value = null
            session.unlock()
            _isWorking.value = false
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        if (_isWorking.value) return

        viewModelScope.launch {
            _isWorking.value = true
            val biometricsAvailable = canAuthenticateWithBiometrics()
            withContext(io) {
                val config = repository.getConfig()
                repository.setConfig(
                    config.copy(
                        biometricEnabled = enabled && biometricsAvailable
                    )
                )
            }
            _isWorking.value = false
        }
    }

    private fun setLockAfterMinutes(minutes: Int) {
        viewModelScope.launch {
            withContext(io) {
                val config = repository.getConfig()
                val lockAfterMinutes = minutes.coerceAtLeast(0)
                if (config.lockAfterMinutes != lockAfterMinutes) {
                    repository.setConfig(
                        config.copy(lockAfterMinutes = lockAfterMinutes)
                    )
                }
            }
        }
    }

    fun changePassword(password: String, confirmPassword: String) {
        if (_isWorking.value) return

        when {
            password.isEmpty() -> {
                _passwordError.value = R.string.app_lock_error_enter_password
                return
            }

            password != confirmPassword -> {
                _passwordError.value = R.string.app_lock_error_passwords_do_not_match
                return
            }
        }

        viewModelScope.launch {
            _isWorking.value = true
            try {
                withContext(io) {
                    val verifier = passwordHasher.createVerifier(password)
                    repository.setConfig(
                        repository.getConfig().copy(
                            passwordSalt = verifier.salt,
                            passwordHash = verifier.hash,
                            passwordHashIterations = verifier.iterations,
                            passwordHashAlgorithm = verifier.algorithm,
                        )
                    )
                }
                _passwordError.value = null
                _passwordChangedEvents.send(Unit)
            } catch (e: AppLockPasswordHasher.AppLockPasswordHashingException) {
                _errorEvents.send(R.string.app_lock_error_password_hashing_unavailable)
            } finally {
                _isWorking.value = false
            }
        }
    }

    fun clearPasswordError() {
        _passwordError.value = null
    }

    private fun canAuthenticateWithBiometrics(): Boolean {
        return biometricManager.canAuthenticate(BIOMETRIC_AUTHENTICATORS) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }
}
