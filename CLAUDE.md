# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Track & Graph is an Android app for tracking personal data and creating custom graphs. The project uses a multi-module architecture with Kotlin, Jetpack Compose, Room database, and Hilt dependency injection.

## Architecture

### Module Structure
- **app**: Main Android application module containing UI, activities, and screens
- **data**: Data layer with Room database, DTOs, and DataInteractor interface
- **functions**: Custom data processing functions using Lua scripting and graph-based function composition

### Key Technologies
- **UI Framework**: Jetpack Compose with Material 3 design
- **Dependency Injection**: Hilt (Dagger-based)
- **Database**: Room with coroutines and Flow-based reactive queries
- **Navigation**: Navigation3 with type-safe arguments using kotlinx.serialization
- **Testing**: Screenshot tests using Facebook's Shot library, instrumented tests with Hilt
- **Scripting**: Lua integration for custom data functions

### Architecture Patterns
- **MVVM**: ViewModels with LiveData/StateFlow for reactive UI
- **Repository Pattern**: DataInteractor interface abstracts data operations
- **Composition over Inheritance**: Function system uses graph-based composition of data processing nodes
- **Dependency Injection**: All components use Hilt for clean architecture

## Development Commands

### Building (Prefer Debug Variants)
```bash
app/gradlew :app:assembleDebug           # Build debug APK (fast, no minification)
app/gradlew :app:testDebugUnitTest       # Run unit tests on debug variant
app/gradlew :app:connectedDebugAndroidTest  # Run instrumented tests on debug
```

### Screenshot Testing (Use Makefile Commands)
```bash
make snapshots-record             # Record screenshot baselines (sets up emulator, records, cleans up)
make snapshots-verify             # Verify screenshots against baselines
make playstore-record             # Generate high-res Play Store promotional screenshots
make tutorial-record              # Generate tutorial images for in-app use
```

### Emulator Management
```bash
make ensure-avd-shot-api35-low    # Create low-res test emulator if needed
make boot-and-prep-shot-api35-low # Boot and configure emulator for testing
make kill-emulator                # Shutdown running emulator
```

### Lua Development
```bash
make deep-link-inject FILE=path/to/script.lua     # Inject Lua script via ADB to device
make deep-link-serve-local FILE=path/to/script.lua # Serve script locally for testing
```

### Configuration Validation
```bash
make validate-remote-config       # Validate remote configuration files
```

## Code Conventions

### Package Structure
- `com.samco.trackandgraph.*` - Main app package
- UI organized by feature: `addgroup`, `adddatapoint`, `graphstatview`, etc.
- Compose UI components in `ui.compose.ui.*`
- Data models and DTOs in `data.database.dto.*`

### Naming Patterns
- Activities: `*Activity.kt` (e.g., MainActivity)
- Compose Screens: `*Screen.kt` (e.g., GroupScreen)
- ViewModels: `*ViewModel.kt`
- Compose UI components: Descriptive names in `ui.compose.ui.*`

### Database Conventions
- DTOs use data classes with Room annotations
- All database operations return Flow for reactive updates
- Use suspend functions for write operations
- DataInteractor interface centralizes all data operations

### Function System Architecture
The functions module implements a graph-based data processing system:
- **Nodes**: Represent data sources or processing functions
- **Connections**: Define data flow between nodes
- **Lua Integration**: Custom functions written in Lua
- **Prebuilt Functions**: Functions written in kotlin are there to support basic and legacy graph configs. New functions should be implemented in Lua.

### Testing Patterns
- Use `HiltTestRunner` for instrumented tests
- Screenshot tests in dedicated build variant with disabled minification
- Test fixtures available in data module via `testFixtures(project(":data"))`
- Mock dependencies using Mockito with Kotlin extensions

### Compose Guidelines
- Use Material 3 theming via `TnGComposeTheme`
- Custom UI components in `ui.compose.ui.*`
- Follow composition local pattern for settings: `LocalSettings`
- Use Hilt ViewModels with Compose integration

### Build Variants
- **debug**: Fast builds, no minification (preferred for development)
- **debugMinify**: Debug with minification enabled
- **screenshots**: Special variant for screenshot tests (no alarms, debug signing)
- **promo**: For generating promotional screenshots

### Deep Link Support
The app supports deep linking for Lua script injection. During development you can use:
- `make deep-link-inject FILE=path/to/script.lua` - Inject Lua script via ADB
At runtime the app can download lua files with deep links like:
- `trackandgraph://lua_inject_url?url=https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/docs/docs/lua/community/text/fraction/script.lua`
