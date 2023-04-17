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
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.samco.trackandgraph.adddatapoint

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

interface AddDataPointTutorialEvents {
    val onTutorialComplete: Flow<Unit>
}

interface AddDataPointTutorialViewModel {
    val currentPage: LiveData<Int>

    fun onButtonClicked()
    fun onSwipeToPage(page: Int)
    fun reset()
}

class AddDataPointTutorialViewModelImpl :
    AddDataPointTutorialViewModel,
    AddDataPointTutorialEvents,
    CoroutineScope {

    override val coroutineContext: CoroutineContext = Job()

    private sealed class PageEvent {
        object NextPage : PageEvent()
        object Reset : PageEvent()
        data class SwipeToPage(val page: Int) : PageEvent()
    }

    private val pageEvent = MutableSharedFlow<PageEvent>(extraBufferCapacity = 1)

    private val currentPageFlow = pageEvent
        .scan(0) { currentPage, event ->
            when (event) {
                is PageEvent.NextPage -> currentPage + 1
                is PageEvent.SwipeToPage -> event.page
                is PageEvent.Reset -> 0
            }
        }
        .onStart { emit(0) }
        .stateIn(this, SharingStarted.Eagerly, 0)

    override val currentPage = currentPageFlow.asLiveData()

    override val onTutorialComplete: Flow<Unit> = currentPageFlow
        .filter { it == 2 }
        .flatMapLatest {
            //We need to stop consuming the events if the page is changed
            pageEvent.takeWhile {
                it is PageEvent.NextPage || (it is PageEvent.SwipeToPage && it.page == 2)
            }
        }
        .filter { it == PageEvent.NextPage }
        .map { }
        .shareIn(this, SharingStarted.Eagerly, 1)

    override fun onButtonClicked() {
        launch { pageEvent.emit(PageEvent.NextPage) }
    }

    override fun onSwipeToPage(page: Int) {
        launch { pageEvent.emit(PageEvent.SwipeToPage(page)) }
    }

    override fun reset() {
        launch { pageEvent.emit(PageEvent.Reset) }
    }
}
