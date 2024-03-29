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

/**
 * The base interface for trackers and functions. A feature is a reference to a data source and
 * can be used to retrieve a data sample which may be generated by a function or read straight from
 * the underlying database.
 *
 * All implementations of [Feature] should override [equals] and [hashCode] as it may be used as a
 * map key. You should not need to construct an implementation of [Feature] directly from the app
 * module.
 */
interface Feature {
    val featureId: Long
    val name: String
    val groupId: Long
    val displayIndex: Int
    val description: String
}

internal data class FeatureDtoImpl(
    override val featureId: Long,
    override val name: String,
    override val groupId: Long,
    override val displayIndex: Int,
    override val description: String
) : Feature

internal fun Feature.toEntity() = com.samco.trackandgraph.base.database.entity.Feature(
    featureId,
    name,
    groupId,
    displayIndex,
    description
)
