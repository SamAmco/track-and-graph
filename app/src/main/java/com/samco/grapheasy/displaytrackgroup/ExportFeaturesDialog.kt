package com.samco.grapheasy.displaytrackgroup

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
import com.samco.grapheasy.R
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.GraphEasyDatabase
import com.samco.grapheasy.util.CSVReadWriter
import kotlinx.coroutines.*
import org.threeten.bp.OffsetDateTime

const val TRACK_GROUP_NAME_KEY = "TRACK_GROUP_NAME_KEY"
const val TRACK_GROUP_ID_KEY = "TRACK_GROUP_ID_KEY"
const val CREATE_FILE_REQUEST_CODE = 123

class ExportFeaturesDialog : DialogFragment() {

    private var trackGroupName: String? = null
    private var trackGroupId: Long? = null

    private lateinit var features: List<Feature>
    private lateinit var listener: ExportFeaturesDialogListener
    private lateinit var viewModel: ExportFeaturesViewModel

    private var updateJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + updateJob)

    private lateinit var alertDialog: AlertDialog
    private lateinit var fileButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var checkboxLayout: LinearLayout

    private var selectedFileUri: Uri? = null

    interface ExportFeaturesDialogListener {
        fun getViewModel(): ExportFeaturesViewModel
    }

    interface ExportFeaturesViewModel {
        var selectedFeatures: MutableList<Feature>?
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            listener = parentFragment as ExportFeaturesDialogListener
            viewModel = listener.getViewModel()
            val view = it.layoutInflater.inflate(R.layout.export_features_dialog, null)
            trackGroupName = arguments!!.getString(TRACK_GROUP_NAME_KEY)
            trackGroupId = arguments!!.getLong(TRACK_GROUP_ID_KEY)

            fileButton = view.findViewById(R.id.fileButton)
            progressBar = view.findViewById(R.id.progressBar)
            checkboxLayout = view.findViewById(R.id.checkboxLayout)

            progressBar.visibility = View.INVISIBLE
            fileButton.setOnClickListener { onFileButtonClicked() }
            createCheckboxes()
            fileButton.text = getString(R.string.select_file)
            fileButton.setTextColor(ContextCompat.getColor(context!!, R.color.errorText))

            var builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setPositiveButton(R.string.exportButton) { _, _ -> null }
                .setNegativeButton(R.string.cancel) { _, _ -> onCancel() }
            alertDialog = builder.create()
            alertDialog.setCanceledOnTouchOutside(true)
            alertDialog.setOnShowListener {
                val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.isEnabled = false
                positiveButton.setOnClickListener { onExportClicked() }
            }
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun initSelectedFeatures() {
        if (viewModel.selectedFeatures == null)
            viewModel.selectedFeatures = features.toMutableList()
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
                    selectedFileUri = uri
                    setFileButtonTextFromUri(uri)
                }
            }
        }
    }

    private fun setFileButtonTextFromUri(uri: Uri) {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        val cursor = activity?.contentResolver?.query(uri, projection, null, null, null)
        val index = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor?.moveToFirst()
        if (cursor != null && index != null) {
            fileButton.text = cursor.getString(index)
            fileButton.setTextColor(ContextCompat.getColor(context!!, R.color.regularText))
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
        }
    }

    private fun createCheckboxes() {
        val application = requireActivity().application
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        progressBar.visibility = View.VISIBLE
        uiScope.launch {
            withContext(Dispatchers.IO) {
                features = dao.getFeaturesForTrackGroupSync(trackGroupId!!)
                initSelectedFeatures()
            }
            withContext(Dispatchers.Main) {
                createFeatureCheckboxes()
                progressBar.visibility = View.INVISIBLE
            }
        }
    }

    private fun createFeatureCheckboxes() {
        for (feature in features) {
            val item = layoutInflater.inflate(R.layout.list_item_feature_checkbox, checkboxLayout, false)
            val checkBox = item.findViewById<CheckBox>(R.id.checkbox)
            checkBox.text = feature.name
            checkBox.isChecked = viewModel.selectedFeatures!!.contains(feature)
            checkBox.setOnCheckedChangeListener { _, b ->
                if (b && !viewModel.selectedFeatures!!.contains(feature)) viewModel.selectedFeatures!!.add(feature)
                else if (!b && viewModel.selectedFeatures!!.contains(feature)) viewModel.selectedFeatures!!.remove(feature)
            }
            checkboxLayout.addView(item)
        }
    }

    private fun onExportClicked() {
        progressBar.visibility = View.VISIBLE
        selectedFileUri?.let {
            uiScope.launch { withContext(Dispatchers.IO) {
                val outStream = activity!!.contentResolver.openOutputStream(it)
                if (outStream != null) {
                    val application = requireActivity().application
                    val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
                    CSVReadWriter.writeFeaturesToCSV(viewModel.selectedFeatures!!, dao, outStream)
                }
                viewModel.selectedFeatures = null
                dismiss()
                updateJob.cancel()
            } }
        }
    }

    private fun onCancel() {
        viewModel.selectedFeatures = null
        updateJob.cancel()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancel()
    }
}
