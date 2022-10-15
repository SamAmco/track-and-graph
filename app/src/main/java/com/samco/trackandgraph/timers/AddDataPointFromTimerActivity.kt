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

package com.samco.trackandgraph.timers

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.adddatapoint.DURATION_SECONDS_KEY
import com.samco.trackandgraph.adddatapoint.TRACKER_LIST_KEY
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.util.hideKeyboard
import com.samco.trackandgraph.widgets.TrackWidgetDataPointInputDialog
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import javax.inject.Inject

@AndroidEntryPoint
class AddDataPointFromTimerActivity : AppCompatActivity() {

    companion object {
        const val TRACKER_ID_KEY = "TRACKER_ID_KEY"
        const val START_TIME_KEY = "START_TIME_KEY"
    }

    private val viewModel by viewModels<AddDataPointFromTimerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.extras
        val trackerId = bundle?.getLong(TRACKER_ID_KEY)
        val startTimeStr = bundle?.getString(START_TIME_KEY)

        if (trackerId == null || startTimeStr == null) {
            finish()
        } else {
            val startInstant = Instant.parse(startTimeStr)
            val duration = Duration.between(startInstant, Instant.now()).seconds
            viewModel.stopTimer(trackerId)
            showDialog(trackerId, duration)
        }
    }

    private fun showDialog(featureId: Long, duration: Long) {
        val dialog = TrackWidgetDataPointInputDialog()
        val args = Bundle()
        args.putLongArray(TRACKER_LIST_KEY, longArrayOf(featureId))
        args.putLong(DURATION_SECONDS_KEY, duration)
        dialog.arguments = args
        supportFragmentManager.let { dialog.show(it, "input_data_points_dialog") }
    }

    override fun onDestroy() {
        super.onDestroy()
        window.hideKeyboard()
    }
}

@HiltViewModel
class AddDataPointFromTimerViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel() {
    fun stopTimer(trackerId: Long) {
        viewModelScope.launch(io) {
            dataInteractor.stopTimerForTracker(trackerId)
        }
    }

}
