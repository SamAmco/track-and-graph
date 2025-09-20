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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.ui.viewmodels.asTextFieldValue
import com.samco.trackandgraph.ui.viewmodels.asValidatedDouble
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface FilterableFeatureConfigBehaviour {
    val availableLabels: List<String>
    val selectedLabels: List<String>
    val fromValue: TextFieldValue
    val toValue: TextFieldValue
    val filterByLabel: Boolean
    val filterByRange: Boolean
    val loadingLabels: Boolean

    fun updateFromValue(value: TextFieldValue)
    fun updateToValue(value: TextFieldValue)
    fun updateSelectedLabels(labels: List<String>)
    fun updateFilterByLabel(filter: Boolean)
    fun updateFilterByRange(filter: Boolean)
}

class FilterableFeatureConfigBehaviourImpl @Inject constructor(
    private val dataSampler: DataSampler
) :
    FilterableFeatureConfigBehaviour {

    var featureId: Long? = null
    override var availableLabels: List<String> by mutableStateOf(listOf())
    override var selectedLabels: List<String> by mutableStateOf(listOf())
    override var fromValue: TextFieldValue by mutableStateOf(TextFieldValue("0", TextRange(1)))
    override var toValue: TextFieldValue by mutableStateOf(TextFieldValue("1", TextRange(1)))
    override var filterByLabel: Boolean by mutableStateOf(false)
    override var filterByRange: Boolean by mutableStateOf(false)
    override var loadingLabels: Boolean by mutableStateOf(false)

    private var labelUpdateJob: Job? = null

    private lateinit var onUpdate: () -> Unit
    private lateinit var io: CoroutineDispatcher
    private lateinit var ui: CoroutineDispatcher
    private lateinit var coroutineScope: CoroutineScope

    fun initFilterableFeatureConfigBehaviour(
        onUpdate: () -> Unit,
        io: CoroutineDispatcher,
        ui: CoroutineDispatcher,
        coroutineScope: CoroutineScope,
    ) {
        this.onUpdate = onUpdate
        this.io = io
        this.ui = ui
        this.coroutineScope = coroutineScope
    }

    fun onConfigLoaded(
        featureId: Long?,
        filterByLabel: Boolean?,
        filterByRange: Boolean?,
        fromValue: Double?,
        toValue: Double?,
        selectedLabels: List<String>?
    ) {
        this.filterByLabel = filterByLabel ?: false
        this.filterByRange = filterByRange ?: false
        fromValue?.asTextFieldValue()?.let { this.fromValue = it }
        toValue?.asTextFieldValue()?.let { this.toValue = it }
        this.selectedLabels = selectedLabels ?: emptyList()
        this.featureId = featureId
        getAvailableLabels()
    }

    private fun getAvailableLabels() {
        featureId?.let { fId ->
            labelUpdateJob?.cancel()
            labelUpdateJob = coroutineScope.launch(ui) {
                loadingLabels = true
                val labels = withContext(io) {
                    dataSampler.getLabelsForFeatureId(fId)
                        .filter { it.isNotEmpty() }
                        .sorted()
                }
                availableLabels = labels
                loadingLabels = false
            }
        }
    }

    fun onFeatureIdUpdated(id: Long) {
        if (id == featureId) return
        featureId = id
        getAvailableLabels()
        selectedLabels = emptyList()
        onUpdate()
    }

    override fun updateFromValue(value: TextFieldValue) {
        fromValue = value.asValidatedDouble()
        onUpdate()
    }

    override fun updateToValue(value: TextFieldValue) {
        toValue = value.asValidatedDouble()
        onUpdate()
    }

    override fun updateSelectedLabels(labels: List<String>) {
        selectedLabels = labels
        onUpdate()
    }

    override fun updateFilterByLabel(filter: Boolean) {
        filterByLabel = filter
        onUpdate()
    }

    override fun updateFilterByRange(filter: Boolean) {
        filterByRange = filter
        onUpdate()
    }
}