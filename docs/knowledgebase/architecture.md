# Architecture

## Overview

- **MVVM** - UI pattern with ViewModels
- **DataInteractor** - Single entry point for all data operations
- **Room** - SQLite database with migration tracking in `/schemas`
- **Kotlin Coroutines + Flow** - Async and reactive programming
- **Hilt** - Dependency injection

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
