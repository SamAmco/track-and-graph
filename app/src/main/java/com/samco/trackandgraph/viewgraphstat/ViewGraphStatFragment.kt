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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.samco.trackandgraph.main.AppBarViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ViewGraphStatFragment : Fragment() {
    private val viewModel: ViewGraphStatViewModel by viewModels<ViewGraphStatViewModelImpl>()
    private val appBarViewModel by activityViewModels<AppBarViewModel>()
    private val args: ViewGraphStatFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.initFromGraphStatId(args.graphStatId)
        return ComposeView(requireContext()).apply {
            setContent {
                ViewGraphStatScreen(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appBarViewModel.setNavBarConfig(
            AppBarViewModel.NavBarConfig(visible = false)
        )
    }
}

