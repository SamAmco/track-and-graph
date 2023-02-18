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

package com.samco.trackandgraph.graphstatinput

import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.database.dto.Group
import com.samco.trackandgraph.base.database.sampling.DataSampleProperties
import com.samco.trackandgraph.ui.FeaturePathProvider

class FeatureDataProvider(
    private val dataSourceData: Map<DataSourceData, Group>
) : FeaturePathProvider(dataSourceData.map { it.key.feature to it.value }.toMap()) {

    constructor(dataSourceData: List<DataSourceData>, groups: List<Group>) : this(
        dataSourceData.mapNotNull { dataSource ->
            val group = groups.firstOrNull { it.id == dataSource.feature.groupId }
                ?: return@mapNotNull null
            dataSource to group
        }.toMap()
    )

    fun dataSourceDataAlphabetically() =
        dataSourceData.keys.sortedBy { getPathForFeature(it.feature.featureId) }

    fun getDataSampleProperties(featureId: Long) = dataSourceData.keys.firstOrNull {
        it.feature.featureId == featureId
    }?.dataProperties

    data class DataSourceData(
        val feature: Feature,
        val labels: Set<String>,
        val dataProperties: DataSampleProperties
    )
}