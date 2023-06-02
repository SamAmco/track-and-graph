package com.samco.trackandgraph.graphstatinput.customviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatinput.configviews.behaviour.YRangeConfigBehaviour
import com.samco.trackandgraph.ui.compose.ui.MiniNumericTextField

@Composable
fun YRangeFromToInputs(viewModel: YRangeConfigBehaviour) = Row(
    modifier = Modifier
        .padding(horizontal = dimensionResource(id = R.dimen.card_padding))
        .fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    Text(
        modifier = Modifier.alignByBaseline(),
        text = stringResource(id = R.string.from),
        style = MaterialTheme.typography.subtitle2
    )

    MiniNumericTextField(
        modifier = Modifier
            .weight(1f)
            .alignByBaseline(),
        textAlign = TextAlign.Center,
        textFieldValue = viewModel.yRangeFrom,
        onValueChange = { viewModel.updateYRangeFrom(it) }
    )

    Text(
        modifier = Modifier.alignByBaseline(),
        text = stringResource(id = R.string.to),
        style = MaterialTheme.typography.subtitle2
    )

    MiniNumericTextField(
        modifier = Modifier
            .weight(1f)
            .alignByBaseline(),
        textAlign = TextAlign.Center,
        textFieldValue = viewModel.yRangeTo,
        onValueChange = { viewModel.updateYRangeTo(it) }
    )
}