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
package com.samco.trackandgraph.adddatapoint

import com.samco.trackandgraph.data.database.dto.IDataPoint
import com.samco.trackandgraph.data.database.dto.Tracker
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionType
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.sampling.DataSampler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import javax.inject.Inject

data class SuggestedValue(
    val value: Double?,
    val label: String?
)

interface SuggestedValueHelper {
    fun getSuggestedValues(tracker: Tracker): Flow<List<SuggestedValue>>
}

@OptIn(ExperimentalCoroutinesApi::class)
class SuggestedValueHelperImpl @Inject constructor(
    private val dataSampler: DataSampler,
    @IODispatcher private val io: CoroutineDispatcher
) : SuggestedValueHelper {

    companion object {
        const val MAX_VALUES = 1000
    }

    private fun getDataPoints(tracker: Tracker): Flow<IDataPoint> = flow {
        val sample = dataSampler.getDataSampleForFeatureId(tracker.featureId)
        emit(
            flow { for (dp in sample) emit(dp) }
                .onCompletion { sample.dispose() }
        )
    }.flatMapLatest { it }

    override fun getSuggestedValues(tracker: Tracker): Flow<List<SuggestedValue>> =
        getDataPoints(tracker)
            .take(MAX_VALUES)
            .onCompletion { }
            .map { SuggestedValue(it.value, it.label.ifEmpty { null }) }
            .scan(emptyList<SuggestedValue>()) { acc, value -> acc + value }
            .buffer(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            .sort(tracker)
            .mapToSuggestionType(tracker)
            .map { it.distinct() }
            .buffer(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            .flowOn(io)

    private val emptySuggestion = SuggestedValue(null, null)

    private fun Flow<List<SuggestedValue>>.mapToSuggestionType(tracker: Tracker) =
        when (tracker.suggestionType) {
            TrackerSuggestionType.VALUE_AND_LABEL -> this
            TrackerSuggestionType.VALUE_ONLY -> this.map { list -> list.map { it.copy(label = null) } }
            TrackerSuggestionType.LABEL_ONLY -> this.map { list -> list.map { it.copy(value = null) } }
            TrackerSuggestionType.NONE -> this.map { emptyList() }
        }.map { list -> list.filter { it != emptySuggestion } }

    private fun Flow<List<SuggestedValue>>.sort(tracker: Tracker): Flow<List<SuggestedValue>> {
        return if (tracker.suggestionType == TrackerSuggestionType.NONE) this.map { emptyList() }
        else when (tracker.suggestionOrder) {
            TrackerSuggestionOrder.VALUE_ASCENDING -> this.map { values -> values.sortedBy { it.value } }
            TrackerSuggestionOrder.VALUE_DESCENDING -> this.map { values -> values.sortedByDescending { it.value } }
            TrackerSuggestionOrder.LABEL_ASCENDING -> this.map { values ->
                values.sortedWith(
                    compareBy(stringComparatorWithEmpty) { it.label }
                )
            }
            TrackerSuggestionOrder.LABEL_DESCENDING -> this.map { values ->
                values.sortedWith(
                    compareBy(stringComparatorReversedWithEmpty) { it.label }
                )
            }
            TrackerSuggestionOrder.LATEST -> this
            TrackerSuggestionOrder.OLDEST -> this.map { values -> values.reversed() }
        }

    }

    private val stringComparatorWithEmpty = Comparator<String?> { s1, s2 ->
        return@Comparator checkEmptys(s1, s2) ?: s1!!.compareTo(s2!!)
    }

    private val stringComparatorReversedWithEmpty = Comparator<String?> { s1, s2 ->
        return@Comparator checkEmptys(s1, s2) ?: s2!!.compareTo(s1!!)
    }

    //The common code from the above two functions is extracted into a separate function
    private fun checkEmptys(s1: String?, s2: String?): Int? {
        if (s1 == null && s2 == null) return 0
        if (s1 == null) return 1
        if (s2 == null) return -1
        if (s1.isEmpty() && s2.isEmpty()) return 0
        if (s1.isEmpty()) return 1
        if (s2.isEmpty()) return -1
        return null
    }
}