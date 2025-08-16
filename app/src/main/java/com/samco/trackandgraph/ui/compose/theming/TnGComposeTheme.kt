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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.ui.shapes

// Color definitions from colors.xml
val lightBlue = Color(0xFF74ADD1)
val darkBlue = Color(0xFF427EA0)
val orange = Color(0xFFF46D43)
val darkOrange = Color(0xFFBB3D18)
val blueBlack = Color(0xFF2B3B45)

val darkCharcoal = Color(0xFF121212)
val midCharcoal = Color(0xFF222222)
val lightCharcoal = Color(0xFF373737)
val fadedLightBlue = Color(0xFF88AABF)
val fadedDarkBlue = Color(0xFF54788C)
val fadedOrange = Color(0xFFE17656)
val fadedDarkOrange = Color(0xFFA24A2F)
val fadedGreen = Color(0xFF4ABF50)

val blueWhitePastel = Color(0xFFD3DADE)
val chalkyWhite = Color(0xFFF3F3F3)
val lightGray = Color(0xFFE0E0E0)
val midGray = Color(0xFF7C7C7C)
val darkGray = Color(0xFF4C4C4C)
val white = Color(0xFFFFFFFF)
val black = Color(0xFF000000)

data class TngColors(
    val colorScheme: ColorScheme,
    val selectorButtonColor: Color,
    val textColorSecondary: Color,
    val toolbarBackgroundColor: Color,
    val hyperlinkColor: Color,
) {
    // Convenience accessors for Material 3 ColorScheme
    val primary get() = colorScheme.primary
    val onPrimary get() = colorScheme.onPrimary
    val primaryContainer get() = colorScheme.primaryContainer
    val onPrimaryContainer get() = colorScheme.onPrimaryContainer
    val secondary get() = colorScheme.secondary
    val onSecondary get() = colorScheme.onSecondary
    val secondaryContainer get() = colorScheme.secondaryContainer
    val onSecondaryContainer get() = colorScheme.onSecondaryContainer
    val tertiary get() = colorScheme.tertiary
    val onTertiary get() = colorScheme.onTertiary
    val tertiaryContainer get() = colorScheme.tertiaryContainer
    val onTertiaryContainer get() = colorScheme.onTertiaryContainer
    val error get() = colorScheme.error
    val onError get() = colorScheme.onError
    val errorContainer get() = colorScheme.errorContainer
    val onErrorContainer get() = colorScheme.onErrorContainer
    val background get() = colorScheme.background
    val onBackground get() = colorScheme.onBackground
    val surface get() = colorScheme.surface
    val onSurface get() = colorScheme.onSurface
    val surfaceVariant get() = colorScheme.surfaceVariant
    val onSurfaceVariant get() = colorScheme.onSurfaceVariant
    val outline get() = colorScheme.outline
    val outlineVariant get() = colorScheme.outlineVariant
    val scrim get() = colorScheme.scrim
    val inverseSurface get() = colorScheme.inverseSurface
    val inverseOnSurface get() = colorScheme.inverseOnSurface
    val inversePrimary get() = colorScheme.inversePrimary
    val surfaceDim get() = colorScheme.surfaceDim
    val surfaceBright get() = colorScheme.surfaceBright
    val surfaceContainerLowest get() = colorScheme.surfaceContainerLowest
    val surfaceContainerLow get() = colorScheme.surfaceContainerLow
    val surfaceContainer get() = colorScheme.surfaceContainer
    val surfaceContainerHigh get() = colorScheme.surfaceContainerHigh
    val surfaceContainerHighest get() = colorScheme.surfaceContainerHighest

    val disabledAlpha get() = 0.38f
}

data class TngTypography(
    val materialTypography: Typography,
    val code: TextStyle = TextStyle(
        fontFamily = FontFamily(
            Font(resId = R.font.roboto_mono, weight = FontWeight(750))
        ),
        fontSize = materialTypography.bodyLarge.fontSize,
        lineHeight = 22.sp,
    )
)

// Custom Typography matching original Material 2 text sizes from dimens.xml
private val CustomTypography = Typography(
    // Display styles (largest text)
    displayLarge = Typography().displayLarge.copy(fontSize = 70.sp),
    displayMedium = Typography().displayMedium.copy(fontSize = 45.sp),
    displaySmall = Typography().displaySmall.copy(fontSize = 36.sp),

    // Headline styles
    headlineLarge = Typography().headlineLarge.copy(fontSize = 30.sp),
    headlineMedium = Typography().headlineMedium.copy(fontSize = 22.sp),
    headlineSmall = Typography().headlineSmall.copy(fontSize = 20.sp),

    // Title styles
    titleLarge = Typography().titleLarge.copy(fontSize = 28.sp),
    titleMedium = Typography().titleMedium.copy(fontSize = 20.sp),
    titleSmall = Typography().titleSmall.copy(fontSize = 17.sp),

    // Body styles
    bodyLarge = Typography().bodyLarge.copy(fontSize = 15.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 15.sp),
    bodySmall = Typography().bodySmall.copy(fontSize = 13.sp),

    // Label styles (smallest text)
    labelLarge = Typography().labelLarge.copy(fontSize = 14.sp),
    labelMedium = Typography().labelMedium.copy(fontSize = 11.sp),
    labelSmall = Typography().labelSmall.copy(fontSize = 10.sp),
)

// Light theme ColorScheme based on themes.xml
private val LightColorScheme = lightColorScheme(
    primary = orange,
    onPrimary = blueBlack,
    primaryContainer = orange,
    onPrimaryContainer = blueBlack,
    secondary = lightBlue,
    onSecondary = blueBlack,
    secondaryContainer = orange,
    onSecondaryContainer = white,
    tertiary = lightBlue,
    onTertiary = blueBlack,
    error = darkOrange,
    onError = fadedGreen,
    errorContainer = darkOrange,
    onErrorContainer = white,
    background = chalkyWhite,
    onBackground = black,
    surface = white,
    onSurface = black,
    surfaceVariant = white,
    onSurfaceVariant = darkGray,
    outline = lightGray,
    outlineVariant = lightGray,
    scrim = black,
)

// Dark theme ColorScheme based on themes-night.xml
private val DarkColorScheme = darkColorScheme(
    primary = fadedOrange,
    onPrimary = white,
    primaryContainer = fadedOrange,
    onPrimaryContainer = black,
    secondary = fadedLightBlue,
    onSecondary = blueBlack,
    secondaryContainer = fadedOrange,
    onSecondaryContainer = black,
    tertiary = fadedLightBlue,
    onTertiary = blueBlack,
    error = fadedDarkOrange,
    onError = fadedGreen,
    errorContainer = fadedDarkOrange,
    onErrorContainer = white,
    background = darkCharcoal,
    onBackground = lightGray,
    surface = darkCharcoal,
    onSurface = lightGray,
    surfaceVariant = midCharcoal,
    onSurfaceVariant = lightGray,
    outline = darkGray,
    outlineVariant = darkGray,
    scrim = black,
)

private val LightTngColors = TngColors(
    colorScheme = LightColorScheme,
    selectorButtonColor = lightGray,
    textColorSecondary = darkGray,
    toolbarBackgroundColor = blueWhitePastel,
    hyperlinkColor = darkBlue, // Using dark_blue from colorSecondaryVariant
)

private val DarkTngColors = TngColors(
    colorScheme = DarkColorScheme,
    selectorButtonColor = darkGray,
    textColorSecondary = lightGray,
    toolbarBackgroundColor = midCharcoal,
    hyperlinkColor = fadedDarkBlue, // Using faded_dark_blue from colorSecondaryVariant
)

@Composable
fun TnGComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    block: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkTngColors else LightTngColors
    val typography = TngTypography(CustomTypography)

    CompositionLocalProvider(LocalColors provides colors) {
        CompositionLocalProvider(LocalTypography provides typography) {
            MaterialTheme(
                colorScheme = colors.colorScheme,
                typography = typography.materialTypography,
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
                colorScheme = it.colorScheme.copy(
                    background = midCharcoal,
                    surface = midCharcoal,
                )
            )
        } else it
    }

    CompositionLocalProvider(LocalColors provides colors) {
        block()
    }
}

val LocalColors = staticCompositionLocalOf<TngColors> {
    error("No TngColors provided")
}

val LocalTypography = staticCompositionLocalOf<TngTypography> {
    error("No TngTypography provided")
}

val MaterialTheme.tngColors: TngColors
    @Composable
    @ReadOnlyComposable
    get() = LocalColors.current

val MaterialTheme.tngTypography: TngTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalTypography.current
