package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.theming.tngColors

@Composable
fun FadingLazyRow(
    modifier: Modifier = Modifier,
    size: Float = 32f,
    color: Color = MaterialTheme.tngColors.surface,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    state: LazyListState = rememberLazyListState(),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal =
        if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    val endColors = listOf(Color.Transparent, color)
    val startColors = listOf(color, Color.Transparent)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.99f }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.horizontalGradient(startColors, 0f, size),
                        size = Size(size, this.size.height),
                        blendMode = BlendMode.Companion.SrcOver
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            endColors,
                            this.size.width - size,
                            this.size.width
                        ),
                        topLeft = Offset(this.size.width - size, 0f),
                        size = Size(size, this.size.height),
                        blendMode = BlendMode.Companion.SrcOver
                    )
                },
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            content = content
        )
    }
}
