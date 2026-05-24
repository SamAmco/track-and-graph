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

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.samco.trackandgraph.R

fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

fun FragmentActivity.showAppLockBiometricPrompt(
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onUsePassword: () -> Unit,
) {
    val prompt = BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED
                ) {
                    onError(errString.toString())
                } else {
                    onUsePassword()
                }
            }

            override fun onAuthenticationFailed() {
                onError(getString(R.string.app_lock_biometric_not_recognized))
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(getString(R.string.app_lock_unlock_biometric_title))
        .setSubtitle(getString(R.string.app_lock_unlock_biometric_subtitle))
        .setAllowedAuthenticators(BIOMETRIC_AUTHENTICATORS)
        .setConfirmationRequired(false)
        .setNegativeButtonText(getString(R.string.app_lock_use_password))
        .build()

    prompt.authenticate(promptInfo)
}
