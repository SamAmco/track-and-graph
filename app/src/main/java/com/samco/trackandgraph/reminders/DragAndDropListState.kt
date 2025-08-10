package com.samco.trackandgraph.reminders

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay

@Composable
fun rememberDragAndDropListState(
    lazyListState: LazyListState = rememberLazyListState(),
    onMove: (Int, Int) -> Unit
): DragAndDropListState {
    val dragAndDropListState = remember { DragAndDropListState(lazyListState, onMove) }

    // When the user drags the item to the edge of the screen, we need to scroll
    LaunchedEffect(dragAndDropListState.overscrollAmount) {
        while (true) {
            val scrollAmount = dragAndDropListState.overscrollAmount
            if (scrollAmount != 0f) {
                dragAndDropListState.lazyListState.scrollBy(scrollAmount)
            }
            delay(16)
        }
    }

    return dragAndDropListState
}

/**
 * Drag and drop state management for LazyColumn with reminder items
 */
class DragAndDropListState(
    val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit
) {
    var draggedItemId by mutableStateOf<Long?>(null)
        private set
    private var draggedItemIndex: Int? = null

    // The y position of the pointer in the list view
    private var currentPointerY by mutableFloatStateOf(0f)

    // How far into the item the pointer was when it was first dragged
    private var pointerOffsetInItem by mutableFloatStateOf(0f)

    // The y position to draw the dragged item relative to the parent list view
    val draggedItemOffset: Int
        get() = (currentPointerY - pointerOffsetInItem).toInt()

    // The amount to scroll the list by, if the user drags the item to the edge,
    // this value will become non-zero
    var overscrollAmount by mutableFloatStateOf(0f)
        private set

    /**
     * Finds the visible item that contains the given Y position
     */
    private fun findItemAtCurrentPosition() =
        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            currentPointerY.toInt() in item.offset..(item.offset + item.size)
        }

    fun onDragStart(offset: Offset) {
        currentPointerY = offset.y
        // Find the item the pointer is over
        findItemAtCurrentPosition()?.also { item ->
            draggedItemId = item.key as? Long
            draggedItemIndex = item.index
            // Store the offset from the pointer that the dragged item will need to be drawn at
            pointerOffsetInItem = offset.y - item.offset.toFloat()
        }
    }

    fun onDrag(offset: Offset) {
        //Update pointer position
        currentPointerY += offset.y

        val targetIndex = findItemAtCurrentPosition()?.index
            ?.takeIf { it in 0..<lazyListState.layoutInfo.totalItemsCount }

        // If we have a valid target and it's different from current, move the dragged item
        // to the new position in the list
        draggedItemIndex?.let { currentIndex ->
            if (targetIndex != null && targetIndex != currentIndex) {
                draggedItemIndex = targetIndex
                val firstVisibleIndex = lazyListState.firstVisibleItemIndex
                val firstVisibleOffset = lazyListState.firstVisibleItemScrollOffset
                onMove.invoke(currentIndex, targetIndex)

                // This is necessary because otherwise when you move the item the
                // lazy list will scroll to try and maintain its position on screen and
                // the dragged item will now be intersecting a different item on screen
                // causing a chain effect. This forces the lazy list to maintain its
                // scroll position when items are swapped
                lazyListState.requestScrollToItem(firstVisibleIndex, firstVisibleOffset)
            }
        }

        // Trigger list scrolling if the pointer is close to the edge
        updateOverscroll(currentPointerY)
    }

    private fun updateOverscroll(pointerY: Float) {
        // If the pointer is within 50 pixels of the top or bottom of the list then
        // we start scrolling
        val scrollInsetThreshold = 50
        val topEdge = lazyListState.layoutInfo.viewportStartOffset + scrollInsetThreshold
        val bottomEdge = lazyListState.layoutInfo.viewportEndOffset - scrollInsetThreshold
        overscrollAmount = when {
            pointerY < topEdge -> pointerY - topEdge
            pointerY > bottomEdge -> pointerY - bottomEdge
            else -> 0f
        }
    }

    fun onDragInterrupted() {
        draggedItemId = null
        draggedItemIndex = null
        currentPointerY = 0f
        pointerOffsetInItem = 0f
        overscrollAmount = 0f
    }
}
