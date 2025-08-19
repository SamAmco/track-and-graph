package com.samco.trackandgraph.featurehistory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.ui.viewmodels.DurationInputViewModel
import com.samco.trackandgraph.ui.viewmodels.DurationInputViewModelImpl
import com.samco.trackandgraph.ui.viewmodels.asValidatedDouble
import kotlinx.coroutines.flow.combine

interface UpdateDialogViewModel {
    val isUpdating: LiveData<Boolean>

    val isDuration: LiveData<Boolean>

    val whereValueEnabled: LiveData<Boolean>
    val whereLabelEnabled: LiveData<Boolean>
    val whereValue: TextFieldValue
    val whereDurationViewModel: DurationInputViewModel
    val whereLabel: TextFieldValue

    val toValueEnabled: LiveData<Boolean>
    val toLabelEnabled: LiveData<Boolean>
    val toValue: TextFieldValue
    val toDurationViewModel: DurationInputViewModel
    val toLabel: TextFieldValue

    fun setWhereTextValue(value: TextFieldValue)
    fun setToTextValue(value: TextFieldValue)
    fun setWhereTextLabel(label: TextFieldValue)
    fun setToTextLabel(label: TextFieldValue)
    fun setWhereValueEnabled(enabled: Boolean)
    fun setWhereLabelEnabled(enabled: Boolean)
    fun setToValueEnabled(enabled: Boolean)
    fun setToLabelEnabled(enabled: Boolean)

    val showUpdateWarning: LiveData<Boolean>

    val updateButtonEnabled: LiveData<Boolean>

    fun onUpdateClicked()
    fun onCancelUpdate()
    fun onConfirmUpdateWarning()
    fun onCancelUpdateWarning()
}

abstract class UpdateDialogViewModelImpl: ViewModel(), UpdateDialogViewModel {

    override val whereValueEnabled = MutableLiveData(false)
    override val whereLabelEnabled = MutableLiveData(false)
    override val toValueEnabled = MutableLiveData(false)
    override val toLabelEnabled = MutableLiveData(false)
    override var whereValue by mutableStateOf(TextFieldValue("1.0", TextRange(3)))
    override var whereLabel by mutableStateOf(TextFieldValue("", TextRange(0)))
    override var toValue by mutableStateOf(TextFieldValue("1.0", TextRange(3)))
    override var toLabel by mutableStateOf(TextFieldValue("", TextRange(0)))

    override val whereDurationViewModel = DurationInputViewModelImpl()
    override val toDurationViewModel = DurationInputViewModelImpl()

    override fun setWhereTextValue(value: TextFieldValue) {
        whereValue = value.copy(text = value.text.asValidatedDouble())
    }

    override fun setToTextValue(value: TextFieldValue) {
        toValue = value.copy(text = value.text.asValidatedDouble())
    }

    override fun setWhereTextLabel(label: TextFieldValue) {
        whereLabel = label
    }

    override fun setToTextLabel(label: TextFieldValue) {
        toLabel = label
    }

    override fun setWhereValueEnabled(enabled: Boolean) {
        whereValueEnabled.value = enabled
    }

    override fun setWhereLabelEnabled(enabled: Boolean) {
        whereLabelEnabled.value = enabled
    }

    override fun setToValueEnabled(enabled: Boolean) {
        toValueEnabled.value = enabled
    }

    override fun setToLabelEnabled(enabled: Boolean) {
        toLabelEnabled.value = enabled
    }

    override val showUpdateWarning = MutableLiveData(false)

    override val updateButtonEnabled by lazy {
        combine(
            whereValueEnabled.asFlow(),
            whereLabelEnabled.asFlow(),
            toValueEnabled.asFlow(),
            toLabelEnabled.asFlow()
        ) { whereVal, whereLabel, toVal, toLabel ->
            (whereVal || whereLabel) && (toVal || toLabel)
        }.asLiveData(viewModelScope.coroutineContext)
    }

    override fun onUpdateClicked() {
        showUpdateWarning.value = true
    }

    override fun onCancelUpdateWarning() {
        showUpdateWarning.value = false
    }

    protected fun getToLabelString() = when {
        toLabelEnabled.value != true -> null
        else -> toLabel.text
    }

    protected fun getToValueDouble() = when {
        toValueEnabled.value != true -> null
        isDuration.value == true -> toDurationViewModel.getDurationAsDouble()
        else -> toValue.text.toDoubleOrNull()
    }

    protected fun getWhereLabelString() = when {
        whereLabelEnabled.value != true -> null
        else -> whereLabel.text
    }

    protected fun getWhereValueDouble() = when {
        whereValueEnabled.value != true -> null
        isDuration.value == true -> whereDurationViewModel.getDurationAsDouble()
        else -> whereValue.text.toDoubleOrNull()
    }
}