package com.samco.grapheasy.displaytrackgroup

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.samco.grapheasy.database.GraphEasyDatabaseDao

class DisplayTrackGroupViewModelFactory(
    private val trackGroupId: Long,
    private val dataSource: GraphEasyDatabaseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DisplayTrackGroupViewModel::class.java)) {
            return DisplayTrackGroupViewModel(trackGroupId, dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
