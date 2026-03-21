---
title: group_items_table — junction table and display ordering
description: The group_items_table schema, composite identity (type + child_id together identify a component), display_index ordering managed by helpers, multi-group membership (including same-group duplicates), groupItemId as placement identity, symlink creation disabled-items pattern, drag-and-drop flow, and null group_id for groupless reminders.
topics:
  - Schema: id, group_id (nullable), display_index, child_id, type, created_at
  - Composite identity: (type, child_id) together identify a component — NOT child_id alone
  - child_id is the type-specific entity ID (NOT features_table.id for trackers/functions)
  - groupItemId (GroupItem.id) as placement identity: uniquely identifies WHERE a component is placed
  - Same component can appear multiple times in the same group (no unique constraint)
  - Symlink creation: AddSymlinkViewModel disables ancestors (cycle prevention) only
  - Display index: managed by helpers; UI combines via GroupViewModel flows (children not pre-sorted)
  - Drag-and-drop: temporary local list for instant UI; DB write on drop; VM waits for dbDisplayIndices alignment
  - null group_id: valid ONLY for reminders (Reminders screen)
keywords: [group_items, junction, display_index, ordering, null, groupless, child_id, composite-identity, drag-and-drop, DAO, multi-group, symlink, AddSymlinkViewModel, disabled-items, SelectItemDialog, groupItemId, placement-identity]
---

# Group Items (Junction Table)

`group_items_table` is the central junction table that places components into groups.

## Schema

```kotlin
@Entity(tableName = "group_items_table")
data class GroupItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "group_id")
    val groupId: Long?,           // null = reminders screen (for reminders only)

    @ColumnInfo(name = "display_index")
    val displayIndex: Int,        // sort order within the group

    @ColumnInfo(name = "child_id")
    val childId: Long,            // ID of the component

    @ColumnInfo(name = "type")
    val type: GroupItemType,      // TRACKER, FUNCTION, GRAPH, REMINDER, GROUP

    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0
)
```

## Key Concepts

### Composite Identity — Critical

A child is uniquely identified by the **combination of `(type, child_id)`**, not by `child_id` alone. Two different component types (e.g. a TRACKER and a GRAPH) can share the same `child_id` value and must not be confused. Any map or lookup over group items must key on both fields.

### child_id Identity — Critical

`child_id` stores the **type-specific entity ID**, NOT the `features_table.id`:

| type | child_id references |
|------|---------------------|
| TRACKER | `trackers_table.id` |
| FUNCTION | `functions_table.id` |
| GRAPH | `graphs_and_stats_table2.id` |
| GROUP | `groups_table.id` |
| REMINDER | `reminders_table.id` |

Both Trackers and Functions also have a `featureId` (pointing to `features_table`), but that is a **different ID** from the tracker/function ID. The group_items_table always uses the tracker/function primary key, not the shared feature key. Getting these confused causes items to silently end up in the wrong groups or not appear at all.

### Display Index

- Determines the visual sort order of items within a group
- Stored on GroupItem, NOT on the component itself
- When inserting at the top, existing items are shifted down (`displayIndex + 1`)
- Managed by helper classes (TrackerHelper, GraphHelper, etc.)
- **UI layer**: `GroupViewModel` fetches display indices as a separate flow via `GroupHelper.getDisplayIndicesForGroup()` and combines them with children at the `currentChildren` stage. Children are NOT sorted in the data flows — sorting happens last using O(1) map lookups.
- **Drag-and-drop**: During a drag, a temporary local list is used for instant UI updates. On drop, `GroupHelper.updateGroupChildOrder()` writes to DB, then the VM waits for `dbDisplayIndices` to align before switching back to the real flow.

### Multi-Group Membership

A component can exist in multiple groups, and even multiple times in the **same** group:

```kotlin
// Tracker 1 in Group A
GroupItem(id = 1, groupId = 1, childId = 100, type = TRACKER, displayIndex = 0)

// Same Tracker 1 also in Group B
GroupItem(id = 2, groupId = 2, childId = 100, type = TRACKER, displayIndex = 3)

// Same Tracker 1 duplicated within Group A (e.g. at a different position)
GroupItem(id = 3, groupId = 1, childId = 100, type = TRACKER, displayIndex = 5)
```

There is **no unique constraint** on `(group_id, child_id, type)` — this was removed in migration 59→60 to allow same-group duplicates. Each `GroupItem.id` (the auto-generated PK) uniquely identifies a **placement** of a component within a group. This `groupItemId` is the primary way the data layer identifies which specific symlink to operate on (delete, move, duplicate).

### Null Group ID

Only valid for **reminders**. A reminder with `groupId = null` appears in the dedicated Reminders screen but not in any group.

## Common Operations

### Insert Component into Group

```kotlin
// 1. Shift existing items down
groupItemDao.shiftDisplayIndexesDown(groupId)

// 2. Insert at position 0
groupItemDao.insertGroupItem(
    GroupItem(
        groupId = groupId,
        displayIndex = 0,
        childId = componentId,
        type = componentType,
        createdAt = timeProvider.epochMilli()
    )
)
```

### Move Component Between Groups

```kotlin
// 1. Look up GroupItem by its placement ID
val existing = groupItemDao.getGroupItemById(groupItemId)

// 2. Delete old entry
groupItemDao.deleteGroupItem(existing.id)

// 3. Insert new entry in target group (uses standard insert flow above)
```

### Delete Component from Group (Symlink Removal)

```kotlin
// Just delete the GroupItem - component survives in other groups
groupItemDao.deleteGroupItem(groupItemId)
```

### Delete Component Entirely

```kotlin
// 1. Delete all GroupItem entries for this component
groupItemDao.deleteGroupItemsByChild(childId, type)

// 2. Delete the component itself
// (handled by type-specific DAO)
```

## Symlink Creation — Disabled Items in Picker

When the user opens the "Add Symlink" dialog (`AddSymlinkViewModel`), certain items are disabled (visible but greyed out, not selectable):

1. **Ancestor groups (cycle prevention)**: The current group and all its transitive ancestors are disabled as GROUP targets, since symlinking any of them would create a cycle in the DAG. Computed via `GroupHelper.getAncestorAndSelfGroupIds()`.

Since there is no unique constraint on `(group_id, child_id, type)`, items already present in the group are **not** disabled — the same component can be symlinked into a group multiple times.

The `SelectItemDialog` supports both `hiddenItems` (completely removed from the tree) and `disabledItems` (visible but not clickable, shown at reduced alpha). The symlink flow uses `disabledItems` for ancestor groups.

## Key DAO Methods

```kotlin
interface GroupItemDao {
    fun getGroupItemById(groupItemId: Long): GroupItem?
    fun getGroupItemsForGroup(groupId: Long): List<GroupItem>
    fun getGroupItemsWithNoGroup(): List<GroupItem>
    fun getGroupItemsForChild(childId: Long, type: GroupItemType): List<GroupItem>
    fun shiftDisplayIndexesDown(groupId: Long)
    fun shiftDisplayIndexesDownForNullGroup()
    fun getAllGroupItems(): List<GroupItem>
}
```

Note: `getGroupItem(groupId, childId, type)` was removed — use `getGroupItemById(groupItemId)` instead. The old method relied on composite lookup which is no longer unique.
