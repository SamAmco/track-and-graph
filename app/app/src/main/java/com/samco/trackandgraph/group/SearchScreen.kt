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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.appbar.AppBarConfig
import com.samco.trackandgraph.ui.compose.appbar.LocalTopBarController
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme

@Composable
fun SearchScreen(
    navArgs: GroupNavKey,
    onBack: () -> Unit,
) {
    // Handle back press
    BackHandler(onBack = onBack)

    // Set up the top bar with search title and back navigation
    SearchTopBarContent(
        navArgs = navArgs,
        onBack = onBack
    )

    SearchScreenContent()
}

@Composable
private fun SearchTopBarContent(
    navArgs: GroupNavKey,
    onBack: () -> Unit,
) {
    val topBarController = LocalTopBarController.current
    val title = stringResource(R.string.search)

    topBarController.Set(
        destination = navArgs,
        newConfig = AppBarConfig(
            title = title,
            backNavigationAction = true,
            overrideBackNavigationAction = onBack,
        )
    )
}

@Composable
private fun SearchScreenContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = stringResource(R.string.search))
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenContentPreview() {
    TnGComposeTheme {
        SearchScreenContent()
    }
}
