package com.samco.trackandgraph.graphstatinput.configviews

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javax.inject.Inject

interface SingleFeatureConfigBehaviour {
    val featureId: Long?
    val featureMap: Map<Long, String>?
    fun updateFeatureId(id: Long)
}

class SingleFeatureConfigBehaviourImpl @Inject constructor() : SingleFeatureConfigBehaviour {
    override var featureId: Long? by mutableStateOf(null)
    override var featureMap: Map<Long, String>? by mutableStateOf(null)
        private set

    lateinit var onUpdate: () -> Unit
    lateinit var featureChangeCallback: (Long) -> Unit

    override fun updateFeatureId(id: Long) {
        featureId = id
    }

    fun setFeatureMap(map: Map<Long, String>) {
        featureMap = map
        if (featureId == null) featureId = map.keys.firstOrNull()
    }

    fun initSingleFeatureConfigBehaviour(
        onUpdate: () -> Unit,
        featureChangeCallback: (Long) -> Unit = {}
    ) {
        this.onUpdate = onUpdate
        this.featureChangeCallback = featureChangeCallback
    }
}
