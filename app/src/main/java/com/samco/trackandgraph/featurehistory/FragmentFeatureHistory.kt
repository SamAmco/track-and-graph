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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.map
import androidx.navigation.fragment.navArgs
import com.samco.trackandgraph.R
import com.samco.trackandgraph.main.AppBarViewModel
import com.samco.trackandgraph.settings.TngSettings
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.util.resumeScoped
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class FragmentFeatureHistory : Fragment() {
    private val viewModel by viewModels<FeatureHistoryViewModelImpl>()
    private val appBarViewModel by activityViewModels<AppBarViewModel>()
    private val args: FragmentFeatureHistoryArgs by navArgs()

    @Inject
    lateinit var tngSettings: TngSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.initViewModel(args.featureId)
        setAppBarTitle()
        viewModel.dateScrollData.map { it.items.size }.observe(viewLifecycleOwner) { updateDataPointsCount(it) }

        return ComposeView(requireContext()).apply {
            setContent {
                CompositionLocalProvider(LocalSettings provides tngSettings) {
                    TnGComposeTheme {
                        FeatureHistoryView(viewModel = viewModel)
                    }
                }
            }
        }
    }

    private fun updateDataPointsCount(numDataPoints: Int) {
        val subtitle = if (numDataPoints > 0) {
            context?.getString(R.string.data_points, numDataPoints)
        } else {
            null
        }

        setAppBarTitle(subtitle)
    }

    private fun setAppBarTitle(subtitle: String? = null) {
        appBarViewModel.setNavBarConfig(
            AppBarViewModel.NavBarConfig(
                title = args.featureName,
                subtitle = subtitle,
                actions = listOf(
                    AppBarViewModel.Action.Info,
                    AppBarViewModel.Action.Update
                )
            )
        )
    }

    override fun onResume() {
        super.onResume()
        resumeScoped {
            for (action in appBarViewModel.actionsTaken) {
                when (action) {
                    AppBarViewModel.Action.Info -> viewModel.onShowFeatureInfo()
                    AppBarViewModel.Action.Update -> viewModel.showUpdateAllDialog()
                    else -> {}
                }
            }
        }
    }
}

