@file:OptIn(ExperimentalFoundationApi::class)

package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors


@Composable
fun TextChip(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean = false,
    isEnabled: Boolean = true,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {}
) = TngChip(
    modifier = modifier.widthIn(min = buttonSize),
    isSelected = isSelected,
    isEnabled = isEnabled,
    onClick = onClick,
    onLongPress = onLongPress,
) {
    Text(
        modifier = Modifier.padding(horizontal = cardMarginSmall),
        text = text,
        color = if (isSelected)
            MaterialTheme.tngColors.onPrimary
        else MaterialTheme.tngColors.onSurface,
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
fun AddChipButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) = TngChip(
    modifier = modifier,
    onClick = onClick,
    shape = RoundedCornerShape(100.dp),
    contentPaddingValues = PaddingValues(cardMarginSmall)
) {
    Icon(
        painter = painterResource(id = R.drawable.add_icon),
        contentDescription = stringResource(id = R.string.add_a_note),
        tint = MaterialTheme.tngColors.onSurface
    )
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.tngColors.onSurface
    )
    DialogInputSpacing()
}

@Composable
fun TngChip(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isEnabled: Boolean = true,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
    shape: Shape = MaterialTheme.shapes.medium,
    contentPaddingValues: PaddingValues = PaddingValues(
        horizontal = cardPadding,
        vertical = 8.dp
    ),
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val buttonDown by interactionSource.collectIsPressedAsState()

    Surface(
        modifier = modifier
            .clip(shape)
            .let {
                if (isEnabled) it.combinedClickable(
                    indication = LocalIndication.current,
                    onClick = onClick,
                    onLongClick = onLongPress,
                    interactionSource = interactionSource
                ) else it
            }
            .widthIn(min = 60.dp),
        color =
            if (isSelected || (isEnabled && buttonDown)) MaterialTheme.tngColors.primary
            else MaterialTheme.tngColors.surface,
        border = BorderStroke(
            2.dp,
            if (isSelected || (isEnabled && buttonDown)) SolidColor(MaterialTheme.tngColors.primary)
            else SolidColor(MaterialTheme.tngColors.selectorButtonColor)
        ),
        shape = shape,
    ) {
        Row(
            Modifier.padding(contentPaddingValues),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TextChipPreview() {
    TnGComposeTheme {
        var isSelected by remember { mutableStateOf(false) }

        TextChip(
            text = "Sample Chip",
            isSelected = isSelected,
            onClick = { isSelected = !isSelected }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TextChipSelectedPreview() {
    TnGComposeTheme {
        var isSelected by remember { mutableStateOf(true) }

        TextChip(
            text = "Sample Chip",
            isSelected = isSelected,
            onClick = { isSelected = !isSelected }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddChipButtonPreview() {
    TnGComposeTheme {
        AddChipButton(
            text = "Add Item",
            onClick = { }
        )
    }
}