package com.samco.trackandgraph.widgets

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.samco.trackandgraph.displaytrackgroup.FEATURE_LIST_KEY
import com.samco.trackandgraph.displaytrackgroup.InputDataPointDialog

class TrackWidgetInputDataPoint : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.extras

        bundle?.getLong(FEATURE_KEY)?.let {featureId ->
            val args = Bundle()
            args.putLongArray(FEATURE_LIST_KEY, longArrayOf(featureId))

            val dialog = InputDataPointDialog()
            dialog.arguments = args

            if (savedInstanceState == null) {
                supportFragmentManager.let { dialog.show(it, "input_data_points_dialog") }
            }
        }
    }
}