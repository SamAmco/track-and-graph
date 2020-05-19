package com.samco.trackandgraph.backupandrestore

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.samco.trackandgraph.R
import com.samco.trackandgraph.databinding.BackupAndRestoreFragmentBinding
import kotlinx.coroutines.*
import org.threeten.bp.OffsetDateTime

const val EXPORT_DATABASE_REQUEST_CODE = 235
const val RESTORE_DATABASE_REQUEST_CODE = 578
const val SQLITE_MIME_TYPE = "application/vnd.sqlite3"

class BackupAndRestoreFragment : Fragment() {
    private lateinit var binding: BackupAndRestoreFragmentBinding
    private lateinit var viewModel: BackupAndRestoreViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BackupAndRestoreFragmentBinding.inflate(inflater)

        viewModel = ViewModelProviders.of(this).get(BackupAndRestoreViewModel::class.java)
        listenToViewModel()

        binding.backupButton.setOnClickListener { onBackupClicked() }
        binding.restoreButton.setOnClickListener { onRestoreClicked() }
        return binding.root
    }

    private fun listenToViewModel() {
        listenToInProgress()
        listenToRestoreResult()
        listenToBackupResult()
    }

    private fun listenToInProgress() {
        viewModel.inProgress.observe(this, Observer {
            binding.progressOverlay.visibility = if (it) View.VISIBLE else View.GONE
        })
    }

    private fun listenToRestoreResult() {
        viewModel.restoreResult.observe(this, Observer {
            when (it) {
                true -> {
                    val color = ContextCompat.getColor(requireContext(), R.color.successTextColor)
                    binding.restoreFeedbackText.setTextColor(color)
                    binding.restoreFeedbackText.text = getString(R.string.restore_successful)
                }
                false -> {
                    val color = ContextCompat.getColor(requireContext(), R.color.errorText)
                    binding.restoreFeedbackText.setTextColor(color)
                    binding.restoreFeedbackText.text =
                        getString(R.string.restore_failed, viewModel.errorText)
                }
            }
        })
    }

    private fun listenToBackupResult() {
        viewModel.backupResult.observe(this, Observer {
            when (it) {
                true -> {
                    val color = ContextCompat.getColor(requireContext(), R.color.successTextColor)
                    binding.backupFeedbackText.setTextColor(color)
                    binding.backupFeedbackText.text = getString(R.string.backup_successful)
                }
                false -> {
                    val color = ContextCompat.getColor(requireContext(), R.color.errorText)
                    binding.backupFeedbackText.setTextColor(color)
                    binding.backupFeedbackText.text =
                        getString(R.string.backup_failed, viewModel.errorText)
                }
            }
        })
    }

    private fun onBackupClicked() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            val now = OffsetDateTime.now()
            val generatedName = getString(
                R.string.backup_file_name_suffix,
                "TrackAndGraphBackup", now.year, now.monthValue + 1,
                now.dayOfMonth, now.hour, now.minute, now.second
            )
            putExtra(Intent.EXTRA_TITLE, generatedName)
            type = SQLITE_MIME_TYPE
        }
        startActivityForResult(intent, EXPORT_DATABASE_REQUEST_CODE)
    }

    private fun onRestoreClicked() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = SQLITE_MIME_TYPE
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/x-sqlite3"))
        }
        startActivityForResult(intent, RESTORE_DATABASE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        when (requestCode) {
            EXPORT_DATABASE_REQUEST_CODE -> viewModel.exportDatabase(resultData?.data)
            RESTORE_DATABASE_REQUEST_CODE -> viewModel.restoreDatabase(resultData?.data)
        }
    }
}

class BackupAndRestoreViewModel : ViewModel() {
    private val _restoreResult: MutableLiveData<Boolean?> = MutableLiveData(null)
    val restoreResult: LiveData<Boolean?> = _restoreResult

    private val _backupResult: MutableLiveData<Boolean?> = MutableLiveData(null)
    val backupResult: LiveData<Boolean?> = _backupResult

    var errorText: String? = null
        private set

    private val _inProgress = MutableLiveData(false)
    val inProgress: LiveData<Boolean> = _inProgress
    private var workerJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.Main + workerJob)

    fun exportDatabase(uri: Uri?) {
        _inProgress.value = true
        ioScope.launch {
            //TODO
            delay(2000)
            withContext(Dispatchers.Main) {
                _inProgress.value = false
                errorText = "An error."
                _backupResult.value = true
            }
        }
    }

    fun restoreDatabase(uri: Uri?) {
        _inProgress.value = true
        ioScope.launch {
            //TODO
            delay(2000)
            withContext(Dispatchers.Main) {
                _inProgress.value = false
                errorText = "An error."
                _restoreResult.value = true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        workerJob.cancel()
    }
}
