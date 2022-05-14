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

package com.samco.trackandgraph.ui

import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.database.dto.Group


open class FeaturePathProvider(
    val features: List<Feature>,
    groups: List<Group>
) : GroupPathProvider(groups) {
    private val featuresById = features.map { it.id to it }.toMap()

    fun featuresSortedAlphabetically() = features.sortedBy { getPathForFeature(it.id) }

    fun getPathForFeature(id: Long): String {
        val feature = featuresById[id] ?: return ""
        val groupPath = getPathForGroup(feature.groupId)
        var path = groupPath
        if (groupPath.lastOrNull() != '/') path += '/'
        return path + feature.name
    }
}