package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val smallIconSize = 20.dp

val buttonSize = 45.dp

val cardCornerRadius: Dp = 4.dp

val shapeMedium = 8.dp

val shapeLarge = 16.dp

val shapes: Shapes
    @Composable
    get() = Shapes(
        small = RoundedCornerShape(cardCornerRadius),
        medium = RoundedCornerShape(shapeMedium),
        large = RoundedCornerShape(shapeLarge)
    )
