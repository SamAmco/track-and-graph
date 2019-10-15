package com.samco.grapheasy.featurehistory

import androidx.lifecycle.ViewModel
import com.samco.grapheasy.database.DataPoint
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.GraphEasyDatabaseDao

class FeatureHistoryViewModel(featureId: Long, dataSource: GraphEasyDatabaseDao) : ViewModel() {
    var feature: Feature? = null
    val dataPoints = dataSource.getDataPointsForFeature(featureId)
    var currentActionDataPoint: DataPoint? = null
}