package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme

@Composable
fun GradientDivider(
    modifier: Modifier = Modifier
) = Box(
    modifier = modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)
                ),
            ),
        ),
)

@Composable
fun Divider(
    modifier: Modifier = Modifier
) = Box(
    modifier = modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
)

@Preview
@Composable
fun GradientDividerPreview() = TnGComposeTheme {
    Column(
        modifier = Modifier
            .width(200.dp)
            .height(100.dp)
    ) {
        DialogInputSpacing()
        GradientDivider()
    }
}