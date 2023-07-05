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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.MainActivity
import com.samco.trackandgraph.NavButtonStyle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.adddatapoint.AddDataPointsViewModelImpl
import com.samco.trackandgraph.base.database.dto.DisplayNote
import com.samco.trackandgraph.base.helpers.getWeekDayNames
import com.samco.trackandgraph.databinding.FragmentNotesBinding
import com.samco.trackandgraph.settings.TngSettings
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import com.samco.trackandgraph.util.FeaturePathProvider
import com.samco.trackandgraph.ui.showNoteDialog
import com.samco.trackandgraph.util.bindingForViewLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotesFragment : Fragment() {
    private var binding: FragmentNotesBinding by bindingForViewLifecycle()
    private lateinit var adapter: NoteListAdapter

    private val viewModel by viewModels<NotesViewModel>()
    private val addDataPointsDialogViewModel by viewModels<AddDataPointsViewModelImpl>()
    private val globalNoteDialogViewModel: GlobalNoteInputViewModel by viewModels<GlobalNoteInputViewModelImpl>()

    @Inject
    lateinit var tngSettings: TngSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotesBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.composeView.setContent {
            CompositionLocalProvider(LocalSettings provides tngSettings) {
                NotesView(
                    addDataPointsDialogViewModel = addDataPointsDialogViewModel,
                    globalNoteDialogViewModel = globalNoteDialogViewModel
                )
            }
        }

        listenToFeatureNameProvider()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(
            NotesMenuProvider(),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private inner class NotesMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.notes_menu, menu)
        }

        override fun onMenuItemSelected(item: MenuItem): Boolean {
            if (item.itemId == R.id.add_global_note) {
                globalNoteDialogViewModel.openDialog(null)
                return true
            }
            return false
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).setActionBarConfig(
            NavButtonStyle.MENU,
            getString(R.string.notes)
        )
    }

    private fun listenToFeatureNameProvider() {
        viewModel.featureNameProvider.observe(viewLifecycleOwner) {
            initListAdapter(it)
            listenToNotes()
        }
    }

    private fun listenToNotes() {
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            if (notes == null) return@observe
            if (notes.isNotEmpty()) binding.noNotesHintText.visibility = View.GONE
            else binding.noNotesHintText.visibility = View.VISIBLE
            adapter.submitList(notes)
        }
        viewModel.onNoteInsertedTop.observe(viewLifecycleOwner) {
            if (it) binding.notesList.postDelayed({
                binding.notesList.smoothScrollToPosition(0)
            }, 100)
        }
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
        val featurePath = note.featureId?.let {
            viewModel.featureNameProvider.value?.getPathForFeature(it)
        } ?: note.featureName
        showNoteDialog(layoutInflater, requireContext(), note, featurePath)
    }

    private fun onDeleteNote(note: DisplayNote) {
        viewModel.deleteNote(note)
    }

    private fun onEditNote(note: DisplayNote) =
        note.trackerId?.let { showEditDataPointDialog(it, note) }
            ?: showEditGlobalNoteDialog(note)

    private fun showEditGlobalNoteDialog(note: DisplayNote) {
        globalNoteDialogViewModel.openDialog(note.timestamp)
    }

    private fun showEditDataPointDialog(trackerId: Long, note: DisplayNote) {
        addDataPointsDialogViewModel.showAddDataPointDialog(trackerId, note.timestamp)
    }
}