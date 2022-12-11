package com.samco.trackandgraph.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.threeten.bp.Duration

interface DurationInputViewModel {
    val hours: LiveData<String>
    val minutes: LiveData<String>
    val seconds: LiveData<String>

    fun setHours(value: String)
    fun setMinutes(value: String)
    fun setSeconds(value: String)
    fun setDurationFromDouble(value: Double)
    fun getDurationAsDouble(): Double
}

open class DurationInputViewModelImpl : DurationInputViewModel {
    override val hours = MutableLiveData("")
    override val minutes = MutableLiveData("")
    override val seconds = MutableLiveData("")

    private fun LiveData<String>.getDouble() = ((this.value ?: "").toDoubleOrNull() ?: 0.0)

    override fun setHours(value: String) {
        hours.value = value.asValidatedInt()
    }

    override fun setMinutes(value: String) {
        minutes.value = value.asValidatedInt()
    }

    override fun setSeconds(value: String) {
        seconds.value = value.asValidatedInt()
    }

    override fun setDurationFromDouble(value: Double) {
        val duration = Duration.ofSeconds(value.toLong())
        val numHours = duration.toHours()
        val numMinutes = duration.minusHours(numHours).toMinutes()
        val numSeconds = duration.minusHours(numHours).minusMinutes(numMinutes).seconds
        hours.value = numHours.toString()
        minutes.value = numMinutes.toString()
        seconds.value = numSeconds.toString()
    }

    override fun getDurationAsDouble(): Double {
        return (hours.getDouble() * 3600.0) + (minutes.getDouble() * 60.0) + (seconds.getDouble())
    }
}