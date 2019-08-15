package com.samco.grapheasy.selecttrackgroup

import android.app.Application
import androidx.lifecycle.ViewModel
import com.samco.grapheasy.database.GraphEasyDatabaseDao

class SelectTrackGroupViewModel(val dataSource: GraphEasyDatabaseDao, val application: Application) : ViewModel() {
    val trackGroups = dataSource.getTrackGroups()

    fun onTrackGroupSelected(groupId: Long) {
        //TODO
    }
}