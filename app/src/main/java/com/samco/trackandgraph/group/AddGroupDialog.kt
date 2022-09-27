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

package com.samco.trackandgraph.group

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.Group
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.ui.ColorSpinnerAdapter
import com.samco.trackandgraph.ui.dataVisColorList
import com.samco.trackandgraph.util.focusAndShowKeyboard
import com.samco.trackandgraph.util.getColorFromAttr
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

const val ADD_GROUP_DIALOG_ID_KEY = "ADD_GROUP_DIALOG_ID_KEY"
const val ADD_GROUP_DIALOG_PARENT_ID_KEY = "ADD_GROUP_DIALOG_PARENT_ID_KEY"

@AndroidEntryPoint
class AddGroupDialog : DialogFragment(), TextWatcher {
    private lateinit var alertDialog: AlertDialog
    private lateinit var editText: EditText
    private lateinit var colorSpinner: Spinner

    private val viewModel by viewModels<AddGroupDialogViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var groupId: Long? = arguments?.getLong(ADD_GROUP_DIALOG_ID_KEY, -1) ?: -1
        groupId?.let { if (it < 0L) groupId = null }
        val parentGroupId = arguments?.getLong(ADD_GROUP_DIALOG_PARENT_ID_KEY) ?: 0

        val dialog = initDialog(groupId != null)
        viewModel.initViewModel(groupId, parentGroupId)
        editText.focusAndShowKeyboard()
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                AddGroupDialogViewModel.AddGroupDialogViewModelState.READY -> onViewModelReady()
                AddGroupDialogViewModel.AddGroupDialogViewModelState.DONE -> dismiss()
                else -> {
                }
            }
        }
    }

    private fun onViewModelReady() {
        viewModel.groupName.observe(viewLifecycleOwner) { name ->
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = name.isNotEmpty()
            if (editText.text.toString() != name) {
                editText.setText(name)
                editText.setSelection(name.length)
            }
        }

        viewModel.colorIndex.observe(viewLifecycleOwner) { index ->
            if (colorSpinner.selectedItemPosition != index) colorSpinner.setSelection(index)
        }
    }

    private fun initDialog(updateMode: Boolean): Dialog {
        return activity?.let {
            val view = it.layoutInflater.inflate(R.layout.fragment_add_group_dialog, null)
            val promptText = view.findViewById<TextView>(R.id.prompt_text)
            promptText.setText(
                if (updateMode) R.string.edit_group
                else R.string.add_group
            )
            editText = view.findViewById(R.id.edit_name_input)
            editText.addTextChangedListener(this)
            val builder = MaterialAlertDialogBuilder(it, R.style.AppTheme_AlertDialogTheme)
            val positiveButtonText = if (updateMode) R.string.update else R.string.add
            setupColorSpinner(view)
            builder.setView(view)
                .setPositiveButton(positiveButtonText) { _, _ -> onPositiveClicked() }
                .setNegativeButton(R.string.cancel) { _, _ -> run {} }
            alertDialog = builder.create()
            alertDialog.setCanceledOnTouchOutside(true)
            alertDialog.setOnShowListener {
                val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                negativeButton.setTextColor(requireContext().getColorFromAttr(R.attr.colorControlNormal))
            }
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun setupColorSpinner(view: View) {
        colorSpinner = view.findViewById(R.id.colorSpinner)
        colorSpinner.adapter = ColorSpinnerAdapter(requireContext(), dataVisColorList)
        var skippedFirst = false
        colorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                if (skippedFirst) viewModel.setColorIndex(index)
                skippedFirst = true
            }
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(editable: Editable?) {
        editable?.let { viewModel.setName(it.toString()) }
    }

    private fun onPositiveClicked() {
        viewModel.addOrUpdateGroup()
    }
}

@HiltViewModel
class AddGroupDialogViewModel @Inject constructor(
    private val dataInteractor: DataInteractor
) : ViewModel() {
    enum class AddGroupDialogViewModelState { INIT, READY, DONE }

    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    private val _groupName = MutableLiveData("")
    val groupName: LiveData<String> get() = _groupName

    private val _colorIndex = MutableLiveData(0)
    val colorIndex: LiveData<Int> get() = _colorIndex

    private val _state = MutableLiveData(AddGroupDialogViewModelState.INIT)
    val state: LiveData<AddGroupDialogViewModelState> get() = _state

    private var groupId: Long? = null
    private var parentGroupId: Long = 0
    private var initialized: Boolean = false

    fun initViewModel(groupId: Long?, parentGroupId: Long) {
        if (initialized) return
        initialized = true

        this.groupId = groupId
        this.parentGroupId = parentGroupId

        ioScope.launch {
            if (groupId != null) {
                val group = dataInteractor.getGroupById(groupId)
                withContext(Dispatchers.Main) {
                    _colorIndex.value = group.colorIndex
                    _groupName.value = group.name
                }
            }
            withContext(Dispatchers.Main) { _state.value = AddGroupDialogViewModelState.READY }
        }
    }

    fun setName(name: String) = _groupName.postValue(name)

    fun setColorIndex(index: Int) = _colorIndex.postValue(index)

    fun addOrUpdateGroup() = ioScope.launch {
        val newName = _groupName.value
        val newColor = _colorIndex.value
        if (newName == null || newColor == null) return@launch
        val newId = groupId
        if (newId != null) {
            val group = dataInteractor.getGroupById(newId)
            dataInteractor.updateGroup(group.copy(name = newName, colorIndex = newColor))
        } else {
            dataInteractor.insertGroup(Group(0, newName, 0, parentGroupId, newColor))
        }
        withContext(Dispatchers.Main) { _state.value = AddGroupDialogViewModelState.DONE }
    }
}