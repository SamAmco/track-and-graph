package com.samco.grapheasy.featurehistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.samco.grapheasy.database.GraphEasyDatabaseDao

class FeatureHistoryViewModelFactory(
    private val featureId: Long,
    private val dataSource: GraphEasyDatabaseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeatureHistoryViewModel::class.java)) {
            return FeatureHistoryViewModel(featureId, dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}