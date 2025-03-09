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
package com.samco.trackandgraph.ui.compose.theming

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.google.android.material.composethemeadapter.createMdcTheme
import com.samco.trackandgraph.ui.compose.ui.shapes

private val lightGray = Color(0xFFE0E0E0)
private val darkGray = Color(0xFF4C4C4C)
private val midCharcoal = Color(0xFF222222)

data class TngColors(
    val material: Colors,
    val selectorButtonColor: Color,
    val textColorSecondary: Color
) {
    val primary get() = material.primary
    val primaryVariant get() = material.primaryVariant
    val secondary get() = material.secondary
    val secondaryVariant get() = material.secondaryVariant
    val background get() = material.background
    val surface get() = material.surface
    val error get() = material.error
    val onPrimary get() = material.onPrimary
    val onSecondary get() = material.onSecondary
    val onBackground get() = material.onBackground
    val onSurface get() = material.onSurface
    val onError get() = material.onError
    val isLight get() = material.isLight
}

data class TngTypography(
    val materialTypography: Typography,
    val code: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight(750),
        fontSize = materialTypography.body1.fontSize,
        lineHeight = 22.sp,
    )
) {
    val h1 get() = materialTypography.h1
    val h2 get() = materialTypography.h2
    val h3 get() = materialTypography.h3
    val h4 get() = materialTypography.h4
    val h5 get() = materialTypography.h5
    val h6 get() = materialTypography.h6
    val subtitle1 get() = materialTypography.subtitle1
    val subtitle2 get() = materialTypography.subtitle2
    val body1 get() = materialTypography.body1
    val body2 get() = materialTypography.body2
    val button get() = materialTypography.button
    val caption get() = materialTypography.caption
    val overline get() = materialTypography.overline
}

private val LightColorPalette = TngColors(
    material = lightColors(),
    selectorButtonColor = lightGray,
    textColorSecondary = darkGray
)

private val DarkColorPalette = TngColors(
    material = darkColors(),
    selectorButtonColor = darkGray,
    textColorSecondary = lightGray
)

val TngColors.disabledAlpha get() = 0.4f

private val LocalColors = staticCompositionLocalOf { LightColorPalette }

private val LocalTypography = staticCompositionLocalOf<TngTypography> {
    error("No typography provided")
}

val MaterialTheme.tngColors: TngColors
    @Composable
    @ReadOnlyComposable
    get() = LocalColors.current

val MaterialTheme.tngTypography: TngTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalTypography.current

@Composable
fun TnGComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    block: @Composable () -> Unit
) {
    val (materialColors, typography, _) = createMdcTheme(
        context = LocalContext.current,
        layoutDirection = LocalLayoutDirection.current
    )

    val colors = tngColors(darkTheme, materialColors)
    val tngTypography = TngTypography(typography ?: Typography())

    CompositionLocalProvider(LocalColors provides colors) {
        CompositionLocalProvider(LocalTypography provides tngTypography) {
            MaterialTheme(
                colors = colors.material,
                typography = typography ?: MaterialTheme.typography,
                shapes = shapes,
                content = block
            )
        }
    }
}

@Composable
fun DialogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    block: @Composable () -> Unit
) = TnGComposeTheme(darkTheme) {

    val colors = MaterialTheme.tngColors.let {
        if (darkTheme) {
            it.copy(
                material = MaterialTheme.tngColors.material.copy(
                    background = midCharcoal,
                    surface = midCharcoal,
                )
            )
        } else it
    }

    CompositionLocalProvider(LocalColors provides colors) {
        MaterialTheme(
            colors = colors.material,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = block
        )
    }
}

@Composable
private fun tngColors(
    darkTheme: Boolean,
    materialColors: Colors?
) = if (darkTheme) {
    DarkColorPalette.copy(material = materialColors ?: darkColors())
} else {
    LightColorPalette.copy(material = materialColors ?: lightColors())
}
