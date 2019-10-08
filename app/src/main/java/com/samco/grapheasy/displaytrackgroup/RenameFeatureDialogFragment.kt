package com.samco.grapheasy.displaytrackgroup

import androidx.fragment.app.Fragment
import com.samco.grapheasy.R
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.ui.NameInputDialogFragment

class RenameFeatureDialogFragment : NameInputDialogFragment() {
    private lateinit var listener: RenameFeatureDialogListener
    private lateinit var feature: Feature

    interface RenameFeatureDialogListener {
        fun getFeature() : Feature
        fun onRenameFeature(feature: Feature)
    }

    override fun registerListener(parentFragment: Fragment?) {
        listener = parentFragment as RenameFeatureDialogListener
        feature = listener.getFeature()
    }

    override fun getPositiveButtonName(): String {
        return getString(R.string.rename)
    }

    override fun onPositiveClicked(name: String) {
        val newFeature = feature.copy(name = name)
        listener.onRenameFeature(newFeature)
    }

    override fun getNameInputHint(): String = getString(R.string.feature_name_hint)

    override fun getTitleText(): String = getString(R.string.new_name)

    override fun getNameInputText(): String = feature.name
}