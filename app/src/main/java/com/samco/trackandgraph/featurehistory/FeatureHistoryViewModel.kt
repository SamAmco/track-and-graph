package com.samco.trackandgraph.featurehistory

import androidx.lifecycle.ViewModel
import com.samco.trackandgraph.database.DataPoint
import com.samco.trackandgraph.database.Feature
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao

class FeatureHistoryViewModel(featureId: Long, dataSource: TrackAndGraphDatabaseDao) : ViewModel() {
    var feature: Feature? = null
    val dataPoints = dataSource.getDataPointsForFeature(featureId)
    var currentActionDataPoint: DataPoint? = null
}