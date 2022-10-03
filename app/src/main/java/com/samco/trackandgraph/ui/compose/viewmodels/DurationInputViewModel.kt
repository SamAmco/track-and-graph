package com.samco.trackandgraph.ui.compose.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

interface DurationInputViewModel {
    val hours: LiveData<String>
    val minutes: LiveData<String>
    val seconds: LiveData<String>

    fun setHours(value: String)
    fun setMinutes(value: String)
    fun setSeconds(value: String)
    fun getDurationAsDouble(): Double
}

class DurationInputViewModelImpl : DurationInputViewModel {
    override val hours = MutableLiveData("")
    override val minutes = MutableLiveData("")
    override val seconds = MutableLiveData("")

    private fun String.validated() =
        this.take(1).filter { it.isDigit() || it == '-' } + this.drop(1).filter { it.isDigit() }

    private fun LiveData<String>.getDouble() = ((hours.value ?: "").toDoubleOrNull() ?: 0.0)

    override fun setHours(value: String) {
        hours.value = value.validated()
    }

    override fun setMinutes(value: String) {
        minutes.value = value.validated()
    }

    override fun setSeconds(value: String) {
        seconds.value = value.validated()
    }

    override fun getDurationAsDouble(): Double {
        return (hours.getDouble() * 3600.0) + (minutes.getDouble() * 60.0) + (seconds.getDouble())
    }
}