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

/**
 * Describes an in-app navigation target. Callers hand a [DeepLink] to the [DeepLinkNavigator]
 * and it figures out how to reshape the back stack so the user lands on the target with the
 * correct group chain behind them.
 *
 * Only [ToGroupItem] exists today. Future variants (by tracker id, by graph id, by URI, etc.)
 * will be added here as external entry points need them.
 */
sealed interface DeepLink {

    /**
     * Navigate to a specific placement of a component along a pre-resolved
     * [GroupDescentPath], relative to whatever `GroupScreen` is on top of the back stack
     * when the navigator fires.
     */
    data class ToGroupItem(val descent: GroupDescentPath) : DeepLink
}
