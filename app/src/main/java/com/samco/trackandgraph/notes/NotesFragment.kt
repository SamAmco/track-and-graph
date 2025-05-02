/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.notes

import android.os.Bundle
import android.view.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.samco.trackandgraph.R
import com.samco.trackandgraph.adddatapoint.AddDataPointsViewModelImpl
import com.samco.trackandgraph.main.AppBarViewModel
import com.samco.trackandgraph.settings.TngSettings
import com.samco.trackandgraph.ui.compose.compositionlocals.LocalSettings
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.util.resumeScoped
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class NotesFragment : Fragment() {
    private val notesViewModel by viewModels<NotesViewModel>()
    private val addDataPointsDialogViewModel by viewModels<AddDataPointsViewModelImpl>()
    private val appBarViewModel by activityViewModels<AppBarViewModel>()
    private val globalNoteDialogViewModel: GlobalNoteInputViewModel by viewModels<GlobalNoteInputViewModelImpl>()

    @Inject
    lateinit var tngSettings: TngSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CompositionLocalProvider(LocalSettings provides tngSettings) {
                    TnGComposeTheme {
                        NotesView(
                            notesViewModel = notesViewModel,
                            addDataPointsDialogViewModel = addDataPointsDialogViewModel,
                            globalNoteDialogViewModel = globalNoteDialogViewModel
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
                title = getString(R.string.notes),
                actions = listOf(AppBarViewModel.Action.AddGlobalNote),
            )
        )

        for (action in appBarViewModel.actionsTaken) {
            when (action) {
                AppBarViewModel.Action.AddGlobalNote -> globalNoteDialogViewModel.openDialog(null)
                else -> {}
            }
        }
    }
}