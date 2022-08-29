package com.samco.trackandgraph.functions

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddFunctionViewModel @Inject constructor(

) : ViewModel() {

    private var groupId: Long? = null

    fun initViewModel(groupId: Long) {
        if (this.groupId != null) return
        this.groupId = groupId
    }
}