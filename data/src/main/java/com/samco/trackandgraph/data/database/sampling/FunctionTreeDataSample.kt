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
package com.samco.trackandgraph.data.database.sampling

import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.entity.queryresponse.FunctionWithFeature
import org.threeten.bp.OffsetDateTime

internal class FunctionTreeDataSample(
    private val function: FunctionWithFeature,
    private val dao: TrackAndGraphDatabaseDao,
) : RawDataSample() {
    override fun dispose() {}

    private val dataPoints = listOf(
        DataPoint(
            timestamp = OffsetDateTime.now(),
            featureId = function.featureId,
            value = 5.0,
            label = "five",
            note = "five note"
        ),
        DataPoint(
            timestamp = OffsetDateTime.now().minusDays(1),
            featureId = function.featureId,
            value = 4.0,
            label = "four",
            note = "four note"
        ),
        DataPoint(
            timestamp = OffsetDateTime.now().minusDays(2),
            featureId = function.featureId,
            value = 3.0,
            label = "three",
            note = "three note"
        ),
        DataPoint(
            timestamp = OffsetDateTime.now().minusDays(3),
            featureId = function.featureId,
            value = 2.0,
            label = "two",
            note = "two note"
        ),
        DataPoint(
            timestamp = OffsetDateTime.now().minusDays(4),
            featureId = function.featureId,
            value = 1.0,
            label = "one",
            note = "one note"
        )
    )

    override fun getRawDataPoints(): List<DataPoint> {
        return dataPoints
    }

    override fun iterator(): Iterator<DataPoint> = dataPoints.iterator()
}