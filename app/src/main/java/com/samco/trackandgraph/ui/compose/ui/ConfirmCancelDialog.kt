package com.samco.trackandgraph.ui.compose.ui

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R

@Composable
fun ConfirmCancelDialog(
    @StringRes body: Int,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    @StringRes continueText: Int = R.string.continue_word,
    @StringRes dismissText: Int = R.string.cancel
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    shape = MaterialTheme.shapes.small,
    text = {
        Text(text = stringResource(id = body))
    },
    confirmButton = {
        TextButton(
            onClick = onConfirm,
            shape = MaterialTheme.shapes.small
        ) {
            Text(stringResource(id = continueText))
        }
    },
    dismissButton = {
        TextButton(
            onClick = onDismissRequest,
            shape = MaterialTheme.shapes.small
        ) {
            Text(stringResource(id = dismissText))
        }
    }
)
