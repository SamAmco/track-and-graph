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
import com.samco.trackandgraph.MainActivity
import com.samco.trackandgraph.NavButtonStyle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DisplayNote
import com.samco.trackandgraph.base.database.dto.GlobalNote
import com.samco.trackandgraph.base.database.dto.NoteType
import com.samco.trackandgraph.base.database.stringFromOdt
import com.samco.trackandgraph.base.helpers.getWeekDayNames
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.databinding.FragmentNotesBinding
import com.samco.trackandgraph.addtracker.DATA_POINT_TIMESTAMP_KEY
import com.samco.trackandgraph.addtracker.FEATURE_LIST_KEY
import com.samco.trackandgraph.addtracker.DataPointInputDialog
import com.samco.trackandgraph.ui.FeaturePathProvider
import com.samco.trackandgraph.ui.showNoteDialog
import com.samco.trackandgraph.util.bindingForViewLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotesFragment : Fragment() {
    private var binding: FragmentNotesBinding by bindingForViewLifecycle()
    private val viewModel by viewModels<NotesViewModel>()
    private lateinit var adapter: NoteListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotesBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        listenToFeatureNameProvdier()

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).setActionBarConfig(
            NavButtonStyle.MENU,
            getString(R.string.notes)
        )
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

    private fun listenToFeatureNameProvdier() {
        viewModel.featureNameProvider.observe(viewLifecycleOwner, Observer {
            initListAdapter(it)
            listenToNotes()
        })
    }

    private fun listenToNotes() {
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

    private fun initListAdapter(featurePathProvider: FeaturePathProvider) {
        adapter = NoteListAdapter(
            NoteClickListener(
                this::onNoteClicked,
                this::onEditNote,
                this::onDeleteNote
            ),
            getWeekDayNames(requireContext()),
            featurePathProvider
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
                val dialog = DataPointInputDialog()
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

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val dataInteractor: DataInteractor
) : ViewModel() {
    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    val notes: LiveData<List<DisplayNote>> = dataInteractor.getAllDisplayNotes()

    lateinit var featureNameProvider: LiveData<FeaturePathProvider> private set

    private var topNote: DisplayNote? = null
    private var notesObserver: Observer<List<DisplayNote>>? = null
    private val _onNoteInsertedTop = MutableLiveData(false)
    val onNoteInsertedTop: LiveData<Boolean> = _onNoteInsertedTop

    init {
        initOnNoteInsertedTop()
        initFeatureNameProvider()
    }

    private fun initFeatureNameProvider() {
        val mediator = MediatorLiveData<FeaturePathProvider>()
        dataInteractor.let {
            val groups = it.getAllGroups()
            val features = it.getAllFeatures()
            val onEmitted = {
                val featureList = features.value
                val groupList = groups.value
                if (groupList != null && featureList != null) {
                    mediator.value = FeaturePathProvider(featureList, groupList)
                }
            }
            mediator.addSource(groups) { onEmitted() }
            mediator.addSource(features) { onEmitted() }
        }
        featureNameProvider = mediator
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
                dataInteractor.removeNote(note.timestamp, it)
            }
            NoteType.GLOBAL_NOTE -> {
                val globalNote = GlobalNote(note.timestamp, note.note)
                dataInteractor.deleteGlobalNote(globalNote)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        notesObserver?.let { notes.removeObserver(it) }
        updateJob.cancel()
    }
}