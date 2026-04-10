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
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController

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

    topBarController.Set(
        destination = navArgs,
        newConfig = AppBarConfig(
            backNavigationAction = true,
            appBarPinned = true,
            overrideBackNavigationAction = onBack,
            searchBarText = searchViewModel.searchQuery,
            actions = {
                if (searchViewModel.searchQuery.text.isNotEmpty()) {
                    IconButton(onClick = { searchViewModel.searchQuery.clearText() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                        )
                    }
                }
            }
        ),
    )
}

@Composable
private fun SearchScreenContent() {
    Box(modifier = Modifier.fillMaxSize())
}
