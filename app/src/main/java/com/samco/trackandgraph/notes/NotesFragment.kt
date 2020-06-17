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

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.databinding.FragmentNotesBinding
import com.samco.trackandgraph.displaytrackgroup.DATA_POINT_TIMESTAMP_KEY
import com.samco.trackandgraph.displaytrackgroup.FEATURE_LIST_KEY
import com.samco.trackandgraph.displaytrackgroup.InputDataPointDialog
import com.samco.trackandgraph.ui.showNoteDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NotesFragment : Fragment() {
    lateinit var binding: FragmentNotesBinding
    private val viewModel by viewModels<NotesViewModel>()
    private lateinit var adapter: NoteListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotesBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        initListAdapter()
        val dataSource =
            TrackAndGraphDatabase.getInstance(requireActivity().applicationContext).trackAndGraphDatabaseDao
        viewModel.init(dataSource)

        listenToViewModel()

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.notes_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_global_note) {
            val dialog = GlobalNoteInputDialog()
            childFragmentManager.let { dialog.show(it, "note_input_dialog") }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun listenToViewModel() {
        viewModel.notes.observe(viewLifecycleOwner, Observer { notes ->
            if (notes == null) return@Observer
            if (notes.isNotEmpty()) binding.noNotesHintText.visibility = View.GONE
            else binding.noNotesHintText.visibility = View.VISIBLE
            adapter.submitList(notes)
        })
        viewModel.onNoteInsertedTop.observe(viewLifecycleOwner, Observer {
            if (it) binding.notesList.postDelayed({
                binding.notesList.smoothScrollToPosition(0)
            }, 100)
        })
    }

    private fun initListAdapter() {
        adapter = NoteListAdapter(
            NoteClickListener(
                this::onNoteClicked,
                this::onEditNote,
                this::onDeleteNote
            )
        )
        binding.notesList.adapter = adapter
        binding.notesList.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
    }

    private fun onNoteClicked(note: DisplayNote) {
        showNoteDialog(layoutInflater, requireContext(), note)
    }

    private fun onDeleteNote(note: DisplayNote) {
        viewModel.deleteNote(note)
    }

    private fun onEditNote(note: DisplayNote) {
        when (note.noteType) {
            NoteType.DATA_POINT -> note.featureId?.let { featureId ->
                val dialog = InputDataPointDialog()
                val argBundle = Bundle()
                argBundle.putLongArray(FEATURE_LIST_KEY, longArrayOf(featureId))
                argBundle.putString(DATA_POINT_TIMESTAMP_KEY, stringFromOdt(note.timestamp))
                dialog.arguments = argBundle
                childFragmentManager.let { dialog.show(it, "input_data_point_dialog") }
            }
            NoteType.GLOBAL_NOTE -> run {
                val dialog = GlobalNoteInputDialog()
                val argBundle = Bundle()
                argBundle.putString(GLOBAL_NOTE_TIMESTAMP_KEY, stringFromOdt(note.timestamp))
                dialog.arguments = argBundle
                childFragmentManager.let { dialog.show(it, "global_note_edit_dialog") }
            }
        }
    }
}

class NotesViewModel : ViewModel() {
    private var dataSource: TrackAndGraphDatabaseDao? = null
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    lateinit var notes: LiveData<List<DisplayNote>>
        private set

    private var topNote: DisplayNote? = null
    private var notesObserver: Observer<List<DisplayNote>>? = null
    private val _onNoteInsertedTop = MutableLiveData(false)
    val onNoteInsertedTop: LiveData<Boolean> = _onNoteInsertedTop

    fun init(dataSource: TrackAndGraphDatabaseDao) {
        if (this.dataSource != null) return
        this.dataSource = dataSource
        notes = dataSource.getAllDisplayNotes()
        initOnNoteInsertedTop()
    }

    private fun initOnNoteInsertedTop() {
        notesObserver = Observer {
            if (it.isNullOrEmpty()) return@Observer
            val newTopNote = it.first()
            if (topNote != null && topNote != newTopNote) {
                _onNoteInsertedTop.value = true
                _onNoteInsertedTop.value = false
            }
            topNote = newTopNote
        }
        notes.observeForever(notesObserver!!)
    }

    fun deleteNote(note: DisplayNote) = ioScope.launch {
        when (note.noteType) {
            NoteType.DATA_POINT -> note.featureId?.let {
                dataSource!!.removeNote(note.timestamp, note.featureId)
            }
            NoteType.GLOBAL_NOTE -> {
                val globalNote = GlobalNote(
                    note.timestamp,
                    note.note
                )
                dataSource!!.deleteGlobalNote(globalNote)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        notesObserver?.let { notes.removeObserver(it) }
        updateJob.cancel()
    }
}