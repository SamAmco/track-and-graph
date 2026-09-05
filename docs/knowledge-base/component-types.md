---
title: Component types — Trackers, Functions, Graphs, Reminders, Groups
description: The 5 component types, their dual-ID pattern, the unique field used by symlink-capable DTOs, the reminder placement exception, and GroupItemType vs GroupChildType enums.
topics:
  - 5 types: TRACKER, FUNCTION, GRAPH, REMINDER, GROUP (GroupItemType enum)
  - Dual IDs: tracker/function have both a primary key and featureId; group_items_table.child_id uses the primary key NOT featureId
  - unique field: derived from placement count for symlink-capable types; reminders always use true
  - GroupItemType (internal/entity layer) vs GroupChildType (public/DTO/UI layer) — 1:1 mapping
keywords: [tracker, function, graph, reminder, group, featureId, primaryKey, component, GroupItemType, GroupChildType, unique, symlink, dual-id]
---

# Component Types

Track & Graph has five component types: TRACKER, FUNCTION, GRAPH, REMINDER, GROUP (see `GroupItemType` enum).

## Dual-ID Pattern — Critical

Trackers and Functions each have **two IDs**: a primary key (`id`) and a `featureId` (FK into `features_table`). These are different values. `group_items_table.child_id` always stores the **primary key**, not the featureId. See [group-items.md](group-items.md).

## Type Enums

There are two parallel type enums with a 1:1 mapping:
- **`GroupItemType`** (internal, entity layer) — used in `group_items_table`
- **`GroupChildType`** (public, DTO layer) — used in UI code (`GroupChild`, `GroupChildDisplayIndex`)

`GroupChildType` exists to avoid exposing the internal entity enum to the UI layer.

## Component Hierarchy Rules

Trackers, functions, and graphs:
- Are placed into groups via `group_items_table`
- Can exist in multiple groups simultaneously (symlinks)
- Have their display order stored in `GroupItem.displayIndex`
- Do NOT store `groupId` or `displayIndex` on their own entity/DTO

Reminders are special: they always have a null-group placement for the global Reminders screen and
may have one additional placement in a single group. These independently ordered rows are not
symlinks, and reminders cannot be selected by the Add Symlink flow. See
[reminders.md](reminders.md).
Groups can contain any component type including other groups — see [group-hierarchy.md](group-hierarchy.md).

### `unique` Field on DTOs

All component DTOs that can appear in the group screen carry a `unique: Boolean` field. For
symlink-capable types it is `true` if the component has exactly one row in `group_items_table` and
`false` if symlinked elsewhere. Reminder DTOs always use `true`; their required global placement
and optional group placement are not symlinks.

**Design decisions:**
- **No default value** — intentionally forces the compiler to catch any call site that forgets to compute it. For UI previews or tests unrelated to uniqueness, pass `unique = true` explicitly.
- **UI uses** (all triggered when `unique == false`):
  - **Symlink icon**: A link icon (`SymlinkIcon` composable in the group package) is shown in the top-left corner of each card. Each card composable (Tracker, Function, Group, GraphStatCardView) checks `!unique` and overlays the icon at `Alignment.TopStart`. See [card-composables.md](card-composables.md) for the shared card API shape.
  - **Delete dialog**: non-unique → "delete everywhere or just here?" dialog. "Remove from this group" sets `deleteEverywhere = false` in `ComponentDeleteRequest` (removes only that GroupItem link); "delete everywhere" sets `deleteEverywhere = true` (full delete). Note: if the component happens to be unique (only one GroupItem), `deleteEverywhere = false` still deletes the underlying component. See [helper-classes.md](helper-classes.md) for the data layer delete pattern.
  - **Symlinks context menu item**: non-unique symlink-capable components expose a "Symlinks" item. Reminders never do. The dialog uses `SymlinksDialogViewModel` (fetches `GroupGraph`, builds `ComponentPathProvider`) — see [group-hierarchy.md](group-hierarchy.md) for path provider details.
- **`DisplayFunction`** is a UI-layer DTO (search `Function.kt` in the group package), unlike other group screen DTOs which come from the data layer. Its `unique` field is populated from the data-layer `Function` DTO.
- For graphs/stats, `unique` lives on the `GraphOrStat` DTO (accessed via `graphStatViewData.graphOrStat.unique`), not on `IGraphStatViewData` itself.
