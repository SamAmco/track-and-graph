---
title: Group DAG structure and symlinks
description: Groups form a DAG (not a tree) — a group can appear in multiple parent groups via symlinks; deletion must handle shared membership; GroupGraph and FeaturePathProvider resolve display paths. DTOs never carry hierarchy info.
topics:
  - Groups can have multiple parents (symlink-like, via group_items_table)
  - Structure must remain acyclic (no circular references)
  - Deletion: remove only GroupItem if group exists in other parents; recursive delete if deleting entirely
  - GroupGraph: tree built from DAG by DataInteractorImpl.buildGroupGraph(); same node may appear at multiple positions
  - FeaturePathProvider: resolves display paths; collapses multi-path items with "..."
  - DTOs do NOT carry hierarchy info (no groupId/parentGroupIds/displayIndex fields)
keywords: [group, DAG, symlink, hierarchy, acyclic, parent, child, GroupGraph, FeaturePathProvider, deletion, GroupHelperImpl, path-resolution]
---

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

The hierarchy is implemented via `group_items_table` — each parent-child relationship is a `GroupItem` row with `type = GROUP`. A group appearing in multiple parents simply has multiple `GroupItem` rows with the same `childId` but different `groupId` values. See [group-items.md](group-items.md).

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

## Finding Code

Search for `GroupHelperImpl` (CRUD + recursive deletion), `GroupItemDao` (relationship queries), and `FeaturePathProvider` (path resolution).
