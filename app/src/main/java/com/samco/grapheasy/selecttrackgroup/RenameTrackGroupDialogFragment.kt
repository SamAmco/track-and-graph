package com.samco.grapheasy.selecttrackgroup

import androidx.fragment.app.Fragment
import com.samco.grapheasy.R
import com.samco.grapheasy.database.TrackGroup
import com.samco.grapheasy.ui.NameInputDialogFragment

class RenameTrackGroupDialogFragment : NameInputDialogFragment() {

    private lateinit var listener: RenameTrackGroupDialogListener
    private lateinit var trackGroup: TrackGroup

    interface RenameTrackGroupDialogListener {
        fun getTrackGroup() : TrackGroup
        fun onRenameTrackGroup(trackGroup: TrackGroup)
    }

    override fun registerListener(parentFragment: Fragment?) {
        listener = parentFragment as RenameTrackGroupDialogListener
        trackGroup = listener.getTrackGroup()
    }

    override fun getPositiveButtonName(): String {
        return getString(R.string.rename)
    }

    override fun onPositiveClicked(name: String) {
        val newTrackGroup = TrackGroup(trackGroup.id, name)
        listener.onRenameTrackGroup(newTrackGroup)
    }

    override fun getNameInputHint(): String = getString(R.string.track_group_name)

    override fun getTitleText(): String = getString(R.string.new_name)

    override fun getNameInputText(): String = trackGroup.name
}