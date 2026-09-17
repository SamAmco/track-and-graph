---
title: Database backup and restore lifecycle
description: Backup checkpointing, consolidated SQLite and Room validation, and the staged restart-based restore protocol that prevents replacing a database while Room is active.
topics:
  - Backups checkpoint WAL before copying the main database file
  - Restores are staged while Room remains open and installed before Room opens in a new process
  - A persistent marker makes main-file replacement and old-sidecar cleanup restart-safe
keywords: [database, backup, restore, Room, SQLite, WAL, SHM, journal, checkpoint, staging, atomic-rename, PendingDatabaseRestore]
---

# Database Backup and Restore

The manual and automatic backup implementation checkpoints the live database's WAL before copying
the main database file. A non-zero checkpoint result fails the backup rather than producing a copy
that omits committed WAL data.

## Restore lifecycle

Never close Room's open helper and overwrite its live database file. Long-lived flows, reminder
observers, workers, or other callers may still be querying it.

Restore instead uses two phases:

1. While the current process is running, copy the selected file to a temporary file and validate it.
   A single data-layer operation owns validation and staging. A restore candidate must pass
   SQLite's full `PRAGMA integrity_check(1)` and have a positive
   `user_version` no newer than the app. Version zero is rejected because Room may treat it as a new
   database. The argument limits error output to one row while still checking the whole healthy
   database. Room then opens a private copy using the production database class and migration list,
   with destructive fallback disabled. Room itself determines whether a migration path exists,
   runs it, and performs its generated current-schema validation. There is no second hard-coded
   minimum version or table list to maintain. The migrated, validated copy is staged beside the
   real database, flushed, and accompanied by a pending marker. The live database remains open and
   unchanged.
2. On the next process start, `TrackAndGraphDatabase.getInstance` checks the marker before building
   Room. `PendingDatabaseRestore` atomically renames the staged database over the real file, removes
   sidecars belonging to the replaced database, and only then clears the marker and allows Room to
   open.

The marker intentionally outlives the main-file rename. If the process stops after that rename but
before removal of the old `-wal`, `-shm`, or `-journal`, the next process sees the marker and
finishes cleanup before Room opens. A temporary or staged file without the marker is never
installed, so an interrupted staging copy cannot affect the live database.

The staging file is in the database directory so promotion uses a same-filesystem atomic rename.
The explicit cloud-backup allowlist includes only the real database name, not restore staging
artifacts.

The normal database's configured destructive-migration policy remains unchanged. Restore
validation never enables that fallback: unsupported or structurally incompatible candidates are
rejected before the live database is replaced. This protocol does not provide a post-install
rollback system.
