package com.samco.trackandgraph.ui.compose.theming

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import com.google.android.material.composethemeadapter3.createMdc3Theme

@Composable
fun TnGComposeTheme(block: @Composable () -> Unit) {
    val (colorScheme, typography, shapes) = createMdc3Theme(
        context = LocalContext.current,
        layoutDirection = LocalLayoutDirection.current
    )
    MaterialTheme(
        colorScheme = colorScheme ?: MaterialTheme.colorScheme,
        typography = typography ?: MaterialTheme.typography,
        shapes = shapes ?: MaterialTheme.shapes,
        content = block
    )
}
