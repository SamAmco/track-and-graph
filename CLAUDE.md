# Track & Graph

Android app for personal data tracking and custom graph visualization.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Lint check
./gradlew lint
```

## Project Structure

- `app/app/` - UI layer (Compose screens, ViewModels, navigation)
- `app/data/` - Data layer (Room DAOs, DTOs, DataInteractor, Lua engine)
- `lua/` - Community Lua functions and tooling

## Architecture

Clean Architecture + MVVM:
- **DataInteractor** (`app/data/.../interactor/`) - centralized interface for all data operations
- Room database with migrations tracked in `/schemas`
- Kotlin Coroutines + Flow for reactive programming
- Hilt for dependency injection

## Conventions

- Prefer imports over fully qualified paths
- DTOs live in `app/data/.../dto/`
- Database entities and DAOs in `app/data/.../database/`
- Compose UI components in `app/app/.../ui/compose/`
