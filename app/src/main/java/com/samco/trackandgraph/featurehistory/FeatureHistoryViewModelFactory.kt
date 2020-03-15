/* 
* This file is part of Track & Graph
* 
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.featurehistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao

class FeatureHistoryViewModelFactory(
    private val featureId: Long,
    private val dataSource: TrackAndGraphDatabaseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeatureHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FeatureHistoryViewModel(featureId, dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}