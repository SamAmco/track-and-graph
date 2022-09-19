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

package com.samco.trackandgraph.base.database.dto

import com.samco.trackandgraph.base.database.entity.queryresponse.TrackerWithFeature

data class Tracker(
    val id: Long,
    override val name: String,
    override val groupId: Long,
    override val featureId: Long,
    override val displayIndex: Int,
    override val description: String,
    val dataType: DataType,
    val discreteValues: List<DiscreteValue>,
    val hasDefaultValue: Boolean,
    val defaultValue: Double,
) : Feature {
    fun getDefaultLabel(): String =
        if (dataType == DataType.DISCRETE)
            discreteValues.first { dv -> dv.index == defaultValue.toInt() }.label
        else ""

    companion object {
        internal fun fromTrackerWithFeature(twf: TrackerWithFeature) = Tracker(
            id = twf.id,
            name = twf.name,
            groupId = twf.groupId,
            featureId = twf.featureId,
            displayIndex = twf.displayIndex,
            description = twf.description,
            dataType = twf.dataType,
            discreteValues = twf.discreteValues,
            hasDefaultValue = twf.hasDefaultValue,
            defaultValue = twf.defaultValue,
        )
    }

    internal fun toEntity() = com.samco.trackandgraph.base.database.entity.Tracker(
        id = id,
        featureId = featureId,
        dataType = dataType,
        discreteValues = discreteValues,
        hasDefaultValue = hasDefaultValue,
        defaultValue = defaultValue
    )

    internal fun toFeatureEntity() = com.samco.trackandgraph.base.database.entity.Feature(
        id = featureId,
        name = name,
        groupId = groupId,
        displayIndex = displayIndex,
        description = description
    )
}
