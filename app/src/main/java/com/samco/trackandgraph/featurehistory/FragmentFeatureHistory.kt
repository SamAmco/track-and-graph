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
package com.samco.trackandgraph.featurehistory

import android.os.Bundle
import android.view.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.samco.trackandgraph.MainActivity
import com.samco.trackandgraph.NavButtonStyle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FragmentFeatureHistory : Fragment() {
    private val viewModel by viewModels<FeatureHistoryViewModelImpl>()
    private val args: FragmentFeatureHistoryArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.initViewModel(args.featureId)
        initMenuProvider()
        return ComposeView(requireContext()).apply {
            setContent {
                TnGComposeTheme {
                    FeatureHistoryView(viewModel = viewModel)
                }
            }
        }
    }

    private fun initMenuProvider() {
        requireActivity().addMenuProvider(
            FeatureHistoryMenuProvider(),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private inner class FeatureHistoryMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.feature_history_menu, menu)
        }

        override fun onMenuItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.infoButton -> viewModel.onShowFeatureInfo()
                else -> return false
            }
            return true
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).setActionBarConfig(NavButtonStyle.UP, args.featureName)
    }
/*

    private fun onViewDataPointClicked(dataPoint: DataPoint) {
        showDataPointDescriptionDialog(
            requireContext(),
            layoutInflater,
            dataPoint,
            viewModel.isDuration.value ?: false
        )
    }
*/
/*

    private fun onEditDataPointClicked(dataPoint: DataPoint) {
        viewModel.tracker.value?.let { tracker ->
            val dialog = DataPointInputDialog()
            val argBundle = Bundle()
            argBundle.putLongArray(TRACKER_LIST_KEY, longArrayOf(tracker.id))
            argBundle.putString(DATA_POINT_TIMESTAMP_KEY, stringFromOdt(dataPoint.timestamp))
            dialog.arguments = argBundle
            dialog.show(childFragmentManager, "input_data_point_dialog")
        }
    }

    private fun onDeleteDataPointClicked(dataPoint: DataPoint) {
        viewModel.currentActionDataPoint = dataPoint
        val dialog = YesCancelDialogFragment
            .create("no id", getString(R.string.ru_sure_del_data_point))
        childFragmentManager.let { dialog.show(it, "ru_sure_del_data_point_fragment") }
    }
*/
}

