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
package com.samco.trackandgraph.ui.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.ui.R
import com.samco.trackandgraph.ui.theming.TnGComposeTheme

@Composable
fun PasswordTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPasswordIconRes: Int = R.drawable.visibility_24px,
    hidePasswordIconRes: Int = R.drawable.visibility_off_24px,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    painter = painterResource(
                        if (passwordVisible) hidePasswordIconRes else showPasswordIconRes
                    ),
                    contentDescription = if (passwordVisible) {
                        "Hide password"
                    } else {
                        "Show password"
                    },
                )
            }
        },
        supportingText = supportingText,
        isError = isError,
    )
}

@Preview(showBackground = true)
@Composable
private fun PasswordTextFieldPreview() {
    TnGComposeTheme {
        Column(
            modifier = Modifier.padding(inputSpacingLarge),
        ) {
            PasswordTextField(
                modifier = Modifier.fillMaxWidth(),
                value = "password",
                onValueChange = {},
                label = "Password",
            )

            DialogInputSpacing()

            PasswordTextField(
                modifier = Modifier.fillMaxWidth(),
                value = "mismatch",
                onValueChange = {},
                label = "Confirm password",
                supportingText = { Text("Passwords do not match.") },
                isError = true,
            )

            DialogInputSpacing()

            PasswordTextField(
                modifier = Modifier.fillMaxWidth(),
                value = "disabled",
                onValueChange = {},
                label = "Disabled password",
                enabled = false,
            )
        }
    }
}
