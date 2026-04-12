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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController

@Composable
fun SearchScreen(
    navArgs: GroupNavKey,
    searchViewModel: GroupSearchViewModel,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    SearchTopBarContent(
        navArgs = navArgs,
        searchViewModel = searchViewModel,
        onBack = onBack,
    )

    SearchScreenContent(searchViewModel = searchViewModel)
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
private fun SearchScreenContent(searchViewModel: GroupSearchViewModel) {
    val results by searchViewModel.searchResults.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(results, key = { it.groupItemId }) { result ->
                ListItem(
                    headlineContent = { Text(result.name) },
                    supportingContent = if (result.description.isNotBlank()) {
                        { Text(result.description, maxLines = 1) }
                    } else null,
                    overlineContent = {
                        Text(
                            text = result.type.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
