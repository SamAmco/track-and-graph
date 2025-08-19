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
package com.samco.trackandgraph.util

import com.samco.trackandgraph.data.database.dto.Feature
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.sampling.DataSampleProperties

class FeatureDataProvider(
    dataSourceData: List<DataSourceData>,
    groups: List<Group>,
) : FeaturePathProvider(dataSourceData.map { it.feature }, groups) {

    private val dataSourceData: Map<DataSourceData, Group> = dataSourceData
        .mapNotNull { dataSource ->
            val group = groups.firstOrNull { it.id == dataSource.feature.groupId }
                ?: return@mapNotNull null
            dataSource to group
        }.toMap()

    fun dataSourceDataAlphabetically() =
        dataSourceData.keys.map {
            DataSourceDataWithPath(
                it.feature,
                it.dataProperties,
                getPathForFeature(it.feature.featureId)
            )
        }.sortedBy { it.path }

    fun getDataSampleProperties(featureId: Long) = dataSourceData.keys
        .firstOrNull { it.feature.featureId == featureId }
        ?.dataProperties

    data class DataSourceData(
        val feature: Feature,
        val dataProperties: DataSampleProperties?
    )

    data class DataSourceDataWithPath(
        val feature: Feature,
        val dataProperties: DataSampleProperties?,
        val path: String
    )
}