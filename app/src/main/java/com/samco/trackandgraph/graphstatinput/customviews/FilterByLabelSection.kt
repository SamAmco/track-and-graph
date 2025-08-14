package com.samco.trackandgraph.graphstatinput.customviews

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatinput.configviews.behaviour.FilterableFeatureConfigBehaviour
import com.samco.trackandgraph.ui.compose.ui.*

@Composable
fun FilterByLabelSection(viewModel: FilterableFeatureConfigBehaviour) {
    val focusRequester = remember { FocusRequester() }

    CheckboxLabeledExpandingSection(
        checked = viewModel.filterByLabel,
        onCheckedChanged = { viewModel.updateFilterByLabel(it) },
        label = stringResource(id = R.string.filter_by_label),
        focusRequester = focusRequester
    ) {
        DialogInputSpacing()
        when {
            viewModel.loadingLabels -> LoadingIndicator(it)
            viewModel.availableLabels.isEmpty() -> NoLabelsIndicator(it)
            else -> LabelsFadingLazyRow(it, viewModel)
        }
        CardPadding()
    }
}

@Composable
fun NoLabelsIndicator(modifier: Modifier) = Text(
    modifier = modifier
        .fillMaxWidth()
        .wrapContentHeight(),
    text = stringResource(id = R.string.no_labels),
    style = MaterialTheme.typography.bodyLarge,
    textAlign = TextAlign.Center
)

@Composable
private fun LoadingIndicator(
    modifier: Modifier
) = Box(
    modifier = modifier
        .fillMaxWidth()
        .wrapContentHeight(),
    contentAlignment = Alignment.Center
) {
    CircularProgressIndicator()
}

@Composable
private fun LabelsFadingLazyRow(
    modifier: Modifier,
    viewModel: FilterableFeatureConfigBehaviour
) {
    FadingLazyRow(
        modifier = modifier,
        fadeColor = MaterialTheme.colorScheme.background,
        horizontalArrangement = Arrangement.spacedBy(
            dialogInputSpacing,
            Alignment.CenterHorizontally
        )
    ) {
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
