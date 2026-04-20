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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.ui.GraphStatCardView
import com.samco.trackandgraph.navigation.DeepLink
import com.samco.trackandgraph.navigation.LocalDeepLinkNavigator
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import com.samco.trackandgraph.ui.compose.ui.cardMarginSmall
import com.samco.trackandgraph.ui.compose.utils.plus

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
    val state by searchViewModel.displayResults.collectAsStateWithLifecycle()
    val navigator = LocalDeepLinkNavigator.current

    var disambiguation by remember { mutableStateOf<SearchResultItem?>(null) }

    val onPathSelected: (ResolvedPath) -> Unit = { resolved ->
        // Close search before navigating so that popping back from the deep-link destination
        // lands on the group view, not back inside the search screen.
        searchViewModel.hideSearch()
        navigator.navigate(DeepLink.ToGroupItem(resolved.descent))
    }

    val onResultClick: (SearchResultItem) -> Unit = { item ->
        when (item.paths.size) {
            0 -> Unit // Nothing to navigate to — shouldn't occur in practice.
            1 -> onPathSelected(item.paths.first())
            else -> disambiguation = item
        }
    }

    when (val displayState = state) {
        SearchDisplayState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        SearchDisplayState.Empty -> {
            CenteredMessage(text = stringResource(R.string.type_to_search))
        }

        is SearchDisplayState.Results -> {
            if (displayState.items.isEmpty()) {
                CenteredMessage(text = stringResource(R.string.no_results))
            } else {
                SearchResultsGrid(
                    items = displayState.items,
                    onResultClick = onResultClick,
                )
            }
        }
    }

    disambiguation?.let { item ->
        SymlinksDialogContent(
            data = SymlinksDialogData(
                componentName = item.child.displayName,
                paths = item.paths.map { it.displayString },
            ),
            onDismiss = { disambiguation = null },
            onPathClick = { index ->
                disambiguation = null
                onPathSelected(item.paths[index])
            },
        )
    }
}

private val GroupChild.displayName: String
    get() = when (this) {
        is GroupChild.ChildGroup -> group.name
        is GroupChild.ChildTracker -> displayTracker.name
        is GroupChild.ChildFunction -> displayFunction.name
        is GroupChild.ChildGraph -> graph.viewData.graphOrStat.name
    }

@Composable
private fun CenteredMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchResultsGrid(
    items: List<SearchResultItem>,
    onResultClick: (SearchResultItem) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columnCount = (maxWidth / minColumnWidth).toInt().coerceAtLeast(2)

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Fixed(columnCount),
            contentPadding = WindowInsets.safeDrawing
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues() + PaddingValues(vertical = cardMarginSmall),
        ) {
            items(
                items = items,
                key = { it.child.groupItemId },
                span = { item ->
                    when (item.child) {
                        is GroupChild.ChildTracker -> GridItemSpan(1)
                        is GroupChild.ChildFunction -> GridItemSpan(1)
                        is GroupChild.ChildGroup -> GridItemSpan(2)
                        is GroupChild.ChildGraph -> GridItemSpan(columnCount)
                    }
                }
            ) { item ->
                when (val child = item.child) {
                    is GroupChild.ChildTracker -> Tracker(
                        tracker = child.displayTracker,
                        onClick = { onResultClick(item) },
                    )

                    is GroupChild.ChildGroup -> Group(
                        group = child.group,
                        onClick = { onResultClick(item) },
                    )

                    is GroupChild.ChildFunction -> Function(
                        displayFunction = child.displayFunction,
                        onClick = { onResultClick(item) },
                    )

                    is GroupChild.ChildGraph -> GraphStatCardView(
                        graphStatViewData = child.graph.viewData,
                        unique = child.graph.unique,
                        onClick = { onResultClick(item) },
                    )
                }
            }
        }
    }
}
