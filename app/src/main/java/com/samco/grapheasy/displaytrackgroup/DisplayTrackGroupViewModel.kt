package com.samco.grapheasy.displaytrackgroup

import androidx.lifecycle.ViewModel
import com.samco.grapheasy.database.DisplayFeature
import com.samco.grapheasy.database.GraphEasyDatabaseDao

class DisplayTrackGroupViewModel(
    private val trackGroupId: Long,
    private val dataSource: GraphEasyDatabaseDao
): ViewModel() {
    var currentActionFeature: DisplayFeature? = null

    val features = dataSource.getDisplayFeaturesForTrackGroup(trackGroupId)
}