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

package com.samco.trackandgraph.data.dependencyanalyser

import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import javax.inject.Inject

/**
 * Provider for creating fresh DependencyAnalyzer instances.
 * Injected into DataInteractor to provide on-demand dependency analysis.
 */
internal class DependencyAnalyserProvider @Inject constructor(
    private val dao: TrackAndGraphDatabaseDao
) {
    /**
     * Creates a fresh DependencyAnalyzer with the current state of the database.
     */
    suspend fun create(): DependencyAnalyser {
        return DependencyAnalyser.create(dao)
    }
}
