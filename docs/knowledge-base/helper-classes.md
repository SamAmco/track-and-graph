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

**Location**: `app/data/src/main/java/com/samco/trackandgraph/data/interactor/DataInteractor.kt`

The public interface for all data operations. UI layer depends only on this interface.

```kotlin
interface DataInteractor {
    // Trackers
    suspend fun createTracker(request: TrackerCreateRequest): Long
    suspend fun getTrackerById(id: Long): Tracker?

    // Functions
    suspend fun insertFunction(request: FunctionCreateRequest): Long?

    // Graphs
    suspend fun createLineGraph(request: LineGraphCreateRequest): Long

    // Groups
    suspend fun insertGroup(request: GroupCreateRequest): Long
    suspend fun deleteGroup(request: GroupDeleteRequest): DeletedGroupInfo

    // Reminders
    suspend fun createReminder(request: ReminderCreateRequest): Long

    // ... many more methods
}
```

## Helper Pattern

Each component type has a dedicated helper class injected with a focused DAO interface:

```kotlin
internal class TrackerHelperImpl @Inject constructor(
    private val transactionHelper: DatabaseTransactionHelper,
    private val dao: TrackerDao,          // focused interface, not the full DAO
    private val groupItemDao: GroupItemDao,
    private val dataPointUpdateHelper: DataPointUpdateHelper,
    private val timeProvider: TimeProvider,
    @IODispatcher private val io: CoroutineDispatcher
) : TrackerHelper {
    // CRUD operations for trackers
}
```

### Common Dependencies

- **transactionHelper**: Wraps operations in database transactions
- **dao**: Focused DAO interface for this component type (see Specialized DAOs)
- **groupItemDao**: For managing group membership
- **timeProvider**: For timestamps (injectable for testing)
- **io**: Coroutine dispatcher for background work

## Specialized DAOs

Each helper injects a **focused DAO interface** containing only the methods it needs, rather than the full `TrackAndGraphDatabaseDao`. This enables small in-memory fakes for unit tests.

```kotlin
// Focused interface — only what FunctionHelper needs
internal interface FunctionDao {
    fun insertFeature(feature: Feature): Long
    fun insertFunction(function: Function): Long
    fun getFunctionById(functionId: Long): FunctionWithFeature?
    fun getFunctionsForGroupSync(groupId: Long): List<FunctionWithFeature>
    // ... etc
}

// The full Room DAO extends all focused interfaces
@Dao
internal interface TrackAndGraphDatabaseDao : TrackerDao, FunctionDao, GraphDao, GroupDao, ReminderDao {
    // All methods satisfied by extending focused interfaces
}
```

Focused DAO interfaces: `TrackerDao`, `FunctionDao`, `GraphDao`, `GroupDao`, `ReminderDao`.
`DataModule` provides each interface with a `@Provides` binding that simply returns the `TrackAndGraphDatabaseDao` instance.

## Delete Pattern (Symlink Logic)

All delete operations follow the same pattern for multi-group membership. The request object carries an optional `groupId`:

- **`groupId` provided AND component in multiple groups** → remove only that GroupItem (symlink); the component itself is preserved
- **Otherwise** → delete all GroupItems then delete the component

```kotlin
// Example from TrackerHelperImpl
val groupItems = groupItemDao.getGroupItemsForChild(tracker.id, GroupItemType.TRACKER)

if (request.groupId != null && groupItems.size > 1) {
    groupItems.filter { it.groupId == request.groupId }
              .forEach { groupItemDao.deleteGroupItem(it.id) }
    return@withTransaction
}

groupItems.forEach { groupItemDao.deleteGroupItem(it.id) }
dao.deleteFeature(tracker.featureId)
```

This pattern is used consistently by `TrackerHelper`, `GraphHelper`, and `ReminderHelper`. For **reminders**, `null` groupId always deletes everywhere (the Reminders screen never passes a groupId).

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

**Fake DAO pattern**: For DAO interfaces, create a `Fake*Dao` in `testFixtures` that implements the interface with an in-memory map/list.

| Helper | Fake DAO used |
|--------|--------------|
| TrackerHelperImpl | `FakeTrackerDao` + `FakeGroupItemDao` |
| FunctionHelperImpl | `FakeFunctionDao` + `FakeGroupItemDao` |
| GraphHelperImpl | `FakeGraphDao` + `FakeGroupItemDao` |
| GroupHelperImpl | `FakeGroupDao` + `FakeGroupItemDao` |
| ReminderHelperImpl | `FakeReminderDao` + `FakeGroupItemDao` |

Fake DAOs are in `app/data/src/testFixtures/kotlin/com/samco/trackandgraph/`.

## Data Update Events

`DataInteractorImpl` emits `DataUpdateType` events via a `SharedFlow` after every mutation. UI layers (e.g. `GroupViewModel`) subscribe to filter for relevant events.

**Contract**: The data layer is responsible for emitting ALL relevant events. For example, creating a tracker emits both `TrackerCreated` and `DisplayIndex(groupId)`. Deleting a tracker emits `TrackerDeleted` plus `GraphOrStatDeleted`/`GraphOrStatUpdated` for any affected graphs. Consumers should NOT need to infer secondary effects from primary events.

`DisplayIndex(groupId)` is emitted by any operation that modifies display indices in a group (creates, moves, reordering). Deletes do NOT emit `DisplayIndex` since remaining items' indices are unchanged.

## Key Files

| File | Purpose |
|------|---------|
| `DataInteractor.kt` | Public interface |
| `DataInteractorImpl.kt` | Implementation delegating to helpers, emits `DataUpdateType` events |
| `TrackerHelperImpl.kt` | Tracker CRUD |
| `FunctionHelperImpl.kt` | Function CRUD |
| `GraphHelperImpl.kt` | Graph/stat CRUD |
| `GroupHelperImpl.kt` | Group CRUD, recursive deletion, display index queries |
| `ReminderHelperImpl.kt` | Reminder CRUD |
