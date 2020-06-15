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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.databinding.FragmentNotesBinding
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

        return binding.root
    }

    private fun listenToViewModel() {
        viewModel.notes.observe(viewLifecycleOwner, Observer { notes ->
            if (notes == null) return@Observer
            if (notes.isNotEmpty()) binding.noNotesHintText.visibility = View.GONE
            else binding.noNotesHintText.visibility = View.VISIBLE
            adapter.submitList(notes)
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
        showNoteDialog(requireContext(), note)
    }

    private fun onDeleteNote(note: DisplayNote) {
        viewModel.deleteNote(note)
    }

    private fun onEditNote(note: DisplayNote) {
    }
}

class NotesViewModel : ViewModel() {
    private var dataSource: TrackAndGraphDatabaseDao? = null
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    lateinit var notes: LiveData<List<DisplayNote>>
        private set

    fun init(dataSource: TrackAndGraphDatabaseDao) {
        if (this.dataSource != null) return
        this.dataSource = dataSource
        notes = dataSource.getAllDisplayNotes()
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
}