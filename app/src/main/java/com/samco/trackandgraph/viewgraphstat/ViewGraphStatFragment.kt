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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.base.helpers.getWeekDayNames
import com.samco.trackandgraph.databinding.FragmentViewGraphStatBinding
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.ui.GraphStatView
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.showDataPointDescriptionDialog
import com.samco.trackandgraph.ui.showNoteDialog
import com.samco.trackandgraph.util.bindingForViewLifecycle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ViewGraphStatFragment : Fragment() {
    private var navController: NavController? = null
    private val viewModel: ViewGraphStatViewModel by viewModels<ViewGraphStatViewModelImpl>()
    private var binding: FragmentViewGraphStatBinding by bindingForViewLifecycle()
    private lateinit var adapter: NotesAdapter
    private val args: ViewGraphStatFragmentArgs by navArgs()
    private val windowSize = Point()
    private val maxGraphHeightRatioPortrait = 0.77f
    private val minGraphHeightRatioPortrait = 0.3f
    private val maxGraphHeightRatioLandscape = 0.7f
    private val minGraphHeightRatioLandscape = 0f

    private var graphHeight by mutableStateOf(0)

    private var showHideNotesAnimator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        this.navController = container?.findNavController()
        binding = FragmentViewGraphStatBinding.inflate(inflater, container, false)

        viewModel.initFromGraphStatId(args.graphStatId)

        setViewInitialState()
        listenToShowingNotes()
        listenToFeaturePathProvider()
        listenToBinding()
        return binding.root
    }

    private fun listenToFeaturePathProvider() {
        viewModel.featureDataProvider.observe(viewLifecycleOwner) { featureDataProvider ->
            adapter = NotesAdapter(
                featureDataProvider,
                getWeekDayNames(requireContext()),
                NoteClickListener(this::onViewNoteClicked)
            )
            binding.notesRecyclerView.adapter = adapter
            binding.notesRecyclerView.layoutManager = LinearLayoutManager(
                context, RecyclerView.VERTICAL, false
            )
            viewModel.notes.observe(viewLifecycleOwner) { onNewNotesList(it) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().windowManager.defaultDisplay.getSize(windowSize)
        val heightRatio =
            if (isPortrait()) maxGraphHeightRatioPortrait else maxGraphHeightRatioLandscape
        graphHeight = (windowSize.y * heightRatio).toInt()

        setUpComposeView()
    }

    private fun setUpComposeView() {
        binding.composeView.setContent {
            TnGComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val timeMarker = viewModel.markedNote.observeAsState()
                    val viewData = viewModel.graphStatViewData.observeAsState()

                    if (viewData.value != null) {
                        GraphStatView(
                            graphStatViewData = viewData.value!!,
                            listMode = false,
                            timeMarker = timeMarker.value?.timestamp,
                            graphHeight = graphHeight
                        )
                    }
                }
            }
        }
    }

    private fun setViewInitialState() {
        binding.showNotesButton.visibility = View.GONE
    }

    private fun onViewNoteClicked(note: GraphNote) {
        viewModel.noteClicked(note)
        when {
            note.isGlobalNote() -> showNoteDialog(
                layoutInflater,
                requireContext(),
                note.globalNote!!
            )
            note.isDataPoint() -> {
                val dataPoint = note.dataPoint!!
                val dataProvider = viewModel.featureDataProvider.value
                val featureDisplayName = dataProvider?.getPathForFeature(dataPoint.featureId) ?: ""
                val isDuration = dataProvider?.getDataSampleProperties(dataPoint.featureId)
                    ?.isDuration ?: false
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

    private fun listenToShowingNotes() {
        viewModel.showingNotes.observe(viewLifecycleOwner) { b -> onShowingNotesChanged(b) }
    }

    private fun onNewNotesList(notes: List<GraphNote>) {
        if (notes.isEmpty()) return
        binding.showNotesButton.visibility = View.VISIBLE
        adapter.submitList(notes)
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
        val startingGraphHeight = graphHeight
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
                graphHeight = nextGraphHeight.toInt()
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
}

