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

package com.samco.trackandgraph.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSpinner


class ExtendedSpinner(context: Context, attrs: AttributeSet?) : AppCompatSpinner(context, attrs) {
    interface OnItemClickedListener {
        fun onItemClicked(index: Int)
    }

    var listener: OnItemClickedListener? = null

    override fun setSelection(position: Int) {
        super.setSelection(position)
        if (listener != null) listener!!.onItemClicked(position)
    }

    fun setOnItemClickedListener(listener: OnItemClickedListener?) {
        this.listener = listener
    }
}