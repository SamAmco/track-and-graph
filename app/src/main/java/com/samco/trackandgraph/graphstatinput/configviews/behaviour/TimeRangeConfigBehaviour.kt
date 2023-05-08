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
package com.samco.trackandgraph.graphstatinput.configviews.behaviour

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.samco.trackandgraph.graphstatinput.customviews.SampleEndingAt
import com.samco.trackandgraph.graphstatinput.dtos.GraphStatDurations
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

interface EndingAtConfigBehaviour {
    val sampleEndingAt: SampleEndingAt
    fun updateSampleEndingAt(endingAt: SampleEndingAt)
}

interface TimeRangeConfigBehaviour : EndingAtConfigBehaviour {
    val selectedDuration: GraphStatDurations
    fun updateDuration(duration: GraphStatDurations)
}

class TimeRangeConfigBehaviourImpl @Inject constructor() : TimeRangeConfigBehaviour {
    private lateinit var onUpdate: () -> Unit

    override var selectedDuration by mutableStateOf(GraphStatDurations.ALL_DATA)

    override var sampleEndingAt by mutableStateOf<SampleEndingAt>(SampleEndingAt.Latest)

    fun initTimeRangeConfigBehaviour(onUpdate: () -> Unit) {
        this.onUpdate = onUpdate
    }

    fun onConfigLoaded(
        duration: Duration?,
        endingAt: OffsetDateTime?
    ) {
        duration?.let { selectedDuration = GraphStatDurations.fromDuration(it) }
        endingAt?.let { sampleEndingAt = SampleEndingAt.fromDateTime(it) }
    }

    override fun updateDuration(duration: GraphStatDurations) {
        selectedDuration = duration
        onUpdate()
    }

    override fun updateSampleEndingAt(endingAt: SampleEndingAt) {
        sampleEndingAt = endingAt
        onUpdate()
    }
}

