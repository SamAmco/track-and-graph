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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.NoteType
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.databinding.FragmentViewGraphStatBinding
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.GraphStatView
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.ui.FeaturePathProvider
import com.samco.trackandgraph.ui.getWeekDayNames
import com.samco.trackandgraph.ui.showDataPointDescriptionDialog
import com.samco.trackandgraph.ui.showNoteDialog
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class ViewGraphStatFragment : Fragment() {
    private var navController: NavController? = null
    private val viewModel by viewModels<ViewGraphStatViewModel>()
    private lateinit var graphStatView: GraphStatView
    private lateinit var binding: FragmentViewGraphStatBinding
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
                val featureDisplayName =
                    viewModel.featurePathProvider.getPathForFeature(dataPoint.featureId)
                val featureType =
                    viewModel.featureTypes?.getOrElse(dataPoint.featureId) { DataType.CONTINUOUS }
                        ?: DataType.CONTINUOUS
                showDataPointDescriptionDialog(
                    requireContext(),
                    layoutInflater,
                    dataPoint,
                    featureType,
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

    override fun onDestroyView() {
        super.onDestroyView()
        graphStatView.dispose()
    }

    private fun observeGraphStatViewData() {
        viewModel.graphStatViewData.observe(viewLifecycleOwner) {
            val decorator = gsiProvider.getDecorator(it.graphOrStat.type, false)
            graphStatView.initFromGraphStat(it, decorator)
        }
    }
}

enum class ViewGraphStatViewModelState { INITIALIZING, WAITING }

@HiltViewModel
class ViewGraphStatViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider
) : ViewModel() {
    var featurePathProvider: FeaturePathProvider = FeaturePathProvider(emptyList(), emptyList())
        private set
    var featureTypes: Map<Long, DataType>? = null
        private set

    val state: LiveData<ViewGraphStatViewModelState>
        get() = _state
    private val _state = MutableLiveData(ViewGraphStatViewModelState.INITIALIZING)

    val graphStatViewData: LiveData<IGraphStatViewData>
        get() = _graphStatViewData
    private val _graphStatViewData = MutableLiveData<IGraphStatViewData>()

    val showingNotes: LiveData<Boolean>
        get() = _showingNotes
    private val _showingNotes = MutableLiveData(false)

    private val _notes = MutableLiveData<List<GraphNote>>(emptyList())
    val notes: LiveData<List<GraphNote>> = _notes

    val markedNote: LiveData<GraphNote?>
        get() = _markedNote
    private val _markedNote = MutableLiveData<GraphNote?>(null)

    private val currJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + currJob)

    private var initialized = false

    fun setGraphStatId(graphStatId: Long) {
        if (initialized) return
        initialized = true

        ioScope.launch {
            initFromGraphStatId(graphStatId)
            getAllFeatureAttributes()
            getAllGlobalNotes()
            withContext(Dispatchers.Main) {
                _state.value = ViewGraphStatViewModelState.WAITING
            }
        }
    }

    //TODO we need to filter these by date/time and only show global notes relevant to the current graph/stat
    private suspend fun getAllGlobalNotes() = withContext(Dispatchers.IO) {
        val globalNotes = dataInteractor.getAllGlobalNotesSync()
            .map { GraphNote(it) }
        val mergedList = _notes.value
            ?.union(globalNotes)
            ?.sortedByDescending { it -> it.timestamp }
        withContext(Dispatchers.Main) { _notes.value = mergedList ?: emptyList() }
    }

    private suspend fun getAllFeatureAttributes() {
        val allFeatures = dataInteractor.getAllFeaturesSync()
        val allGroups = dataInteractor.getAllGroupsSync()
        featurePathProvider = FeaturePathProvider(allFeatures, allGroups)
        featureTypes = allFeatures.associate { it.id to it.featureType }
    }

    private fun onSampledDataPoints(dataPoints: List<DataPoint>) {
        ioScope.launch {
            val dataPointNotes = dataPoints
                .filter { dp -> dp.note.isNotEmpty() }
                .distinct()
                .map { GraphNote(it) }
            val mergedList = _notes.value
                ?.union(dataPointNotes)
                ?.sortedByDescending { it -> it.timestamp }
            withContext(Dispatchers.Main) { _notes.value = mergedList ?: emptyList() }
        }
    }

    private suspend fun initFromGraphStatId(graphStatId: Long) {
        val graphStat = dataInteractor.getGraphStatById(graphStatId)
        val viewData = gsiProvider.getDataFactory(graphStat.type)
            .getViewData(graphStat, this::onSampledDataPoints)
        withContext(Dispatchers.Main) { _graphStatViewData.value = viewData }
    }

    fun showHideNotesClicked() {
        _showingNotes.value = _showingNotes.value?.not()
    }

    fun noteClicked(note: GraphNote) {
        _markedNote.value = note
    }

    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }
}
