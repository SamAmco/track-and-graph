---
title: DataInteractor and Helper pattern
description: DataInteractor public interface, DataInteractorImpl delegation to typed helpers (TrackerHelper, FunctionHelper, GraphHelper, GroupHelper, ReminderHelper), focused DAO interfaces, symlink-aware delete pattern, withContext-inside-withTransaction pitfall, and DataUpdateType events.
topics:
  - DataInteractor: public interface; UI layer depends only on this
  - Helpers: TrackerHelper, FunctionHelper, GraphHelper, GroupHelper, ReminderHelper
  - Focused DAO interfaces injected per helper (enables small in-memory fakes for testing)
  - Delete pattern: groupId provided + multi-group → remove symlink only; otherwise delete all
  - CRITICAL pitfall: never call withContext inside withTransaction (breaks Room thread confinement)
  - DataUpdateType events: emitted by DataInteractorImpl after every mutation; UI subscribes to filter
keywords: [DataInteractor, helper, DAO, TrackerHelper, FunctionHelper, GraphHelper, GroupHelper, ReminderHelper, delete, symlink, withTransaction, withContext, DataUpdateType, events, testing, fakes, DisplayIndex]
---

# Helper Classes and DataInteractor

The data layer uses a hierarchy of helper classes for clean separation of concerns.

## Architecture

```
DataInteractor (public interface)
    │
    ├── DataInteractorImpl
    │       │
    │       ├── TrackerHelper
    │       ├── FunctionHelper
    │       ├── GraphHelper
    │       ├── GroupHelper
    │       └── ReminderHelper
    │
    └── DAOs (Room interfaces)
            ├── TrackAndGraphDatabaseDao  ← extends all focused DAOs below
            ├── GroupItemDao
            ├── TrackerDao
            ├── FunctionDao
            ├── GraphDao
            ├── GroupDao
            └── ReminderDao
```

## DataInteractor

The public interface for all data operations. UI layer depends only on this interface (search `DataInteractor.kt` in the data interactor package). All methods accept/return request/response DTOs — search for `*Request` and `*Response` classes in the data module.

## Helper Pattern

Each component type has a dedicated helper class (e.g. `TrackerHelperImpl`, `FunctionHelperImpl`) injected with a focused DAO interface rather than the full DAO. Search for `*HelperImpl` in the data module. Common injected dependencies include `DatabaseTransactionHelper`, a focused DAO, `GroupItemDao`, `TimeProvider`, and an IO dispatcher.

## Specialized DAOs

Each helper injects a **focused DAO interface** (e.g. `TrackerDao`, `FunctionDao`) containing only the methods it needs, rather than the full `TrackAndGraphDatabaseDao`. The full Room DAO extends all focused interfaces. `DataModule` provides each focused interface with a `@Provides` binding that returns the `TrackAndGraphDatabaseDao` instance.

This design enables small in-memory fakes for unit tests — each fake only needs to implement the focused interface, not the entire DAO.

## Delete Pattern (Symlink Logic)

All delete operations follow the same pattern for multi-group membership. The request object carries an optional `groupId`:

- **`groupId` provided AND component in multiple groups** → remove only that GroupItem (symlink); the component itself is preserved
- **Otherwise** → delete all GroupItems then delete the component

This pattern is implemented consistently by all helpers — search for `getGroupItemsForChild` in any `*HelperImpl` to see it. Special cases:
- **Reminders**: `null` groupId always deletes everywhere (the Reminders screen never passes a groupId)
- **Groups**: the parameter is named `parentGroupId` in both `GroupDeleteRequest` and `GroupViewModel.onDeleteGroup`

## Coroutine + Transaction Pitfall

**Never call `withContext` inside a `withTransaction` block**, even if switching to the same dispatcher. Room binds a transaction to a specific coroutine context; switching context mid-transaction breaks thread confinement and can cause a crash or incorrect behaviour.

This means you must not call any `suspend` function that internally calls `withContext` from within a transaction block — including other helper methods on the same class.

```kotlin
// BAD - getAncestorAndSelfGroupIds calls withContext(io) internally
override suspend fun createSymlink(...) = withContext(io) {
    transactionHelper.withTransaction {
        val ancestors = getAncestorAndSelfGroupIds(inGroupId) // ← withContext inside transaction!
        ...
    }
}

// GOOD - do the context-switching work before entering the transaction
override suspend fun createSymlink(...) = withContext(io) {
    val ancestors = getAncestorAndSelfGroupIds(inGroupId) // ← outside transaction
    transactionHelper.withTransaction {
        if (childId in ancestors) error("...")
        ...
    }
}
```

## Testing Approach

**Never introduce mocks without asking the user first** — see [architecture.md](architecture.md#kmp-compatibility) for the rationale. Always create a real fake implementation instead.

**Fake DAO pattern**: For each focused DAO interface, create a `Fake*Dao` in `testFixtures` (search for `Fake*Dao` in the data module). Each fake implements the interface with an in-memory map/list. Every helper test also needs `FakeGroupItemDao` for group membership operations.

## Data Update Events

`DataInteractorImpl` emits `DataUpdateType` events via a `SharedFlow` after every mutation. UI layers (e.g. `GroupViewModel`) subscribe to filter for relevant events.

**Contract**: The data layer is responsible for emitting ALL relevant events. For example, creating a tracker emits both `TrackerCreated` and `DisplayIndex(groupId)`. Deleting a tracker emits `TrackerDeleted` plus `GraphOrStatDeleted`/`GraphOrStatUpdated` for any affected graphs. Consumers should NOT need to infer secondary effects from primary events.

`DisplayIndex(groupId)` is emitted by any operation that modifies display indices in a group (creates, moves, reordering). Deletes do NOT emit `DisplayIndex` since remaining items' indices are unchanged.

## Finding Code

Search for `DataInteractor.kt` (public interface), `DataInteractorImpl.kt` (delegation + event emission), and `*HelperImpl.kt` (per-type CRUD) in the data module. Fake DAOs live in `testFixtures` — search for `Fake*Dao`.
