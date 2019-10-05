package com.samco.grapheasy.displaytrackgroup

import androidx.lifecycle.ViewModel
import com.samco.grapheasy.database.DataPoint
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.GraphEasyDatabaseDao

class DisplayTrackGroupViewModel(
    private val trackGroupId: Long,
    private val noDataString: String,
    private val dataSource: GraphEasyDatabaseDao
): ViewModel() {
    var currentActionFeature: Feature? = null

    val features = dataSource.getFeaturesForTrackGroup(trackGroupId)

    //TODO getLastDataPointForFeature
    fun getLastDataPointForFeature(feature: Feature): DataPoint? = null

    fun getLastDisplayDataPointValueForFeature(feature: Feature): String {
        val dataPoint = getLastDataPointForFeature(feature)
        return dataPoint?.value ?: ""
    }

    fun getLastDisplayDataPointTimeForFeature(feature: Feature): String {
        val dataPoint = getLastDataPointForFeature(feature)
        return dataPoint?.getDisplayTimestamp() ?: noDataString
    }

    //TODO getNumDataPointsForFeatre
    fun getNumDataPointsForFeatre(feature: Feature): Int {
        return 0
    }
}