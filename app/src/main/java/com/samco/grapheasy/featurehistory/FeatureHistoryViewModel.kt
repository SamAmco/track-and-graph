package com.samco.grapheasy.featurehistory

import androidx.lifecycle.ViewModel
import com.samco.grapheasy.database.GraphEasyDatabaseDao

class FeatureHistoryViewModel(featureId: Long, dataSource: GraphEasyDatabaseDao) : ViewModel() {
    val dataPoints = dataSource.getDataPointsForFeature(featureId)
}