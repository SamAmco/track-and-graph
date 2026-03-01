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

When you `DROP TABLE`, **no cascade deletes fire** on child tables — `ON DELETE CASCADE` only applies to row-level `DELETE` operations, not `DROP TABLE`. Foreign key constraints in child tables resolve by name, so they'll point correctly to the rebuilt table after the rename.

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
