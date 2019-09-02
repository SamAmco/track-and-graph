package com.samco.grapheasy.selecttrackgroup

import android.app.Application
import androidx.lifecycle.ViewModel
import com.samco.grapheasy.database.GraphEasyDatabaseDao
import com.samco.grapheasy.database.TrackGroup

class SelectTrackGroupViewModel(val dataSource: GraphEasyDatabaseDao, val application: Application) : ViewModel() {
    val trackGroups = dataSource.getTrackGroups()
    var currentActionTrackGroup: TrackGroup? = null
}