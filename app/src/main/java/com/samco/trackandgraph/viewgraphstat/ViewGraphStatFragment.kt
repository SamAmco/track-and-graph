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
import android.app.Activity
import android.content.res.Configuration
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
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.databinding.FragmentViewGraphStatBinding
import com.samco.trackandgraph.graphstatview.GraphStatView
import com.samco.trackandgraph.graphstatview.SampleDataCallback
import com.samco.trackandgraph.ui.showDataPointDescriptionDialog
import com.samco.trackandgraph.ui.showNoteDialog
import kotlinx.coroutines.*

class ViewGraphStatFragment : Fragment() {
    private var navController: NavController? = null
    private val viewModel by viewModels<ViewGraphStatViewModel>()
    private lateinit var graphStatView: GraphStatView
    private lateinit var binding: FragmentViewGraphStatBinding
    private lateinit var adapter: NotesAdapter
    private val args: ViewGraphStatFragmentArgs by navArgs()

    private var showHideNotesAnimator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.navController = container?.findNavController()
        viewModel.init(requireActivity(), args.graphStatId)
        binding = FragmentViewGraphStatBinding.inflate(inflater, container, false)
        graphStatView = binding.graphStatView
        setViewInitialState()
        listenToState()
        listenToBinding()
        return binding.root
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
                    viewModel.featureDisplayNames?.getOrElse(dataPoint.featureId) { null }
                val featureType =
                    viewModel.featureTypes?.getOrElse(dataPoint.featureId) { FeatureType.CONTINUOUS }
                        ?: FeatureType.CONTINUOUS
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
        viewModel.state.observe(
            viewLifecycleOwner,
            Observer { state -> onViewModelStateChanged(state) })
        viewModel.showingNotes.observe(
            viewLifecycleOwner,
            Observer { b -> onShowingNotesChanged(b) })
    }

    private fun observeNoteMarker() {
        viewModel.markedNote.observe(viewLifecycleOwner, Observer { note ->
            if (note == null) return@Observer
            binding.graphStatView.placeMarker(note.timestamp)
        })
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
                initGraphStatViewFromViewModel()
                initNotesAdapterFromViewModel()
                observeNoteMarker()
            }
        }
    }

    private fun initNotesAdapterFromViewModel() {
        val featureDisplayNames = viewModel.featureDisplayNames ?: emptyMap()
        val featureTypes = viewModel.featureTypes ?: emptyMap()
        adapter = NotesAdapter(
            featureDisplayNames,
            featureTypes,
            NoteClickListener(this::onViewNoteClicked)
        )
        binding.notesRecyclerView.adapter = adapter
        binding.notesRecyclerView.layoutManager = LinearLayoutManager(
            context, RecyclerView.VERTICAL, false
        )
        viewModel.notes.observe(viewLifecycleOwner, Observer { onNewNotesList(it) })
    }

    private fun onShowingNotesChanged(showNotes: Boolean) {
        val largeSize =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 1.4f
            else 10000f
        val toValue = if (showNotes) largeSize else 0f
        animateNotesRecycler(toValue)
        binding.notesPopupUpButton.visibility = if (showNotes) View.GONE else View.VISIBLE
        binding.notesPopupDownButton.visibility = if (showNotes) View.VISIBLE else View.GONE
    }

    private fun animateNotesRecycler(toValue: Float) {
        showHideNotesAnimator?.cancel()
        val currentWeight =
            (binding.notesRecyclerView.layoutParams as LinearLayout.LayoutParams).weight
        showHideNotesAnimator = ValueAnimator.ofFloat(currentWeight, toValue)
        showHideNotesAnimator!!.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        showHideNotesAnimator!!.interpolator = LinearInterpolator()
        showHideNotesAnimator!!.addUpdateListener { animation ->
            (binding.notesRecyclerView.layoutParams as LinearLayout.LayoutParams).weight =
                animation.animatedValue as Float
            binding.notesRecyclerView.requestLayout()
        }
        showHideNotesAnimator!!.start()
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

    private fun initGraphStatViewFromViewModel() {
        val graphStat = viewModel.graphStatObject
        if (graphStat == null) graphStatView.initError(null, R.string.graph_stat_view_not_found)
        when (viewModel.graphStatInnerObject) {
            null -> graphStatView.initError(null, R.string.graph_stat_view_not_found)
            is LineGraph -> graphStatView.initFromLineGraph(
                graphStat!!,
                viewModel.graphStatInnerObject as LineGraph,
                false,
                SampleDataCallback(viewModel::onSampledDataPoints)
            )
            is PieChart -> graphStatView.initFromPieChart(
                graphStat!!,
                viewModel.graphStatInnerObject as PieChart,
                SampleDataCallback(viewModel::onSampledDataPoints)
            )
            is TimeSinceLastStat -> graphStatView.initTimeSinceStat(
                graphStat!!,
                viewModel.graphStatInnerObject as TimeSinceLastStat,
                SampleDataCallback(viewModel::onSampledDataPoints)
            )
            is AverageTimeBetweenStat -> graphStatView.initAverageTimeBetweenStat(
                graphStat!!,
                viewModel.graphStatInnerObject as AverageTimeBetweenStat,
                SampleDataCallback(viewModel::onSampledDataPoints)
            )
            else -> graphStatView.initError(null, R.string.graph_stat_validation_unknown)
        }
    }
}

enum class ViewGraphStatViewModelState { INITIALIZING, WAITING }
class ViewGraphStatViewModel : ViewModel() {
    var graphStatObject: GraphOrStat? = null
        private set
    var graphStatInnerObject: Any? = null
        private set
    var featureDisplayNames: Map<Long, String>? = null
        private set
    var featureTypes: Map<Long, FeatureType>? = null
        private set

    val state: LiveData<ViewGraphStatViewModelState>
        get() {
            return _state
        }
    private val _state = MutableLiveData(ViewGraphStatViewModelState.INITIALIZING)

    val showingNotes: LiveData<Boolean>
        get() {
            return _showingNotes
        }
    private val _showingNotes = MutableLiveData(false)

    private val _notes = MutableLiveData<List<GraphNote>>(emptyList())
    val notes: LiveData<List<GraphNote>> = _notes

    val markedNote: LiveData<GraphNote?>
        get() {
            return _markedNote
        }
    private val _markedNote = MutableLiveData<GraphNote?>(null)

    private val currJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + currJob)

    private var dataSource: TrackAndGraphDatabaseDao? = null

    fun init(activity: Activity, graphStatId: Long) {
        if (dataSource != null) return
        dataSource =
            TrackAndGraphDatabase.getInstance(activity.application).trackAndGraphDatabaseDao
        _state.value =
            ViewGraphStatViewModelState.INITIALIZING
        ioScope.launch {
            initFromGraphStatId(graphStatId)
            getAllFeatureAttributes()
            getAllGlobalNotes()
            withContext(Dispatchers.Main) {
                _state.value =
                    ViewGraphStatViewModelState.WAITING
            }
        }
    }

    private suspend fun getAllGlobalNotes() = withContext(Dispatchers.IO) {
        val globalNotes = dataSource!!.getAllGlobalNotesSync()
            .map { GraphNote(it) }
        val mergedList = _notes.value
            ?.union(globalNotes)
            ?.sortedByDescending { it -> it.timestamp }
        withContext(Dispatchers.Main) { _notes.value = mergedList }
    }

    private fun getAllFeatureAttributes() {
        val allFeatures = dataSource!!.getAllFeaturesAndTrackGroupsSync()
        featureDisplayNames = allFeatures
            .map { it.id to "${it.trackGroupName} -> ${it.name}" }
            .toMap()
        featureTypes = allFeatures
            .map { it.id to it.featureType }
            .toMap()
    }

    fun onSampledDataPoints(dataPoints: List<DataPoint>) {
        ioScope.launch {
            val dataPointNotes = dataPoints
                .filter { dp -> dp.note.isNotEmpty() }
                .distinct()
                .map { GraphNote(it) }
            val mergedList = _notes.value
                ?.union(dataPointNotes)
                ?.sortedByDescending { it -> it.timestamp }
            withContext(Dispatchers.Main) { _notes.value = mergedList }
        }
    }

    private fun initFromGraphStatId(graphStatId: Long) {
        graphStatObject = dataSource!!.getGraphStatById(graphStatId)
        when (graphStatObject!!.type) {
            GraphStatType.LINE_GRAPH -> initLineGraph()
            GraphStatType.PIE_CHART -> initPieChart()
            GraphStatType.TIME_SINCE -> initTimeSince()
            GraphStatType.AVERAGE_TIME_BETWEEN -> initAverageTimeBetween()
        }
    }

    fun showHideNotesClicked() {
        _showingNotes.value = _showingNotes.value?.not()
    }

    fun noteClicked(note: GraphNote) {
        _markedNote.value = note
    }

    private fun initLineGraph() {
        graphStatInnerObject = dataSource!!.getLineGraphByGraphStatId(graphStatObject!!.id)
    }

    private fun initPieChart() {
        graphStatInnerObject = dataSource!!.getPieChartByGraphStatId(graphStatObject!!.id)
    }

    private fun initTimeSince() {
        graphStatInnerObject = dataSource!!.getTimeSinceLastStatByGraphStatId(graphStatObject!!.id)
    }

    private fun initAverageTimeBetween() {
        graphStatInnerObject =
            dataSource!!.getAverageTimeBetweenStatByGraphStatId(graphStatObject!!.id)
    }

    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }
}
