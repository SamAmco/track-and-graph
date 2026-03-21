---
title: FAB patterns — scroll-hiding, animation constants, and tracker-only guard
description: How scroll-hiding FABs work via NestedScrollConnection passed through AppBarConfig, shared FAB animation constants in Animations.kt, the tracker-only guard pattern, and the FeatureHistoryScreen implementation.
topics:
  - Scroll-hiding FAB via NestedScrollConnection + AppBarConfig.nestedScrollConnection
  - Shared FAB animation constants (fabEnterTransition, fabExitTransition) in Animations.kt
  - Tracker-only FAB guard: isTracker derived from viewModel.tracker.map { it != null }
  - FeatureHistoryScreen and GroupScreen as reference implementations
keywords: [FAB, scroll, NestedScrollConnection, AppBarConfig, nestedScrollConnection, fabEnterTransition, fabExitTransition, Animations, AnimatedVisibility, tracker, isTracker, showFab, GroupScreen, FeatureHistoryScreen, GroupTopBar]
---

# FAB and Scroll Patterns

## Scroll-Hiding FAB Pattern

Several screens hide the FAB when the user scrolls down and show it again when they scroll up. The mechanism spans three layers:

### 1. Local `showFab` state

In the ViewModel-binding composable (e.g. `FeatureHistoryScreen`, `GroupScreen`), a `MutableState<Boolean>` is created with `remember`:

```kotlin
val showFab = remember { mutableStateOf(true) }
```

This state is threaded to both the top-bar composable (which creates the scroll connection) and the screen-content composable (which renders the FAB).

### 2. NestedScrollConnection toggling `showFab`

A `NestedScrollConnection` is created (typically in a `createNestedScrollConnection` private function) that mutates `showFab.value` based on scroll direction:

```kotlin
private fun createNestedScrollConnection(showFab: MutableState<Boolean>): NestedScrollConnection {
    return object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val dy = available.y
            when {
                dy < 0 -> showFab.value = false   // scrolling down — hide FAB
                dy > 0 -> showFab.value = true    // scrolling up — show FAB
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val vy = available.y
            when {
                vy < 0 -> showFab.value = true    // flinging up — show FAB
                vy > 0 -> showFab.value = false   // flinging down — hide FAB
            }
            return Velocity.Zero
        }
    }
}
```

Note: `onPreFling` uses **reversed** polarity from `onPreScroll` — a negative fling velocity means the finger is moving up (list decelerating after scrolling up), which should show the FAB.

The connection is memoized with `remember(showFab)` to avoid recreating it on every recomposition.

### 3. AppBarConfig.nestedScrollConnection

The `NestedScrollConnection` is passed to the top bar controller via `AppBarConfig`:

```kotlin
topBarController.Set(
    navArgs,
    AppBarConfig(
        title = ...,
        nestedScrollConnection = nestedScrollConnection,
        ...
    )
)
```

`AppBarConfig` is defined in `ui/compose/appbar/AppBarController.kt`:

```kotlin
data class AppBarConfig(
    ...
    val nestedScrollConnection: NestedScrollConnection? = null,
    ...
)
```

In `MainScreen.kt`, the Scaffold's root `Modifier` applies the connection when present:

```kotlin
Modifier
    .let {
        if (topBarConfig.nestedScrollConnection != null) {
            it.nestedScroll(topBarConfig.nestedScrollConnection)
        } else it
    }
    .nestedScroll(scrollBehavior.nestedScrollConnection)
```

This means the connection intercepts scroll events for the entire scaffold content area, not just a specific list.

### 4. Rendering the FAB with AnimatedVisibility

The screen-content composable receives `showFab` (as `State<Boolean>` for read-only access, or the raw boolean after combining conditions) and wraps the FAB in `AnimatedVisibility`:

```kotlin
AnimatedVisibility(
    visible = showTrackFab,   // e.g. showFab.value && isTracker && !isMultiSelectMode
    modifier = Modifier.align(Alignment.BottomEnd),
    enter = fabEnterTransition,
    exit = fabExitTransition,
) {
    FloatingActionButton(...)
}
```

---

## Shared FAB Animation Constants

`ui/compose/ui/Animations.kt` defines two top-level constants for FAB enter/exit transitions, used across all screens that animate a FAB:

```kotlin
private const val FAB_ANIMATION_DURATION_MS = 300

val fabEnterTransition: EnterTransition =
    scaleIn(animationSpec = tween(FAB_ANIMATION_DURATION_MS)) +
        fadeIn(animationSpec = tween(FAB_ANIMATION_DURATION_MS))

val fabExitTransition: ExitTransition =
    scaleOut(animationSpec = tween(FAB_ANIMATION_DURATION_MS)) +
        fadeOut(animationSpec = tween(FAB_ANIMATION_DURATION_MS))
```

Import them in any screen that uses `AnimatedVisibility` for a FAB:

```kotlin
import com.samco.trackandgraph.ui.compose.ui.fabEnterTransition
import com.samco.trackandgraph.ui.compose.ui.fabExitTransition
```

---

## Tracker-Only FAB Guard

On `FeatureHistoryScreen`, the "Track" FAB must only appear when viewing a **tracker** (not a function). The guard is derived from the ViewModel's `tracker` LiveData:

```kotlin
val isTracker by viewModel.tracker.map { it != null }.observeAsState(false)
```

`viewModel.tracker` is a `LiveData<Tracker?>` — `null` means the feature is a function. The FAB visibility combines all three conditions:

```kotlin
showTrackFab = showFab.value && isTracker && !isMultiSelectMode
```

- `showFab.value` — scroll-driven show/hide
- `isTracker` — only visible when viewing a tracker
- `!isMultiSelectMode` — hidden during multi-select mode

---

## Reference Implementations

| Screen | File | Notes |
|---|---|---|
| GroupScreen | `group/GroupScreen.kt` + `group/GroupTopBar.kt` | Original pattern; `showFab.value && groupHasTrackers` guards the FAB |
| FeatureHistoryScreen | `featurehistory/FeatureHistoryScreen.kt` | Adds tracker-only and multi-select guards; also uses FABs for multi-select actions |

Both implementations follow the same shape:
1. `remember { mutableStateOf(true) }` in the ViewModel-binding composable
2. `createNestedScrollConnection(showFab)` in the top-bar sub-composable
3. `AppBarConfig(nestedScrollConnection = ...)` to wire it into `MainScreen`
4. `AnimatedVisibility(visible = showFab.value && <guards>, enter = fabEnterTransition, exit = fabExitTransition)` in the view sub-composable
