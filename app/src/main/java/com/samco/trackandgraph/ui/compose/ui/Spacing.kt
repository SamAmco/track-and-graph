package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.samco.trackandgraph.R

@Composable
fun SpacingSmall() = Spacer(
    modifier = Modifier
        .height(dimensionResource(id = R.dimen.dialog_input_spacing))
        .width(dimensionResource(id = R.dimen.dialog_input_spacing))
)

@Composable
fun SpacingLarge() = Spacer(
    modifier = Modifier
        .height(dimensionResource(id = R.dimen.input_spacing_large))
        .width(dimensionResource(id = R.dimen.input_spacing_large))
)
