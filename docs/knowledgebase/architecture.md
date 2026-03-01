# Architecture

## Overview

- **MVVM** - UI pattern with ViewModels
- **DataInteractor** - Single entry point for all data operations
- **Room** - SQLite database with migration tracking in `/schemas`
- **Kotlin Coroutines + Flow** - Async and reactive programming
- **Hilt** - Dependency injection

## KMP Compatibility

The project aims to keep the door open for a future Kotlin Multiplatform (KMP) migration. This shapes several decisions:

- **No Android-specific mocking frameworks** (e.g. Mockito) in tests — use fake implementations instead, since mocking tools don't work on KMP targets
- **Avoid Android-only APIs** in business logic and data layer code where possible — keep platform-specific code in the UI/app layer
- **Prefer pure Kotlin** abstractions over Android framework dependencies in shared modules

The reminder scheduler is a concrete example of this pattern: scheduling logic (`ReminderScheduler`, `WeekDayReminderScheduler`, etc.) is pure Kotlin in the app layer, depending only on the `PlatformScheduler` interface. The Android-specific implementation (`AndroidPlatformScheduler`, `AlarmManager`, `BroadcastReceiver`) is isolated in an `androidplatform/` subpackage and injected via Hilt. Tests use `FakePlatformScheduler`.

When writing code or tests in the data layer, ask: "would this work in a KMP shared module?" If not, reconsider the approach.

## Module Structure

Currently there are only two Gradle modules:

- **`app/app`** — UI layer: Compose, ViewModels, navigation, reminders, widgets, DI
- **`app/data`** — Data layer: Room, DAOs, DTOs, DataInteractor and helpers

The intent is to split into feature modules eventually. To make that easier, the top-level package structure of `app/app` already mirrors the intended feature module boundaries — each package should be treated as if it were a future module, meaning cross-package dependencies should be kept deliberate and minimal. The current top-level packages are:

| Package | Likely future module |
|---------|----------------------|
| `adddatapoint` | `:feature:adddatapoint` |
| `addgroup` | `:feature:addgroup` |
| `addtracker` | `:feature:addtracker` |
| `backupandrestore` | `:feature:backupandrestore` |
| `featurehistory` | `:feature:featurehistory` |
| `functions` | `:feature:functions` |
| `graphstatinput` | `:feature:graphstatinput` |
| `graphstatproviders` | `:feature:graphstatproviders` |
| `graphstatview` | `:feature:graphstatview` |
| `group` | `:feature:group` |
| `importexport` | `:feature:importexport` |
| `notes` | `:feature:notes` |
| `reminders` | `:feature:reminders` |
| `settings` | `:feature:settings` |
| `timers` | `:feature:timers` |
| `viewgraphstat` | `:feature:viewgraphstat` |
| `widgets` | `:feature:widgets` |
| `di`, `main`, `navigation` | `:app` (shell/wiring) |
| `helpers`, `ui`, `util` | `:core:ui` or similar shared module |

When adding new code, place it in the most appropriate existing package and avoid creating imports that would form circular dependencies between packages.

## ViewModel → UI Events (one-shot)

For one-shot UI commands (e.g. scroll to position, show snackbar, navigate) use a `Channel` exposed as a `ReceiveChannel`:

```kotlin
// ViewModel interface
val myEvents: ReceiveChannel<MyEvent>

// ViewModel impl — capacity 1 buffers one event while the UI isn't yet collecting
override val myEvents = Channel<MyEvent>(1)

// emit from a non-suspending context — use launch/send rather than trySend for testability
viewModelScope.launch { myEvents.send(MyEvent.ScrollToTop) }
// emit from a suspending context — call send directly
myEvents.send(MyEvent.ScrollToTop)
```

```kotlin
// Composable — convert to Flow at the call site
LaunchedEffect(Unit) {
    viewModel.myEvents.receiveAsFlow().collect { event -> /* handle */ }
}
```

**Why Channel over MutableSharedFlow:** `SharedFlow` only delivers to active subscribers — events emitted before the `LaunchedEffect` starts collecting are lost. A `Channel` queues events regardless of whether a consumer is ready, guaranteeing delivery. It also semantically models "consumed once" rather than "broadcast to all".

**Why `launch { send() }` over `trySend`:** `send` is a suspend function that respects back-pressure and coroutine cancellation, making it easier to control in tests (you can verify the coroutine ran to completion rather than racing with a fire-and-forget).

## Conventions

- Prefer imports over fully qualified paths
- DTOs in `app/data/.../dto/` (public API)
- Entities in `app/data/.../database/entity/` (internal)
- Test fakes in `app/data/src/testFixtures/`

## Build Commands

See [build-commands.md](build-commands.md).
