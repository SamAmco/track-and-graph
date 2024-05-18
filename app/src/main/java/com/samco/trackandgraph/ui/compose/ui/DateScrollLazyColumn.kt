/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatterBuilder
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.ChronoField
import kotlin.math.roundToInt
import kotlin.math.sin

interface Datable {
    val date: OffsetDateTime
}

enum class DateDisplayResolution {
    MONTH_DAY,
    MONTH_YEAR,
}

data class DateScrollData<T : Datable>(
    val dateDisplayResolution: DateDisplayResolution,

    val items: List<T>
)

private val monthDayFormatter = DateTimeFormatterBuilder()
    .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
    .appendLiteral(' ')
    .appendValue(ChronoField.DAY_OF_MONTH)
    .toFormatter()

private val monthYearFormatter = DateTimeFormatterBuilder()
    .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
    .appendLiteral(' ')
    .appendValue(ChronoField.YEAR)
    .toFormatter()

private val textOverlayAnimationSpec = tween<Float>(durationMillis = 300)
private val scrollBarAnimationSpec = tween<IntOffset>(durationMillis = 300)

@Composable
fun <T : Datable> DateScrollLazyColumn(
    modifier: Modifier = Modifier,
    data: DateScrollData<T>,
    content: @Composable LazyItemScope.(item: T) -> Unit
) = Box(modifier = modifier) {
    val scrollState = rememberLazyListState()

    LazyColumn(state = scrollState) { items(data.items) { content(it) } }

    val isDragging = remember { mutableStateOf(false) }
    val scrolledToIndex = remember { mutableStateOf(0) }
    var isScrollBarVisible by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState.isScrollInProgress, isDragging.value) {
        if (scrollState.isScrollInProgress || isDragging.value) {
            isScrollBarVisible = true
        } else {
            delay(2000)
            isScrollBarVisible = false
        }
    }

    val currentDateText = remember(scrollState, data) {
        derivedStateOf {
            if (data.items.isEmpty() || scrolledToIndex.value !in data.items.indices)
                return@derivedStateOf null
            val date = data.items[scrolledToIndex.value].date
            when (data.dateDisplayResolution) {
                DateDisplayResolution.MONTH_DAY -> date.format(monthDayFormatter)
                DateDisplayResolution.MONTH_YEAR -> date.format(monthYearFormatter)
            }
        }
    }

    val textOverlayVisible by remember(isDragging, currentDateText) {
        derivedStateOf {
            isDragging.value && currentDateText.value != null
        }
    }

    AnimatedVisibility(
        visible = textOverlayVisible,
        enter = fadeIn(animationSpec = textOverlayAnimationSpec),
        exit = fadeOut(animationSpec = textOverlayAnimationSpec)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colors.background.copy(alpha = 1f),
                            MaterialTheme.colors.background.copy(alpha = 0.4f)
                        )
                    )
                )
        ) {
            Text(
                modifier = Modifier.align(Alignment.TopCenter),
                text = currentDateText.value ?: "",
                style = MaterialTheme.typography.h3,
            )
        }
    }

    AnimatedVisibility(
        modifier = Modifier
            .fillMaxHeight()
            .align(Alignment.CenterEnd),
        visible = isScrollBarVisible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = scrollBarAnimationSpec
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = scrollBarAnimationSpec
        )
    ) {
        ScrollBarCanvas(
            data = data,
            scrollState = scrollState,
            isDragging = isDragging,
            scrolledToIndex = scrolledToIndex
        )
    }
}

@Composable
private fun <T : Datable> ScrollBarCanvas(
    data: DateScrollData<T>,
    scrollState: LazyListState,
    isDragging: MutableState<Boolean>,
    scrolledToIndex: MutableState<Int>
) {
    // Scrollbar properties
    var scrollbarOffsetY by remember { mutableStateOf(0f) }
    val scrollGrabberColor = MaterialTheme.colors.primary
    val scrollGrabberDiamDp = 50.dp
    val scrollGrabberRadDp = remember { scrollGrabberDiamDp / 2 }
    val scrollGrabberDiamPx = with(LocalDensity.current) { scrollGrabberDiamDp.toPx() }
    val scrollGrabberRadPx = remember { scrollGrabberDiamPx / 2 }
    val coroutineScope = rememberCoroutineScope()
    var canvasHeight by remember { mutableStateOf(0) }

    //The extra sweep is the extra degrees on a semi circle that we're adding to both sides to make it
    // feel like a circle with a bit of the side cut off.
    val extraSweep = 20f
    val startAngle = remember { -90f + extraSweep }
    val sweepAngle = remember { -180f - (2 * extraSweep) }
    val fullWidth = remember {
        scrollGrabberRadDp.value + (sin(Math.toRadians(extraSweep.toDouble())) * scrollGrabberRadDp.value)
    }

    Canvas(modifier = Modifier
        .width(fullWidth.dp)
        .fillMaxHeight()
        .onGloballyPositioned { canvasHeight = it.size.height }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    isDragging.value = true
                    val columnHeight = size.height - scrollGrabberDiamPx
                    scrollbarOffsetY = (offset.y - scrollGrabberRadPx).coerceIn(0f, columnHeight)
                    val proportion = scrollbarOffsetY / columnHeight
                    val targetScrollOffset = (data.items.size * proportion).roundToInt()
                    scrolledToIndex.value = targetScrollOffset
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val columnHeight = size.height - scrollGrabberDiamPx
                    scrollbarOffsetY = (scrollbarOffsetY + dragAmount.y).coerceIn(0f, columnHeight)
                    val proportion = scrollbarOffsetY / columnHeight
                    val targetScrollOffset = (data.items.size * proportion).roundToInt()
                    scrolledToIndex.value = targetScrollOffset
                    coroutineScope.launch { scrollState.scrollToItem(targetScrollOffset) }
                },
                onDragEnd = { isDragging.value = false },
                onDragCancel = { isDragging.value = false }
            )
        }
    ) {
        drawArc(
            color = scrollGrabberColor,
            topLeft = Offset(0f, scrollbarOffsetY),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            size = Size(scrollGrabberDiamPx, scrollGrabberDiamPx)
        )
    }

    val offset by remember {
        derivedStateOf {
            val canvasScalar = canvasHeight.toFloat() - scrollGrabberDiamPx

            val totalItems = data.items.size
            val firstVisibleItem = scrollState.firstVisibleItemIndex
            val visibleItemCount = scrollState.layoutInfo.visibleItemsInfo.size
            val visibleItemsSize = scrollState.layoutInfo.visibleItemsInfo.sumOf { it.size } +
                    (scrollState.layoutInfo.mainAxisItemSpacing * (visibleItemCount - 1))
            if (visibleItemsSize <= 0 || visibleItemCount <= 0) return@derivedStateOf 0f

            val averageItemSize = visibleItemsSize / visibleItemCount.toFloat()

            val scrollOffset = scrollState.firstVisibleItemScrollOffset
            val viewportHeight = scrollState.layoutInfo.viewportSize.height
            val estimatedAboveItemsSize = (firstVisibleItem * averageItemSize) + scrollOffset
            val clippedAmountOfBottomItem = visibleItemsSize - scrollOffset - viewportHeight
            val estimatedBelowItemsSize =
                ((totalItems - firstVisibleItem - visibleItemCount) * averageItemSize) + clippedAmountOfBottomItem
            val estimatedTotalHiddenContentSize = estimatedAboveItemsSize + estimatedBelowItemsSize

            val estimatedRatioInsideTotalContent =
                estimatedAboveItemsSize / estimatedTotalHiddenContentSize

            canvasScalar * estimatedRatioInsideTotalContent
        }
    }

    LaunchedEffect(isDragging.value, offset) {
        if (!isDragging.value) scrollbarOffsetY = offset
    }
}