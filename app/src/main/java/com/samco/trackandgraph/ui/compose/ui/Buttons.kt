package com.samco.trackandgraph.ui.compose.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.theming.tngColors

@Composable
fun SmallTextButton(
    @StringRes stringRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = TextButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    shape = MaterialTheme.shapes.small,
    colors = colors,
    interactionSource = interactionSource,
) {
    Text(stringResource(stringRes))
}

@Composable
fun SelectorTextButton(
    modifier: Modifier,
    text: String,
    onClick: () -> Unit
) = Button(
    modifier = modifier,
    onClick = onClick,
    shape = MaterialTheme.shapes.small,
    contentPadding = PaddingValues(8.dp),
    colors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.tngColors.selectorButtonColor
    )
) {
    Text(
        text = text,
        fontWeight = MaterialTheme.typography.subtitle2.fontWeight,
        fontSize = MaterialTheme.typography.subtitle2.fontSize,
    )
}