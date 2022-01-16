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

import android.database.Cursor
import com.samco.trackandgraph.database.entity.DataPoint
import com.samco.trackandgraph.database.entity.DataType
import com.samco.trackandgraph.functionslib.DataSample
import com.samco.trackandgraph.functionslib.DataSampleProperties
import com.samco.trackandgraph.functionslib.cache

class DataSamplerImpl(private val dao: TrackAndGraphDatabaseDao) : IDataSampler {
    private fun getDataType(featureId: Long) = dao.getFeatureById(featureId).featureType

    private fun emptyDataSample() = DataSample.fromSequence(emptySequence())

    private fun dataSampleFromDb(cursor: Cursor, dataType: DataType): DataSample {
        val cursorSequence = DataPointCursorSequence(cursor, dataType)
        return DataSample.fromSequence(
            cursorSequence.cache(),
            DataSampleProperties(),
            cursorSequence::getRawDataPoints
        )
    }

    override fun getDataPointsForDataSource(dataSource: DataSource): DataSample {
        return when (dataSource) {
            is DataSource.FeatureDataSource -> {
                dataSampleFromDb(
                    dao.getDataPointsCursorForFeatureSync(dataSource.featureId),
                    getDataType(dataSource.featureId)
                )
            }
            //TODO implement function data source
            is DataSource.FunctionDataSource -> emptyDataSample()
        }
    }
}