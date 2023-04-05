package com.samco.trackandgraph.graphstatinput.customviews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.ui.CheckboxLabeledExpandingSection
import com.samco.trackandgraph.graphstatinput.configviews.FilterableFeatureConfigBehaviour
import com.samco.trackandgraph.ui.compose.ui.FadingLazyRow
import com.samco.trackandgraph.ui.compose.ui.TextChip

@Composable
fun FilterByLabelSection(viewModel: FilterableFeatureConfigBehaviour) {
    val focusRequester = remember { FocusRequester() }

    CheckboxLabeledExpandingSection(
        checked = viewModel.filterByLabel,
        onCheckedChanged = { viewModel.updateFilterByLabel(it) },
        label = stringResource(id = R.string.filter_by_label),
        focusRequester = focusRequester
    ) {
        FadingLazyRow(modifier = it) {
            items(viewModel.availableLabels.size) { index ->
                val label = viewModel.availableLabels[index]
                val selected = viewModel.selectedLabels.contains(label)
                TextChip(
                    text = label,
                    isSelected = selected,
                    onClick = {
                        viewModel.updateSelectedLabels(
                            if (selected) viewModel.selectedLabels - label
                            else viewModel.selectedLabels + label
                        )
                    }
                )
            }

        }
    }
}