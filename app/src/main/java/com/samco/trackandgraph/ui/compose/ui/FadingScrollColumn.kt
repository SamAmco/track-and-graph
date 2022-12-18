package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.theming.tngColors

@Composable
fun FadingScrollColumn(
    modifier: Modifier,
    size: Dp = 32.dp,
    color: Color = MaterialTheme.tngColors.surface,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()

    val bottomColors = listOf(Color.Transparent, color)
    val topColors = listOf(color, Color.Transparent)

    Box {
        Column(
            modifier = modifier.verticalScroll(scrollState),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content
        )

        if (scrollState.value > 1) {
            Spacer(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(size)
                    .background(brush = Brush.verticalGradient(topColors))
            )
        }

        if (scrollState.value < scrollState.maxValue - 1) {
            Spacer(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(size)
                    .background(brush = Brush.verticalGradient(bottomColors))
            )
        }
    }
}
