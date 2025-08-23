/*
* This file is part of Track & Graph
*
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.samco.trackandgraph.adddatapoint

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.settings.mockSettings
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.AddChipButton
import com.samco.trackandgraph.ui.compose.ui.DateTimeButtonRow
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.InputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.ValueInputTextField
import com.samco.trackandgraph.ui.compose.ui.WideButton
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import kotlinx.coroutines.flow.distinctUntilChanged
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

@Composable
fun AddDataPointsTutorial(viewModel: AddDataPointTutorialViewModel) = Column {
    val currentPage by viewModel.currentPage.observeAsState(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)

    // Snap layout info for center snapping
    val snapInfo = remember(listState) {
        SnapLayoutInfoProvider(
            lazyListState = listState,
            snapPosition = SnapPosition.Center
        )
    }

    // Heavy fling behavior for better snapping
    val heavyFling = remember(snapInfo) {
        val heavyDecay = exponentialDecay<Float>(
            frictionMultiplier = 2.5f
        )
        val firmSnap = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )

        snapFlingBehavior(
            snapLayoutInfoProvider = snapInfo,
            decayAnimationSpec = heavyDecay,
            snapAnimationSpec = firmSnap
        )
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .animateContentSize(), // Smooth height transitions
        flingBehavior = heavyFling
    ) {
        items(3) { page ->
            Box(
                modifier = Modifier.fillParentMaxWidth()
            ) {
                when (page) {
                    0 -> TutorialPage0()
                    1 -> TutorialPage1()
                    2 -> TutorialPage2 { viewModel.onNavigateToFaqClicked() }
                }
            }
        }
    }

    val buttonText =
        if (currentPage == 2) stringResource(R.string.got_it)
        else stringResource(R.string.next)

    //Next button
    WideButton(
        text = buttonText,
        onClick = viewModel::onButtonClicked
    )

    // Bidirectional synchronization between ViewModel and LazyRow

    // LazyRow scroll position -> ViewModel
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { page ->
                viewModel.onSwipeToPage(page)
            }
    }

    // ViewModel currentPage -> LazyRow scroll position
    LaunchedEffect(currentPage) {
        if (currentPage != listState.firstVisibleItemIndex) {
            listState.animateScrollToItem(currentPage)
        }
    }
}

@Composable
private fun TutorialPage2(onFaqClicked: () -> Unit) = Column {
    InputSpacingLarge()

    Text(
        modifier = Modifier.padding(horizontal = inputSpacingLarge),
        text = stringResource(R.string.data_point_tutorial_page_3_description),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium
    )

    InputSpacingLarge()

    //A vector drawing of a graph
    Icon(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        painter = painterResource(id = R.drawable.ic_pie_chart_example),
        contentDescription = null,
        tint = Color.Unspecified
    )

    InputSpacingLarge()

    Text(
        modifier = Modifier.padding(horizontal = inputSpacingLarge),
        text = stringResource(R.string.data_point_tutorial_page_3_hint),
        textAlign = TextAlign.Center,
        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
        fontWeight = MaterialTheme.typography.bodyMedium.fontWeight
    )

    Text(
        modifier = Modifier
            .padding(
                horizontal = inputSpacingLarge,
                vertical = cardPadding
            )
            .fillMaxWidth()
            .clickable { onFaqClicked() },
        text = stringResource(R.string.more_details),
        color = MaterialTheme.tngColors.secondary,
        textDecoration = TextDecoration.Underline,
        textAlign = TextAlign.Center,
        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
        fontWeight = MaterialTheme.typography.bodyMedium.fontWeight
    )

    InputSpacingLarge()
}

@Composable
private fun TutorialPage1() = Column(
    horizontalAlignment = Alignment.CenterHorizontally
) {
    InputSpacingLarge()

    Text(
        modifier = Modifier.padding(horizontal = inputSpacingLarge),
        text = stringResource(R.string.data_point_tutorial_page_2_description),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium
    )

    InputSpacingLarge()

    //A vector drawing of a graph
    Icon(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        painter = painterResource(id = R.drawable.ic_graph_example),
        tint = MaterialTheme.tngColors.onSurface,
        contentDescription = null
    )

    InputSpacingLarge()

    Text(
        modifier = Modifier.padding(horizontal = inputSpacingLarge),
        text = stringResource(R.string.data_point_tutorial_page_2_hint),
        textAlign = TextAlign.Center,
        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
        fontWeight = MaterialTheme.typography.bodyMedium.fontWeight
    )

    InputSpacingLarge()
}

@Composable
private fun TutorialPage0(
    displayTime: OffsetDateTime = OffsetDateTime.now()
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DialogInputSpacing()

        Text(
            modifier = Modifier.padding(horizontal = inputSpacingLarge),
            text = stringResource(R.string.adding_your_first_data_point),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )

        InputSpacingLarge()

        Text(
            modifier = Modifier.padding(horizontal = inputSpacingLarge),
            text = stringResource(R.string.each_data_point_has_a_timestamp_and_value),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )

        InputSpacingLarge()

        Box(
            modifier = Modifier
                .height(intrinsicSize = IntrinsicSize.Max)
                .scale(0.8f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DateTimeButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                    selectedDateTime = displayTime,
                    onDateTimeSelected = {}
                )

                DialogInputSpacing()

                ValueInputTextField(
                    textFieldValue = TextFieldValue(""),
                    onValueChange = {}
                )
            }

            //An overlay that fills the parent with a semi transparent background that consumes all click events
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color = MaterialTheme.tngColors.surface.copy(alpha = 0.4f))
                    .clickable(enabled = false, onClick = {})
            )
        }

        InputSpacingLarge()

        Text(
            modifier = Modifier.padding(horizontal = inputSpacingLarge),
            text = stringResource(R.string.it_can_also_optionally_have_a_label_and_a_note),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )

        DialogInputSpacing()

        Box(
            modifier = Modifier.scale(0.8f)
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = inputSpacingLarge),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AddChipButton(text = stringResource(id = R.string.add_a_label)) { }
                AddChipButton(text = stringResource(id = R.string.add_a_note)) {}
            }

            //An overlay that fills the parent with a semi transparent background that consumes all click events
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color = MaterialTheme.tngColors.surface.copy(alpha = 0.4f))
                    .clickable(enabled = false, onClick = {})
            )
        }

        InputSpacingLarge()
    }
}

@Preview(showBackground = true)
@Composable
private fun TutorialPage0Preview() {
    TnGComposeTheme {
        CompositionLocalProvider(LocalSettings provides mockSettings) {
            TutorialPage0(
                OffsetDateTime.of(
                    2023,
                    1,
                    1,
                    0,
                    0,
                    0,
                    0,
                    ZoneOffset.UTC
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TutorialPage1Preview() {
    TnGComposeTheme {
        TutorialPage1()
    }
}

@Preview(showBackground = true)
@Composable
private fun TutorialPage2Preview() {
    TnGComposeTheme {
        TutorialPage2(onFaqClicked = {})
    }
}