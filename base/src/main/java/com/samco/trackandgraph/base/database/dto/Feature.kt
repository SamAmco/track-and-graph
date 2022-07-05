/*
* This file is part of Track & Graph
* 
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.base.database.dto

import com.samco.trackandgraph.base.database.entity.Feature

data class Feature(
    val id: Long,
    val name: String,
    val groupId: Long,
    val featureType: DataType,
    val discreteValues: List<DiscreteValue>,
    val displayIndex: Int,
    val hasDefaultValue: Boolean,
    val defaultValue: Double,
    val description: String,
) {
    fun getDefaultLabel(): String =
        if (featureType == DataType.DISCRETE)
            discreteValues.first { dv -> dv.index == defaultValue.toInt() }.label
        else ""

    internal fun toEntity() = Feature(
        id,
        name,
        groupId,
        featureType,
        discreteValues,
        displayIndex,
        hasDefaultValue,
        defaultValue,
        description
    )
}