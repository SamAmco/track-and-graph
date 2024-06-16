package com.samco.trackandgraph.graphstatinput.customviews

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.ui.CheckboxLabeledExpandingSection
import com.samco.trackandgraph.graphstatinput.configviews.behaviour.FilterableFeatureConfigBehaviour
import com.samco.trackandgraph.ui.compose.ui.MiniNumericTextField
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing

@Composable
fun FilterByValueSection(viewModel: FilterableFeatureConfigBehaviour) {
    val focusRequester = remember { FocusRequester() }

    CheckboxLabeledExpandingSection(
        checked = viewModel.filterByRange,
        onCheckedChanged = { viewModel.updateFilterByRange(it) },
        label = stringResource(id = R.string.filter_by_value),
        focusRequester = focusRequester
    ) {
        DialogInputSpacing()
        Row(Modifier.padding(dimensionResource(id = R.dimen.card_padding))) {
            Text(
                modifier = Modifier.alignByBaseline(),
                text = stringResource(id = R.string.from),
                style = MaterialTheme.typography.body1
            )

            MiniNumericTextField(
                modifier = it
                    .weight(1f)
                    .alignByBaseline(),
                textAlign = TextAlign.Center,
                textFieldValue = viewModel.fromValue,
                onValueChange = { viewModel.updateFromValue(it) }
            )

            DialogInputSpacing()

            Text(
                modifier = Modifier.alignByBaseline(),
                text = stringResource(id = R.string.to),
                style = MaterialTheme.typography.body1
            )

            MiniNumericTextField(
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
                textAlign = TextAlign.Center,
                textFieldValue = viewModel.toValue,
                onValueChange = { viewModel.updateToValue(it) }
            )
        }
    }
}