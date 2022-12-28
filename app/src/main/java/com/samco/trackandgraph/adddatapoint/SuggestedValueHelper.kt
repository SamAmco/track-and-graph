package com.samco.trackandgraph.adddatapoint

import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SuggestedValue(
    val value: Double,
    val label: String
)

interface SuggestedValueHelper {
    suspend fun getSuggestedValues(tracker: Tracker): List<SuggestedValue>
}

class SuggestedValueHelperImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : SuggestedValueHelper {

    companion object {
        const val MAX_VALUES = 1000
    }

    override suspend fun getSuggestedValues(tracker: Tracker): List<SuggestedValue> =
        withContext(io) {
            return@withContext dataInteractor
                .getDataSampleForFeatureId(tracker.featureId)
                .take(MAX_VALUES)
                .map { SuggestedValue(it.value, it.label) }
                .toCollection(LinkedHashSet())
                .sortedBy { it.value }
        }
}