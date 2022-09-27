package com.samco.trackandgraph.ui

import android.content.Context
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType


@Composable
fun materialTheme(context: Context, block: () -> Unit) {

    return MaterialTheme(

    ) {
        block()
    }
}