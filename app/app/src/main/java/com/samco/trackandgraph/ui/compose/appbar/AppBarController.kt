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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * Configuration for the top app bar
 */
@Immutable
data class AppBarConfig(
    val title: String = "",
    val subtitle: String? = null,
    val backNavigationAction: Boolean = false,
    val overrideBackNavigationAction: (() -> Unit)? = null,
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
    private val backStack: NavBackStack<NavKey>,
    initial: AppBarConfig = AppBarConfig("")
) {
    var config by mutableStateOf(initial)
        private set

    /**
     * Publishes [newConfig] for [destination] when (and only when) that destination is the current
     * top of the navigation back stack.
     *
     * Why this exists:
     * Navigation 3 keeps entry content alive during transitions. If the user navigates A → B and then
     * quickly back to A before the transition completes, A may not recompose on return. Relying on
     * composition alone to update the app bar can leave it showing B’s config.
     *
     * Approach:
     * Observe the back stack (top entry) and gate the write on `backStack.lastOrNull() == destination`.
     * This ensures only the foreground entry updates the app bar and avoids stale writes from an
     * exiting screen, even under rapid forward/back navigation.
     */
    @Composable
    fun Set(destination: NavKey, newConfig: AppBarConfig) {
        LaunchedEffect(backStack.lastOrNull(), destination, newConfig) {
            if (backStack.lastOrNull() == destination) {
                config = newConfig 
            }
        }
    }
}

/**
 * Composition local for providing the TopBarController throughout the app
 */
val LocalTopBarController = staticCompositionLocalOf<TopBarController> {
    error("No TopBarController provided")
}
