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

package com.samco.trackandgraph.notes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.samco.trackandgraph.database.GlobalNote
import com.samco.trackandgraph.database.TrackAndGraphDatabase
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.odtFromString
import com.samco.trackandgraph.databinding.GlobalNoteInputDialogBinding
import com.samco.trackandgraph.util.formatDayMonthYear
import kotlinx.coroutines.*
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

const val GLOBAL_NOTE_TIMESTAMP_KEY = "GLOBAL_NOTE_TIME_ID"

class GlobalNoteInputDialog : DialogFragment() {
    private val viewModel by viewModels<GlobalNoteInputViewModel>()
    private lateinit var binding: GlobalNoteInputDialogBinding

    private val timeDisplayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return activity?.let {
            val dao = TrackAndGraphDatabase
                .getInstance(it.application)
                .trackAndGraphDatabaseDao
            val timestampStr = arguments?.getString(GLOBAL_NOTE_TIMESTAMP_KEY)
            viewModel.init(dao, timestampStr)
            listenToViewModel()
            binding = GlobalNoteInputDialogBinding.inflate(inflater, container, false)
            binding.noteInputText.addTextChangedListener { editText ->
                viewModel.noteText = editText.toString()
            }
            binding.cancelButton.setOnClickListener { dismiss() }
            binding.addButton.setOnClickListener { viewModel.onAddClicked() }

            dialog?.setCanceledOnTouchOutside(true)

            binding.root
        }
    }

    private fun listenToViewModel() {
        viewModel.state.observe(viewLifecycleOwner, Observer {
            when (it) {
                GlobalNoteInputState.WAITING -> {
                    initDateButton()
                    initTimeButton()
                    setSelectedDateTime(viewModel.timestamp)
                    binding.noteInputText.requestFocus()
                    binding.noteInputText.setText(viewModel.noteText)
                    binding.noteInputText.setSelection(binding.noteInputText.text.length)
                }
                GlobalNoteInputState.DONE -> dismiss()
                else -> run {}
            }
        })
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setSelectedDateTime(dateTime: OffsetDateTime) {
        viewModel.timestamp = dateTime
        binding.dateButton.text = formatDayMonthYear(requireContext(), dateTime)
        binding.timeButton.text = dateTime.format(timeDisplayFormatter)
    }

    private fun initDateButton() {
        binding.dateButton.setOnClickListener {
            val picker = DatePickerDialog(
                requireContext(),
                DatePickerDialog.OnDateSetListener { _, year, month, day ->
                    onSetDate(year, month, day)
                },
                viewModel.timestamp.year,
                viewModel.timestamp.monthValue - 1,
                viewModel.timestamp.dayOfMonth
            )
            picker.show()
        }
    }

    private fun onSetDate(year: Int, month: Int, day: Int) {
        val oldDate = ZonedDateTime.of(
            viewModel.timestamp.toLocalDateTime(),
            ZoneId.systemDefault()
        )
        val newDate = oldDate
            .withYear(year)
            .withMonth(month + 1)
            .withDayOfMonth(day)
            .toOffsetDateTime()
        setSelectedDateTime(newDate)
    }

    private fun initTimeButton() {
        binding.timeButton.setOnClickListener {
            val picker = TimePickerDialog(
                requireContext(),
                TimePickerDialog.OnTimeSetListener { _, hour, minute -> onSetTime(hour, minute) },
                viewModel.timestamp.hour, viewModel.timestamp.minute, true
            )
            picker.show()
        }
    }

    private fun onSetTime(hour: Int, minute: Int) {
        val oldTime = ZonedDateTime.of(
            viewModel.timestamp.toLocalDateTime(),
            ZoneId.systemDefault()
        )
        val newTime = oldTime
            .withHour(hour)
            .withMinute(minute)
            .toOffsetDateTime()
        setSelectedDateTime(newTime)
    }
}

enum class GlobalNoteInputState { INITIALIZING, WAITING, DONE }
class GlobalNoteInputViewModel : ViewModel() {
    private var dataSource: TrackAndGraphDatabaseDao? = null
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    var oldNote: GlobalNote? = null
    var timestamp: OffsetDateTime = OffsetDateTime.now()
    var noteText: String = ""

    private val _state = MutableLiveData(GlobalNoteInputState.INITIALIZING)
    val state: LiveData<GlobalNoteInputState> = _state

    fun init(dataSource: TrackAndGraphDatabaseDao, timestampStr: String?) {
        if (this.dataSource != null) return
        this.dataSource = dataSource
        ioScope.launch {
            if (timestampStr != null) {
                val noteTimestamp = odtFromString(timestampStr)
                val note = dataSource.getGlobalNoteByTimeSync(noteTimestamp)
                if (note != null) {
                    timestamp = note.timestamp
                    noteText = note.note
                    oldNote = note
                }
            }
            withContext(Dispatchers.Main) {
                _state.value = GlobalNoteInputState.WAITING
            }
        }
    }

    fun onAddClicked() = ioScope.launch {
        oldNote?.let { dataSource?.deleteGlobalNote(it) }
        if (noteText.isNotEmpty()) dataSource?.insertGlobalNote(GlobalNote(timestamp, noteText))
        withContext(Dispatchers.Main) { _state.value = GlobalNoteInputState.DONE }
    }

    override fun onCleared() {
        updateJob.cancel()
    }
}

