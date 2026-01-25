package com.samco.trackandgraph.ui.compose.compositionlocals

import androidx.compose.runtime.staticCompositionLocalOf
import com.samco.trackandgraph.settings.TngSettings

val LocalSettings = staticCompositionLocalOf<TngSettings> {
    error("CompositionLocal LocalSettings not provided")
}