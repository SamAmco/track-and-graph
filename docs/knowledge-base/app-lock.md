---
title: App lock — security boundary, persistence, and UI shell
description: App lock is a presentation/access barrier, not database encryption; configuration uses PrefsPersistenceProvider, unlock state is in-memory, and the main activity gates only app UI while widgets/background work remain usable.
topics:
  - App lock security boundary and what it does not protect
  - MainActivity UI gate and in-memory unlock session
  - PrefsPersistenceProvider-backed app-lock configuration
  - BiometricPrompt behaviour and password fallback
  - Widgets, reminders, notifications, and backups are intentionally outside the lock boundary
keywords: [app-lock, lock, biometric, password, PrefsPersistenceProvider, DataStore, MainActivity, AppLockGate, AppLockSession, backup, widget, reminder, notification, encryption, security]
---

# App Lock

App lock is intended as a privacy barrier for casual access to the UI on an already-unlocked device. It is **not** database encryption and should not be described as securing data at rest.

Known places user data can still exist or be exposed while app lock is enabled:

- The Room database remains unencrypted in app-private storage.
- Manual and auto backups are unencrypted SQLite database files.
- Android cloud/device backup can include app data according to the manifest backup rules.
- Widgets, reminders, notifications, and deep links are not gated by app lock.
- App lock does not prevent the app from being uninstalled.

There is no password recovery mechanism. User-facing docs should tell users that if they forget the app lock password and cannot unlock with biometrics, the recovery route is uninstalling Track & Graph, reinstalling it, and restoring from backup.

This boundary is deliberate so app lock can ship separately from any future encrypted-database feature. If database encryption is added later, it should be treated as a separate architecture because password recovery, backup/restore, migration, and background work semantics change substantially.

## App-Shell Boundary

The app is single-activity for normal UI, so app lock belongs in the app shell rather than around data-layer calls. Gate the main UI in `MainActivity`/the app-lock package and keep `DataInteractor`/Room unaware of the lock. Background work, alarms, timers, and widgets should continue to operate unless the product decision changes.

Unlock state should be in-memory only. That gives the expected behavior that a killed process starts locked again. Relock is based on app lifecycle/device lock events rather than database access.

Device lock should force app lock immediately, regardless of the configured background timeout. In `MainActivity`, keep the screen/user-present receiver registered for the Activity lifetime rather than only between `onStart`/`onStop`; screen-off broadcast delivery can otherwise race `onStop` unregistering the receiver. Also check `KeyguardManager.isKeyguardLocked` in `onStop` and call `AppLockSession.lock()` instead of starting the background timeout when the stop is caused by device locking.

## Persistence

Use `PrefsPersistenceProvider` for app-lock settings, not direct `SharedPreferences`. The app-lock config is app-layer state and should follow the DataStore-backed persistence pattern used by other app features.

Repository reads/writes should stay suspend-based; do not wrap DataStore access in `runBlocking`. ViewModels own UI operations via `viewModelScope`, while `AppLockSession` keeps a cached copy of the config from the repository flow so activity lifecycle callbacks can make synchronous lock/unlock decisions without blocking.

Do not store plaintext passwords. Store a salted verifier/hash only. This verifier is not a database-encryption key; if an attacker can read it directly from app storage, they can also read the unencrypted database. Its purpose is only to validate app-lock password entry.

The app-lock password verifier stores its PBKDF2 iteration count and algorithm in `AppLockConfig`. New verifiers should be fast enough for UI unlock because app lock is not protecting data at rest; storing verifier parameters keeps future changes/migrations explicit. Use `PBKDF2WithHmacSHA1` for new verifiers because `PBKDF2WithHmacSHA256` is not available from the platform crypto provider on API 24. Hashing/provider failures must be caught and surfaced as UI errors/toasts rather than crashing the app.

## UI Notes

The unlock screen should have one stable visual state. If biometric unlock is enabled, `BiometricPrompt` can appear automatically over the same password screen; dismissing the prompt should not swap the underlying screen to a different layout. User-facing copy should use "biometric" language because `BIOMETRIC_STRONG` can cover any strong biometric modality available on the device.

For app-lock settings UI, reuse shared controls from `app/ui` first: shared buttons, text fields, dividers, spacing, and row controls keep the screen aligned with the rest of the app. See [Compose UI patterns](compose-ui-patterns.md) for the broader shared-UI guidance.

Keep the security-boundary explanation out of the settings screen body. The settings screen should use the common top-app-bar info action (`about_icon`) to open the app-lock docs page via `UrlNavigator.Location.APP_LOCK_DOCS`.

The settings screen's relock-time field uses a ViewModel-owned state-based `TextFieldState`, not `rememberSaveable` text in the composable. Seed that field once from the repository before observing keyboard edits, then let the text field drive valid numeric persistence. Do not continuously copy repository emissions back into the text field because async DataStore echoes can race active typing and overwrite newer keyboard input. Async guards such as `isWorking` belong in ViewModel methods/collectors rather than in row-control callbacks; this avoids UI flicker from temporarily nulling callbacks while DataStore/hash work is in flight.
