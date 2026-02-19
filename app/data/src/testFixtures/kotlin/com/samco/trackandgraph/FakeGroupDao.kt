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

package com.samco.trackandgraph

import com.samco.trackandgraph.data.database.GroupDao
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.entity.Group
import com.samco.trackandgraph.data.database.entity.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.entity.TrackerSuggestionType
import com.samco.trackandgraph.data.database.entity.queryresponse.FunctionWithFeature
import com.samco.trackandgraph.data.database.entity.queryresponse.TrackerWithFeature

/**
 * A fake in-memory implementation of [GroupDao] for testing purposes.
 * This allows tests to run without a real database and without mocking frameworks.
 */
internal class FakeGroupDao : GroupDao {

    private var nextGroupId = 1L
    private val groups = mutableMapOf<Long, Group>()
    private val trackers = mutableMapOf<Long, TrackerWithFeature>()
    private val functions = mutableMapOf<Long, FunctionWithFeature>()

    val deletedFeatureIds = mutableSetOf<Long>()
    val deletedGraphIds = mutableSetOf<Long>()
    val deletedReminderIds = mutableSetOf<Long>()

    // =========================================================================
    // Test helper methods
    // =========================================================================

    fun addTracker(trackerId: Long, featureId: Long) {
        trackers[trackerId] = TrackerWithFeature(
            id = trackerId,
            name = "Tracker $trackerId",
            featureId = featureId,
            description = "",
            dataType = DataType.CONTINUOUS,
            hasDefaultValue = false,
            defaultValue = 0.0,
            defaultLabel = "",
            suggestionType = TrackerSuggestionType.NONE,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    }

    fun addFunction(functionId: Long, featureId: Long) {
        functions[functionId] = FunctionWithFeature(
            id = functionId,
            featureId = featureId,
            functionGraph = "{}",
            name = "Function $functionId",
            description = ""
        )
    }

    fun clear() {
        groups.clear()
        trackers.clear()
        functions.clear()
        deletedFeatureIds.clear()
        deletedGraphIds.clear()
        deletedReminderIds.clear()
        nextGroupId = 1L
    }

    // =========================================================================
    // GroupDao implementation
    // =========================================================================

    override fun insertGroup(group: Group): Long {
        val id = if (group.id == 0L) nextGroupId++ else group.id
        groups[id] = group.copy(id = id)
        return id
    }

    override fun updateGroup(group: Group) {
        groups[group.id] = group
    }

    override fun deleteGroup(id: Long) {
        groups.remove(id)
    }

    override fun getGroupById(id: Long): Group? = groups[id]

    override fun getAllGroupsSync(): List<Group> = groups.values.toList()

    override fun getGroupsForGroupSync(id: Long): List<Group> {
        // This would need group items to work properly, simplified for tests
        return emptyList()
    }

    override fun hasAnyGroups(): Boolean = groups.isNotEmpty()

    override fun getTrackerById(trackerId: Long): TrackerWithFeature? = trackers[trackerId]

    override fun getFunctionById(functionId: Long): FunctionWithFeature? = functions[functionId]

    override fun deleteFeature(id: Long) {
        deletedFeatureIds.add(id)
        // Remove trackers/functions with this feature
        trackers.entries.removeIf { it.value.featureId == id }
        functions.entries.removeIf { it.value.featureId == id }
    }

    override fun deleteGraphOrStat(id: Long) {
        deletedGraphIds.add(id)
    }

    override fun deleteReminder(id: Long) {
        deletedReminderIds.add(id)
    }
}
