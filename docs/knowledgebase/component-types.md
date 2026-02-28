# Component Types

Track & Graph has five types of components that can exist within the group hierarchy.

## GroupItemType Enum

```kotlin
enum class GroupItemType {
    TRACKER,
    FUNCTION,
    GRAPH,
    REMINDER,
    GROUP
}
```

## Trackers

**Purpose**: Manual data entry points for tracking values over time.

**Tables**: `trackers_table` + `features_table` (1:1 relationship)

**Key Properties**:
- Data type (continuous, duration, discrete)
- Default value and label
- Suggestion settings

**Deletion**: Deleting a tracker deletes its Feature, which cascades to delete all DataPoints.

## Functions

**Purpose**: Computed/derived data using Lua scripts. Can reference other features as inputs.

**Tables**: `functions_table` + `features_table` (1:1 relationship)

**Key Properties**:
- Lua function graph (serialized JSON)
- Input feature IDs (dependencies)

**Deletion**: Same as trackers - deletes Feature and cascades.

## Graphs

**Purpose**: Visual representations of data (line graphs, pie charts, bar charts, histograms, statistics).

**Tables**: `graphs_and_stats_table` + type-specific tables (line_graphs, pie_charts, etc.)

**Key Properties**:
- Graph type
- Configuration (features to display, time range, styling)

**Deletion**: Simple deletion from graphs_and_stats_table.

## Reminders

**Purpose**: Scheduled notifications to prompt data entry.

**Tables**: `reminders_table`

**Key Properties**:
- Name, associated feature (optional)
- Encoded reminder parameters (schedule type, times, days)

**Special Behavior**: Can exist with `groupId = null`, appearing only in the dedicated Reminders screen. See [reminders.md](reminders.md).

## Groups

**Purpose**: Organizational containers (like folders/directories).

**Tables**: `groups_table`

**Key Properties**:
- Name, color index

**Special Behavior**: Can contain any component type including other groups. See [group-hierarchy.md](group-hierarchy.md).

## Type Enums

There are two type enums:

- **`GroupItemType`** (internal, entity layer): `TRACKER`, `FUNCTION`, `GRAPH`, `REMINDER`, `GROUP` — used in the `group_items_table`
- **`GroupChildType`** (public, DTO layer): `TRACKER`, `FUNCTION`, `GRAPH`, `REMINDER`, `GROUP` — used in UI code (`GroupChild`, `GroupChildDisplayIndex`)

These are 1:1 mappings. `GroupChildType` exists to avoid exposing the internal entity enum.

## Common Patterns

All components (except groups themselves):
- Are placed into groups via `group_items_table`
- Can exist in multiple groups simultaneously
- Have their display order stored in `GroupItem.displayIndex`
- Do NOT store `groupId` or `displayIndex` on their own entity/DTO
