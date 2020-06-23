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
package com.samco.trackandgraph.displaytrackgroup

import androidx.fragment.app.Fragment
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.entity.Feature
import com.samco.trackandgraph.ui.NameInputDialogFragment

class RenameFeatureDialogFragment : NameInputDialogFragment() {

    private lateinit var listener: RenameFeatureDialogListener
    private lateinit var feature: Feature

    interface RenameFeatureDialogListener {
        fun getFeature() : Feature
        fun onRenameFeature(newName: String)
        fun getMaxFeatureNameChars(): Int
    }

    override fun getMaxChars(): Int = listener.getMaxFeatureNameChars()

    override fun registerListener(parentFragment: Fragment?) {
        listener = parentFragment as RenameFeatureDialogListener
        feature = listener.getFeature()
    }

    override fun getPositiveButtonName(): String {
        return getString(R.string.rename)
    }

    override fun onPositiveClicked(name: String) = listener.onRenameFeature(name)

    override fun getNameInputHint(): String = ""

    override fun getTitleText(): String = getString(R.string.new_name)

    override fun getNameInputText(): String = feature.name
}