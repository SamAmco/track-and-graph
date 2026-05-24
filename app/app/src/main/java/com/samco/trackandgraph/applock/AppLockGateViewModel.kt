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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.di.IODispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppLockGateState(
    val initialized: Boolean = false,
    val enabled: Boolean = false,
    val unlocked: Boolean = true,
    val biometricEnabled: Boolean = false,
    val canAuthenticateWithBiometrics: Boolean = false,
    val isWorking: Boolean = false,
)

@HiltViewModel
class AppLockGateViewModel @Inject constructor(
    private val repository: AppLockRepository,
    private val session: AppLockSession,
    private val passwordHasher: AppLockPasswordHasher,
    private val biometricManager: BiometricManager,
    @IODispatcher private val io: CoroutineDispatcher,
) : ViewModel() {

    private val _passwordError = MutableStateFlow<Int?>(null)
    private val _isWorking = MutableStateFlow(false)
    val passwordError: StateFlow<Int?> = _passwordError.asStateFlow()

    val state: StateFlow<AppLockGateState> = combine(
        repository.config,
        session.unlocked,
        _isWorking,
    ) { config, unlocked, isWorking ->
        AppLockGateState(
            initialized = true,
            enabled = config.enabled,
            unlocked = !config.enabled || unlocked,
            biometricEnabled = config.biometricEnabled,
            canAuthenticateWithBiometrics = canAuthenticateWithBiometrics(),
            isWorking = isWorking,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppLockGateState()
    )

    fun unlockWithPassword(password: String) {
        if (_isWorking.value) return

        viewModelScope.launch {
            _isWorking.value = true
            val result = withContext(io) {
                val config = repository.getConfig()
                if (!config.enabled) return@withContext UnlockResult.Success

                val salt = config.passwordSalt
                val hash = config.passwordHash
                val iterations = config.passwordHashIterations
                val algorithm = config.passwordHashAlgorithm
                if (salt == null || hash == null || iterations == null || algorithm == null) {
                    return@withContext UnlockResult.Error(
                        R.string.app_lock_error_missing_password
                    )
                }

                try {
                    if (passwordHasher.verify(password, salt, hash, iterations, algorithm)) {
                        UnlockResult.Success
                    } else {
                        UnlockResult.Error(R.string.app_lock_error_incorrect_password)
                    }
                } catch (e: AppLockPasswordHasher.AppLockPasswordHashingException) {
                    UnlockResult.Error(R.string.app_lock_error_password_hashing_unavailable)
                }
            }

            when (result) {
                is UnlockResult.Error -> _passwordError.value = result.messageRes
                UnlockResult.Success -> {
                    _passwordError.value = null
                    session.unlock()
                }
            }
            _isWorking.value = false
        }
    }

    fun unlockAfterBiometric() {
        _passwordError.value = null
        session.unlock()
    }

    fun clearPasswordError() {
        _passwordError.value = null
    }

    private fun canAuthenticateWithBiometrics(): Boolean {
        return biometricManager.canAuthenticate(BIOMETRIC_AUTHENTICATORS) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    private sealed interface UnlockResult {
        data object Success : UnlockResult
        data class Error(@StringRes val messageRes: Int) : UnlockResult
    }
}
