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

package com.samco.trackandgraph.group

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import com.samco.trackandgraph.ui.compose.appbar.SearchBarState

@Composable
fun SearchScreen(
    navArgs: GroupNavKey,
    searchViewModel: GroupSearchViewModel,
    onBack: () -> Unit,
) {
    // Handle back press
    BackHandler(onBack = onBack)

    SearchTopBarContent(
        navArgs = navArgs,
        searchViewModel = searchViewModel,
        onBack = onBack,
    )

    SearchScreenContent()
}

@Composable
private fun SearchTopBarContent(
    navArgs: GroupNavKey,
    searchViewModel: GroupSearchViewModel,
    onBack: () -> Unit,
) {
    val topBarController = LocalTopBarController.current
    val placeholder = stringResource(R.string.search)

    topBarController.Set(
        destination = navArgs,
        newConfig = AppBarConfig(
            backNavigationAction = true,
            appBarPinned = true,
            overrideBackNavigationAction = onBack,
            searchBar = SearchBarState(
                query = searchViewModel.searchQuery,
                placeholder = placeholder,
            ),
        ),
    )
}

@Composable
private fun SearchScreenContent() {
    Box(modifier = Modifier.fillMaxSize())
}
