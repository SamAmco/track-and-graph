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
package com.samco.trackandgraph.widgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.adddatapoint.AddDataPointsDialog
import com.samco.trackandgraph.adddatapoint.AddDataPointsViewModelImpl
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.Tracker
import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.data.model.di.IODispatcher
import com.samco.trackandgraph.data.model.di.MainDispatcher
import com.samco.trackandgraph.settings.TngSettings
import com.samco.trackandgraph.timers.TimerServiceInteractor
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import com.samco.trackandgraph.util.hideKeyboard
import com.samco.trackandgraph.util.performTrackVibrate
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

@AndroidEntryPoint
class TrackWidgetInputDataPointActivity : AppCompatActivity() {
    private val viewModel by viewModels<TrackWidgetInputDataPointViewModel>()

    private val addDataPointDialogViewModel by viewModels<AddDataPointsViewModelImpl>()

    @Inject
    lateinit var tngSettings: TngSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val featureId = intent.extras?.getLong(EXTRA_FEATURE_ID) ?: run {
            finish()
            return
        }
        val isStopTimer = intent.extras?.getBoolean(EXTRA_IS_STOP_TIMER, false) ?: false

        viewModel.initFromFeatureId(featureId, isStopTimer)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                observeDialogData()
            }
        }

        val composeView = ComposeView(this).apply {
            setContent {
                CompositionLocalProvider(LocalSettings provides tngSettings) {
                    AddDataPointsDialog(
                        viewModel = addDataPointDialogViewModel,
                        onDismissRequest = { finish() }
                    )
                }
            }
        }
        setContentView(composeView)
    }

    private suspend fun observeDialogData() = viewModel.dialogData
        .filterNotNull().collect { data ->
            when (data) {
                is DialogData.Invalid -> finish()

                is DialogData.Valid -> {
                    val tracker = data.tracker
                    when {
                        tracker.hasDefaultValue && data.customInitialValue == null -> {
                            viewModel.addDefaultDataPoint()
                            performTrackVibrate()
                            finish()
                        }

                        else -> {
                            addDataPointDialogViewModel.showAddDataPointDialog(
                                trackerId = data.trackerId,
                                customInitialValue = data.customInitialValue
                            )
                        }
                    }
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        window.hideKeyboard()
    }

    companion object {
        private const val EXTRA_FEATURE_ID = "featureId"
        private const val EXTRA_IS_STOP_TIMER = "isStopTimer"

        fun startActivityInputDataPoint(context: Context, featureId: Long) = actionStartActivity(
            ComponentName(context, TrackWidgetInputDataPointActivity::class.java),
            actionParametersOf(
                ActionParameters.Key<Long>(EXTRA_FEATURE_ID) to featureId,
                ActionParameters.Key<Boolean>(EXTRA_IS_STOP_TIMER) to false
            ),
        )

        fun startActivityStopTimer(context: Context, featureId: Long) = actionStartActivity(
            ComponentName(context, TrackWidgetInputDataPointActivity::class.java),
            actionParametersOf(
                ActionParameters.Key<Long>(EXTRA_FEATURE_ID) to featureId,
                ActionParameters.Key<Boolean>(EXTRA_IS_STOP_TIMER) to true
            )
        )

        fun createStopTimerIntent(context: Context, featureId: Long): Intent {
            return Intent(context, TrackWidgetInputDataPointActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_FEATURE_ID, featureId)
                putExtra(EXTRA_IS_STOP_TIMER, true)
            }
        }
    }
}

sealed class DialogData {
    data class Valid(
        val trackerId: Long,
        val tracker: Tracker,
        val customInitialValue: Double?
    ) : DialogData()

    data object Invalid : DialogData()
}

@HiltViewModel
class TrackWidgetInputDataPointViewModel @Inject constructor(
    private var dataInteractor: DataInteractor,
    private val timerServiceInteractor: TimerServiceInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel() {

    private val _dialogData = MutableStateFlow<DialogData?>(null)
    val dialogData: StateFlow<DialogData?> get() = _dialogData.asStateFlow()

    private var initialized = false

    fun initFromFeatureId(featureId: Long, isStopTimer: Boolean) {
        if (initialized) return
        initialized = true
        viewModelScope.launch(io) {
            val tracker = dataInteractor.getTrackerByFeatureId(featureId)
            if (tracker == null) {
                withContext(ui) { _dialogData.value = DialogData.Invalid }
                return@launch
            }

            val customInitialValue = if (isStopTimer) {
                // Stop timer and get duration in seconds
                val duration = dataInteractor.stopTimerForTracker(tracker.id)
                timerServiceInteractor.requestWidgetUpdatesForFeatureId(featureId)
                duration?.seconds?.toDouble()
            } else {
                null
            }

            withContext(ui) {
                _dialogData.value = DialogData.Valid(
                    trackerId = tracker.id,
                    tracker = tracker,
                    customInitialValue = customInitialValue
                )
            }
        }
    }

    fun addDefaultDataPoint() {
        val currentData = _dialogData.value
        if (currentData is DialogData.Valid) {
            val tracker = currentData.tracker
            viewModelScope.launch(io) {
                val newDataPoint = DataPoint(
                    timestamp = OffsetDateTime.now(),
                    featureId = tracker.featureId,
                    value = tracker.defaultValue,
                    label = tracker.defaultLabel,
                    note = ""
                )
                dataInteractor.insertDataPoint(newDataPoint)
            }
        }
    }
}