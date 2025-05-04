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
package com.samco.trackandgraph.graphstatinput

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.samco.trackandgraph.R
import com.samco.trackandgraph.main.AppBarViewModel
import com.samco.trackandgraph.remoteconfig.UrlNavigator
import com.samco.trackandgraph.settings.TngSettings
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.util.hideKeyboard
import com.samco.trackandgraph.util.resumeScoped
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class GraphStatInputFragment : Fragment() {
    private var navController: NavController? = null
    private val args: GraphStatInputFragmentArgs by navArgs()
    private val viewModel: GraphStatInputViewModel by viewModels<GraphStatInputViewModelImpl>()
    private val appBarViewModel by activityViewModels<AppBarViewModel>()

    @Inject
    lateinit var tngSettings: TngSettings

    @Inject
    lateinit var urlNavigator: UrlNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initViewModel(args.groupId, args.graphStatId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observePopBack()
    }

    private fun observePopBack() {
        lifecycleScope.launch {
            viewModel.complete.receive()
            navController?.popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        navController = container?.findNavController()
        return ComposeView(requireContext()).apply {
            setContent {
                CompositionLocalProvider(LocalSettings provides tngSettings) {
                    TnGComposeTheme {
                        GraphStatInputView(
                            viewModelStoreOwner = this@GraphStatInputFragment,
                            viewModel = viewModel,
                            graphStatId = args.graphStatId
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeScoped { setupMenu() }
    }

    private suspend fun setupMenu() {
        appBarViewModel.setNavBarConfig(
            AppBarViewModel.NavBarConfig(
                title = getString(R.string.add_a_graph_or_stat),
                actions = listOf(AppBarViewModel.Action.Info),
            )
        )

        for (action in appBarViewModel.actionsTaken) {
            when (action) {
                AppBarViewModel.Action.Info -> onInfoClicked()
                else -> {}
            }
        }
    }

    private fun onInfoClicked() = urlNavigator.triggerNavigation(requireContext(), UrlNavigator.Location.TUTORIAL_GRAPHS)

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.hideKeyboard(requireActivity().currentFocus?.windowToken)
    }
}

