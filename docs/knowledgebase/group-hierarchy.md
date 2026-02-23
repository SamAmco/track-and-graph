# Group Hierarchy

Groups in Track & Graph form a **Directed Acyclic Graph (DAG)**, similar to a filesystem with symlinks.

## Structure

- Groups can contain other groups, creating nested hierarchies
- A group can appear in **multiple parent groups** (like a symlink)
- The structure must remain acyclic (no circular references)

## Example

```
Root
├── Health/
│   ├── Exercise/
│   │   └── [Tracker: Steps]
│   └── Sleep/
│       └── [Tracker: Hours Slept]
└── Dashboard/
    ├── Exercise/  (same group as above - symlink)
    └── [Graph: Weekly Summary]
```

In this example, the "Exercise" group appears in both "Health" and "Dashboard".

## Implementation

The hierarchy is implemented via `group_items_table`:

```kotlin
// Group A contains Group B
GroupItem(
    groupId = groupA.id,      // parent
    childId = groupB.id,      // child
    type = GroupItemType.GROUP,
    displayIndex = 0
)

// Group B also appears in Group C (symlink)
GroupItem(
    groupId = groupC.id,      // different parent
    childId = groupB.id,      // same child
    type = GroupItemType.GROUP,
    displayIndex = 1
)
```

## Deletion Behavior

When deleting a group that appears in multiple places:

1. **If deleting from a specific parent**: Only remove the GroupItem linking them. The group and its contents survive in other locations.

2. **If deleting the group entirely**: Recursively delete contents, but only delete child items if ALL their parent references are being deleted.

See `GroupHelperImpl.deleteGroup()` for the implementation:
- Collects all groups to delete via DFS
- For each item, checks if it has parents outside the deletion set
- Only deletes items that would become orphaned

## Key Files

- `GroupHelperImpl.kt` - Group CRUD operations and recursive deletion
- `GroupItemDao.kt` - Database queries for group relationships
- `GroupItem.kt` - Entity definition
