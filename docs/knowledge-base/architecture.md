---
title: Architecture and conventions
description: MVVM pattern, module structure (app shell/features, shared UI, data layer, developer tooling), KMP compatibility constraints, Hilt DI, Channel-based UI events, build logic, and coding conventions.
topics:
  - Module structure: app/app (app shell/features), app/ui (shared Compose UI), app/data (Room/DAOs/DTOs), and changelog-viewer tooling
  - KMP compatibility: no mocking frameworks without user approval; prefer testFixtures fakes
  - DataInteractor: single entry point for all data operations from UI layer
  - ViewModel UI events: Channel<T> pattern (not SharedFlow)
  - Conventions: DTOs in dto/, entities in entity/, fakes in testFixtures/
keywords: [architecture, MVVM, Hilt, KMP, modules, ui, shared-ui, build-logic, changelog-viewer, Room, coroutines, DataInteractor, Channel, conventions, testing]
---

# Architecture

## Overview

- **MVVM** - UI pattern with ViewModels
- **DataInteractor** - Single entry point for all data operations
- **Room** - SQLite database with migration tracking in `/schemas`
- **Kotlin Coroutines + Flow** - Async and reactive programming
- **Hilt** - Dependency injection

## KMP Compatibility

The project aims to keep the door open for a future Kotlin Multiplatform (KMP) migration. This shapes several decisions:

- **No mocking frameworks, ever, without explicit user approval.** This is a hard rule. Mockito, MockK, and similar tools are Android-specific and incompatible with KMP. Before introducing any mock, exhaust all alternatives and ask the user first. Prefer real fake implementations in `testFixtures/`.
- **Avoid Android-only APIs** in business logic and data layer code where possible — keep platform-specific code in the UI/app layer
- **Prefer pure Kotlin** abstractions over Android framework dependencies in shared modules

The reminder scheduler is a concrete example of this pattern: scheduling logic (`ReminderScheduler`, `WeekDayReminderScheduler`, etc.) is pure Kotlin in the app layer, depending only on the `PlatformScheduler` interface. The Android-specific implementation (`AndroidPlatformScheduler`, `AlarmManager`, `BroadcastReceiver`) is isolated in an `androidplatform/` subpackage and injected via Hilt. Tests use `FakePlatformScheduler`.

When writing code or tests in the data layer, ask: "would this work in a KMP shared module?" If not, reconsider the approach.

## Module Structure

The main Gradle modules are:

- **`app/app`** — app shell and feature UI: Compose screens, ViewModels, navigation, reminders, widgets, DI
- **`app/ui`** — shared Compose UI components, theming, markdown/dialog primitives, data-visualization color constants, and the UI resources those components own
- **`app/data`** — Data layer: Room, DAOs, DTOs, DataInteractor and helpers
- **`app/changelog-viewer`** — developer-only Android app for pasting release-note markdown and previewing the in-app changelog dialog
- **`app/build-logic`** — included Gradle build containing Android application/library convention plugins

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
| `helpers`, `util` | `:core` or similar shared module |

Shared UI belongs in `app/ui` when it is reusable and does not depend on app feature packages, ViewModels, `DataInteractor`, or data DTOs. Keep feature-coupled components in `app/app` until they can be parameterized cleanly. Generic buttons, dialogs, markdown rendering, chips, spinners, text fields, theme, and UI-only resources belong in `app/ui`; app-specific dialogs or previews that import data DTOs, helpers, settings, or feature callbacks should stay in `app/app`.

When adding new code, place it in the most appropriate existing package and avoid creating imports that would form circular dependencies between packages.

## Build Logic

`app/build-logic` provides convention plugins for common Android defaults:

- `tng.android.application`
- `tng.android.library`

These plugins apply the Android plugin and centralize compile SDK, min/target SDK, Java compatibility, Kotlin toolchain, JVM target, and shared compiler flags. Keep Compose, Hilt, KSP, Room, signing, build types, and module-specific dependencies in module build files unless a pattern repeats broadly enough to justify a dedicated convention plugin.

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

## String Resources and Localization

The app is localized into 4 languages: English (`values/`), French (`values-fr/`), Spanish (`values-es/`), and German (`values-de-rDE/`). When modifying any user-visible string in `strings.xml`, **always update all 4 translations**. Search for the string name across all `strings.xml` files to find every locale.

## Build Commands

See [build-commands.md](build-commands.md).
