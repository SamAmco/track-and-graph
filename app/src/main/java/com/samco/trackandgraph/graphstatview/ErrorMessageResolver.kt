/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.graphstatview

import android.content.Context
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.exceptions.GraphNotFoundException
import com.samco.trackandgraph.graphstatview.exceptions.NotEnoughDataException

class ErrorMessageResolver(private val context: Context) {
    fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is GraphStatInitException -> context.getString(throwable.errorTextId)
            is NotEnoughDataException -> context.getString(R.string.graph_stat_view_not_enough_data_graph)
            is GraphNotFoundException -> context.getString(R.string.graph_stat_view_not_found)
            else -> context.getString(R.string.graph_stat_validation_unknown)
            //TODO flesh this out with support for common functions exceptions
        }
    }
}