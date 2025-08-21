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
package com.samco.trackandgraph.ui.compose.appbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection

/**
 * Configuration for the top app bar
 */
data class AppBarConfig(
    val title: String = "",
    val subtitle: String? = null,
    val backNavigationAction: Boolean = false,
    val actions: @Composable RowScope.() -> Unit = {},
    val visible: Boolean = true,
    val nestedScrollConnection: NestedScrollConnection? = null,
    val appBarPinned: Boolean = false,
)

/**
 * Controller for managing the top app bar state across the app
 */
@Stable
class TopBarController(
    initial: AppBarConfig = AppBarConfig("")
) {
    var config by mutableStateOf(initial)
        private set
        
    fun set(newConfig: AppBarConfig) { 
        config = newConfig 
    }
}

/**
 * Composition local for providing the TopBarController throughout the app
 */
val LocalTopBarController = staticCompositionLocalOf<TopBarController> {
    error("No TopBarController provided")
}
