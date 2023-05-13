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
        onUpdate()
        featureChangeCallback(id)
    }

    fun onConfigLoaded(map: Map<Long, String>, featureId: Long) {
        this.featureId = featureId
        featureMap = map
    }

    fun initSingleFeatureConfigBehaviour(
        onUpdate: () -> Unit,
        featureChangeCallback: (Long) -> Unit = {}
    ) {
        this.onUpdate = onUpdate
        this.featureChangeCallback = featureChangeCallback
    }
}
