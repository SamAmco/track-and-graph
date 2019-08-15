package com.samco.grapheasy.selecttrackgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SelectTrackGroupViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectTrackGroupViewModel::class.java)) {
            return SelectTrackGroupViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}