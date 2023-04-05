package com.samco.trackandgraph.graphstatinput.configviews

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.samco.trackandgraph.graphstatinput.customviews.SampleEndingAt
import com.samco.trackandgraph.graphstatinput.dtos.GraphStatDurations
import javax.inject.Inject

interface TimeRangeConfigBehaviour {
    val selectedDuration: GraphStatDurations
    val sampleEndingAt: SampleEndingAt
    fun updateDuration(duration: GraphStatDurations)
    fun updateSampleEndingAt(endingAt: SampleEndingAt)
}

class TimeRangeConfigBehaviourImpl @Inject constructor() : TimeRangeConfigBehaviour {
    private lateinit var onUpdate: () -> Unit

    fun initTimeRangeConfigBehaviour(onUpdate: () -> Unit) {
        this.onUpdate = onUpdate
    }

    override var selectedDuration by mutableStateOf(GraphStatDurations.ALL_DATA)

    override var sampleEndingAt by mutableStateOf<SampleEndingAt>(SampleEndingAt.Latest)

    override fun updateDuration(duration: GraphStatDurations) {
        selectedDuration = duration
        onUpdate()
    }

    override fun updateSampleEndingAt(endingAt: SampleEndingAt) {
        sampleEndingAt = endingAt
        onUpdate()
    }
}

