@file:OptIn(ExperimentalFoundationApi::class)

package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.dimensionResource
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
) {
    Surface(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongPress
        ),
        border = BorderStroke(
            1.4.dp,
            if (isSelected) SolidColor(MaterialTheme.tngColors.primary)
            else SolidColor(MaterialTheme.tngColors.secondary)
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = text,
            Modifier.padding(dimensionResource(id = R.dimen.card_padding)),
            fontSize = MaterialTheme.typography.subtitle2.fontSize,
            fontWeight = MaterialTheme.typography.subtitle2.fontWeight,
        )
    }
}
