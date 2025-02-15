package com.samco.trackandgraph.graphstatview.factories.viewdto

import androidx.annotation.ColorInt

sealed class ColorSpec {
    data class ColorIndex(val index: Int) : ColorSpec()
    data class ColorValue(@ColorInt val value: Int) : ColorSpec()
}