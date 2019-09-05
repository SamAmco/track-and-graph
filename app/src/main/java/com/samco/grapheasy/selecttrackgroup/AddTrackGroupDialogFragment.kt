package com.samco.grapheasy.selecttrackgroup

import androidx.fragment.app.Fragment
import com.samco.grapheasy.R
import com.samco.grapheasy.ui.NameInputDialogFragment


class AddTrackGroupDialogFragment : NameInputDialogFragment() {
    private lateinit var listener: AddTrackGroupDialogListener

    interface AddTrackGroupDialogListener {
        fun onAddTrackGroup(name: String)
    }

    override fun registerListener(parentFragment: Fragment?) {
        listener = parentFragment as AddTrackGroupDialogListener
    }

    override fun getPositiveButtonName(): String = getString(R.string.add)

    override fun onPositiveClicked(name: String) = listener.onAddTrackGroup(name)

    override fun getNameInputHint(): String = getString(R.string.track_group_name)

    override fun getTitleText(): String = getString(R.string.add_track_group)

    override fun getNameInputText(): String = ""
}