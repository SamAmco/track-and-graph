---
title: Component types — Trackers, Functions, Graphs, Reminders, Groups
description: The 5 component types, their dual-ID pattern (primary key vs featureId), the unique field on DTOs for symlink detection (no default — must always be computed), and GroupItemType vs GroupChildType enums.
topics:
  - 5 types: TRACKER, FUNCTION, GRAPH, REMINDER, GROUP (GroupItemType enum)
  - Dual IDs: tracker/function have both a primary key and featureId; group_items_table.child_id uses the primary key NOT featureId
  - unique field on DTOs: true if component has exactly one GroupItem row, false if symlinked elsewhere
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

All components (except groups themselves):
- Are placed into groups via `group_items_table`
- Can exist in multiple groups simultaneously (symlinks)
- Have their display order stored in `GroupItem.displayIndex`
- Do NOT store `groupId` or `displayIndex` on their own entity/DTO

Reminders are special: they can exist with `groupId = null` (groupless). See [reminders.md](reminders.md). Groups can contain any component type including other groups — see [group-hierarchy.md](group-hierarchy.md).

### `unique` Field on DTOs

All component DTOs that can appear in the group screen carry a `unique: Boolean` field. It is `true` if the component has exactly one row in `group_items_table`, `false` if symlinked elsewhere. Each helper computes this via `groupItemDao.getGroupItemsForChild(id, type).size == 1` — search for `isTrackerUnique`, `isFunctionUnique`, etc.

**Design decisions:**
- **No default value** — intentionally forces the compiler to catch any call site that forgets to compute it. For UI previews or tests unrelated to uniqueness, pass `unique = true` explicitly.
- **UI flow**: unique → simple "are you sure?" delete dialog; non-unique → "delete everywhere or just here?" dialog. "Remove from this group" passes the current `groupId` to the data layer (removes only that GroupItem link); "delete everywhere" passes `null` (full delete). See [helper-classes.md](helper-classes.md) for the data layer delete pattern.
- **`DisplayFunction`** is a UI-layer DTO (search `Function.kt` in the group package), unlike other group screen DTOs which come from the data layer. Its `unique` field is populated from the data-layer `Function` DTO.
- For GROUP deletion, the parameter is named `parentGroupId` (in both `GroupDeleteRequest` and `GroupViewModel.onDeleteGroup`).
