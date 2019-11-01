package com.samco.trackandgraph.featurehistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao

class FeatureHistoryViewModelFactory(
    private val featureId: Long,
    private val dataSource: TrackAndGraphDatabaseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeatureHistoryViewModel::class.java)) {
            return FeatureHistoryViewModel(featureId, dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}