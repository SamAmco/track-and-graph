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
import androidx.compose.runtime.remember
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
import com.samco.trackandgraph.ui.compose.theming.tngColors

@Preview
@Composable
fun TextChip(
    modifier: Modifier = Modifier,
    text: String = "Chip",
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {}
) = TngChip(
    modifier = modifier,
    onClick = onClick,
    onLongPress = onLongPress,
    isSelected = isSelected
) {
    Text(
        text = text,
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
            .combinedClickable(
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongPress,
                interactionSource = interactionSource
            )
            .widthIn(min = 60.dp),
        border = BorderStroke(
            1.4.dp,
            if (isSelected || buttonDown) SolidColor(MaterialTheme.tngColors.primary)
            else SolidColor(MaterialTheme.tngColors.secondary)
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