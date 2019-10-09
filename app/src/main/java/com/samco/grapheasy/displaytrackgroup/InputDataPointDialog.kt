package com.samco.grapheasy.displaytrackgroup

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.samco.grapheasy.database.DataPoint
import org.threeten.bp.OffsetDateTime
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewParent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.samco.grapheasy.R
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.FeatureType
import kotlinx.android.synthetic.main.data_point_input_dialog.*

class InputDataPointDialog : DialogFragment(), DataPointInputFragment.InputDataPointFragmentListener,
    ViewPager.OnPageChangeListener {
    private lateinit var listener: InputDataPointDialogListener
    private lateinit var viewModel: InputDataPointDialogViewModel

    private lateinit var cancelButton: Button
    private lateinit var skipButton: Button
    private lateinit var addButton: Button
    private lateinit var indexText: TextView

    interface InputDataPointDialogListener {
        fun getDisplayDateTimeForInputDataPoint(): OffsetDateTime?
        fun getIdForInputDataPoint(): Long?
        fun getValueForInputDataPoint(): String?
        fun onDataPointInput(dataPoint: DataPoint)
        fun getFeatures(): List<Feature>
        fun getViewModel(): InputDataPointDialogViewModel
    }

    interface InputDataPointDialogViewModel : DataPointInputFragment.InputDataPointViewModel {
        var currentDataPointInputFeature: Feature?
        fun clearDataPointDisplayData()
        fun isDataPointDisplayDataInitialized(): Boolean
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return activity?.let {
            listener = parentFragment as InputDataPointDialogListener
            viewModel = listener.getViewModel()
            if (!viewModel.isDataPointDisplayDataInitialized()) initViewModelDisplayData()
            val view = it.layoutInflater.inflate(R.layout.data_point_input_dialog, null)
            val viewPager = view.findViewById<ViewPager>(R.id.viewPager)
            viewPager.adapter = ViewPagerAdapter(listener.getFeatures(), childFragmentManager)

            cancelButton = view.findViewById(R.id.cancelButton)
            skipButton = view.findViewById(R.id.skipButton)
            addButton = view.findViewById(R.id.addButton)
            indexText = view.findViewById(R.id.indexText)

            cancelButton.setOnClickListener { onCancelClicked() }
            skipButton.setOnClickListener { skip() }
            addButton.setOnClickListener { onAddClicked() }

            if (listener.getFeatures().size == 1) indexText.visibility = View.INVISIBLE
            viewPager.addOnPageChangeListener(this)
            setupViewFeature(listener.getFeatures()[0], 0)

            view
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun initViewModelDisplayData() {
        for (feature in listener.getFeatures()) {
            val displayData = DataPointInputFragment.DataPointDisplayData(feature.id)
            displayData.feature = feature
            displayData.selectedDateTime = listener.getDisplayDateTimeForInputDataPoint() ?: OffsetDateTime.now()
            displayData.value = listener.getValueForInputDataPoint() ?: ""
            viewModel.putDataPointDisplayData(displayData)
        }
    }

    private class ViewPagerAdapter(val features: List<Feature>, fragmentManager: FragmentManager)
        : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            val args = Bundle()
            args.putLong(FEATURE_ID_KEY, features[position].id)
            val frag = DataPointInputFragment()
            frag.arguments = args
            return frag
        }

        override fun getCount() = features.size
    }

    override fun onPageScrollStateChanged(state: Int) { }
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }
    override fun onPageSelected(position: Int) {
        setupViewFeature(listener.getFeatures()[position], position)
    }

    private fun setupViewFeature(feature: Feature, index: Int) {
        viewModel.currentDataPointInputFeature = feature
        if (feature.featureType == FeatureType.DISCRETE) addButton.visibility = View.GONE
        else addButton.visibility = View.VISIBLE
        if (index == listener.getFeatures().size-1) skipButton.visibility = View.GONE
        else skipButton.visibility = View.VISIBLE
        indexText.text = "${index+1} / ${listener.getFeatures().size}"

        //SHOW/HIDE KEYBOARD
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if (feature.featureType == FeatureType.CONTINUOUS) imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        else imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }


    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun getViewModel(): DataPointInputFragment.InputDataPointViewModel = listener.getViewModel()

    override fun onValueSubmitted(value: String, timestamp: OffsetDateTime) {
        onSubmitResult(value, timestamp)
    }

    private fun onCancelClicked() {
        closeDialog()
    }

    private fun onAddClicked() {
        val displayData = viewModel.getDataPointDisplayData(viewModel.currentDataPointInputFeature!!.id)
        val value = if (displayData.value.isEmpty()) "0" else displayData.value
        onSubmitResult(value, displayData.selectedDateTime)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        closeDialog()
    }

    private fun onSubmitResult(value: String, timestamp: OffsetDateTime) {
        var id = listener.getIdForInputDataPoint()
        if (id == null) id = 0
        val currFeatureId = viewModel.currentDataPointInputFeature!!.id
        val dataPoint = DataPoint(id, currFeatureId, value, timestamp)
        listener.onDataPointInput(dataPoint)
        val allFeatures = listener.getFeatures().map { f -> f.id }
        if (allFeatures.indexOf(currFeatureId) == allFeatures.size-1) closeDialog()
        else skip()
    }

    private fun skip() = viewPager.setCurrentItem(viewPager.currentItem + 1, true)

    private fun closeDialog() {
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
        viewModel.clearDataPointDisplayData()
        dismiss()
    }
}
