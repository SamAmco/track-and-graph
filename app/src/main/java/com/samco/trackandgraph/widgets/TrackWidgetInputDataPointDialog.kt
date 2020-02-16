package com.samco.trackandgraph.widgets

import com.samco.trackandgraph.displaytrackgroup.InputDataPointDialog

class TrackWidgetInputDataPointDialog : InputDataPointDialog() {
    override fun onPause() {
        super.onPause()
        activity?.finish()
    }
}