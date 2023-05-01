package com.samco.trackandgraph.graphstatinput.configviews

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatinput.customviews.FilterByLabelSection
import com.samco.trackandgraph.graphstatinput.customviews.FilterByValueSection
import com.samco.trackandgraph.graphstatinput.customviews.GraphStatEndingAtSpinner
import com.samco.trackandgraph.ui.compose.ui.SpacingLarge
import com.samco.trackandgraph.ui.compose.ui.SpacingSmall
import com.samco.trackandgraph.ui.compose.ui.TextMapSpinner

@Composable
fun LastValueConfigView(viewModel: LastValueConfigViewModel) {

    GraphStatEndingAtSpinner(
        modifier = Modifier,
        sampleEndingAt = viewModel.sampleEndingAt
    ) { viewModel.updateSampleEndingAt(it) }

    SpacingSmall()

    Divider()

    SpacingLarge()

    Text(
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.card_padding)),
        text = stringResource(id = R.string.select_a_feature),
        style = MaterialTheme.typography.subtitle2
    )

    SpacingSmall()

    val featureId = viewModel.featureId
    val featureMap = viewModel.featureMap

    if (featureId != null && featureMap != null) {
        TextMapSpinner(
            strings = featureMap,
            selectedItem = featureId,
            onItemSelected = { viewModel.updateFeatureId(it) }
        )
    }

    SpacingLarge()

    FilterByLabelSection(viewModel)

    SpacingLarge()

    FilterByValueSection(viewModel)

    SpacingSmall()
}