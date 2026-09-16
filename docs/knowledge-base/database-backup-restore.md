---
title: Database backup and restore lifecycle
description: Backup checkpointing and the staged, restart-based restore protocol that prevents replacing a database while Room is active.
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

1. While the current process is running, copy the selected file to a temporary file and perform the
   existing backup validation. `TrackAndGraphDatabase.stageRestore` then copies it to a private
   staging file beside the real database, flushes it, and creates a pending marker. The live
   database remains open and unchanged.
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

This protocol deliberately retains the existing version check, Room migrations, and destructive
migration policy. It solves live-file replacement and stale-sidecar races; it is not a separate
schema-validation or rollback system.
