package com.samco.trackandgraph.base.model

import com.samco.trackandgraph.base.database.entity.DataPoint
import javax.inject.Inject

internal class DataPointUpdateHelper @Inject constructor() {

    companion object {
        private const val BUFFER_SIZE = 1000
    }

    fun interface DataPointRetriever {
        operator fun invoke(limit: Int, offset: Int): List<DataPoint>
    }

    private fun validArguments(
        whereValue: Double?,
        whereLabel: String?,
        toValue: Double?,
        toLabel: String?
    ) = listOf(whereValue, whereLabel).any { it != null }
            && listOf(toValue, toLabel).any { it != null }

    fun performUpdate(
        whereValue: Double?,
        whereLabel: String?,
        toValue: Double?,
        toLabel: String?,
        getNumDataPoints: () -> Int,
        getDataPoints: DataPointRetriever,
        performUpdate: (List<DataPoint>) -> Unit
    ) {
        if (!validArguments(whereValue, whereLabel, toValue, toLabel))
            throw Exception("At least one of whereValue and whereLabel must be non-null and at least one of toValue and toLabel must be non-null")

        val count = getNumDataPoints()
        var consumed = 0
        while (consumed < count) {
            val frame = getDataPoints(BUFFER_SIZE, consumed)
            if (frame.isEmpty()) break

            val update = mutableListOf<DataPoint>()
            for (dataPoint in frame) {
                if (matchesWhere(dataPoint, whereValue, whereLabel)) {
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
        whereLabel: String?
    ) = when {
        whereValue != null && whereLabel != null -> dataPoint.value == whereValue && dataPoint.label == whereLabel
        whereValue != null -> dataPoint.value == whereValue
        whereLabel != null -> dataPoint.label == whereLabel
        else -> false
    }
}