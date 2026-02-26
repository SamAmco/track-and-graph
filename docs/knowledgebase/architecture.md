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

## Conventions

- Prefer imports over fully qualified paths
- DTOs in `app/data/.../dto/` (public API)
- Entities in `app/data/.../database/entity/` (internal)
- Test fakes in `app/data/src/testFixtures/`

## Build Commands

```bash
cd app && ./gradlew assembleDebug        # Build debug APK
cd app && ./gradlew testDebugUnitTest    # Run unit tests
```
