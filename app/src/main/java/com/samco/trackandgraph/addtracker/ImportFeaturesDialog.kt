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
package com.samco.trackandgraph.addtracker

import android.app.Dialog
import android.content.ContentResolver
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.ImportFeaturesException
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.util.ImportExportFeatureUtils
import com.samco.trackandgraph.util.getColorFromAttr
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

const val OPEN_FILE_REQUEST_CODE = 124

enum class ImportState { WAITING, IMPORTING, DONE }

@AndroidEntryPoint
class ImportFeaturesDialog : DialogFragment() {
    private var trackGroupName: String? = null
    private var trackGroupId: Long? = null

    private val viewModel by viewModels<ImportFeaturesViewModel>()
    private lateinit var alertDialog: AlertDialog
    private lateinit var fileButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val view = it.layoutInflater.inflate(R.layout.import_features_dialog, null)
            trackGroupName = requireArguments().getString(GROUP_NAME_KEY)
            trackGroupId = requireArguments().getLong(GROUP_ID_KEY)

            fileButton = view.findViewById(R.id.fileButton)
            progressBar = view.findViewById(R.id.progressBar)

            progressBar.visibility = View.INVISIBLE
            fileButton.setOnClickListener { onFileButtonClicked() }
            fileButton.text = getString(R.string.select_file)
            fileButton.setTextColor(fileButton.context.getColorFromAttr(R.attr.colorError))

            val builder = MaterialAlertDialogBuilder(it, R.style.AppTheme_AlertDialogTheme)
            builder.setView(view)
                .setPositiveButton(R.string.importButton) { _, _ -> run {} }
                .setNegativeButton(R.string.cancel) { _, _ -> run {} }
            alertDialog = builder.create()
            alertDialog.setCanceledOnTouchOutside(true)
            alertDialog.setOnShowListener { setAlertDialogShowListeners() }
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun setAlertDialogShowListeners() {
        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.isEnabled = false
        positiveButton.setOnClickListener { onImportClicked() }
        listenToUri()
        listenToImportState()
        listenToException()
        val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        negativeButton.setOnClickListener { dismiss() }
        alertDialog.setOnCancelListener { run {} }
    }

    private fun listenToUri() {
        viewModel.selectedFileUri.observe(this, Observer { uri ->
            if (uri != null) {
                ImportExportFeatureUtils.setFileButtonTextFromUri(
                    activity,
                    uri,
                    fileButton,
                    alertDialog
                )
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        })
    }

    private fun listenToImportState() {
        viewModel.importState.observe(this, Observer { state ->
            when (state) {
                ImportState.WAITING -> {
                    progressBar.visibility = View.INVISIBLE
                }
                ImportState.IMPORTING -> {
                    progressBar.visibility = View.VISIBLE
                    val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    positiveButton.isEnabled = false
                    fileButton.isEnabled = false
                }
                ImportState.DONE -> dismiss()
                else -> {}
            }
        })
    }

    private fun listenToException() {
        viewModel.importException.observe(this, Observer { exception ->
            exception?.let {
                Toast.makeText(
                    requireActivity(),
                    getStringForImportException(it),
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun getStringForImportException(exception: ImportFeaturesException) = when (exception) {
        is ImportFeaturesException.Unknown ->
            getString(R.string.import_exception_unknown)
        is ImportFeaturesException.InconsistentDataType -> getString(
            R.string.import_exception_inconsistent_data_type,
            exception.lineNumber
        )
        is ImportFeaturesException.InconsistentRecord -> getString(
            R.string.import_exception_inconsistent_record,
            exception.lineNumber
        )
        is ImportFeaturesException.BadTimestamp -> getString(
            R.string.import_exception_bad_timestamp,
            exception.lineNumber
        )
        is ImportFeaturesException.BadHeaders -> getString(
            R.string.import_exception_bad_headers,
            exception.requiredHeaders
        )
    }

    private fun onFileButtonClicked() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/csv"
            putExtra(
                Intent.EXTRA_MIME_TYPES, arrayOf(
                    "text/csv",
                    "application/csv",
                    "application/comma-separated-values",
                    "text/comma-separated-values"
                )
            )
        }
        startActivityForResult(intent, OPEN_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == OPEN_FILE_REQUEST_CODE) {
            resultData?.data.also { uri ->
                if (uri != null) {
                    viewModel.selectedFileUri.value = uri
                }
            }
        }
    }

    private fun onImportClicked() {
        progressBar.visibility = View.VISIBLE
        viewModel.beginImport(trackGroupId!!)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (viewModel.importState.value != ImportState.IMPORTING) dismiss()
    }
}

@HiltViewModel
class ImportFeaturesViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val contentResolver: ContentResolver,
    @MainDispatcher private val ui: CoroutineDispatcher,
    @IODispatcher private val io: CoroutineDispatcher
) : ViewModel() {

    val selectedFileUri: MutableLiveData<Uri?> by lazy {
        val uri = MutableLiveData<Uri?>()
        uri.value = null
        return@lazy uri
    }

    val importState: LiveData<ImportState>
        get() {
            return _importState
        }
    private val _importState: MutableLiveData<ImportState> by lazy {
        val state = MutableLiveData<ImportState>()
        state.value = ImportState.WAITING
        return@lazy state
    }

    private val _importException: MutableLiveData<ImportFeaturesException?> by lazy {
        val exception = MutableLiveData<ImportFeaturesException?>()
        exception.value = null
        return@lazy exception
    }

    val importException: LiveData<ImportFeaturesException?>
        get() {
            return _importException
        }

    //TODO this should probably be scheduled to run in a service
    fun beginImport(trackGroupId: Long) {
        if (_importState.value == ImportState.IMPORTING) return
        val uri = selectedFileUri.value ?: return
        viewModelScope.launch(ui) {
            _importState.value = ImportState.IMPORTING
            doImport(uri, trackGroupId).exceptionOrNull()?.let {
                if (it is ImportFeaturesException) _importException.value = it
                else _importException.value = ImportFeaturesException.Unknown()
            }
            _importState.value = ImportState.DONE
        }
    }

    private suspend fun doImport(uri: Uri, trackGroupId: Long) = runCatching {
        withContext(io) {
            contentResolver.openInputStream(uri)?.let { inputStream ->
                dataInteractor.readFeaturesFromCSV(inputStream, trackGroupId)
            }
        }
    }
}
