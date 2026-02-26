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

## GroupGraph and Path Resolution

`GroupGraph` is a tree built from the DAG by `DataInteractorImpl.buildGroupGraph()`. Since a group/feature can have multiple parents, the same node may appear at multiple positions in the tree.

`FeaturePathProvider` (in `app/app/.../util/`) handles path display for **both** groups and features:
- Takes a `GroupGraph`, walks it to collect all paths for each group ID and feature ID
- Single-path items get a full path like `/Health/Exercise/Steps`
- Multi-path items get a collapsed path with `...` showing common prefix/suffix: `/Health/.../Steps`
- Results are lazily cached per ID

**Important**: Group and Feature DTOs do **not** carry hierarchy information (`groupId`, `parentGroupIds`, `displayIndex`). All hierarchy is expressed through `group_items_table` and `GroupGraph`. Do not add hierarchy fields to DTOs.

## Key Files

- `GroupHelperImpl.kt` - Group CRUD operations and recursive deletion
- `GroupItemDao.kt` - Database queries for group relationships
- `GroupItem.kt` - Entity definition
- `FeaturePathProvider.kt` - Path resolution for groups and features
