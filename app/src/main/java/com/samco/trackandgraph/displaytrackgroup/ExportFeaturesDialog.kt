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
package com.samco.trackandgraph.displaytrackgroup

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.entity.Feature
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import com.samco.trackandgraph.util.CSVReadWriter
import com.samco.trackandgraph.util.ImportExportFeatureUtils
import com.samco.trackandgraph.util.getColorFromAttr
import kotlinx.coroutines.*
import org.threeten.bp.OffsetDateTime

const val GROUP_ID_KEY = "GROUP_ID_KEY"
const val GROUP_NAME_KEY = "GROUP_NAME_KEY"
const val CREATE_FILE_REQUEST_CODE = 123

enum class ExportState { LOADING, WAITING, EXPORTING, DONE }
class ExportFeaturesDialog : DialogFragment() {

    private var groupName: String? = null
    private var groupId: Long? = null

    private val viewModel by viewModels<ExportFeaturesViewModel>()

    private lateinit var alertDialog: AlertDialog
    private lateinit var fileButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var checkboxLayout: LinearLayout
    private lateinit var positiveButton: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val view = it.layoutInflater.inflate(R.layout.export_features_dialog, null)
            groupName = requireArguments().getString(GROUP_NAME_KEY)
            groupId = requireArguments().getLong(GROUP_ID_KEY)

            fileButton = view.findViewById(R.id.fileButton)
            progressBar = view.findViewById(R.id.progressBar)
            checkboxLayout = view.findViewById(R.id.checkboxLayout)

            fileButton.setOnClickListener { onFileButtonClicked() }
            fileButton.text = getString(R.string.select_file)
            fileButton.setTextColor(fileButton.context.getColorFromAttr(R.attr.colorError))

            val builder = AlertDialog.Builder(it, R.style.AppTheme_AlertDialogTheme)
            builder.setView(view)
                .setPositiveButton(R.string.exportButton) { _, _ -> run {} }
                .setNegativeButton(R.string.cancel) { _, _ -> run {} }
            alertDialog = builder.create()
            alertDialog.setCanceledOnTouchOutside(true)
            alertDialog.setOnShowListener { setAlertDialogShowListeners() }
            alertDialog.setOnShowListener {
                val negativeButton = alertDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                negativeButton.setTextColor(requireContext().getColorFromAttr(R.attr.colorControlNormal))
            }
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun setAlertDialogShowListeners() {
        positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setTextColor(positiveButton.context.getColorFromAttr(R.attr.colorSecondary))
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(alertDialog.context.getColorFromAttr(R.attr.colorControlNormal))
        positiveButton.isEnabled = false
        viewModel.loadFeatures(requireActivity(), groupId!!)
        listenToState()
        setUriListeners()
        listenToFeatures()
        positiveButton.setOnClickListener { viewModel.beginExport(requireActivity()) }
    }

    private fun listenToState() {
        viewModel.exportState.observe(this, Observer { state ->
            when (state) {
                ExportState.LOADING -> {
                    progressBar.visibility = View.VISIBLE
                    positiveButton.isEnabled = false
                }
                ExportState.WAITING -> { progressBar.visibility = View.INVISIBLE }
                ExportState.EXPORTING -> {
                    progressBar.visibility = View.VISIBLE
                    positiveButton.isEnabled = false
                }
                ExportState.DONE -> {
                    dismiss()
                }
                else -> {}
            }
        })
    }

    private fun setUriListeners() {
        viewModel.selectedFileUri.observe(this, Observer { uri ->
            if (uri != null) {
                ImportExportFeatureUtils.setFileButtonTextFromUri(activity, uri, fileButton, alertDialog)
            }
        })
    }

    private fun listenToFeatures() {
        viewModel.featuresLoaded.observe(this, Observer { loaded ->
            if (loaded) { createFeatureCheckboxes() }
        })
    }

    private fun onFileButtonClicked() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            val now = OffsetDateTime.now()
            val generatedName = getString( R.string.export_file_name_suffix,
                "TrackAndGraph", groupName, now.year, now.monthValue,
                now.dayOfMonth, now.hour, now.minute, now.second)
            putExtra(Intent.EXTRA_TITLE, generatedName)
            type = "text/csv"
        }
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == CREATE_FILE_REQUEST_CODE) {
            resultData?.data.also { uri ->
                if (uri != null) {
                    viewModel.selectedFileUri.value = uri
                }
            }
        }
    }

    private fun createFeatureCheckboxes() {
        for (feature in viewModel.features) {
            val item = layoutInflater.inflate(R.layout.list_item_feature_checkbox, checkboxLayout, false)
            val checkBox = item.findViewById<CheckBox>(R.id.checkbox)
            checkBox.text = feature.name
            checkBox.isChecked = viewModel.selectedFeatures.contains(feature)
            checkBox.setOnCheckedChangeListener { _, b ->
                if (b && !viewModel.selectedFeatures.contains(feature)) viewModel.selectedFeatures.add(feature)
                else if (!b && viewModel.selectedFeatures.contains(feature)) viewModel.selectedFeatures.remove(feature)
            }
            checkboxLayout.addView(item)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        dismiss()
    }
}


class ExportFeaturesViewModel : ViewModel() {
    private var updateJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + updateJob)

    lateinit var features: List<Feature>

    val exportState: LiveData<ExportState> get() { return _exportState }
    private val _exportState by lazy {
        val state = MutableLiveData<ExportState>()
        state.value = ExportState.WAITING
        return@lazy state
    }

    lateinit var selectedFeatures: MutableList<Feature>
    val selectedFileUri by lazy {
        val uri = MutableLiveData<Uri?>()
        uri.value = null
        return@lazy uri
    }

    val featuresLoaded: LiveData<Boolean> get() { return _featuresLoaded }
    private val _featuresLoaded by lazy {
        val loaded = MutableLiveData<Boolean>()
        loaded.value = false
        return@lazy loaded
    }

    fun loadFeatures(activity: Activity, groupId: Long) {
        if (_featuresLoaded.value == false) {
            uiScope.launch {
                _exportState.value = ExportState.LOADING
                val application = activity.application
                val dao = TrackAndGraphDatabase.getInstance(application).trackAndGraphDatabaseDao
                withContext(Dispatchers.IO) {
                    features = dao.getFeaturesForGroupSync(groupId).toMutableList()
                }
                selectedFeatures = features.toMutableList()
                _featuresLoaded.value = true
                _exportState.value = ExportState.WAITING
            }
        }
    }

    fun beginExport(activity: Activity) {
        selectedFileUri.value?.let {
            uiScope.launch {
                _exportState.value = ExportState.EXPORTING
                withContext(Dispatchers.IO) {
                    val outStream = activity.contentResolver.openOutputStream(it)
                    if (outStream != null) {
                        val application = activity.application
                        val dao = TrackAndGraphDatabase.getInstance(application).trackAndGraphDatabaseDao
                        CSVReadWriter.writeFeaturesToCSV(selectedFeatures, dao, outStream)
                    }
                }
                _exportState.value = ExportState.DONE
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        updateJob.cancel()
    }
}
