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

import com.samco.trackandgraph.base.database.entity.Feature

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
) : com.samco.trackandgraph.base.database.dto.Feature {
    fun getDefaultLabel(): String =
        if (dataType == DataType.DISCRETE)
            discreteValues.first { dv -> dv.index == defaultValue.toInt() }.label
        else ""

    companion object {
        internal fun fromEntities(
            tracker: com.samco.trackandgraph.base.database.entity.Tracker,
            feature: Feature
        ) = Tracker(
            id = tracker.id,
            name = feature.name,
            groupId = feature.groupId,
            featureId = feature.id,
            displayIndex = feature.displayIndex,
            description = feature.description,
            dataType = tracker.dataType,
            discreteValues = tracker.discreteValues,
            hasDefaultValue = tracker.hasDefaultValue,
            defaultValue = tracker.defaultValue,
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

    internal fun toFeatureEntity() = Feature(
        id = featureId,
        name = name,
        groupId = groupId,
        displayIndex = displayIndex,
        description = description
    )
}
