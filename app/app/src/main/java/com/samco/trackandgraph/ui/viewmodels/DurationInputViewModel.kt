package com.samco.trackandgraph.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.util.getDoubleFromTextOrNull
import org.threeten.bp.Duration

interface DurationInputViewModel {
    val hours: TextFieldValue
    val minutes: TextFieldValue
    val seconds: TextFieldValue

    fun setHoursText(value: TextFieldValue)
    fun setMinutesText(value: TextFieldValue)
    fun setSecondsText(value: TextFieldValue)

    fun setDurationFromDouble(value: Double)
    fun getDurationAsDouble(): Double
}

open class DurationInputViewModelImpl : DurationInputViewModel {
    override var hours by mutableStateOf(TextFieldValue(""))
    override var minutes by mutableStateOf(TextFieldValue(""))
    override var seconds by mutableStateOf(TextFieldValue(""))

    private var onChange: ((Double) -> Unit)? = null

    private fun TextFieldValue.getDouble() = getDoubleFromTextOrNull(this.text ?: "") ?: 0.0

    fun setOnChangeListener(onChange: (Double) -> Unit) {
        this.onChange = onChange
    }

    override fun setHoursText(value: TextFieldValue) {
        hours = value.copy(text = value.text.asValidatedInt())
        this.onChange?.invoke(getDurationAsDouble())
    }

    override fun setMinutesText(value: TextFieldValue) {
        minutes = value.copy(text = value.text.asValidatedInt())
        this.onChange?.invoke(getDurationAsDouble())
    }

    override fun setSecondsText(value: TextFieldValue) {
        seconds = value.copy(text = value.text.asValidatedDouble())
        this.onChange?.invoke(getDurationAsDouble())
    }

    override fun setDurationFromDouble(value: Double) {
        val duration = Duration.ofSeconds(value.toLong())
        val numHours = duration.toHours()
        val numMinutes = duration.minusHours(numHours).toMinutes()
        val numSeconds = duration.minusHours(numHours).minusMinutes(numMinutes).seconds

        val hrsString = numHours.toString()
        val minString = numMinutes.toString()
        val secString = numSeconds.toString()

        hours = TextFieldValue(numHours.toString(), TextRange(hrsString.length))
        minutes = TextFieldValue(numMinutes.toString(), TextRange(minString.length))
        seconds = TextFieldValue(numSeconds.toString(), TextRange(secString.length))
        this.onChange?.invoke(getDurationAsDouble())
    }

    override fun getDurationAsDouble(): Double {
        return (hours.getDouble() * 3600.0) + (minutes.getDouble() * 60.0) + (seconds.getDouble())
    }
}