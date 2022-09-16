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

package com.samco.trackandgraph.viewgraphstat

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.helpers.getWeekDayNames
import com.samco.trackandgraph.databinding.FragmentViewGraphStatBinding
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.GraphStatView
import com.samco.trackandgraph.ui.showDataPointDescriptionDialog
import com.samco.trackandgraph.ui.showNoteDialog
import com.samco.trackandgraph.util.bindingForViewLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ViewGraphStatFragment : Fragment() {
    private var navController: NavController? = null
    private val viewModel by viewModels<ViewGraphStatViewModel>()
    private lateinit var graphStatView: GraphStatView
    private var binding: FragmentViewGraphStatBinding by bindingForViewLifecycle()
    private lateinit var adapter: NotesAdapter
    private val args: ViewGraphStatFragmentArgs by navArgs()
    private val windowSize = Point()
    private val maxGraphHeightRatioPortrait = 0.77f
    private val minGraphHeightRatioPortrait = 0.3f
    private val maxGraphHeightRatioLandscape = 0.7f
    private val minGraphHeightRatioLandscape = 0f

    private var showHideNotesAnimator: ValueAnimator? = null

    @Inject
    lateinit var gsiProvider: GraphStatInteractorProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        this.navController = container?.findNavController()
        binding = FragmentViewGraphStatBinding.inflate(inflater, container, false)
        graphStatView = binding.graphStatView

        viewModel.setGraphStatId(args.graphStatId)

        setViewInitialState()
        listenToState()
        listenToBinding()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().windowManager.defaultDisplay.getSize(windowSize)
        val heightRatio =
            if (isPortrait()) maxGraphHeightRatioPortrait else maxGraphHeightRatioLandscape
        graphStatView.setGraphHeight((windowSize.y * heightRatio).toInt())
    }

    private fun setViewInitialState() {
        graphStatView.addLineGraphPanAndZoom()
        binding.showNotesButton.visibility = View.GONE
    }

    private fun onViewNoteClicked(note: GraphNote) {
        viewModel.noteClicked(note)
        when (note.noteType) {
            NoteType.GLOBAL_NOTE -> showNoteDialog(
                layoutInflater,
                requireContext(),
                note.globalNote!!
            )
            NoteType.DATA_POINT -> {
                val dataPoint = note.dataPoint!!
                val featureDisplayName = viewModel.featurePathProvider.getPathForFeature(
                    dataPoint.featureId,
                    DataSourceType.FEATURE
                )
                val isDuration = viewModel.featureTypes
                    ?.get(dataPoint.featureId) == DataType.DURATION
                showDataPointDescriptionDialog(
                    requireContext(),
                    layoutInflater,
                    dataPoint,
                    isDuration,
                    featureDisplayName
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listenToBinding() {
        binding.showNotesButton.setOnTouchListener { _, motionEvent ->
            if (motionEvent?.action == MotionEvent.ACTION_DOWN) {
                viewModel.showHideNotesClicked()
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }
    }

    private fun listenToState() {
        viewModel.state.observe(viewLifecycleOwner) { state -> onViewModelStateChanged(state) }
        viewModel.showingNotes.observe(viewLifecycleOwner) { b -> onShowingNotesChanged(b) }
    }

    private fun observeNoteMarker() {
        viewModel.markedNote.observe(viewLifecycleOwner) { note ->
            if (note == null) return@observe
            binding.graphStatView.placeMarker(note.timestamp)
        }
    }

    private fun onNewNotesList(notes: List<GraphNote>) {
        if (notes.isEmpty()) return
        binding.showNotesButton.visibility = View.VISIBLE
        adapter.submitList(notes)
    }

    private fun onViewModelStateChanged(state: ViewGraphStatViewModelState) {
        when (state) {
            ViewGraphStatViewModelState.INITIALIZING -> graphStatView.initLoading()
            ViewGraphStatViewModelState.WAITING -> {
                observeGraphStatViewData()
                initNotesAdapterFromViewModel()
                observeNoteMarker()
            }
        }
    }

    private fun initNotesAdapterFromViewModel() {
        val featurePathProvider = viewModel.featurePathProvider
        val featureTypes = viewModel.featureTypes ?: emptyMap()
        adapter = NotesAdapter(
            featurePathProvider,
            featureTypes,
            getWeekDayNames(requireContext()),
            NoteClickListener(this::onViewNoteClicked)
        )
        binding.notesRecyclerView.adapter = adapter
        binding.notesRecyclerView.layoutManager = LinearLayoutManager(
            context, RecyclerView.VERTICAL, false
        )
        viewModel.notes.observe(viewLifecycleOwner) { onNewNotesList(it) }
    }

    private fun onShowingNotesChanged(showNotes: Boolean) {
        val maxGraphHeightRatio =
            if (isPortrait()) maxGraphHeightRatioPortrait else maxGraphHeightRatioLandscape
        val minGraphHeightRatio =
            if (isPortrait()) minGraphHeightRatioPortrait else minGraphHeightRatioLandscape
        val maxNotesHeight = windowSize.y * (maxGraphHeightRatio - minGraphHeightRatio)
        val targetNotesHeight = if (showNotes) maxNotesHeight else 0
        val targetGraphHeight =
            windowSize.y * (if (showNotes) minGraphHeightRatio else maxGraphHeightRatio)

        animateViewHeights(targetNotesHeight.toInt(), targetGraphHeight.toInt())
        binding.notesPopupUpButton.visibility = if (showNotes) View.GONE else View.VISIBLE
        binding.notesPopupDownButton.visibility = if (showNotes) View.VISIBLE else View.GONE
    }

    private fun isPortrait() =
        resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    private fun animateViewHeights(notesTargetHeight: Int, graphTargetHeight: Int) {
        showHideNotesAnimator?.cancel()
        val startingNotesHeight =
            (binding.notesRecyclerView.layoutParams as LinearLayout.LayoutParams).height
        val startingGraphHeight = graphStatView.getGraphHeight()
        showHideNotesAnimator = ValueAnimator.ofFloat(0f, 1f)
        showHideNotesAnimator?.apply {
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val nextRatio = animation.animatedValue as Float
                val nextGraphHeight =
                    startingGraphHeight + ((graphTargetHeight - startingGraphHeight) * nextRatio)
                val nextNotesHeight =
                    startingNotesHeight + ((notesTargetHeight - startingNotesHeight) * nextRatio)
                graphStatView.setGraphHeight(nextGraphHeight.toInt())
                (binding.notesRecyclerView.layoutParams as LinearLayout.LayoutParams).height =
                    nextNotesHeight.toInt()
                binding.notesRecyclerView.requestLayout()
            }
        }?.start()
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar!!.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity).supportActionBar!!.show()
    }

    private fun observeGraphStatViewData() {
        viewModel.graphStatViewData.observe(viewLifecycleOwner) {
            val decorator = gsiProvider.getDecorator(it.graphOrStat.type, false)
            graphStatView.initFromGraphStat(it, decorator)
        }
    }
}

