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

package com.samco.trackandgraph.database

import com.samco.trackandgraph.database.dto.IDataPoint
import com.samco.trackandgraph.database.entity.DataPoint
import com.samco.trackandgraph.database.entity.DataType
import com.samco.trackandgraph.functionslib.DataSample
import org.threeten.bp.OffsetDateTime

class DataSamplerImpl(private val dao: TrackAndGraphDatabaseDao) : IDataSampler {
    private fun getDataType(featureId: Long) = dao.getFeatureById(featureId).featureType

    private fun emptyDataSample() = DataSample.fromSequence(emptySequence())

    private fun dataSampleFromDb(dataPoints: List<DataPoint>, dataType: DataType): DataSample {
        return DataSample.fromSequence(dataPoints.asSequence().map { toIDataPoint(it, dataType) })
    }

    private fun toIDataPoint(dataPoint: DataPoint, dataType: DataType): IDataPoint {
        return object : IDataPoint() {
            override val timestamp = dataPoint.timestamp
            override val dataType = dataType
            override val value = dataPoint.value
            override val label = dataPoint.label
            override val note = dataPoint.note
        }
    }

    override fun getLastDataPointBetween(
        dataSource: DataSource,
        min: String,
        max: String
    ): IDataPoint? {
        return when (dataSource) {
            is DataSource.FeatureDataSource -> {
                val dataPoint = dao.getLastDataPointBetween(
                    dataSource.featureId,
                    min,
                    max
                )
                if (dataPoint == null) {
                    null
                } else {
                    val dataType = getDataType(dataSource.featureId)
                    toIDataPoint(dataPoint, dataType)
                }
            }
            //TODO implement function data source
            is DataSource.FunctionDataSource -> null
        }
    }

    override fun getLastDataPointWithValue(dataSource: DataSource, values: List<Int>): IDataPoint? {
        return when (dataSource) {
            is DataSource.FeatureDataSource -> {
                val dataPoint = dao.getLastDataPointWithValue(
                    dataSource.featureId,
                    values
                )
                if (dataPoint == null) {
                    null
                } else {
                    val dataType = getDataType(dataSource.featureId)
                    toIDataPoint(dataPoint, dataType)
                }
            }
            //TODO implement function data source
            is DataSource.FunctionDataSource -> null
        }
    }

    override fun getDataPointsBetween(
        dataSource: DataSource,
        min: String,
        max: String
    ): DataSample {
        return when (dataSource) {
            is DataSource.FeatureDataSource -> {
                dataSampleFromDb(
                    dao.getDataPointsBetween(dataSource.featureId, min, max),
                    getDataType(dataSource.featureId)
                )
            }
            //TODO implement function data source
            is DataSource.FunctionDataSource -> emptyDataSample()
        }
    }

    override fun getDataPointsWithValue(dataSource: DataSource, values: List<Int>): DataSample {
        return when (dataSource) {
            is DataSource.FeatureDataSource -> {
                dataSampleFromDb(
                    dao.getDataPointsWithValue(dataSource.featureId, values),
                    getDataType(dataSource.featureId)
                )
            }
            //TODO implement function data source
            is DataSource.FunctionDataSource -> emptyDataSample()
        }
    }

    override fun getDataPointsForDataSource(dataSource: DataSource): DataSample {
        return when (dataSource) {
            is DataSource.FeatureDataSource -> {
                dataSampleFromDb(
                    dao.getDataPointsForFeatureSync(dataSource.featureId),
                    getDataType(dataSource.featureId)
                )
            }
            //TODO implement function data source
            is DataSource.FunctionDataSource -> emptyDataSample()
        }
    }
}