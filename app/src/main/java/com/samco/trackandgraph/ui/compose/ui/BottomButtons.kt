package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.tngColors

@Composable
fun AddCancelBottomButtons(
    modifier: Modifier = Modifier,
    updateMode: Boolean,
    onCancelClicked: () -> Unit,
    onAddClicked: () -> Unit,
    addButtonEnabled: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SmallTextButton(
            stringRes = R.string.cancel,
            onClick = onCancelClicked,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.tngColors.onSurface
            )
        )
        val addButtonRes = if (updateMode) R.string.update else R.string.add
        SmallTextButton(
            stringRes = addButtonRes,
            enabled = addButtonEnabled,
            onClick = {
                focusManager.clearFocus()
                onAddClicked()
            }
        )
    }
}