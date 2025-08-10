/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.disabledAlpha
import com.samco.trackandgraph.ui.compose.theming.tngColors

/**
 * Internal composable containing the shared loading overlay content
 */
@Composable
private fun LoadingOverlayContent() {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
            .focusable(),
        color = MaterialTheme.tngColors.surface.copy(
            alpha = MaterialTheme.tngColors.disabledAlpha
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Loading overlay that fills the maximum available size
 */
@Composable
fun LoadingOverlay(
    modifier: Modifier = Modifier
) = Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) {
    LoadingOverlayContent()
}

/**
 * Loading overlay that matches parent size within a BoxScope
 */
@Composable
fun BoxScope.LoadingOverlay() = Box(
    modifier = Modifier.matchParentSize(),
    contentAlignment = Alignment.Center
) {
    LoadingOverlayContent()
}

@Preview(showBackground = true)
@Composable
private fun LoadingOverlayPreview() {
    TnGComposeTheme {
        Box(
            modifier = Modifier.height(300.dp)
        ) {
            // Sample content that should be covered by the overlay
            Column {
                Text(
                    text = "This is sample content that should be covered by the loading overlay",
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = "The overlay should cover this content completely with a semi-transparent background",
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Loading overlay using the regular composable
            LoadingOverlay()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BoxScopeLoadingOverlayPreview() {
    TnGComposeTheme {
        Box(
            modifier = Modifier.height(250.dp)
        ) {
            // Sample content that should be covered by the overlay
            Text(
                text = "Sample BoxScope content",
                style = MaterialTheme.typography.h5,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                text = "This demonstrates the BoxScope loading overlay",
                style = MaterialTheme.typography.body2,
                modifier = Modifier.align(Alignment.BottomEnd)
            )

            // Loading overlay using the BoxScope extension
            LoadingOverlay()
        }
    }
}
