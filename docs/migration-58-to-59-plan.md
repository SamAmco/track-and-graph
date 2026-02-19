# Migration Plan: Database Version 58 to 59 (GroupItem Junction Table)

This document outlines the code changes required to complete the migration from database version 58 to 59, which introduces the `group_items_table` junction table to enable the "symlinks" feature.

## Overview

### What Changed (Already Done)
The following entity changes have already been made:
- Removed `groupId` and `displayIndex` from `Feature` entity (`app/data/src/main/java/com/samco/trackandgraph/data/database/entity/Feature.kt`)
- Removed `groupId` and `displayIndex` from `GraphOrStat` entity (`app/data/src/main/java/com/samco/trackandgraph/data/database/entity/GraphOrStat.kt`)
- Removed `parentGroupId` and `displayIndex` from `Group` entity (`app/data/src/main/java/com/samco/trackandgraph/data/database/entity/Group.kt`)
- Removed `groupId` and `displayIndex` from `Reminder` entity (`app/data/src/main/java/com/samco/trackandgraph/data/database/entity/Reminder.kt`)
- Added new `GroupItem` entity (`app/data/src/main/java/com/samco/trackandgraph/data/database/entity/GroupItem.kt`)
- Updated database version to 59 in `TrackAndGraphDatabase.kt`
- Created placeholder `Migration58to59.kt`

### GroupItem Entity Structure
```kotlin
// File: app/data/src/main/java/com/samco/trackandgraph/data/database/entity/GroupItem.kt
@Entity(tableName = "group_items_table")
internal data class GroupItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long = 0,

    @ColumnInfo(name = "group_id")
    val groupId: Long?,           // The parent group (null for reminders without a group)

    @ColumnInfo(name = "display_index")
    val displayIndex: Int,

    @ColumnInfo(name = "child_id")
    val childId: Long,            // ID of the item in its entity table

    @ColumnInfo(name = "type")
    val type: GroupItemType,      // FEATURE, GRAPH, GROUP, or REMINDER

    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0       // Epoch millis, 0 for legacy migrated data
)

enum class GroupItemType {
    FEATURE, GRAPH, GROUP, REMINDER
}
```

### Key Patterns
1. **Creation**: Insert entity first, then create a `GroupItem` entry with `System.currentTimeMillis()`
2. **Query**: JOIN `group_items_table` to get `groupId`/`displayIndex`
3. **Update display order**: Modify `group_items_table`, not entity tables
4. **Move between groups**: Update `GroupItem.groupId`

---

## Phase 1: Create GroupItemDao

### Task
Create a new DAO interface for the `GroupItem` entity.

### File to Create
`app/data/src/main/java/com/samco/trackandgraph/data/database/GroupItemDao.kt`

### Complete File Contents
```kotlin
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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType

@Dao
internal interface GroupItemDao {

    @Insert
    fun insertGroupItem(groupItem: GroupItem): Long

    @Update
    fun updateGroupItem(groupItem: GroupItem)

    @Update
    fun updateGroupItems(groupItems: List<GroupItem>)

    @Query("DELETE FROM group_items_table WHERE id = :id")
    fun deleteGroupItem(id: Long)

    @Query("DELETE FROM group_items_table WHERE child_id = :childId AND type = :type")
    fun deleteGroupItemsByChild(childId: Long, type: GroupItemType)

    @Query("SELECT * FROM group_items_table WHERE child_id = :childId AND type = :type")
    fun getGroupItemsForChild(childId: Long, type: GroupItemType): List<GroupItem>

    @Query("SELECT * FROM group_items_table WHERE group_id = :groupId ORDER BY display_index ASC")
    fun getGroupItemsForGroup(groupId: Long): List<GroupItem>

    @Query("SELECT * FROM group_items_table WHERE group_id IS NULL ORDER BY display_index ASC")
    fun getGroupItemsWithNoGroup(): List<GroupItem>

    @Query("SELECT * FROM group_items_table WHERE group_id IS NULL AND type = :type ORDER BY display_index ASC")
    fun getGroupItemsWithNoGroupByType(type: GroupItemType): List<GroupItem>

    @Query("SELECT * FROM group_items_table WHERE group_id = :groupId AND type = :type ORDER BY display_index ASC")
    fun getGroupItemsByType(groupId: Long, type: GroupItemType): List<GroupItem>

    @Query("SELECT * FROM group_items_table WHERE (group_id = :groupId OR (:groupId IS NULL AND group_id IS NULL)) AND type = :type ORDER BY display_index ASC")
    fun getGroupItemsByTypeNullable(groupId: Long?, type: GroupItemType): List<GroupItem>

    @Query("SELECT MAX(display_index) FROM group_items_table WHERE group_id = :groupId")
    fun getMaxDisplayIndexForGroup(groupId: Long): Int?

    @Query("SELECT MAX(display_index) FROM group_items_table WHERE group_id IS NULL")
    fun getMaxDisplayIndexForNullGroup(): Int?

    @Query("UPDATE group_items_table SET display_index = display_index + 1 WHERE group_id = :groupId")
    fun shiftDisplayIndexesDown(groupId: Long)

    @Query("UPDATE group_items_table SET display_index = display_index + 1 WHERE group_id IS NULL")
    fun shiftDisplayIndexesDownForNullGroup()

    @Query("SELECT * FROM group_items_table WHERE group_id = :groupId AND child_id = :childId AND type = :type LIMIT 1")
    fun getGroupItem(groupId: Long, childId: Long, type: GroupItemType): GroupItem?

    @Query("SELECT * FROM group_items_table WHERE group_id IS NULL AND child_id = :childId AND type = :type LIMIT 1")
    fun getGroupItemWithNullGroup(childId: Long, type: GroupItemType): GroupItem?
}
```

### File to Modify
`app/data/src/main/java/com/samco/trackandgraph/data/database/TrackAndGraphDatabase.kt`

### Change Required
Add the abstract property for the new DAO. Find this line:
```kotlin
internal abstract val trackAndGraphDatabaseDao: TrackAndGraphDatabaseDao
```

Add after it:
```kotlin
internal abstract val groupItemDao: GroupItemDao
```

### Verification
- File compiles without errors
- Run `./gradlew :app:data:compileDebugKotlin` to verify

---

## Phase 2: Update Query Response Classes

These are data classes used to hold results from complex JOIN queries. They currently reference `group_id` and `display_index` columns that no longer exist in the source tables.

### 2.1 TrackerWithFeature.kt

**File:** `app/data/src/main/java/com/samco/trackandgraph/data/database/entity/queryresponse/TrackerWithFeature.kt`

**Current problematic code (lines 16-17, 22-23, 46-52):**
```kotlin
@ColumnInfo(name = "group_id")
val groupId: Long,

@ColumnInfo(name = "display_index")
val displayIndex: Int,

fun toFeatureEntity() = Feature(
    id = featureId,
    name = name,
    groupId = groupId,           // ERROR: Feature no longer has groupId
    displayIndex = displayIndex, // ERROR: Feature no longer has displayIndex
    description = description
)
```

**Replace the entire file with:**
```kotlin
package com.samco.trackandgraph.data.database.entity.queryresponse

import androidx.room.ColumnInfo
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.database.entity.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.entity.TrackerSuggestionType

internal data class TrackerWithFeature(
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "feature_id")
    val featureId: Long,

    @ColumnInfo(name = "feature_description")
    val description: String,

    @ColumnInfo(name = "type")
    val dataType: DataType,

    @ColumnInfo(name = "has_default_value")
    val hasDefaultValue: Boolean,

    @ColumnInfo(name = "default_value")
    val defaultValue: Double,

    @ColumnInfo(name = "default_label")
    val defaultLabel: String,

    @ColumnInfo(name = "suggestion_type")
    val suggestionType: TrackerSuggestionType,

    @ColumnInfo(name = "suggestion_order")
    val suggestionOrder: TrackerSuggestionOrder,

    // These come from group_items_table via JOIN
    @ColumnInfo(name = "gi_group_id")
    val groupId: Long?,

    @ColumnInfo(name = "gi_display_index")
    val displayIndex: Int
) {
    fun toFeatureEntity() = Feature(
        id = featureId,
        name = name,
        description = description
    )
}
```

### 2.2 DisplayTracker.kt

**File:** `app/data/src/main/java/com/samco/trackandgraph/data/database/entity/queryresponse/DisplayTracker.kt`

**Current problematic code (lines 37-38, 58-59):**
```kotlin
@ColumnInfo(name = "group_id")
val groupId: Long,

@ColumnInfo(name = "display_index")
val displayIndex: Int,
```

**Replace the entire file with:**
```kotlin
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

package com.samco.trackandgraph.data.database.entity.queryresponse

import androidx.room.ColumnInfo
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

internal data class DisplayTracker(
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "feature_id")
    var featureId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val featureType: DataType = DataType.CONTINUOUS,

    @ColumnInfo(name = "has_default_value")
    val hasDefaultValue: Boolean,

    @ColumnInfo(name = "default_value")
    val defaultValue: Double,

    @ColumnInfo(name = "default_label")
    val defaultLabel: String,

    @ColumnInfo(name = "last_epoch_milli")
    val lastEpochMilli: Long,

    @ColumnInfo(name = "last_utc_offset_sec")
    val lastUtcOffsetSec: Int,

    @ColumnInfo(name = "feature_description")
    val description: String,

    @ColumnInfo(name = "start_instant")
    val timerStartInstant: Instant?,

    // These come from group_items_table via JOIN
    @ColumnInfo(name = "gi_group_id")
    val groupId: Long?,

    @ColumnInfo(name = "gi_display_index")
    val displayIndex: Int
) {
    fun toDto() = DisplayTracker(
        id = id,
        featureId = featureId,
        name = name,
        groupId = groupId,
        dataType = featureType,
        hasDefaultValue = hasDefaultValue,
        defaultValue = defaultValue,
        defaultLabel = defaultLabel,
        timestamp = lastEpochMilli.takeIf { it != 0L }?.let {
            OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(it),
                ZoneOffset.ofTotalSeconds(lastUtcOffsetSec)
            )
        },
        displayIndex = displayIndex,
        description = description,
        timerStartInstant = timerStartInstant
    )
}
```

### 2.3 FunctionWithFeature.kt

**File:** `app/data/src/main/java/com/samco/trackandgraph/data/database/entity/queryresponse/FunctionWithFeature.kt`

**Current problematic code (lines 35-39, 51-53):**
```kotlin
@ColumnInfo(name = "group_id")
val groupId: Long,

@ColumnInfo(name = "display_index")
val displayIndex: Int,

// In toDto():
groupIds = setOf(groupId),
displayIndex = displayIndex,
```

**Replace the entire file with:**
```kotlin
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

package com.samco.trackandgraph.data.database.entity.queryresponse

import androidx.room.ColumnInfo

internal data class FunctionWithFeature(
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "feature_id")
    val featureId: Long,

    @ColumnInfo(name = "function_graph")
    val functionGraph: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "feature_description")
    val description: String,

    // These come from group_items_table via JOIN
    @ColumnInfo(name = "gi_group_id")
    val groupId: Long?,

    @ColumnInfo(name = "gi_display_index")
    val displayIndex: Int
) {
    fun toDto(
        functionGraphDto: com.samco.trackandgraph.data.database.dto.FunctionGraph,
        inputFeatures: List<Long>,
    ) = com.samco.trackandgraph.data.database.dto.Function(
        id = id,
        featureId = featureId,
        name = name,
        groupIds = groupId?.let { setOf(it) } ?: emptySet(),
        displayIndex = displayIndex,
        description = description,
        functionGraph = functionGraphDto,
        inputFeatureIds = inputFeatures
    )
}
```

### Verification
- All three files compile without errors
- Run `./gradlew :app:data:compileDebugKotlin`

---

## Phase 3: Update TrackAndGraphDatabaseDao Queries

**File:** `app/data/src/main/java/com/samco/trackandgraph/data/database/TrackAndGraphDatabaseDao.kt`

All queries that reference `group_id` or `display_index` from the old entity tables must be updated to JOIN with `group_items_table`.

### 3.1 Update getTrackersQuery constant (lines 55-71)

**Find this code:**
```kotlin
private const val getTrackersQuery = """
    SELECT
        features_table.name as name,
        features_table.group_id as group_id,
        features_table.display_index as display_index,
        features_table.feature_description as feature_description,
        trackers_table.id as id,
        trackers_table.feature_id as feature_id,
        trackers_table.type as type,
        trackers_table.has_default_value as has_default_value,
        trackers_table.default_value as default_value,
        trackers_table.default_label as default_label,
        trackers_table.suggestion_type as suggestion_type,
        trackers_table.suggestion_order as suggestion_order
    FROM trackers_table
    LEFT JOIN features_table ON trackers_table.feature_id = features_table.id
            """
```

**Replace with:**
```kotlin
private const val getTrackersQuery = """
    SELECT
        features_table.name as name,
        features_table.feature_description as feature_description,
        trackers_table.id as id,
        trackers_table.feature_id as feature_id,
        trackers_table.type as type,
        trackers_table.has_default_value as has_default_value,
        trackers_table.default_value as default_value,
        trackers_table.default_label as default_label,
        trackers_table.suggestion_type as suggestion_type,
        trackers_table.suggestion_order as suggestion_order,
        gi.group_id as gi_group_id,
        gi.display_index as gi_display_index
    FROM trackers_table
    LEFT JOIN features_table ON trackers_table.feature_id = features_table.id
    LEFT JOIN group_items_table gi ON features_table.id = gi.child_id AND gi.type = 'FEATURE'
            """
```

### 3.2 Update getDisplayTrackersQuery constant (lines 73-104)

**Find this code:**
```kotlin
private const val getDisplayTrackersQuery = """
    SELECT
        features_table.name as name,
        features_table.group_id as group_id,
        features_table.display_index as display_index,
        features_table.feature_description as feature_description,
        ...
```

**Replace with:**
```kotlin
private const val getDisplayTrackersQuery = """
    SELECT
        features_table.name as name,
        features_table.feature_description as feature_description,
        trackers_table.id as id,
        trackers_table.feature_id as feature_id,
        trackers_table.type as type,
        trackers_table.has_default_value as has_default_value,
        trackers_table.default_value as default_value,
        trackers_table.default_label as default_label,
        last_epoch_milli,
        last_utc_offset_sec,
        start_instant,
        gi.group_id as gi_group_id,
        gi.display_index as gi_display_index
        FROM (
            trackers_table
            LEFT JOIN features_table ON trackers_table.feature_id = features_table.id
            LEFT JOIN group_items_table gi ON features_table.id = gi.child_id AND gi.type = 'FEATURE'
            LEFT JOIN (
                SELECT feature_id, epoch_milli as last_epoch_milli, utc_offset_sec as last_utc_offset_sec
                FROM data_points_table as dpt
                INNER JOIN (
                    SELECT feature_id as fid, MAX(epoch_milli) as max_epoch_milli
                    FROM data_points_table
                    GROUP BY feature_id
                ) as max_data ON max_data.fid = dpt.feature_id AND dpt.epoch_milli = max_data.max_epoch_milli
            ) as last_data ON last_data.feature_id = trackers_table.feature_id
            LEFT JOIN (
                SELECT * FROM feature_timers_table
            ) as timer_data ON timer_data.feature_id = trackers_table.feature_id
        )
    """
```

### 3.3 Update getDisplayTrackersForGroupSync (line 156-157)

**Find:**
```kotlin
@Query("$getDisplayTrackersQuery WHERE group_id = :groupId ORDER BY features_table.display_index ASC, id DESC")
fun getDisplayTrackersForGroupSync(groupId: Long): List<DisplayTracker>
```

**Replace with:**
```kotlin
@Query("$getDisplayTrackersQuery WHERE gi.group_id = :groupId ORDER BY gi.display_index ASC, trackers_table.id DESC")
fun getDisplayTrackersForGroupSync(groupId: Long): List<DisplayTracker>
```

### 3.4 Update getFeaturesForGroupSync (line 159-160)

**Find:**
```kotlin
@Query("SELECT features_table.* FROM features_table WHERE group_id = :groupId ORDER BY features_table.display_index ASC")
fun getFeaturesForGroupSync(groupId: Long): List<Feature>
```

**Replace with:**
```kotlin
@Query("""
    SELECT f.*
    FROM features_table f
    INNER JOIN group_items_table gi ON f.id = gi.child_id AND gi.type = 'FEATURE'
    WHERE gi.group_id = :groupId
    ORDER BY gi.display_index ASC
""")
fun getFeaturesForGroupSync(groupId: Long): List<Feature>
```

### 3.5 Update getTrackersForGroupSync (lines 162-182)

**Find:**
```kotlin
@Query(
    """
        SELECT
            features_table.name as name,
            features_table.group_id as group_id,
            features_table.display_index as display_index,
            features_table.feature_description as feature_description,
            trackers_table.id as id,
            trackers_table.feature_id as feature_id,
            trackers_table.type as type,
            trackers_table.has_default_value as has_default_value,
            trackers_table.default_value as default_value,
            trackers_table.default_label as default_label,
            trackers_table.suggestion_order as suggestion_order,
            trackers_table.suggestion_type as suggestion_type
        FROM trackers_table
        LEFT JOIN features_table ON features_table.id = trackers_table.feature_id
        WHERE features_table.group_id = :groupId ORDER BY features_table.display_index ASC
    """
)
fun getTrackersForGroupSync(groupId: Long): List<TrackerWithFeature>
```

**Replace with:**
```kotlin
@Query(
    """
        SELECT
            features_table.name as name,
            features_table.feature_description as feature_description,
            trackers_table.id as id,
            trackers_table.feature_id as feature_id,
            trackers_table.type as type,
            trackers_table.has_default_value as has_default_value,
            trackers_table.default_value as default_value,
            trackers_table.default_label as default_label,
            trackers_table.suggestion_order as suggestion_order,
            trackers_table.suggestion_type as suggestion_type,
            gi.group_id as gi_group_id,
            gi.display_index as gi_display_index
        FROM trackers_table
        LEFT JOIN features_table ON features_table.id = trackers_table.feature_id
        LEFT JOIN group_items_table gi ON features_table.id = gi.child_id AND gi.type = 'FEATURE'
        WHERE gi.group_id = :groupId
        ORDER BY gi.display_index ASC
    """
)
fun getTrackersForGroupSync(groupId: Long): List<TrackerWithFeature>
```

### 3.6 Update getAllFeaturesSync (line 135-136)

**Find:**
```kotlin
@Query("""SELECT features_table.* FROM features_table ORDER BY display_index ASC, id DESC""")
fun getAllFeaturesSync(): List<Feature>
```

**Replace with:**
```kotlin
@Query("""
    SELECT f.*
    FROM features_table f
    LEFT JOIN group_items_table gi ON f.id = gi.child_id AND gi.type = 'FEATURE'
    ORDER BY gi.display_index ASC, f.id DESC
""")
fun getAllFeaturesSync(): List<Feature>
```

### 3.7 Update getFeaturesByIdsSync (line 187-188)

**Find:**
```kotlin
@Query("""SELECT * from features_table WHERE id IN (:featureIds) ORDER BY display_index ASC, id DESC""")
fun getFeaturesByIdsSync(featureIds: List<Long>): List<Feature>
```

**Replace with:**
```kotlin
@Query("""
    SELECT f.*
    FROM features_table f
    LEFT JOIN group_items_table gi ON f.id = gi.child_id AND gi.type = 'FEATURE'
    WHERE f.id IN (:featureIds)
    ORDER BY gi.display_index ASC, f.id DESC
""")
fun getFeaturesByIdsSync(featureIds: List<Long>): List<Feature>
```

### 3.8 Update getAllRemindersSync (lines 120-124)

**Find:**
```kotlin
@Query("""SELECT * FROM reminders_table ORDER BY display_index ASC, id DESC""")
fun getAllReminders(): Flow<List<Reminder>>

@Query("""SELECT * FROM reminders_table ORDER BY display_index ASC, id DESC""")
override fun getAllRemindersSync(): List<Reminder>
```

**Replace with:**
```kotlin
@Query("""
    SELECT r.*
    FROM reminders_table r
    LEFT JOIN group_items_table gi ON r.id = gi.child_id AND gi.type = 'REMINDER'
    ORDER BY gi.display_index ASC, r.id DESC
""")
fun getAllReminders(): Flow<List<Reminder>>

@Query("""
    SELECT r.*
    FROM reminders_table r
    LEFT JOIN group_items_table gi ON r.id = gi.child_id AND gi.type = 'REMINDER'
    ORDER BY gi.display_index ASC, r.id DESC
""")
override fun getAllRemindersSync(): List<Reminder>
```

### 3.9 Update getAllGroupsSync (lines 129-133)

**Find:**
```kotlin
@Query("""SELECT groups_table.* FROM groups_table ORDER BY display_index ASC, id DESC""")
fun getAllGroups(): Flow<List<Group>>

@Query("""SELECT groups_table.* FROM groups_table ORDER BY display_index ASC, id DESC""")
fun getAllGroupsSync(): List<Group>
```

**Replace with:**
```kotlin
@Query("""
    SELECT g.*
    FROM groups_table g
    LEFT JOIN group_items_table gi ON g.id = gi.child_id AND gi.type = 'GROUP'
    ORDER BY gi.display_index ASC, g.id DESC
""")
fun getAllGroups(): Flow<List<Group>>

@Query("""
    SELECT g.*
    FROM groups_table g
    LEFT JOIN group_items_table gi ON g.id = gi.child_id AND gi.type = 'GROUP'
    ORDER BY gi.display_index ASC, g.id DESC
""")
fun getAllGroupsSync(): List<Group>
```

### 3.10 Update getGraphsAndStatsByGroupIdSync (line 255-256)

**Find:**
```kotlin
@Query("SELECT * FROM graphs_and_stats_table2 WHERE group_id = :groupId ORDER BY display_index ASC, id DESC")
override fun getGraphsAndStatsByGroupIdSync(groupId: Long): List<GraphOrStat>
```

**Replace with:**
```kotlin
@Query("""
    SELECT g.*
    FROM graphs_and_stats_table2 g
    INNER JOIN group_items_table gi ON g.id = gi.child_id AND gi.type = 'GRAPH'
    WHERE gi.group_id = :groupId
    ORDER BY gi.display_index ASC, g.id DESC
""")
override fun getGraphsAndStatsByGroupIdSync(groupId: Long): List<GraphOrStat>
```

### 3.11 Update getAllGraphStatsSync (line 258-259)

**Find:**
```kotlin
@Query("SELECT * FROM graphs_and_stats_table2 ORDER BY display_index ASC, id DESC")
override fun getAllGraphStatsSync(): List<GraphOrStat>
```

**Replace with:**
```kotlin
@Query("""
    SELECT g.*
    FROM graphs_and_stats_table2 g
    LEFT JOIN group_items_table gi ON g.id = gi.child_id AND gi.type = 'GRAPH'
    ORDER BY gi.display_index ASC, g.id DESC
""")
override fun getAllGraphStatsSync(): List<GraphOrStat>
```

### 3.12 Update getAllDisplayNotes query (lines 261-276)

**Find the reference to `f.group_id`:**
```kotlin
LEFT JOIN groups_table as g ON f.group_id = g.id
```

**Replace with:**
```kotlin
LEFT JOIN group_items_table as gi ON f.id = gi.child_id AND gi.type = 'FEATURE'
LEFT JOIN groups_table as g ON gi.group_id = g.id
```

### 3.13 Add new method: getGroupsForGroupSync

Add this new method to the DAO:
```kotlin
@Query("""
    SELECT g.*
    FROM groups_table g
    INNER JOIN group_items_table gi ON g.id = gi.child_id AND gi.type = 'GROUP'
    WHERE gi.group_id = :parentGroupId
    ORDER BY gi.display_index ASC
""")
fun getGroupsForGroupSync(parentGroupId: Long): List<Group>
```

### Verification
- Run `./gradlew :app:data:compileDebugKotlin`
- All queries compile without SQL errors

---

## Phase 4: Update Helper Classes

Each helper class needs to be updated to:
1. Inject `GroupItemDao`
2. Create `GroupItem` entries when creating entities
3. Query `GroupItem` for `groupId`/`displayIndex` when converting to DTOs
4. Update `GroupItem` instead of entities for display order changes

### 4.1 GroupHelperImpl.kt

**File:** `app/data/src/main/java/com/samco/trackandgraph/data/interactor/GroupHelperImpl.kt`

**Current constructor:**
```kotlin
internal class GroupHelperImpl @Inject constructor(
    private val dao: TrackAndGraphDatabaseDao,
    @IODispatcher private val io: CoroutineDispatcher,
) : GroupHelper {
```

**New constructor:**
```kotlin
internal class GroupHelperImpl @Inject constructor(
    private val dao: TrackAndGraphDatabaseDao,
    private val groupItemDao: GroupItemDao,
    @IODispatcher private val io: CoroutineDispatcher,
) : GroupHelper {
```

**Add import:**
```kotlin
import com.samco.trackandgraph.data.database.GroupItemDao
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType
```

**Update insertGroup method:**

Find:
```kotlin
override suspend fun insertGroup(request: GroupCreateRequest): Long = withContext(io) {
    val group = Group(
        id = 0L,
        name = request.name,
        displayIndex = 0,
        parentGroupId = request.parentGroupId,
        colorIndex = request.colorIndex
    )
    dao.insertGroup(group)
}
```

Replace with:
```kotlin
override suspend fun insertGroup(request: GroupCreateRequest): Long = withContext(io) {
    val group = Group(
        id = 0L,
        name = request.name,
        colorIndex = request.colorIndex
    )
    val groupId = dao.insertGroup(group)

    // Create GroupItem entry for this group in its parent
    if (request.parentGroupId != null) {
        groupItemDao.shiftDisplayIndexesDown(request.parentGroupId)
    } else {
        groupItemDao.shiftDisplayIndexesDownForNullGroup()
    }

    val groupItem = GroupItem(
        groupId = request.parentGroupId,
        displayIndex = 0,
        childId = groupId,
        type = GroupItemType.GROUP,
        createdAt = System.currentTimeMillis()
    )
    groupItemDao.insertGroupItem(groupItem)

    groupId
}
```

**Update getGroupById method:**

Find:
```kotlin
override suspend fun getGroupById(id: Long): Group? = withContext(io) {
    dao.getGroupById(id).toDto()
}
```

Replace with:
```kotlin
override suspend fun getGroupById(id: Long): Group? = withContext(io) {
    val entity = dao.getGroupById(id)
    val groupItems = groupItemDao.getGroupItemsForChild(id, GroupItemType.GROUP)
    val parentGroupIds = groupItems.mapNotNull { it.groupId }.toSet()
    val displayIndex = groupItems.firstOrNull()?.displayIndex ?: 0
    entity.toDto(parentGroupIds, displayIndex)
}
```

**Update getAllGroupsSync method:**

Find:
```kotlin
override suspend fun getAllGroupsSync(): List<Group> = withContext(io) {
    dao.getAllGroupsSync().map { it.toDto() }
}
```

Replace with:
```kotlin
override suspend fun getAllGroupsSync(): List<Group> = withContext(io) {
    dao.getAllGroupsSync().map { entity ->
        val groupItems = groupItemDao.getGroupItemsForChild(entity.id, GroupItemType.GROUP)
        val parentGroupIds = groupItems.mapNotNull { it.groupId }.toSet()
        val displayIndex = groupItems.firstOrNull()?.displayIndex ?: 0
        entity.toDto(parentGroupIds, displayIndex)
    }
}
```

**Update getGroupsForGroupSync method:**

Find:
```kotlin
override suspend fun getGroupsForGroupSync(groupId: Long): List<Group> = withContext(io) {
    dao.getGroupsForGroupSync(groupId).map { it.toDto() }
}
```

Replace with:
```kotlin
override suspend fun getGroupsForGroupSync(groupId: Long): List<Group> = withContext(io) {
    dao.getGroupsForGroupSync(groupId).map { entity ->
        val groupItems = groupItemDao.getGroupItemsForChild(entity.id, GroupItemType.GROUP)
        val parentGroupIds = groupItems.mapNotNull { it.groupId }.toSet()
        val displayIndex = groupItems.firstOrNull()?.displayIndex ?: 0
        entity.toDto(parentGroupIds, displayIndex)
    }
}
```

### 4.2 TrackerHelperImpl.kt

**File:** `app/data/src/main/java/com/samco/trackandgraph/data/interactor/TrackerHelperImpl.kt`

**Add to constructor:**
```kotlin
private val groupItemDao: GroupItemDao,
```

**Add imports:**
```kotlin
import com.samco.trackandgraph.data.database.GroupItemDao
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType
```

**Update createTracker method - find the Feature creation:**
```kotlin
val feature = Feature(
    id = 0L,
    name = request.name,
    groupId = request.groupId,
    displayIndex = 0,
    description = request.description
)
val featureId = dao.insertFeature(feature)
```

**Replace with:**
```kotlin
val feature = Feature(
    id = 0L,
    name = request.name,
    description = request.description
)
val featureId = dao.insertFeature(feature)

// Create GroupItem entry
groupItemDao.shiftDisplayIndexesDown(request.groupId)
val groupItem = GroupItem(
    groupId = request.groupId,
    displayIndex = 0,
    childId = featureId,
    type = GroupItemType.FEATURE,
    createdAt = System.currentTimeMillis()
)
groupItemDao.insertGroupItem(groupItem)
```

**Update updateTracker method - find:**
```kotlin
val currentGroupId = old.groupIds.first()
val newFeature = Feature(
    id = old.featureId,
    name = request.name ?: old.name,
    groupId = currentGroupId,
    displayIndex = old.displayIndex,
    description = request.description ?: old.description
)
```

**Replace with:**
```kotlin
val newFeature = Feature(
    id = old.featureId,
    name = request.name ?: old.name,
    description = request.description ?: old.description
)
// Note: groupId and displayIndex live in GroupItem, not modified here
```

### 4.3 FunctionHelperImpl.kt

**File:** `app/data/src/main/java/com/samco/trackandgraph/data/interactor/FunctionHelperImpl.kt`

Follow the same pattern as TrackerHelperImpl:
1. Add `groupItemDao: GroupItemDao` to constructor
2. Add imports for `GroupItemDao`, `GroupItem`, `GroupItemType`
3. Update `insertFunction` to create `GroupItem` after inserting feature
4. Update `updateFunction` to remove `groupId`/`displayIndex` from Feature constructor
5. Update `duplicateFunction` to create `GroupItem` for the new feature

### 4.4 GraphHelperImpl.kt

**File:** `app/data/src/main/java/com/samco/trackandgraph/data/interactor/GraphHelperImpl.kt`

1. Add `groupItemDao: GroupItemDao` to constructor
2. Add imports
3. Update `insertGraphOrStat` private method to create `GroupItem`
4. Update `duplicateGraphOrStat` to create `GroupItem`

**Find insertGraphOrStat:**
```kotlin
private fun insertGraphOrStat(name: String, groupId: Long, type: GraphStatType): Long =
    graphDao.insertGraphOrStat(
        GraphOrStat(
            id = 0L,
            groupId = groupId,
            name = name,
            type = type,
            displayIndex = 0
        )
    )
```

**Replace with:**
```kotlin
private fun insertGraphOrStat(name: String, groupId: Long, type: GraphStatType): Long {
    val graphOrStat = GraphOrStat(
        id = 0L,
        name = name,
        type = type
    )
    val graphStatId = graphDao.insertGraphOrStat(graphOrStat)

    groupItemDao.shiftDisplayIndexesDown(groupId)
    val groupItem = GroupItem(
        groupId = groupId,
        displayIndex = 0,
        childId = graphStatId,
        type = GroupItemType.GRAPH,
        createdAt = System.currentTimeMillis()
    )
    groupItemDao.insertGroupItem(groupItem)

    return graphStatId
}
```

### 4.5 ReminderHelperImpl.kt

**File:** `app/data/src/main/java/com/samco/trackandgraph/data/interactor/ReminderHelperImpl.kt`

1. Add `groupItemDao: GroupItemDao` to constructor
2. Add imports
3. Update `createReminder` to create `GroupItem` instead of setting fields on entity
4. Update `updateReminderDisplayOrder` to update `GroupItem` instead of entity
5. Update `fromEntity` to lookup `groupId`/`displayIndex` from `GroupItem`

**Update createReminder - find:**
```kotlin
// Shift existing reminders in the same group down
val existingInGroup = reminderDao.getAllRemindersSync()
    .filter { it.groupId == request.groupId }

existingInGroup.forEach { existing ->
    reminderDao.updateReminder(existing.copy(displayIndex = existing.displayIndex + 1))
}

val entity = ReminderEntity(
    id = 0L,
    displayIndex = 0,
    alarmName = request.reminderName,
    groupId = request.groupId,
    featureId = request.featureId,
    encodedReminderParams = encodedParams
)
reminderDao.insertReminder(entity)
```

**Replace with:**
```kotlin
// Shift existing reminders via GroupItem
if (request.groupId != null) {
    groupItemDao.shiftDisplayIndexesDown(request.groupId)
} else {
    groupItemDao.shiftDisplayIndexesDownForNullGroup()
}

val entity = ReminderEntity(
    id = 0L,
    alarmName = request.reminderName,
    featureId = request.featureId,
    encodedReminderParams = encodedParams
)
val reminderId = reminderDao.insertReminder(entity)

val groupItem = GroupItem(
    groupId = request.groupId,
    displayIndex = 0,
    childId = reminderId,
    type = GroupItemType.REMINDER,
    createdAt = System.currentTimeMillis()
)
groupItemDao.insertGroupItem(groupItem)

reminderId
```

**Update fromEntity - find:**
```kotlin
private fun fromEntity(entity: ReminderEntity): Reminder? {
    val params = reminderSerializer.deserializeParams(entity.encodedReminderParams)
        ?: return null

    return Reminder(
        id = entity.id,
        displayIndex = entity.displayIndex,
        reminderName = entity.alarmName,
        groupId = entity.groupId,
        featureId = entity.featureId,
        params = params
    )
}
```

**Replace with:**
```kotlin
private fun fromEntity(entity: ReminderEntity): Reminder? {
    val params = reminderSerializer.deserializeParams(entity.encodedReminderParams)
        ?: return null

    val groupItem = groupItemDao.getGroupItemsForChild(entity.id, GroupItemType.REMINDER)
        .firstOrNull()

    return Reminder(
        id = entity.id,
        displayIndex = groupItem?.displayIndex ?: 0,
        reminderName = entity.alarmName,
        groupId = groupItem?.groupId,
        featureId = entity.featureId,
        params = params
    )
}
```

---

## Phase 5: Update DataInteractorImpl

**File:** `app/data/src/main/java/com/samco/trackandgraph/data/interactor/DataInteractorImpl.kt`

### 5.1 Add GroupItemDao to constructor

Add `private val groupItemDao: GroupItemDao` and the necessary imports.

### 5.2 Update shiftUpGroupChildIndexes method

**Find:**
```kotlin
private suspend fun shiftUpGroupChildIndexes(groupId: Long) {
    dao.getFeaturesForGroupSync(groupId).let { features ->
        dao.updateFeatures(features.map { it.copy(displayIndex = it.displayIndex + 1) })
    }
    dao.getGraphsAndStatsByGroupIdSync(groupId).let { graphs ->
        dao.updateGraphStats(graphs.map { it.copy(displayIndex = it.displayIndex + 1) })
    }
    dao.getGroupsForGroupSync(groupId).let { groups ->
        dao.updateGroups(groups.map { it.copy(displayIndex = it.displayIndex + 1) })
    }
}
```

**Replace with:**
```kotlin
private suspend fun shiftUpGroupChildIndexes(groupId: Long) {
    groupItemDao.shiftDisplayIndexesDown(groupId)
}
```

### 5.3 Update updateGroupChildOrder method

This method updates display indexes for all children in a group. It must now update `GroupItem` entries.

**Find code that does:**
```kotlin
feature.copy(displayIndex = newDisplayIndex)
graph.copy(displayIndex = newDisplayIndex)
group.copy(displayIndex = newDisplayIndex)
```

**Replace with code that updates GroupItem:**
```kotlin
val groupItem = groupItemDao.getGroupItem(groupId, childId, childType)
if (groupItem != null && groupItem.displayIndex != newDisplayIndex) {
    groupItemDao.updateGroupItem(groupItem.copy(displayIndex = newDisplayIndex))
}
```

### 5.4 Update moveComponent methods

For moving items between groups, update `GroupItem.groupId` instead of the entity.

**Pattern for TRACKER/FUNCTION:**
```kotlin
// Old: dao.updateFeature(feature.copy(groupId = request.toGroupId))
// New:
val groupItem = groupItemDao.getGroupItemsForChild(feature.id, GroupItemType.FEATURE)
    .firstOrNull { it.groupId == request.fromGroupId }
if (groupItem != null) {
    groupItemDao.updateGroupItem(groupItem.copy(groupId = request.toGroupId))
}
```

**Pattern for GROUP (with circular reference check):**
```kotlin
val groupItem = groupItemDao.getGroupItemsForChild(group.id, GroupItemType.GROUP)
    .firstOrNull { it.groupId == request.fromGroupId }

// Circular reference check
val visited = mutableSetOf<Long>()
var currentParentId = request.toGroupId
while (currentParentId != null && currentParentId != 0L) {
    if (visited.contains(currentParentId)) {
        throw IllegalArgumentException("Circular group reference detected")
    }
    visited.add(currentParentId)
    val parentGroupItem = groupItemDao.getGroupItemsForChild(currentParentId, GroupItemType.GROUP).firstOrNull()
    currentParentId = parentGroupItem?.groupId
}

if (groupItem != null) {
    groupItemDao.updateGroupItem(groupItem.copy(groupId = request.toGroupId))
}
```

---

## Phase 6: Update Test Fixtures and Tests

### 6.1 Create FakeGroupItemDao.kt

**File to create:** `app/data/src/testFixtures/kotlin/com/samco/trackandgraph/FakeGroupItemDao.kt`

```kotlin
package com.samco.trackandgraph

import com.samco.trackandgraph.data.database.GroupItemDao
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType

class FakeGroupItemDao : GroupItemDao {
    private val items = mutableListOf<GroupItem>()
    private var nextId = 1L

    override fun insertGroupItem(groupItem: GroupItem): Long {
        val id = nextId++
        items.add(groupItem.copy(id = id))
        return id
    }

    override fun updateGroupItem(groupItem: GroupItem) {
        val index = items.indexOfFirst { it.id == groupItem.id }
        if (index >= 0) items[index] = groupItem
    }

    override fun updateGroupItems(groupItems: List<GroupItem>) {
        groupItems.forEach { updateGroupItem(it) }
    }

    override fun deleteGroupItem(id: Long) {
        items.removeAll { it.id == id }
    }

    override fun deleteGroupItemsByChild(childId: Long, type: GroupItemType) {
        items.removeAll { it.childId == childId && it.type == type }
    }

    override fun getGroupItemsForChild(childId: Long, type: GroupItemType): List<GroupItem> {
        return items.filter { it.childId == childId && it.type == type }
    }

    override fun getGroupItemsForGroup(groupId: Long): List<GroupItem> {
        return items.filter { it.groupId == groupId }.sortedBy { it.displayIndex }
    }

    override fun getGroupItemsWithNoGroup(): List<GroupItem> {
        return items.filter { it.groupId == null }.sortedBy { it.displayIndex }
    }

    override fun getGroupItemsWithNoGroupByType(type: GroupItemType): List<GroupItem> {
        return items.filter { it.groupId == null && it.type == type }.sortedBy { it.displayIndex }
    }

    override fun getGroupItemsByType(groupId: Long, type: GroupItemType): List<GroupItem> {
        return items.filter { it.groupId == groupId && it.type == type }.sortedBy { it.displayIndex }
    }

    override fun getGroupItemsByTypeNullable(groupId: Long?, type: GroupItemType): List<GroupItem> {
        return items.filter { it.groupId == groupId && it.type == type }.sortedBy { it.displayIndex }
    }

    override fun getMaxDisplayIndexForGroup(groupId: Long): Int? {
        return items.filter { it.groupId == groupId }.maxOfOrNull { it.displayIndex }
    }

    override fun getMaxDisplayIndexForNullGroup(): Int? {
        return items.filter { it.groupId == null }.maxOfOrNull { it.displayIndex }
    }

    override fun shiftDisplayIndexesDown(groupId: Long) {
        items.replaceAll {
            if (it.groupId == groupId) it.copy(displayIndex = it.displayIndex + 1)
            else it
        }
    }

    override fun shiftDisplayIndexesDownForNullGroup() {
        items.replaceAll {
            if (it.groupId == null) it.copy(displayIndex = it.displayIndex + 1)
            else it
        }
    }

    override fun getGroupItem(groupId: Long, childId: Long, type: GroupItemType): GroupItem? {
        return items.find { it.groupId == groupId && it.childId == childId && it.type == type }
    }

    override fun getGroupItemWithNullGroup(childId: Long, type: GroupItemType): GroupItem? {
        return items.find { it.groupId == null && it.childId == childId && it.type == type }
    }

    // Test helper
    fun clear() {
        items.clear()
        nextId = 1L
    }
}
```

### 6.2 Update existing test fixtures

Update `FakeGraphDao.kt`, `FakeReminderDao.kt`, and other fakes to:
1. Remove references to `groupId`/`displayIndex` in entity constructors
2. Work with the new entity structures

### 6.3 Update tests

For each test file that creates entities with `groupId` or `displayIndex`:
1. Remove those fields from entity constructors
2. Create corresponding `GroupItem` entries using `FakeGroupItemDao`
3. Update assertions that check `groupId`/`displayIndex`

**Files to update:**
- `TrackerHelperImpl_UpdateDataPoints_Test.kt`
- `ReminderHelperImplTest.kt`
- `DataInteractorImplTest.kt`
- `GroupViewModelTest.kt`
- `ReminderInteractorImplTest.kt`
- `CSVReadWriterImplTest.kt`
- `DependencyAnalyserTest.kt`
- `FunctionValidatorTest.kt`
- And any others that reference the changed fields

---

## Phase 7: Implement the Migration

**File:** `app/data/src/main/java/com/samco/trackandgraph/data/database/migrations/Migration58to59.kt`

**Replace the entire file with:**
```kotlin
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
package com.samco.trackandgraph.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 58 to 59: Introduce group_items_table for symlinks feature.
 */
val MIGRATION_58_59 = object : Migration(58, 59) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create the new group_items_table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS group_items_table (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                group_id INTEGER,
                display_index INTEGER NOT NULL,
                child_id INTEGER NOT NULL,
                type TEXT NOT NULL,
                created_at INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(group_id) REFERENCES groups_table(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // 2. Create indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS index_group_items_table_id ON group_items_table(id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_group_items_table_group_id ON group_items_table(group_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_group_items_table_child_id_type ON group_items_table(child_id, type)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_group_items_table_group_id_child_id_type ON group_items_table(group_id, child_id, type)")

        // 3. Migrate features_table data
        db.execSQL("""
            INSERT INTO group_items_table (group_id, display_index, child_id, type, created_at)
            SELECT group_id, display_index, id, 'FEATURE', 0
            FROM features_table
        """.trimIndent())

        // 4. Migrate graphs_and_stats_table2 data
        db.execSQL("""
            INSERT INTO group_items_table (group_id, display_index, child_id, type, created_at)
            SELECT group_id, display_index, id, 'GRAPH', 0
            FROM graphs_and_stats_table2
        """.trimIndent())

        // 5. Migrate groups_table data (parentGroupId becomes group_id, skip root group id=0)
        db.execSQL("""
            INSERT INTO group_items_table (group_id, display_index, child_id, type, created_at)
            SELECT parent_group_id, display_index, id, 'GROUP', 0
            FROM groups_table
            WHERE id != 0
        """.trimIndent())

        // 6. Migrate reminders_table data
        db.execSQL("""
            INSERT INTO group_items_table (group_id, display_index, child_id, type, created_at)
            SELECT group_id, display_index, id, 'REMINDER', 0
            FROM reminders_table
        """.trimIndent())

        // 7. Recreate features_table without group_id and display_index
        db.execSQL("""
            CREATE TABLE features_table_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                feature_description TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("INSERT INTO features_table_new (id, name, feature_description) SELECT id, name, feature_description FROM features_table")
        db.execSQL("DROP TABLE features_table")
        db.execSQL("ALTER TABLE features_table_new RENAME TO features_table")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_features_table_id ON features_table(id)")

        // 8. Recreate graphs_and_stats_table2 without group_id and display_index
        db.execSQL("""
            CREATE TABLE graphs_and_stats_table2_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                graph_stat_type INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("INSERT INTO graphs_and_stats_table2_new (id, name, graph_stat_type) SELECT id, name, graph_stat_type FROM graphs_and_stats_table2")
        db.execSQL("DROP TABLE graphs_and_stats_table2")
        db.execSQL("ALTER TABLE graphs_and_stats_table2_new RENAME TO graphs_and_stats_table2")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graphs_and_stats_table2_id ON graphs_and_stats_table2(id)")

        // 9. Recreate groups_table without parent_group_id and display_index
        db.execSQL("""
            CREATE TABLE groups_table_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                color_index INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("INSERT INTO groups_table_new (id, name, color_index) SELECT id, name, color_index FROM groups_table")
        db.execSQL("DROP TABLE groups_table")
        db.execSQL("ALTER TABLE groups_table_new RENAME TO groups_table")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_groups_table_id ON groups_table(id)")

        // 10. Recreate reminders_table without group_id and display_index
        db.execSQL("""
            CREATE TABLE reminders_table_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                feature_id INTEGER,
                encoded_reminder_params TEXT NOT NULL,
                FOREIGN KEY(feature_id) REFERENCES features_table(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("INSERT INTO reminders_table_new (id, name, feature_id, encoded_reminder_params) SELECT id, name, feature_id, encoded_reminder_params FROM reminders_table")
        db.execSQL("DROP TABLE reminders_table")
        db.execSQL("ALTER TABLE reminders_table_new RENAME TO reminders_table")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_table_id ON reminders_table(id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_table_feature_id ON reminders_table(feature_id)")
    }
}
```

---

## Phase 8: Dependency Injection Updates

**File:** `app/data/src/main/java/com/samco/trackandgraph/data/di/DataModule.kt`

### Add GroupItemDao provider

Add this method to the `DataModule` class:

```kotlin
@Provides
internal fun getGroupItemDao(database: TrackAndGraphDatabase): GroupItemDao =
    database.groupItemDao
```

Add import:
```kotlin
import com.samco.trackandgraph.data.database.GroupItemDao
```

---

## Execution Order

Execute phases in this order:

1. **Phase 1**: Create GroupItemDao (enables compilation)
2. **Phase 8**: DI Updates (enables injection)
3. **Phase 7**: Implement Migration (enables database to work)
4. **Phase 2**: Update Query Response Classes
5. **Phase 3**: Update DAO Queries
6. **Phase 4**: Update Helper Classes (in order: GroupHelper, TrackerHelper, FunctionHelper, GraphHelper, ReminderHelper)
7. **Phase 5**: Update DataInteractorImpl
8. **Phase 6**: Update Tests

### Verification After Each Phase
```bash
# Compile check
./gradlew :app:data:compileDebugKotlin

# Run tests (after Phase 6)
./gradlew test

# Run instrumented tests (after all phases)
./gradlew connectedAndroidTest
```

---

## Notes

- The root group (id=0) is special - it should NOT have a GroupItem entry pointing to it
- `ON DELETE CASCADE` on `group_items_table.group_id` ensures cleanup when parent groups are deleted
- When deleting a child entity, delete its `GroupItem` entries too (or rely on application logic)
- New items get `createdAt = System.currentTimeMillis()`, migrated items get `createdAt = 0`
