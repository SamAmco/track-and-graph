package com.samco.grapheasy.displaytrackgroup

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.samco.grapheasy.R
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.GraphEasyDatabase
import com.samco.grapheasy.util.CSVReadWriter
import com.samco.grapheasy.util.ImportExportFeatureUtils
import kotlinx.coroutines.*
import org.threeten.bp.OffsetDateTime

const val CREATE_FILE_REQUEST_CODE = 123

enum class ExportState { LOADING, WAITING, EXPORTING, DONE }
class ExportFeaturesDialog : DialogFragment() {

    private var trackGroupName: String? = null
    private var trackGroupId: Long? = null

    private lateinit var viewModel: ExportFeaturesViewModel

    private lateinit var alertDialog: AlertDialog
    private lateinit var fileButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var checkboxLayout: LinearLayout
    private lateinit var positiveButton: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            viewModel = ViewModelProviders.of(this).get(ExportFeaturesViewModel::class.java)
            val view = it.layoutInflater.inflate(R.layout.export_features_dialog, null)
            trackGroupName = arguments!!.getString(TRACK_GROUP_NAME_KEY)
            trackGroupId = arguments!!.getLong(TRACK_GROUP_ID_KEY)

            fileButton = view.findViewById(R.id.fileButton)
            progressBar = view.findViewById(R.id.progressBar)
            checkboxLayout = view.findViewById(R.id.checkboxLayout)

            fileButton.setOnClickListener { onFileButtonClicked() }
            fileButton.text = getString(R.string.select_file)
            fileButton.setTextColor(ContextCompat.getColor(context!!, R.color.errorText))

            val builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setPositiveButton(R.string.exportButton) { _, _ -> null }
                .setNegativeButton(R.string.cancel) { _, _ -> null }
            alertDialog = builder.create()
            alertDialog.setCanceledOnTouchOutside(true)
            alertDialog.setOnShowListener { setAlertDialogShowListeners() }
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun setAlertDialogShowListeners() {
        positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        viewModel.loadFeatures(activity!!, trackGroupId!!)
        listenToState()
        setUriListeners()
        listenToFeatures()
        positiveButton.setOnClickListener { viewModel.beginExport(activity!!) }
    }

    private fun listenToState() {
        viewModel.exportState.observe(this, Observer { state ->
            when (state) {
                ExportState.LOADING -> {
                    progressBar.visibility = View.VISIBLE
                    positiveButton.isEnabled = false
                }
                ExportState.WAITING -> {
                    progressBar.visibility = View.INVISIBLE
                    positiveButton.isEnabled = true
                }
                ExportState.EXPORTING -> {
                    progressBar.visibility = View.VISIBLE
                    positiveButton.isEnabled = false
                }
                ExportState.DONE -> {
                    dismiss()
                }
            }
        })
    }

    private fun setUriListeners() {
        viewModel.selectedFileUri.observe(this, Observer { uri ->
            if (uri != null) {
                ImportExportFeatureUtils.setFileButtonTextFromUri(activity, context!!, uri, fileButton, alertDialog)
            }
        })
    }

    private fun listenToFeatures() {
        viewModel.featuresLoaded.observe(this, Observer { loaded ->
            if (loaded) {
                createFeatureCheckboxes()
            }
        })
    }

    private fun onFileButtonClicked() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            val now = OffsetDateTime.now()
            val generatedName = getString( R.string.export_file_name_suffix,
                getString(R.string.app_name), trackGroupName, now.year, now.monthValue + 1,
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

    fun loadFeatures(activity: Activity, trackGroupId: Long) {
        if (_featuresLoaded.value == false) {
            uiScope.launch {
                _exportState.value = ExportState.LOADING
                val application = activity.application
                val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
                withContext(Dispatchers.IO) {
                    features = dao.getFeaturesForTrackGroupSync(trackGroupId).toMutableList()
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
                        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
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















