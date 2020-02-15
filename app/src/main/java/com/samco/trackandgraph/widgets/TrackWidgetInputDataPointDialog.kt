package com.samco.trackandgraph.widgets

import android.util.Log
import com.samco.trackandgraph.displaytrackgroup.InputDataPointDialog

class TrackWidgetInputDataPointDialog : InputDataPointDialog() {
    override fun onPause() {
        super.onPause()
        Log.d("Widget", "on pause! closing activity!")
        activity?.finish()
    }
}