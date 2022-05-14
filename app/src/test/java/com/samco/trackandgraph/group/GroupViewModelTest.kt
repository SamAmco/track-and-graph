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

package com.samco.trackandgraph.group

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.asLiveData
import com.nhaarman.mockitokotlin2.*
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.model.DataInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelTest {

    //Allows observing live data from a test
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dataInteractor: DataInteractor = mock()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var uut: GroupViewModel

    @Before
    fun before() {
        Dispatchers.setMain(testDispatcher)
        uut = GroupViewModel(
            dataInteractor,
            testDispatcher,
            testDispatcher,
            testDispatcher
        )
    }

    @Test
    fun `When data update events are emitted the viewmodel updates the graphs`() = runTest {
        //TODO I would like to have a working test for this but right now the view model is tightly
        // coupled with too many dependencies and the test would be too complicated. I need to inject
        // the graph stat data factories
/*
        //PREPARE
        val groupId = 1L
        val graphStatId = 2L
        val featureId = 3L

        val testGraphStat = GraphOrStat(
            id = graphStatId,
            groupId = groupId,
            name = "some name",
            type = GraphStatType.PIE_CHART,
            displayIndex = 0
        )

        val testPieChart = PieChart(
            id = 0L,
            graphStatId = graphStatId,
            featureId = featureId,
            duration = null,
            endDate = null
        )

        val dataUpdateEvents = MutableSharedFlow<Unit>()
        whenever(dataInteractor.getDataUpdateEvents()).thenReturn(dataUpdateEvents)
        whenever(dataInteractor.getGroupsForGroupSync(eq(groupId))).thenReturn(emptyList())
        whenever(dataInteractor.getFeaturesForGroupSync(eq(groupId))).thenReturn(emptyList())
        whenever(dataInteractor.getGraphsAndStatsByGroupIdSync(eq(groupId)))
            .thenReturn(listOf(testGraphStat))
        whenever(dataInteractor.getPieChartByGraphStatId(eq(graphStatId))).thenReturn(testPieChart)
        whenever(dataInteractor.getDisplayFeaturesForGroupSync(eq(groupId))).thenReturn(emptyList())

        uut.setGroup(groupId)
        yield()

        uut.groupChildren.observeForever { }
        yield()

        //EXECUTE
        dataUpdateEvents.emit(Unit)

        //VERIFY
        verify(dataInteractor, times(2)).getPieChartByGraphStatId(eq(graphStatId))
*/
    }
}