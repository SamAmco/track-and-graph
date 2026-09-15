/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.reminders.ui

import androidx.lifecycle.ViewModel
import com.samco.trackandgraph.data.database.dto.IntervalPeriodPair
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderInput
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

enum class OneTimeStartType { DATE_TIME, AFTER_DELAY }

interface OneTimeReminderConfigurationViewModel {
    val reminderName: StateFlow<String>
    val enabled: StateFlow<Boolean>
    val startType: StateFlow<OneTimeStartType>
    val starts: StateFlow<OffsetDateTime>
    val delayInterval: StateFlow<String>
    val delayPeriod: StateFlow<Period>
    val hasRepeatInterval: StateFlow<Boolean>
    val repeatInterval: StateFlow<String>
    val repeatPeriod: StateFlow<Period>

    fun initializeFromReminder(reminder: Reminder?, params: ReminderParams.OneTimeParams?)
    fun updateReminderName(name: String)
    fun updateEnabled(enabled: Boolean)
    fun updateStartType(startType: OneTimeStartType)
    fun updateStarts(starts: OffsetDateTime)
    fun updateDelayInterval(interval: String)
    fun updateDelayPeriod(period: Period)
    fun updateHasRepeatInterval(hasRepeatInterval: Boolean)
    fun updateRepeatInterval(interval: String)
    fun updateRepeatPeriod(period: Period)
    fun getReminderInput(): ReminderInput
}

@HiltViewModel
class OneTimeReminderConfigurationViewModelImpl @Inject constructor(
    private val timeProvider: TimeProvider,
) : ViewModel(), OneTimeReminderConfigurationViewModel {
    private val _reminderName = MutableStateFlow("")
    override val reminderName = _reminderName.asStateFlow()

    private val _enabled = MutableStateFlow(true)
    override val enabled = _enabled.asStateFlow()

    private val _startType = MutableStateFlow(OneTimeStartType.DATE_TIME)
    override val startType = _startType.asStateFlow()

    private val _starts = MutableStateFlow(now().plusHours(1).toOffsetDateTime())
    override val starts = _starts.asStateFlow()

    private val _delayInterval = MutableStateFlow("1")
    override val delayInterval = _delayInterval.asStateFlow()

    private val _delayPeriod = MutableStateFlow(Period.HOURS)
    override val delayPeriod = _delayPeriod.asStateFlow()

    private val _hasRepeatInterval = MutableStateFlow(false)
    override val hasRepeatInterval = _hasRepeatInterval.asStateFlow()

    private val _repeatInterval = MutableStateFlow("1")
    override val repeatInterval = _repeatInterval.asStateFlow()

    private val _repeatPeriod = MutableStateFlow(Period.DAYS)
    override val repeatPeriod = _repeatPeriod.asStateFlow()

    override fun initializeFromReminder(
        reminder: Reminder?,
        params: ReminderParams.OneTimeParams?,
    ) {
        if (reminder != null) _reminderName.value = reminder.reminderName
        if (params == null) return

        _enabled.value = params.enabled
        _starts.value = params.starts.atZone(timeProvider.defaultZone()).toOffsetDateTime()
        _startType.value = if (params.initialDelay == null) {
            OneTimeStartType.DATE_TIME
        } else {
            OneTimeStartType.AFTER_DELAY
        }
        params.initialDelay?.let {
            _delayInterval.value = it.interval.toString()
            _delayPeriod.value = it.period
        }
        _hasRepeatInterval.value = params.repeatInterval != null
        params.repeatInterval?.let {
            _repeatInterval.value = it.interval.toString()
            _repeatPeriod.value = it.period
        }
    }

    override fun updateReminderName(name: String) { _reminderName.value = name }
    override fun updateEnabled(enabled: Boolean) { _enabled.value = enabled }
    override fun updateStartType(startType: OneTimeStartType) { _startType.value = startType }
    override fun updateStarts(starts: OffsetDateTime) { _starts.value = starts }
    override fun updateDelayInterval(interval: String) { _delayInterval.value = interval }
    override fun updateDelayPeriod(period: Period) { _delayPeriod.value = period }
    override fun updateHasRepeatInterval(hasRepeatInterval: Boolean) {
        _hasRepeatInterval.value = hasRepeatInterval
    }
    override fun updateRepeatInterval(interval: String) { _repeatInterval.value = interval }
    override fun updateRepeatPeriod(period: Period) { _repeatPeriod.value = period }

    override fun getReminderInput(): ReminderInput {
        val createdAt = now()
        val initialDelay = if (_startType.value == OneTimeStartType.AFTER_DELAY) {
            IntervalPeriodPair(parseInterval(_delayInterval.value), _delayPeriod.value)
        } else {
            null
        }
        val starts = initialDelay?.let { createdAt.plus(it) }
            ?: _starts.value.atZoneSameInstant(timeProvider.defaultZone()).toLocalDateTime()
        val repeatInterval = if (_hasRepeatInterval.value) {
            IntervalPeriodPair(parseInterval(_repeatInterval.value), _repeatPeriod.value)
        } else {
            null
        }

        return ReminderInput(
            reminderName = _reminderName.value,
            featureId = null,
            params = ReminderParams.OneTimeParams(
                createdAt = createdAt.toLocalDateTime(),
                starts = starts,
                initialDelay = initialDelay,
                repeatInterval = repeatInterval,
                enabled = _enabled.value,
            ),
        )
    }

    private fun now(): ZonedDateTime = timeProvider.now().withZoneSameInstant(timeProvider.defaultZone())

    private fun parseInterval(value: String): Int =
        (value.toDoubleOrNull()?.toInt() ?: 1).coerceAtLeast(1)

    private fun ZonedDateTime.plus(interval: IntervalPeriodPair): LocalDateTime =
        when (interval.period) {
            Period.MINUTES -> plusMinutes(interval.interval.toLong())
            Period.HOURS -> plusHours(interval.interval.toLong())
            Period.DAYS -> plusDays(interval.interval.toLong())
            Period.WEEKS -> plusWeeks(interval.interval.toLong())
            Period.MONTHS -> plusMonths(interval.interval.toLong())
            Period.YEARS -> plusYears(interval.interval.toLong())
        }.toLocalDateTime()
}
