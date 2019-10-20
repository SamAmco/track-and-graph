package com.samco.grapheasy.displaytrackgroup

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import org.threeten.bp.OffsetDateTime
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.samco.grapheasy.R
import com.samco.grapheasy.database.*
import kotlinx.android.synthetic.main.data_point_input_dialog.*
import kotlinx.coroutines.*
import timber.log.Timber

const val FEATURE_LIST_KEY = "FEATURE_LIST_KEY"
const val DATA_POINT_TIMESTAMP_KEY = "DATA_POINT_ID"

class InputDataPointDialog : DialogFragment(), ViewPager.OnPageChangeListener {
    private lateinit var viewModel: InputDataPointDialogViewModel
    private val inputViews = mutableMapOf<Int, DataPointInputView>()

    private lateinit var cancelButton: Button
    private lateinit var skipButton: Button
    private lateinit var addButton: Button
    private lateinit var indexText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return activity?.let {
            viewModel = ViewModelProviders.of(this).get(InputDataPointDialogViewModel::class.java)
            initViewModel()
            val view = it.layoutInflater.inflate(R.layout.data_point_input_dialog, null)
            val viewPager = view.findViewById<ViewPager>(R.id.viewPager)

            cancelButton = view.findViewById(R.id.cancelButton)
            skipButton = view.findViewById(R.id.skipButton)
            addButton = view.findViewById(R.id.addButton)
            indexText = view.findViewById(R.id.indexText)

            cancelButton.setOnClickListener { onCancelClicked() }
            skipButton.setOnClickListener { skip() }
            addButton.setOnClickListener { onAddClicked() }

            listenToFeatures()
            listenToIndex()
            listenToState()

            viewPager.addOnPageChangeListener(this)

            view
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun listenToState() {
        viewModel.state.observe(this, Observer { state ->
            when (state) {
                InputDataPointDialogState.LOADING -> { addButton.isEnabled = false }
                InputDataPointDialogState.WAITING -> { addButton.isEnabled = true }
                InputDataPointDialogState.ADDING -> { addButton.isEnabled = false }
                InputDataPointDialogState.ADDED -> {
                    addButton.isEnabled = true
                    if (viewModel.currentFeatureIndex.value!! < viewModel.features.value!!.size-1) {
                        skip()
                        viewModel.onFinishedTransition()
                    }
                    else closeDialog()
                }
            }
        })
    }

    private fun listenToIndex() {
        viewModel.currentFeatureIndex.observe(this, Observer { index ->
            if (index != null) setupViewFeature(viewModel.features.value!![index], index)
        })
    }

    private fun listenToFeatures() {
        viewModel.features.observe(this, Observer { features ->
            if (features.isEmpty()) return@Observer
            viewPager.adapter = ViewPagerAdapter(
                context!!,
                features,
                DataPointInputView.DataPointInputClickListener(this::onSubmitResult),
                viewModel.uiStates,
                inputViews
            )
            if (features.size == 1) indexText.visibility = View.INVISIBLE
        })
    }

    private fun initViewModel() {
        val timestampStr = arguments!!.getString(DATA_POINT_TIMESTAMP_KEY)
        val timestamp = if (timestampStr != null) odtFromString(timestampStr) else null
        viewModel.loadData(activity!!, arguments!!.getLongArray(FEATURE_LIST_KEY)!!.toList(), timestamp)
    }

    private class ViewPagerAdapter(val context: Context,
                                   val features: List<Feature>,
                                   val clickListener: DataPointInputView.DataPointInputClickListener,
                                   val uiStates: Map<Feature, DataPointInputView.DataPointInputData>,
                                   val inputViews: MutableMap<Int, DataPointInputView>) : PagerAdapter() {
        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view == `object`
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val view = DataPointInputView(context, uiStates[features[position]]!!)
            val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            view.layoutParams = params
            view.setOnClickListener(clickListener)
            inputViews[position] = view
            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getCount() = features.size
    }

    override fun onPageScrollStateChanged(state: Int) { }
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }
    override fun onPageSelected(position: Int) { viewModel.currentFeatureIndex.value = position }

    private fun setupViewFeature(feature: Feature, index: Int) {
        if (feature.featureType == FeatureType.DISCRETE) addButton.visibility = View.GONE
        else addButton.visibility = View.VISIBLE
        if (index == viewModel.features.value!!.size-1) skipButton.visibility = View.GONE
        else skipButton.visibility = View.VISIBLE
        indexText.text = "${index+1} / ${viewModel.features.value!!.size}"

        //SHOW/HIDE KEYBOARD
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if (feature.featureType == FeatureType.CONTINUOUS) imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        else imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }


    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun onCancelClicked() { closeDialog() }

    private fun onAddClicked() {
        val currIndex = viewModel.currentFeatureIndex.value!!
        val currFeature = viewModel.features.value!![currIndex]
        onSubmitResult(viewModel.uiStates[currFeature]!!)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        closeDialog()
    }

    private fun onSubmitResult(dataPointInputData: DataPointInputView.DataPointInputData) {
        val value = if (dataPointInputData.value.isEmpty()) "0" else dataPointInputData.value
        viewModel.onDataPointInput(activity!!,
            DataPoint(dataPointInputData.dateTime, dataPointInputData.feature.id, value, dataPointInputData.label)
        )
    }

    private fun skip() = viewPager.setCurrentItem(viewPager.currentItem + 1, true)

    private fun closeDialog() {
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
        dismiss()
    }
}

enum class InputDataPointDialogState { LOADING, WAITING, ADDING, ADDED }
class InputDataPointDialogViewModel : ViewModel() {
    private var updateJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + updateJob)

    val state: LiveData<InputDataPointDialogState> get() { return _state }
    private val _state by lazy {
        val state = MutableLiveData<InputDataPointDialogState>()
        state.value = InputDataPointDialogState.LOADING
        return@lazy state
    }

    lateinit var uiStates: Map<Feature, DataPointInputView.DataPointInputData>
    val features: LiveData<List<Feature>> get() { return _features }
    private val _features by lazy {
        val feats = MutableLiveData<List<Feature>>()
        feats.value = listOf()
        return@lazy feats
    }

    val currentFeatureIndex: MutableLiveData<Int?> get() { return _currentFeatureIndex }
    private val _currentFeatureIndex by lazy {
        val index = MutableLiveData<Int?>()
        index.value = null
        return@lazy index
    }

    fun loadData(activity: Activity, featureIds: List<Long>, dataPointTimestamp: OffsetDateTime?) {
        if (features.value!!.isNotEmpty()) return
        val application = activity.application
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        uiScope.launch {
            _state.value = InputDataPointDialogState.LOADING
            lateinit var featureData: List<Feature>
            var dataPointData: DataPoint? = null
            withContext(Dispatchers.IO) {
                featureData = dao.getFeaturesByIdsSync(featureIds)
                if (dataPointTimestamp != null) {
                    dataPointData = dao.getDataPointByTimestampAndFeatureSync(featureData[0].id, dataPointTimestamp)
                }
            }

            val defaultValue = if (dataPointData != null) dataPointData!!.value else ""
            val defaultLabel = if (dataPointData != null) dataPointData!!.label else ""
            val timestamp = if (dataPointData != null) dataPointData!!.timestamp else OffsetDateTime.now()
            val timeModifiable = dataPointData == null
            uiStates = featureData.map { f ->
                f to DataPointInputView.DataPointInputData(f, timestamp, defaultValue, defaultLabel, timeModifiable)
            }.toMap()
            _features.value = featureData
            _currentFeatureIndex.value = 0
            _state.value = InputDataPointDialogState.WAITING
        }
    }

    fun onDataPointInput(activity: Activity, dataPoint: DataPoint) {
        if (state.value != InputDataPointDialogState.WAITING) return
        val application = activity.application
        val dao = GraphEasyDatabase.getInstance(application).graphEasyDatabaseDao
        uiScope.launch {
            _state.value = InputDataPointDialogState.ADDING
            withContext(Dispatchers.IO) {
                Timber.d("adding data point: ${dataPoint.value} to feature ${dataPoint.featureId}")
                dao.insertDataPoint(dataPoint)
            }
            _state.value = InputDataPointDialogState.ADDED
        }
    }

    fun onFinishedTransition() {
        _state.value = InputDataPointDialogState.WAITING
    }

    override fun onCleared() {
        super.onCleared()
        uiScope.cancel()
    }
}
