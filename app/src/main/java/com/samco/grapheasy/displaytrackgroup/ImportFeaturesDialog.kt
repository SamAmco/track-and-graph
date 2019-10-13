package com.samco.grapheasy.displaytrackgroup

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.samco.grapheasy.R
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.GraphEasyDatabase
import com.samco.grapheasy.util.CSVReadWriter
import com.samco.grapheasy.util.ImportExportFeatureUtils
import kotlinx.coroutines.*
import org.threeten.bp.OffsetDateTime
import timber.log.Timber
import java.lang.Exception

const val OPEN_FILE_REQUEST_CODE = 124

class ImportFeaturesDialog : DialogFragment() {
    private var trackGroupName: String? = null
    private var trackGroupId: Long? = null

    private var updateJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + updateJob)

    private lateinit var listener: ImportFeaturesDialogListener
    private lateinit var viewModel: ImportFeaturesViewModel
    private lateinit var alertDialog: AlertDialog
    private lateinit var fileButton: Button
    private lateinit var progressBar: ProgressBar

    interface ImportFeaturesDialogListener { fun getViewModel(): ImportFeaturesViewModel }
    interface ImportFeaturesViewModel { var selectedFileUri: Uri? }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            listener = parentFragment as ImportFeaturesDialogListener
            viewModel = listener.getViewModel()
            val view = it.layoutInflater.inflate(R.layout.import_features_dialog, null)
            trackGroupName = arguments!!.getString(TRACK_GROUP_NAME_KEY)
            trackGroupId = arguments!!.getLong(TRACK_GROUP_ID_KEY)

            fileButton = view.findViewById(R.id.fileButton)
            progressBar = view.findViewById(R.id.progressBar)

            progressBar.visibility = View.INVISIBLE
            fileButton.setOnClickListener { onFileButtonClicked() }
            fileButton.text = getString(R.string.select_file)
            fileButton.setTextColor(ContextCompat.getColor(context!!, R.color.errorText))

            val builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setPositiveButton(R.string.importButton) { _, _ -> null }
                .setNegativeButton(R.string.cancel) { _, _ -> onDone() }
            alertDialog = builder.create()
            alertDialog.setCanceledOnTouchOutside(true)
            alertDialog.setOnShowListener {
                val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.isEnabled = false
                positiveButton.setOnClickListener { onImportClicked() }
                if (viewModel.selectedFileUri != null) setFileButtonTextFromUri(viewModel.selectedFileUri!!)
            }
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun onFileButtonClicked() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
        }
        startActivityForResult(intent, OPEN_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == OPEN_FILE_REQUEST_CODE) {
            resultData?.data.also { uri ->
                if (uri != null) {
                    viewModel.selectedFileUri = uri
                    setFileButtonTextFromUri(uri)
                }
            }
        }
    }

    private fun setFileButtonTextFromUri(uri: Uri) {
        ImportExportFeatureUtils.setFileButtonTextFromUri(activity, context!!, uri, fileButton, alertDialog)
    }

    private fun onImportClicked() {
        progressBar.visibility = View.VISIBLE
        viewModel.selectedFileUri?.let {
            uiScope.launch {
                var exception: CSVReadWriter.ImportFeaturesException? = null
                withContext(Dispatchers.IO) {
                    try {
                        val inputStream = activity!!.contentResolver.openInputStream(it)
                        if (inputStream != null) {
                            val application = requireActivity().application
                            val database = GraphEasyDatabase.getInstance(application)
                            val dao = database.graphEasyDatabaseDao
                            database.runInTransaction {
                                CSVReadWriter.readFeaturesFromCSV(dao, inputStream, trackGroupId!!,
                                    getString(R.string.standard_name_allowed_digits))
                            }
                        }
                    } catch (e: CSVReadWriter.ImportFeaturesException) { exception = e }
                }
                withContext(Dispatchers.Main) {
                    if (exception != null) {
                        val message =
                            if (exception!!.stringArgs == null) getString(exception!!.stringId)
                            else getString(exception!!.stringId, exception!!.stringArgs!!)
                        Toast.makeText(activity!!, message, Toast.LENGTH_LONG).show()
                    }
                    onDone()
                }
            }
        }

    }

    private fun onDone() {
        viewModel.selectedFileUri = null
        updateJob.cancel()
        dismiss()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onDone()
    }
}