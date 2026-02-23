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

### Display Index

- Determines the visual sort order of items within a group
- Stored on GroupItem, NOT on the component itself
- When inserting at the top, existing items are shifted down (`displayIndex + 1`)
- Managed by helper classes (TrackerHelper, GraphHelper, etc.)

### Multi-Group Membership

A component can exist in multiple groups:

```kotlin
// Tracker 1 in Group A
GroupItem(groupId = 1, childId = 100, type = TRACKER, displayIndex = 0)

// Same Tracker 1 also in Group B
GroupItem(groupId = 2, childId = 100, type = TRACKER, displayIndex = 3)
```

**Constraint**: A component should never appear twice in the same group (enforced by application logic).

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
// 1. Find existing GroupItem
val existing = groupItemDao.getGroupItem(oldGroupId, childId, type)

// 2. Delete old entry
groupItemDao.deleteGroupItem(existing.id)

// 3. Insert new entry (uses standard insert flow above)
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

## Key DAO Methods

```kotlin
interface GroupItemDao {
    fun getGroupItemsForGroup(groupId: Long): List<GroupItem>
    fun getGroupItemsWithNoGroup(): List<GroupItem>
    fun getGroupItemsForChild(childId: Long, type: GroupItemType): List<GroupItem>
    fun getGroupItem(groupId: Long, childId: Long, type: GroupItemType): GroupItem?
    fun shiftDisplayIndexesDown(groupId: Long)
    fun shiftDisplayIndexesDownForNullGroup()
    fun getAllGroupItems(): List<GroupItem>
}
```
