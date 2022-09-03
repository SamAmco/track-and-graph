package com.samco.trackandgraph.functions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.base.database.dto.FunctionDto
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

interface AddFunctionViewModel {
    fun initViewModel(groupId: Long, functionId: Long)

    val functionName: LiveData<String>
    val functionDescription: LiveData<String>
    val functionScript: LiveData<String>
    val isLoading: LiveData<Boolean>
    val finished: LiveData<Boolean>
    val isUpdateMode: LiveData<Boolean>
    val createUpdateButtonEnabled: LiveData<Boolean>
    //TODO we need an error text

    fun setFunctionName(name: String)
    fun setFunctionDescription(description: String)
    fun setFunctionScript(body: String)
    fun onCreateOrUpdateClicked()
}

@HiltViewModel
class AddFunctionViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel(), AddFunctionViewModel {

    override val functionName = MutableLiveData("")
    override val functionDescription = MutableLiveData("")
    override val functionScript = MutableLiveData("")
    override val isLoading = MutableLiveData(false)
    override val finished = MutableLiveData(false)
    override val isUpdateMode = MutableLiveData(false)

    //TODO validate when update button should be enabled
    override val createUpdateButtonEnabled = MutableLiveData(true)

    private var groupId: Long? = null
    private var functionId: Long? = null

    override fun initViewModel(groupId: Long, functionId: Long) {
        if (this.groupId != null) return
        this.groupId = groupId
        if (functionId != -1L) viewModelScope.launch { readExistingFunction(functionId) }
    }

    private suspend fun readExistingFunction(functionId: Long) = withContext(io) {
        withContext(ui) { isLoading.value = true }
        val existingFunction = dataInteractor.getFunctionById(functionId)
        if (existingFunction != null) {
            this@AddFunctionViewModelImpl.functionId = functionId
            withContext(ui) {
                isUpdateMode.value = true
                functionName.value = existingFunction.name
                functionDescription.value = existingFunction.description
                functionScript.value = existingFunction.script
                //TODO dataSources: List<DataSourceDescriptor>,
            }
        }
        withContext(ui) { isLoading.value = false }
    }

    override fun setFunctionName(name: String) {
        functionName.value = name
    }

    override fun setFunctionDescription(description: String) {
        functionDescription.value = description
    }

    override fun setFunctionScript(body: String) {
        //TODO validate the script and disable/enable the add button
        functionScript.value = body
    }

    override fun onCreateOrUpdateClicked() {
        viewModelScope.launch(io) {
            withContext(ui) { isLoading.value = true }

            //TODO validate the input first
            if (functionId != null) dataInteractor.updateFunction(createDto())
            else dataInteractor.createFunction(createDto())

            withContext(ui) {
                isLoading.value = false
                finished.value = true
            }
        }
    }

    private fun createDto() = FunctionDto(
        functionId ?: 0L,
        functionName.value!!,
        groupId!!,
        functionDescription.value!!,
        emptyList(),//TODO implement data sources list
        functionScript.value!!
    )
}