package com.samco.trackandgraph.graphstatinput.configviews

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.ui.viewmodels.asValidatedDouble
import kotlinx.coroutines.*
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

class FilterableFeatureConfigBehaviourImpl @Inject constructor() :
    FilterableFeatureConfigBehaviour {

    private var featureId: Long? = null
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
    private lateinit var dataInteractor: DataInteractor

    fun initFilterableFeatureConfigBehaviour(
        onUpdate: () -> Unit,
        io: CoroutineDispatcher,
        ui: CoroutineDispatcher,
        coroutineScope: CoroutineScope,
        dataInteractor: DataInteractor
    ) {
        this.onUpdate = onUpdate
        this.io = io
        this.ui = ui
        this.coroutineScope = coroutineScope
        this.dataInteractor = dataInteractor
    }

    fun getAvailableLabels() {
        featureId?.let { fId ->
            labelUpdateJob?.cancel()
            labelUpdateJob = coroutineScope.launch(ui) {
                loadingLabels = true
                val labels = withContext(io) { dataInteractor.getLabelsForFeatureId(fId) }
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