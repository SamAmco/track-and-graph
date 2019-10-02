package com.samco.grapheasy.selecttrackgroup

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.samco.grapheasy.database.GraphEasyDatabaseDao

class SelectTrackGroupViewModelFactory(private val dataSource: GraphEasyDatabaseDao,
                                       private val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectTrackGroupViewModel::class.java)) {
            return SelectTrackGroupViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}