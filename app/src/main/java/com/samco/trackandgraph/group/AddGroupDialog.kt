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

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.entity.Group
import com.samco.trackandgraph.util.getColorFromAttr
import com.samco.trackandgraph.util.showKeyboard
import kotlinx.coroutines.*

const val ADD_GROUP_DIALOG_ID_KEY = "ADD_GROUP_DIALOG_ID_KEY"
const val ADD_GROUP_DIALOG_PARENT_ID_KEY = "ADD_GROUP_DIALOG_PARENT_ID_KEY"

class AddGroupDialog : DialogFragment(), TextWatcher {
    private lateinit var alertDialog: AlertDialog

    private lateinit var editText: EditText

    private val viewModel by viewModels<AddGroupDialogViewModel>()


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var groupId: Long? = arguments?.getLong(ADD_GROUP_DIALOG_ID_KEY, -1) ?: -1
        groupId?.let { if (it < 0L) groupId = null }
        val parentGroupId = arguments?.getLong(ADD_GROUP_DIALOG_PARENT_ID_KEY) ?: 0

        val dialog = initDialog(groupId != null)
        val dao = TrackAndGraphDatabase
            .getInstance(requireActivity().application)
            .trackAndGraphDatabaseDao
        viewModel.initViewModel(dao, groupId, parentGroupId)
        observeViewModel()
        return dialog
    }

    override fun onStart() {
        super.onStart()
        requireContext().showKeyboard()
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            when (state) {
                AddGroupDialogViewModel.AddGroupDialogViewModelState.READY -> onViewModelReady()
                AddGroupDialogViewModel.AddGroupDialogViewModelState.DONE -> dismiss()
                else -> {
                }
            }
        }
    }

    private fun onViewModelReady() {
        viewModel.groupName.observe(this) { name ->
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = name.isNotEmpty()
            if (editText.text.toString() != name) {
                editText.setText(name)
                editText.setSelection(name.length)
            }
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
            val builder = AlertDialog.Builder(it, R.style.AppTheme_AlertDialogTheme)
            val positiveButtonText = if (updateMode) R.string.update else R.string.add
            builder.setView(view)
                .setPositiveButton(positiveButtonText) { _, _ -> onPositiveClicked() }
                .setNegativeButton(R.string.cancel) { _, _ -> run {} }
            alertDialog = builder.create()
            alertDialog.setCanceledOnTouchOutside(true)
            alertDialog.setOnShowListener {
                val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setTextColor(requireContext().getColorFromAttr(R.attr.colorSecondary))
                val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                negativeButton.setTextColor(requireContext().getColorFromAttr(R.attr.colorControlNormal))
            }
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
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

class AddGroupDialogViewModel : ViewModel() {
    enum class AddGroupDialogViewModelState { INIT, READY, DONE }

    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    private lateinit var dao: TrackAndGraphDatabaseDao

    private val _groupName = MutableLiveData("")
    val groupName: LiveData<String> get() = _groupName

    private val _colorIndex = MutableLiveData<Int>(0)
    val colorIndex: LiveData<Int> get() = _colorIndex

    private val _state = MutableLiveData(AddGroupDialogViewModelState.INIT)
    val state: LiveData<AddGroupDialogViewModelState> get() = _state

    private var groupId: Long? = null
    private var parentGroupId: Long = 0

    fun initViewModel(dao: TrackAndGraphDatabaseDao, groupId: Long?, parentGroupId: Long) {
        if (this::dao.isInitialized) return
        this.dao = dao
        this.groupId = groupId
        this.parentGroupId = parentGroupId

        ioScope.launch {
            if (groupId != null) {
                val group = dao.getGroupById(groupId)
                withContext(Dispatchers.Main) {
                    _colorIndex.value = group.colorIndex
                    _groupName.value = group.name
                }
            }
            withContext(Dispatchers.Main) { _state.value = AddGroupDialogViewModelState.READY }
        }
    }

    fun setName(name: String) = _groupName.postValue(name)

    fun addOrUpdateGroup() = ioScope.launch {
        val newName = _groupName.value
        val newColor = _colorIndex.value
        if (newName == null || newColor == null) return@launch
        val newId = groupId
        if (newId != null) {
            val group = dao.getGroupById(newId)
            dao.updateGroup(group.copy(name = newName, colorIndex = newColor))
        } else {
            dao.insertGroup(Group(0, newName, 0, parentGroupId, newColor))
        }
        withContext(Dispatchers.Main) { _state.value = AddGroupDialogViewModelState.DONE }
    }
}