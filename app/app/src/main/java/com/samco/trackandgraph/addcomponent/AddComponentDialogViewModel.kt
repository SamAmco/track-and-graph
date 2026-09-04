/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.addcomponent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.interactor.DataInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AddComponentWarning {
    GRAPH_REQUIRES_TRACKER,
    FUNCTION_REQUIRES_TRACKER,
}

interface AddComponentDialogViewModel {
    val warning: StateFlow<AddComponentWarning?>

    fun dismissWarning()
    fun addGraphOrStat(groupId: Long, onAddGraphOrStat: (Long) -> Unit)
    fun addFunction(groupId: Long, onAddFunction: (Long) -> Unit)
}

@HiltViewModel
class AddComponentDialogViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
) : ViewModel(), AddComponentDialogViewModel {
    private val _warning = MutableStateFlow<AddComponentWarning?>(null)
    override val warning: StateFlow<AddComponentWarning?> = _warning.asStateFlow()

    override fun dismissWarning() {
        _warning.value = null
    }

    override fun addGraphOrStat(groupId: Long, onAddGraphOrStat: (Long) -> Unit) {
        addTrackerDependentComponent(
            groupId = groupId,
            warning = AddComponentWarning.GRAPH_REQUIRES_TRACKER,
            onAdd = onAddGraphOrStat,
        )
    }

    override fun addFunction(groupId: Long, onAddFunction: (Long) -> Unit) {
        addTrackerDependentComponent(
            groupId = groupId,
            warning = AddComponentWarning.FUNCTION_REQUIRES_TRACKER,
            onAdd = onAddFunction,
        )
    }

    private fun addTrackerDependentComponent(
        groupId: Long,
        warning: AddComponentWarning,
        onAdd: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            if (dataInteractor.hasAtLeastOneTracker()) {
                onAdd(groupId)
            } else {
                _warning.value = warning
            }
        }
    }
}
