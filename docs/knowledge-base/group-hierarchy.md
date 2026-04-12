---
title: Group DAG structure and symlinks
description: Groups form a DAG (not a tree) — a group can appear in multiple parent groups via symlinks; deletion must handle shared membership; GroupGraph and FeaturePathProvider resolve display paths. DTOs never carry hierarchy info.
topics:
  - Groups can have multiple parents (symlink-like, via group_items_table)
  - Structure must remain acyclic (no circular references)
  - Deletion: remove only GroupItem if group exists in other parents; recursive delete if deleting entirely
  - GroupGraph: tree built from DAG by DataInteractorImpl.buildGroupGraph(); same node may appear at multiple positions
  - FeaturePathProvider: resolves display paths; collapses multi-path items with "..."
  - ComponentPathProvider: returns all full paths for a component; used by symlinks dialog
  - DTOs do NOT carry hierarchy info (no groupId/parentGroupIds/displayIndex fields)
keywords: [group, DAG, symlink, hierarchy, acyclic, parent, child, GroupGraph, FeaturePathProvider, ComponentPathProvider, deletion, GroupHelperImpl, path-resolution, SymlinksDialog]
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

### GroupGraphItem carries groupItemId

Each `GroupGraphItem` node carries a `groupItemId` — the globally unique placement ID from `group_items_table`. This is critical because:

- Entity IDs (tracker.id, graph.id, etc.) are only unique within their own type's table, so a tracker and a graph can share the same numeric ID.
- Even `(childId, type)` pairs are **not unique within a single group** — the same component can be placed in the same group multiple times (same-group duplicates, see [group-items.md](group-items.md)).
- `groupItemId` is the only universally unique identifier for a specific placement of a component.

`buildGroupGraph()` fetches `groupItemDao.getGroupItemsForGroup()` at each level to resolve these IDs. When iterating `GroupGraphItem` children, always use `child.groupItemId` for identity (e.g. as list keys), never entity IDs.

There are two path provider classes (both in `app/app/.../util/`), each walking the `GroupGraph` but serving different purposes:

**`FeaturePathProvider`** — for display in dropdowns and selectors where a single string per item is needed:
- Collects all paths for each group ID and feature ID (trackers/functions only, not graphs)
- Single-path items get a full path like `/Health/Exercise/Steps`
- Multi-path items get a collapsed path with `...` showing common prefix/suffix: `/Health/.../Steps`
- Results are lazily cached per ID
- Used by: graph config screens, function editor, notes, select-item dialogs

**`ComponentPathProvider`** — for listing all locations of a symlinked component:
- Collects all paths for all component types including graphs (by primary key ID, not featureId)
- Returns every path individually as a `List<String>` — no collapsing
- Used by: `SymlinksDialogViewModel` to show the symlinks dialog from context menus

The separation exists because `FeaturePathProvider` intentionally deduplicates/collapses paths (desirable for selectors) while `ComponentPathProvider` must show every location (the whole point of the symlinks dialog). `FeaturePathProvider` also indexes by featureId (for trackers/functions), while `ComponentPathProvider` indexes by primary key ID (matching `group_items_table.child_id`).

**Path deduplication for same-group duplicates**: Both providers call `.distinct()` on collected paths to handle same-group duplicate placements. Without this, a component placed twice in the same group would produce two identical path entries, causing `FeaturePathProvider` to show a misleading collapsed path (e.g. `/.../Steps` instead of just `/Steps`) and `ComponentPathProvider` to list the same path twice in the symlinks dialog.

**Important**: Group and Feature DTOs do **not** carry hierarchy information (`groupId`, `parentGroupIds`, `displayIndex`). All hierarchy is expressed through `group_items_table` and `GroupGraph`. Do not add hierarchy fields to DTOs.

## Finding Code

Search for `GroupHelperImpl` (CRUD + recursive deletion), `GroupItemDao` (relationship queries), `FeaturePathProvider` (collapsed path resolution), and `ComponentPathProvider` (full path listing).
