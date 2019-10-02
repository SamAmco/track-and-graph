package com.samco.grapheasy.displaytrackgroup

import androidx.lifecycle.ViewModel
import com.samco.grapheasy.database.GraphEasyDatabaseDao

class DisplayTrackGroupViewModel(
    trackGroupId: Long,
    dataSource: GraphEasyDatabaseDao
): ViewModel() {
    val features = dataSource.getFeaturesForTrackGroup(trackGroupId)
}