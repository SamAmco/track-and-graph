---
title: DataInteractor and Helper pattern
description: DataInteractor public interface, DataInteractorImpl delegation to typed helpers, focused DAO interfaces, symlink-aware delete pattern (groupItemId + deleteEverywhere), simplified duplicate/move patterns using groupItemId as placement identity, withContext-inside-withTransaction pitfall, and DataUpdateType events.
topics:
  - DataInteractor: public interface; UI layer depends only on this
  - Helpers: TrackerHelper, FunctionHelper, GraphHelper, GroupHelper, ReminderHelper
  - Focused DAO interfaces injected per helper (enables small in-memory fakes for testing)
  - Delete pattern: groupItemId + deleteEverywhere boolean; unique component + !deleteEverywhere still deletes component
  - Duplicate pattern: single entry point dispatches by GroupItemType, then GraphStatType; returns CreatedComponent
  - Move pattern: MoveComponentRequest carries only groupItemId + toGroupId
  - CRITICAL pitfall: never call withContext inside withTransaction (breaks Room thread confinement)
  - DataUpdateType events: emitted by DataInteractorImpl after every mutation; UI subscribes to filter
keywords: [DataInteractor, helper, DAO, TrackerHelper, FunctionHelper, GraphHelper, GroupHelper, ReminderHelper, delete, symlink, withTransaction, withContext, DataUpdateType, events, testing, fakes, DisplayIndex, groupItemId, deleteEverywhere, duplicate, move, CreatedComponent, performAtomicUpdate]
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

All delete operations use a single unified DTO:

```kotlin
data class ComponentDeleteRequest(
    val groupItemId: Long,
    val deleteEverywhere: Boolean = false,
)
```

This replaced the previous per-type delete DTOs (`TrackerDeleteRequest`, `FunctionDeleteRequest`, etc.) — a single request works for all component types because the helper can derive the component's `childId` and `type` from the GroupItem lookup.

Semantics:
- **`deleteEverywhere = false` AND component has multiple GroupItems** → remove only that one GroupItem (symlink); the component itself is preserved
- **`deleteEverywhere = false` AND component has only one GroupItem** → deletes the component entirely (not just the GroupItem). The KDoc makes this clear — "unique component + remove from group = delete everywhere".
- **`deleteEverywhere = true`** → delete all GroupItems then delete the component

Each helper implementation starts with `groupItemDao.getGroupItemById(request.groupItemId)` to derive the component's `childId`, then proceeds with deletion logic. `DataInteractorImpl` overrides for `deleteTracker` and `deleteFunction` also do this lookup first to get the `featureId` for dependency analysis before delegating to the helper.

Search for `getGroupItemsForChild` in any `*HelperImpl` to see the multi-placement logic.

### Duplicate Pattern

All duplicate operations take `groupItemId` as placement identity — callers never need the underlying component ID. The helper methods return `CreatedComponent(componentId, groupItemId)` — the same DTO used by create methods — so `DataInteractorImpl` can emit events using the *newly created* GroupItem rather than redundantly re-fetching the original.

`DataInteractorImpl.duplicateGraphOrStat(groupItemId)` dispatches by `GroupItemType` with an exhaustive `when`:
- GRAPH → delegates to `GraphHelper.duplicateGraphOrStat(groupItemId)`, which has its own exhaustive `when` on `GraphStatType`
- FUNCTION → delegates to `FunctionHelper.duplicateFunction(groupItemId)`

Both return `CreatedComponent?`. `DataInteractorImpl` uses the returned `groupItemId` to look up the group for `DisplayIndex` event emission.

`duplicateReminder(groupItemId)` is a separate method on `ReminderHelper` (not dispatched through `duplicateGraphOrStat`). It looks up the GroupItem first, then derives `reminderId = existingGroupItem.childId`. Returns `CreatedComponent(componentId, groupItemId)` like all other duplicate methods.

### Move Pattern

`MoveComponentRequest` carries only `groupItemId` and `toGroupId`. The data layer derives `type`, `childId`, and `fromGroupId` from the GroupItem lookup.

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

**Visibility**: Many helper interfaces and their dependencies (e.g. `DataPointUpdateHelper`) are `internal`. Fakes for `internal` interfaces must also be `internal` — a `public` fake that exposes `internal` types in its signature will fail to compile.

**Composite key pitfall**: Fake DAOs must mirror the real table's primary key for their in-memory storage. For example, `FakeTrackerDao` stores data points keyed by `Pair<epochMilli, featureId>` (matching the real `(epoch_milli, feature_id)` composite PK). Using `epochMilli` alone would silently overwrite data points from different features that share a timestamp.

## Data Update Events

`DataInteractorImpl` emits `DataUpdateType` events via a `SharedFlow` after every mutation. UI layers (e.g. `GroupViewModel`) subscribe to filter for relevant events.

**Contract**: The data layer is responsible for emitting ALL relevant events. For example, creating a tracker emits both `TrackerCreated` and `DisplayIndex(groupId)`. Deleting a tracker emits `TrackerDeleted` plus `GraphOrStatDeleted`/`GraphOrStatUpdated` for any affected graphs. Consumers should NOT need to infer secondary effects from primary events.

`DisplayIndex(groupId)` is emitted by any operation that modifies display indices in a group (creates, duplicates, moves, reordering). Deletes do NOT emit `DisplayIndex` since remaining items' indices are unchanged — but UI flows that track which items are in a group (like `dbDisplayIndices` in `GroupViewModel`) must also listen for the component-deleted events (`TrackerDeleted`, `GraphOrStatDeleted`, `GroupDeleted`, `FunctionDeleted`, `Reminder`) to refresh when a GroupItem is removed.

**Pitfall — double emission**: Methods that use `performAtomicUpdate` must NOT also emit events inside the lambda, because `performAtomicUpdate` already emits via its `.also` block. For example, `shiftUpGroupChildIndexes` wraps `shiftDisplayIndexesDown` in `performAtomicUpdate(DataUpdateType.DisplayIndex(groupId))` — adding a second `dataUpdateEvents.emit` inside would cause duplicate `DisplayIndex` events.

### Delegation Pitfall — New Helper Methods Need Event Overrides

`DataInteractorImpl` uses Kotlin `by` delegation (e.g. `TrackerHelper by trackerHelper`) to forward helper methods. When you **add a new method** to a helper interface, it is automatically delegated — but it **won't emit `DataUpdateType` events**. You must explicitly override the method in `DataInteractorImpl` to call the helper and then emit the appropriate events. This is easy to miss because the code compiles without the override.

## Finding Code

Search for `DataInteractor.kt` (public interface), `DataInteractorImpl.kt` (delegation + event emission), and `*HelperImpl.kt` (per-type CRUD) in the data module. Fake DAOs live in `testFixtures` — search for `Fake*Dao`.
