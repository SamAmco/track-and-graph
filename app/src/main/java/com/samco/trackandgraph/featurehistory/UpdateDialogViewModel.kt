package com.samco.trackandgraph.featurehistory

import androidx.lifecycle.*
import com.samco.trackandgraph.ui.compose.viewmodels.DurationInputViewModel
import com.samco.trackandgraph.ui.compose.viewmodels.DurationInputViewModelImpl
import com.samco.trackandgraph.ui.compose.viewmodels.asValidatedDouble
import kotlinx.coroutines.flow.combine

interface UpdateDialogViewModel {
    val isUpdating: LiveData<Boolean>

    val isDuration: LiveData<Boolean>

    val whereValueEnabled: LiveData<Boolean>
    val whereLabelEnabled: LiveData<Boolean>
    val whereValue: LiveData<String>
    val whereDurationViewModel: DurationInputViewModel
    val whereLabel: LiveData<String>

    val toValueEnabled: LiveData<Boolean>
    val toLabelEnabled: LiveData<Boolean>
    val toValue: LiveData<String>
    val toDurationViewModel: DurationInputViewModel
    val toLabel: LiveData<String>

    fun setWhereValue(value: String)
    fun setToValue(value: String)
    fun setWhereLabel(label: String)
    fun setToLabel(label: String)
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
    override val whereValue = MutableLiveData("1.0")
    override val whereLabel = MutableLiveData("")
    override val toValue = MutableLiveData("1.0")
    override val toLabel = MutableLiveData("")

    override val whereDurationViewModel = DurationInputViewModelImpl()
    override val toDurationViewModel = DurationInputViewModelImpl()


    override fun setWhereValue(value: String) {
        whereValue.value = value.asValidatedDouble()
    }

    override fun setToValue(value: String) {
        toValue.value = value.asValidatedDouble()
    }

    override fun setWhereLabel(label: String) {
        whereLabel.value = label
    }

    override fun setToLabel(label: String) {
        toLabel.value = label
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
        else -> toLabel.value
    }

    protected fun getToValueDouble() = when {
        toValueEnabled.value != true -> null
        isDuration.value == true -> toDurationViewModel.getDurationAsDouble()
        else -> toValue.value?.toDouble()
    }

    protected fun getWhereLabelString() = when {
        whereLabelEnabled.value != true -> null
        else -> whereLabel.value
    }

    protected fun getWhereValueDouble() = when {
        whereValueEnabled.value != true -> null
        isDuration.value == true -> whereDurationViewModel.getDurationAsDouble()
        else -> whereValue.value?.toDouble()
    }
}