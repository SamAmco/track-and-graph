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

package com.samco.trackandgraph.reminders.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.database.dto.IntervalPeriodPair
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.util.FeaturePathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

interface TimeSinceLastReminderConfigurationViewModel {
    val reminderName: StateFlow<String>
    val firstInterval: StateFlow<String>
    val firstPeriod: StateFlow<Period>
    val secondInterval: StateFlow<String>
    val secondPeriod: StateFlow<Period>
    val hasSecondInterval: StateFlow<Boolean>
    val featureName: StateFlow<String>
    val continueEnabled: StateFlow<Boolean>

    fun updateReminderName(name: String)
    fun updateFirstInterval(interval: String)
    fun updateFirstPeriod(period: Period)
    fun updateSecondInterval(interval: String)
    fun updateSecondPeriod(period: Period)
    fun updateHasSecondInterval(hasSecondInterval: Boolean)
    fun updateFeatureId(id: Long?)
    fun getReminder(): Reminder
    fun initializeFromReminder(reminder: Reminder?, params: ReminderParams.TimeSinceLastParams?)
    fun reset()
}

@HiltViewModel
class TimeSinceLastReminderConfigurationViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor
) : ViewModel(), TimeSinceLastReminderConfigurationViewModel {

    private val _reminderName = MutableStateFlow("")
    override val reminderName: StateFlow<String> = _reminderName.asStateFlow()

    private val _firstInterval = MutableStateFlow("1")
    override val firstInterval: StateFlow<String> = _firstInterval.asStateFlow()

    private val _firstPeriod = MutableStateFlow(Period.DAYS)
    override val firstPeriod: StateFlow<Period> = _firstPeriod.asStateFlow()

    private val _secondInterval = MutableStateFlow("1")
    override val secondInterval: StateFlow<String> = _secondInterval.asStateFlow()

    private val _secondPeriod = MutableStateFlow(Period.DAYS)
    override val secondPeriod: StateFlow<Period> = _secondPeriod.asStateFlow()

    private val _hasSecondInterval = MutableStateFlow(false)
    override val hasSecondInterval: StateFlow<Boolean> = _hasSecondInterval.asStateFlow()

    private val _featureId = MutableStateFlow<Long?>(null)

    private val _featureName = MutableStateFlow("")
    override val featureName: StateFlow<String> = _featureName.asStateFlow()

    override val continueEnabled: StateFlow<Boolean> = _featureId
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val featurePathMap = MutableStateFlow<Map<Long, String>?>(null)
    
    private var editingReminder: Reminder? = null

    init {
        // Initialize feature path map
        viewModelScope.launch {
            val allFeatures = dataInteractor.getAllFeaturesSync()
            val allGroups = dataInteractor.getAllGroupsSync()
            val pathProvider = FeaturePathProvider(allFeatures, allGroups)
            featurePathMap.value = pathProvider.sortedFeatureMap()
        }
    }

    override fun updateReminderName(name: String) {
        _reminderName.value = name
    }

    override fun updateFirstInterval(interval: String) {
        _firstInterval.value = interval
    }

    override fun updateFirstPeriod(period: Period) {
        _firstPeriod.value = period
    }

    override fun updateSecondInterval(interval: String) {
        _secondInterval.value = interval
    }

    override fun updateSecondPeriod(period: Period) {
        _secondPeriod.value = period
    }

    override fun updateHasSecondInterval(hasSecondInterval: Boolean) {
        _hasSecondInterval.value = hasSecondInterval
    }

    override fun updateFeatureId(id: Long?) {
        _featureId.value = id
        viewModelScope.launch {
            val pathMap = featurePathMap.filterNotNull().first()
            _featureName.value = if (id != null) {
                pathMap[id] ?: ""
            } else {
                ""
            }
        }
    }

    override fun getReminder(): Reminder {
        val firstIntervalInt = (_firstInterval.value.toDoubleOrNull()?.toInt() ?: 1).coerceAtLeast(1)
        val secondIntervalInt = (_secondInterval.value.toDoubleOrNull()?.toInt() ?: 1).coerceAtLeast(1)

        val params = ReminderParams.TimeSinceLastParams(
            firstInterval = IntervalPeriodPair(interval = firstIntervalInt, period = _firstPeriod.value),
            secondInterval = if (_hasSecondInterval.value) {
                IntervalPeriodPair(interval = secondIntervalInt, period = _secondPeriod.value)
            } else null
        )

        return editingReminder?.copy(
            reminderName = _reminderName.value,
            featureId = _featureId.value,
            params = params
        ) ?: Reminder(
            id = 0L,
            displayIndex = 0,
            reminderName = _reminderName.value,
            groupId = null,
            featureId = _featureId.value,
            params = params
        )
    }

    override fun initializeFromReminder(
        reminder: Reminder?,
        params: ReminderParams.TimeSinceLastParams?
    ) {
        editingReminder = reminder
        
        if (reminder != null) {
            _reminderName.value = reminder.reminderName
            _featureId.value = reminder.featureId
            // Wait for feature path map to be loaded before setting feature name
            viewModelScope.launch {
                val pathMap = featurePathMap.filterNotNull().first()
                _featureName.value = pathMap[reminder.featureId] ?: ""
            }
        }
        if (params != null) {
            _firstInterval.value = params.firstInterval.interval.toString()
            _firstPeriod.value = params.firstInterval.period
            _hasSecondInterval.value = params.secondInterval != null
            params.secondInterval?.let {
                _secondInterval.value = it.interval.toString()
                _secondPeriod.value = it.period
            }
        }
    }

    override fun reset() {
        _reminderName.value = ""
        _firstInterval.value = "1"
        _firstPeriod.value = Period.DAYS
        _secondInterval.value = "1"
        _secondPeriod.value = Period.DAYS
        _hasSecondInterval.value = false
        _featureId.value = null
        _featureName.value = ""
        editingReminder = null
    }
}
