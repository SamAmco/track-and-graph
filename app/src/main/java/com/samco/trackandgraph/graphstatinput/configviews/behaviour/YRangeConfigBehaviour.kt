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
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.samco.trackandgraph.graphstatinput.configviews.behaviour

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.ui.viewmodels.DurationInputViewModel
import com.samco.trackandgraph.ui.viewmodels.DurationInputViewModelImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

interface YRangeConfigBehaviour {
    val yRangeType: YRangeType
    val timeBasedRange: Flow<Boolean>
    val yRangeFrom: TextFieldValue
    val yRangeTo: TextFieldValue

    val yRangeFromDurationViewModel: DurationInputViewModel
    val yRangeToDurationViewModel: DurationInputViewModel

    fun updateYRangeType(yRangeType: YRangeType)
    fun updateYRangeFrom(yRangeFrom: TextFieldValue)
    fun updateYRangeTo(yRangeTo: TextFieldValue)
}

class YRangeConfigBehaviourImpl @Inject constructor() : YRangeConfigBehaviour {
    override var yRangeType by mutableStateOf(YRangeType.DYNAMIC)
        private set
    override var yRangeFrom by mutableStateOf(TextFieldValue("0.0"))
        private set
    override var yRangeTo by mutableStateOf(TextFieldValue("1.0"))
        private set

    override val yRangeFromDurationViewModel = DurationInputViewModelImpl()
        .apply { setOnChangeListener { onUpdate() } }
    override val yRangeToDurationViewModel = DurationInputViewModelImpl()
        .apply { setOnChangeListener { onUpdate() } }

    private val timeBasedRangeFlow = MutableStateFlow<Flow<Boolean>>(emptyFlow())
    override val timeBasedRange: Flow<Boolean> = timeBasedRangeFlow
        .flatMapLatest { it }

    private lateinit var onUpdate: () -> Unit

    fun initYRangeConfigBehaviour(onUpdate: () -> Unit) {
        this.onUpdate = onUpdate
    }

    fun onConfigLoaded(
        yRangeType: YRangeType?,
        yFrom: Double?,
        yTo: Double?,
        timeBasedRange: Flow<Boolean>
    ) {
        this.yRangeType = yRangeType ?: YRangeType.DYNAMIC
        this.yRangeFrom = TextFieldValue(yFrom?.toString() ?: "0.0")
        this.yRangeTo = TextFieldValue(yTo?.toString() ?: "1.0")
        this.yRangeFromDurationViewModel.setDurationFromDouble(yFrom ?: 0.0)
        this.yRangeToDurationViewModel.setDurationFromDouble(yTo ?: (60.0 * 60.0))
        this.timeBasedRangeFlow.value = timeBasedRange
    }

    override fun updateYRangeType(yRangeType: YRangeType) {
        this.yRangeType = yRangeType
        onUpdate()
    }

    override fun updateYRangeFrom(yRangeFrom: TextFieldValue) {
        this.yRangeFrom = yRangeFrom
        onUpdate()
    }

    override fun updateYRangeTo(yRangeTo: TextFieldValue) {
        this.yRangeTo = yRangeTo
        onUpdate()
    }
}
