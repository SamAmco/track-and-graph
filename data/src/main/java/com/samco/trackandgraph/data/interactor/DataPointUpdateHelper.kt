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

package com.samco.trackandgraph.data.interactor

import com.samco.trackandgraph.data.database.entity.DataPoint
import javax.inject.Inject

internal interface DataPointUpdateHelper {
    fun interface DataPointRetriever {
        operator fun invoke(limit: Int, offset: Int): List<DataPoint>
    }

    fun performUpdate(
        whereValue: Double?,
        whereLabel: String?,
        toValue: Double?,
        toLabel: String?,
        getNumDataPoints: () -> Int,
        isDuration: () -> Boolean,
        getDataPoints: DataPointRetriever,
        performUpdate: (List<DataPoint>) -> Unit
    )
}

internal class DataPointUpdateHelperImpl @Inject constructor() : DataPointUpdateHelper {

    companion object {
        private const val BUFFER_SIZE = 1000
    }

    private fun validArguments(
        whereValue: Double?,
        whereLabel: String?,
        toValue: Double?,
        toLabel: String?
    ) = listOf(whereValue, whereLabel).any { it != null }
            && listOf(toValue, toLabel).any { it != null }

    override fun performUpdate(
        whereValue: Double?,
        whereLabel: String?,
        toValue: Double?,
        toLabel: String?,
        getNumDataPoints: () -> Int,
        isDuration: () -> Boolean,
        getDataPoints: DataPointUpdateHelper.DataPointRetriever,
        performUpdate: (List<DataPoint>) -> Unit
    ) {
        if (!validArguments(whereValue, whereLabel, toValue, toLabel))
            throw Exception("At least one of whereValue and whereLabel must be non-null and at least one of toValue and toLabel must be non-null")

        val count = getNumDataPoints()
        val isDurationBool = isDuration()
        var consumed = 0
        while (consumed < count) {
            val frame = getDataPoints(BUFFER_SIZE, consumed)
            if (frame.isEmpty()) break

            val update = mutableListOf<DataPoint>()
            for (dataPoint in frame) {
                if (matchesWhere(dataPoint, whereValue, whereLabel, isDurationBool)) {
                    update.add(performTo(dataPoint, toValue, toLabel))
                }
                consumed++
            }
            performUpdate(update)
        }
    }

    private fun performTo(
        dataPoint: DataPoint,
        toValue: Double?,
        toLabel: String?
    ) = when {
        toValue != null && toLabel != null -> dataPoint.copy(
            value = toValue,
            label = toLabel
        )
        toValue != null -> dataPoint.copy(value = toValue)
        toLabel != null -> dataPoint.copy(label = toLabel)
        else -> dataPoint
    }

    private fun matchesWhere(
        dataPoint: DataPoint,
        whereValue: Double?,
        whereLabel: String?,
        isDuration: Boolean
    ) = when {
        whereValue != null && whereLabel != null ->
            valueMatches(dataPoint.value, whereValue, isDuration) && dataPoint.label == whereLabel
        whereValue != null -> valueMatches(dataPoint.value, whereValue, isDuration)
        whereLabel != null -> dataPoint.label == whereLabel
        else -> false
    }

    private fun valueMatches(dp: Double, where: Double, isDuration: Boolean): Boolean = when {
        isDuration -> dp.toLong() == where.toLong()
        else -> dp == where
    }
}