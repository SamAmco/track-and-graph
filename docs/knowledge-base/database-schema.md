---
title: Database tables and relationships
description: Room/SQLite schema overview — all core tables (groups, group_items, features, trackers, functions, graphs, reminders, data_points), their columns, and foreign key relationships.
topics:
  - groups_table: id, name, color_index
  - group_items_table: id, group_id (nullable), display_index, child_id, type, created_at
  - features_table: id, name, feature_description (base for trackers and functions)
  - trackers_table, functions_table: reference features_table (1:1, CASCADE delete)
  - data_points_table: references features_table (1:many)
  - graphs_and_stats_table, reminders_table
keywords: [database, schema, Room, SQLite, tables, columns, foreign-key, groups, features, trackers, functions, data_points, cascade]
---

# Database Schema Overview

Track & Graph uses Room (SQLite) for persistence. The database is defined in `app/data/src/main/java/com/samco/trackandgraph/data/database/`.

## Core Tables

### groups_table
Stores group metadata (like directory names).

| Column | Type | Description |
|--------|------|-------------|
| id | Long | Primary key |
| name | String | Group name |
| color_index | Int | Color theme index |

### group_items_table
Junction table placing components into groups. See [group-items.md](group-items.md).

| Column | Type | Description |
|--------|------|-------------|
| id | Long | Primary key |
| group_id | Long? | Parent group (null = root level or reminders screen) |
| display_index | Int | Sort order within the group |
| child_id | Long | ID of the component |
| type | GroupItemType | TRACKER, FUNCTION, GRAPH, REMINDER, or GROUP |
| created_at | Long | Timestamp |

### features_table
Base table for data-producing entities (trackers and functions share this).

| Column | Type | Description |
|--------|------|-------------|
| id | Long | Primary key |
| name | String | Display name |
| feature_description | String | User description |

### trackers_table
Tracker-specific configuration. References features_table.

### functions_table
Function-specific configuration (Lua graph definitions). References features_table.

### graphs_and_stats_table
Graph/statistic configurations.

### reminders_table
Reminder configurations.

### data_points_table
Actual tracked data. References features_table.

## Key Relationships

```
features_table
    ^
    |-- trackers_table (1:1, CASCADE delete)
    |-- functions_table (1:1, CASCADE delete)
    |-- data_points_table (1:many)

groups_table
    ^
    |-- group_items_table (many:many junction)

group_items_table --> trackers/functions/graphs/reminders/groups (polymorphic via type + child_id)
```

## Important Notes

- Deleting a Feature cascades to delete its Tracker or Function
- GroupItem entries must be cleaned up when deleting components
- Components can appear in multiple groups via multiple GroupItem entries
- A component should never appear twice in the same group
