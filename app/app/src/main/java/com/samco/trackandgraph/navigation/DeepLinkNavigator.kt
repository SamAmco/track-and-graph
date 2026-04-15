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
package com.samco.trackandgraph.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.samco.trackandgraph.group.GroupNavKey

/**
 * Fire-and-forget entry point for navigating to a [DeepLink]. Implementations append onto
 * the current back stack so the destination's ancestor chain sits below it, and ensure the
 * innermost [GroupNavKey] carries the scroll-to-item hint.
 *
 * Provided via [LocalDeepLinkNavigator] from the top of the composition tree. Consumers just
 * call `LocalDeepLinkNavigator.current.navigate(DeepLink.ToGroupItem(descent))` without
 * plumbing a callback through intermediate screens.
 */
interface DeepLinkNavigator {
    fun navigate(link: DeepLink)
}

val LocalDeepLinkNavigator = staticCompositionLocalOf<DeepLinkNavigator> {
    error("No DeepLinkNavigator provided — wrap the composition in a CompositionLocalProvider.")
}

/**
 * Default [DeepLinkNavigator] implementation. Not `@Singleton`: it holds a reference to the
 * [NavBackStack] which is created per MainScreen composition.
 */
class DeepLinkNavigatorImpl(
    private val backStack: NavBackStack<NavKey>,
) : DeepLinkNavigator {
    override fun navigate(link: DeepLink) {
        when (link) {
            is DeepLink.ToGroupItem -> backStack.applyGroupDescentPath(link.descent)
        }
    }
}

/**
 * Appends a [GroupDescentPath] onto the current back stack. Pushes one [GroupNavKey] per id
 * in [GroupDescentPath.groupIds] (outer-to-inner); the last entry carries
 * [GroupDescentPath.groupItemId] so the destination scrolls to the placement.
 *
 * An empty [GroupDescentPath.groupIds] means the target lives directly in the current group —
 * the top entry is replaced with a copy carrying the scroll hint, triggering a scroll on the
 * existing screen without a nav transition.
 *
 * Nav keys pushed here carry `groupName = null`; [com.samco.trackandgraph.group.GroupViewModel]
 * resolves the name for the top app bar once the screen mounts.
 */
internal fun NavBackStack<NavKey>.applyGroupDescentPath(descent: GroupDescentPath) {
    if (descent.groupIds.isEmpty()) {
        val top = lastOrNull() as? GroupNavKey ?: return
        removeLastOrNull()
        add(top.copy(scrollToGroupItemId = descent.groupItemId))
        return
    }
    descent.groupIds.forEachIndexed { index, groupId ->
        val isLast = index == descent.groupIds.lastIndex
        add(
            GroupNavKey(
                groupId = groupId,
                scrollToGroupItemId = if (isLast) descent.groupItemId else null,
            )
        )
    }
}
