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
            ├── TrackAndGraphDatabaseDao
            ├── GroupItemDao
            ├── GroupDao
            ├── GraphDao
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

Each component type has a dedicated helper class:

```kotlin
internal class TrackerHelperImpl @Inject constructor(
    private val transactionHelper: DatabaseTransactionHelper,
    private val dao: TrackAndGraphDatabaseDao,
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
- **dao**: Main database DAO for component-specific queries
- **groupItemDao**: For managing group membership
- **timeProvider**: For timestamps (injectable for testing)
- **io**: Coroutine dispatcher for background work

## Specialized DAOs

For testability, some helpers use smaller interfaces:

```kotlin
// Instead of injecting the full TrackAndGraphDatabaseDao
internal interface GroupDao {
    fun insertGroup(group: Group): Long
    fun getGroupById(id: Long): Group?
    fun deleteGroup(id: Long)
    // Only methods needed by GroupHelper
}

// TrackAndGraphDatabaseDao extends GroupDao
@Dao
interface TrackAndGraphDatabaseDao : GroupDao, GraphDao, ReminderDao {
    // Full interface
}
```

This allows tests to use small fake implementations.

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

**Always use fake implementations, never mocks.** The project avoids Mockito/mocking frameworks because the codebase may eventually migrate to Kotlin Multiplatform (KMP), which is incompatible with Android-specific mocking tools.

The pattern: define a small `*Dao` interface for each helper (as shown in "Specialized DAOs" above), then create a `Fake*Dao` in `testFixtures` that implements it in-memory.

| Helper | Test approach |
|--------|---------------|
| GroupHelperImpl | `FakeGroupDao` + `FakeGroupItemDao` |
| GraphHelperImpl | `FakeGraphDao` + `FakeGroupItemDao` |
| ReminderHelperImpl | `FakeReminderDao` + `FakeGroupItemDao` |
| TrackerHelperImpl | Should use `FakeTrackerDao` + `FakeGroupItemDao` (not Mockito) |

If a helper currently depends on the large `TrackAndGraphDatabaseDao` directly, the fix is to extract a focused `TrackerDao` interface (following the same pattern as `GroupDao`, `GraphDao`, `ReminderDao`) and create a `FakeTrackerDao` in testFixtures.

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
