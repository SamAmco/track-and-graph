package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.theming.tngColors

@Composable
fun CheckboxLabeledExpandingSection(
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    label: String,
    focusRequester: FocusRequester,
    input: @Composable (modifier: Modifier) -> Unit
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
        .alpha(if (checked) 1.0f else MaterialTheme.tngColors.disabledAlpha)
        .fillMaxWidth()
        .border(
            BorderStroke(1.dp, MaterialTheme.tngColors.onSurface),
            shape = MaterialTheme.shapes.small
        )
        .padding(cardMarginSmall)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onCheckedChanged(!checked) }
            .fillMaxWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChanged(it) }
        )
        InputSpacingLarge()
        Text(text = label)
    }
    if (checked) {
        input(
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}