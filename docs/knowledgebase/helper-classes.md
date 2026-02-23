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

## Testing Approach

Helpers are tested with fake DAOs:

```kotlin
class GroupHelperImplTest {
    private lateinit var fakeGroupDao: FakeGroupDao
    private lateinit var fakeGroupItemDao: FakeGroupItemDao

    @Before
    fun before() {
        fakeGroupDao = FakeGroupDao()
        fakeGroupItemDao = FakeGroupItemDao()

        uut = GroupHelperImpl(
            groupDao = fakeGroupDao,
            groupItemDao = fakeGroupItemDao,
            // ... other dependencies
        )
    }
}
```

Fake DAOs are in `app/data/src/testFixtures/kotlin/com/samco/trackandgraph/`.

## Key Files

| File | Purpose |
|------|---------|
| `DataInteractor.kt` | Public interface |
| `DataInteractorImpl.kt` | Implementation delegating to helpers |
| `TrackerHelperImpl.kt` | Tracker CRUD |
| `FunctionHelperImpl.kt` | Function CRUD |
| `GraphHelperImpl.kt` | Graph/stat CRUD |
| `GroupHelperImpl.kt` | Group CRUD and recursive deletion |
| `ReminderHelperImpl.kt` | Reminder CRUD |
