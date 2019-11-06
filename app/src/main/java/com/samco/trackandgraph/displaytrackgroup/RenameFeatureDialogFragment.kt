package com.samco.trackandgraph.displaytrackgroup

import androidx.fragment.app.Fragment
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.Feature
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

    override fun getNameInputHint(): String = getString(R.string.feature_name_hint)

    override fun getTitleText(): String = getString(R.string.new_name)

    override fun getNameInputText(): String = feature.name
}