---
title: Writing Room database migrations
description: How to write Room migrations safely — always copy CREATE TABLE SQL from schema JSON, SQLite table-rebuild pattern (can't drop columns), DROP TABLE cascade pitfall, and migration 58→59 as a detailed example with child_id pitfall.
topics:
  - Always copy CREATE TABLE SQL from schema JSON using jq (never infer from entity code)
  - SQLite table rebuild: CREATE new, INSERT FROM old, DROP old, RENAME new
  - DROP TABLE does NOT trigger ON DELETE CASCADE on child tables
  - child_id must be tracker/function primary key, NOT features_table.id (use JOIN)
  - Migration 58→59: introduced group_items_table, migrated group_id/display_index columns
  - Migration 62→63: multi-output functions — nullable parent_function_id (self FK) and output_node_id on functions_table
keywords: [migration, Room, SQLite, schema, ALTER, DROP, cascade, rebuild, jq, child_id, group_items, 58-59, 59-60, 60-61, 61-62, 62-63, multi-output, parent_function_id, lua_graph_packages, configuration, unique-constraint]
---

# Database Migrations

Room migrations live in `app/data/src/main/java/com/samco/trackandgraph/data/database/migrations/`. All migrations are registered in `DatabaseMigrations.kt`.

Schema JSON snapshots are in `app/data/schemas/com.samco.trackandgraph.data.database.TrackAndGraphDatabase/`.

## Writing Migrations

### Always copy CREATE TABLE SQL from the schema JSON

Never infer `CREATE TABLE` or `CREATE INDEX` SQL by reading Room entity code — it may differ subtly (nullable constraints, FK details, column order). Instead:

```bash
jq '.database.entities[] | select(.tableName == "my_table") | .createSql' 59.json
jq '.database.entities[] | select(.tableName == "my_table") | .indices[].createSql' 59.json
```

Replace the `${TABLE_NAME}` placeholder with the actual table name.

### SQLite table rebuild pattern

SQLite doesn't support dropping columns, so to alter a table:
1. `CREATE TABLE new_name (...)`
2. `INSERT INTO new_name SELECT ... FROM old_name`
3. `DROP TABLE old_name`
4. `ALTER TABLE new_name RENAME TO old_name`
5. Recreate all indices

Do not assume child rows will survive `DROP TABLE`. With foreign keys enabled, SQLite can apply foreign-key actions while dropping the parent table. Before rebuilding a referenced table, back up and explicitly drop its child table, then recreate the child from the target schema and restore its rows after the parent has been replaced.

### Structure migrations with sub-functions

Split a complex migration into helper functions (one per table being rebuilt) rather than a single `migrate()` body. See `Migration58to59.kt` as an example.

## Migration 58 → 59 (group_items_table introduction)

This migration:
1. Created `group_items_table` as a new junction table
2. Populated it from the `group_id` / `display_index` columns that previously lived on `features_table`, `graphs_and_stats_table2`, `groups_table`, and `reminders_table`
3. Rebuilt those four tables without the now-migrated columns

### Key pitfall: feature ID vs tracker/function ID

When migrating TRACKER and FUNCTION rows into `group_items_table`, the correct `child_id` is the **tracker/function primary key**, not `features_table.id`. Use a JOIN:

```sql
-- Trackers
INSERT INTO group_items_table (group_id, display_index, child_id, type, created_at)
SELECT ft.group_id, ft.display_index, t.id, 'TRACKER', 0
FROM features_table ft
JOIN trackers_table t ON t.feature_id = ft.id

-- Functions
INSERT INTO group_items_table (group_id, display_index, child_id, type, created_at)
SELECT ft.group_id, ft.display_index, f.id, 'FUNCTION', 0
FROM features_table ft
JOIN functions_table f ON f.feature_id = ft.id
```

Storing `features_table.id` as the child_id would cause trackers to appear in wrong groups and functions to not appear at all, because the helper code looks them up by tracker/function ID.

## Migration 59 → 60 (remove unique constraint on group_items_table)

This migration drops the `(group_id, child_id, type)` unique index entirely (not just makes it non-unique). The index is not recreated — there was no performance justification for keeping it, and removing it allows the same component to appear multiple times in the same group (same-group duplicates/symlinks).

```kotlin
val MIGRATION_59_60 = object : Migration(59, 60) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS `index_group_items_table_group_id_child_id_type`")
    }
}
```

The `GroupItem` entity annotation retains only two indices: `(group_id)` and `(child_id, type)`. The old triple-column index is gone entirely.

## Migration 60 → 61 (versioned Lua graph packages)

This migration creates `lua_graph_packages_table`, keyed by (`package_id`, `version`), with the original ZIP-compatible package in an `archive` BLOB. It rebuilds `lua_graphs_table` so `script` is nullable and adds an optional composite package foreign key. Existing rows retain their inline script and receive null package columns; legacy scripts are not repackaged.

Because `lua_graph_features_table` has a cascading foreign key to `lua_graphs_table`, its rows are copied to a temporary backup and the child table is explicitly rebuilt around the parent-table replacement. The migration test must include a feature mapping so schema validation alone cannot conceal accidental child-row loss.

## Migration 61 → 62 (Lua graph instance configuration)

This migration adds nullable `configuration TEXT` to `lua_graphs_table`. It stores JSON state per graph instance rather than on `lua_graph_packages_table`, because several graphs may share the same package while using different configuration. As with every Room migration, the migration SQL and committed schema snapshot must match Room's generated schema exactly, and the instrumentation migration test should verify both inline and package-backed rows retain their source fields while configuration begins null.

## Migration 62 → 63 (functions with multiple outputs)

This migration adds two nullable columns to `functions_table`: `parent_function_id` (self-referencing foreign key, `ON DELETE CASCADE`) and `output_node_id`, plus the `index_functions_table_parent_function_id` index Room expects for the foreign key. Existing rows keep both null, which means "the primary output of my own graph", so no data is rewritten and the graph JSON needs no migration (`FunctionGraph.additionalOutputNodes` defaults to empty).

Unlike the rebuild migrations above, a plain `ALTER TABLE ... ADD COLUMN ... REFERENCES` is enough here: SQLite allows adding a foreign-key column when its default is NULL, and Room's schema validation compares the foreign key list rather than the original `CREATE TABLE` text. The instrumentation test (`Migration62to63Test`) checks existing rows and their input features survive, new rows can use the columns, deleting an owner cascades to its additional outputs, and the index exists. Run it on a device with `./gradlew :data:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.samco.trackandgraph.data.database.Migration62to63Test`.
