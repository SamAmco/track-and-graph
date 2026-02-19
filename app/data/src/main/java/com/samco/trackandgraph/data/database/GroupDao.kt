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

package com.samco.trackandgraph.data.database

import com.samco.trackandgraph.data.database.entity.Group
import com.samco.trackandgraph.data.database.entity.queryresponse.FunctionWithFeature
import com.samco.trackandgraph.data.database.entity.queryresponse.TrackerWithFeature

/**
 * Data access interface for group-related operations.
 * This interface abstracts the database operations needed by GroupHelper,
 * allowing for different implementations (Room, fake for testing, etc.)
 */
internal interface GroupDao {

    // =========================================================================
    // Group CRUD operations
    // =========================================================================

    fun insertGroup(group: Group): Long

    fun updateGroup(group: Group)

    fun deleteGroup(id: Long)

    fun getGroupById(id: Long): Group?

    fun getAllGroupsSync(): List<Group>

    fun getGroupsForGroupSync(id: Long): List<Group>

    fun hasAnyGroups(): Boolean

    // =========================================================================
    // Operations for recursive group deletion
    // =========================================================================

    fun getTrackerById(trackerId: Long): TrackerWithFeature?

    fun getFunctionById(functionId: Long): FunctionWithFeature?

    fun deleteFeature(id: Long)

    fun deleteGraphOrStat(id: Long)

    fun deleteReminder(id: Long)
}
