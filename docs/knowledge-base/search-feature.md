---
title: Search feature — in-place screen swap pattern
description: How the group search feature works — GroupSearchViewModel visibility toggle, SearchScreen composable, in-place swap inside GroupScreen, overrideBackNavigationAction for back handling, and current stub status.
topics:
  - GroupSearchViewModel — simple isSearchVisible StateFlow toggle
  - In-place screen swap: GroupScreen renders SearchScreen or normal content based on isSearchVisible
  - SearchScreen sets its own AppBarConfig with overrideBackNavigationAction
  - BackHandler wires physical/gesture back to hideSearch()
  - Current implementation status (stub)
keywords: [search, GroupSearchViewModel, SearchScreen, isSearchVisible, in-place, screen-swap, overrideBackNavigationAction, BackHandler, AppBarConfig, GroupScreen, GroupTopBar, onSearchClick]
---

# Search Feature

## Current Status

The search feature is a **stub**. `SearchScreen` renders a placeholder text element — no actual search logic, query handling, or results are implemented yet. The infrastructure for showing/hiding the search screen is complete.

## GroupSearchViewModel

`GroupSearchViewModelImpl` (in `group/`) is a minimal ViewModel holding a single `MutableStateFlow<Boolean>`:

```kotlin
interface GroupSearchViewModel {
    val isSearchVisible: StateFlow<Boolean>
    fun showSearch()
    fun hideSearch()
}
```

It is injected via Hilt into `GroupScreen` alongside the other group ViewModels.

## In-Place Screen Swap

Rather than navigating to a new route, `GroupScreen` conditionally renders either the search screen or the normal group content **within the same composable**:

```kotlin
val isSearchVisible by searchViewModel.isSearchVisible.collectAsStateWithLifecycle()

if (isSearchVisible) {
    SearchScreen(
        navArgs = navArgs,
        onBack = { searchViewModel.hideSearch() }
    )
} else {
    GroupTopBarContent(...)
    GroupScreenContent(...)
}
```

When `isSearchVisible` becomes true, both the top bar and the group content are replaced in one recomposition. Because `SearchScreen` calls `topBarController.Set(...)` with its own `AppBarConfig`, the top bar switches to a search bar with a back button — no separate navigation needed.

## SearchScreen

`SearchScreen` (in `group/`) takes `navArgs` and `onBack`, and does two things:

1. Installs a `BackHandler` so the system/gesture back calls `onBack` (which calls `searchViewModel.hideSearch()`).
2. Calls `topBarController.Set(navArgs, AppBarConfig(..., overrideBackNavigationAction = onBack))` to replace the group's top bar with one that has a back button wired to `onBack` rather than the default pop-back-stack behaviour.

`overrideBackNavigationAction` is a field on `AppBarConfig` (defined in `ui/compose/appbar/AppBarController.kt`) that lets a composable intercept the top bar back button without triggering navigation.

## Top Bar Wiring

The search button lives in `GroupTopBarContent` / `createTopBarActions`. It is passed in as `onSearchClick: () -> Unit` and is called `{ searchViewModel.showSearch() }` from `GroupScreen`. Both `onAddSymlink` and `onSearchClick` are parameters of `GroupTopBarContent` and `createTopBarActions`.

## Conflict-Resolution Note

When rebasing search work on top of symlinks work (or vice versa), the main conflict point is the `if (isSearchVisible)` wrapper in `GroupScreen`. The symlinks work adds parameters to `GroupTopBarContent` and `GroupScreenContent`; those additions must land inside the `else` branch of the search wrapper. Both params (`onAddSymlink`, `onSearchClick`) must appear in all three places: the `GroupTopBarContent` signature, the `createTopBarActions` signature, and the `createTopBarActions` call site.
