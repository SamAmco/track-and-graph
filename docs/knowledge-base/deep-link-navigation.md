---
title: Deep-link navigation — DeepLink, descent-relative paths, navigator, back-stack append
description: How in-app deep-link navigation works — the DeepLink sealed type carries a pre-resolved GroupDescentPath (a chain from the user's current GroupScreen to the destination); callers compute paths client-side because a component can have multiple placements when ancestors are symlinked; DeepLinkNavigator is provided via LocalDeepLinkNavigator CompositionLocal; applyGroupDescentPath appends onto the current back stack.
topics:
  - DeepLink is a narrow in-app data type; only ToGroupItem(descent) exists today
  - Paths are descent-relative — anchored at the user's current GroupScreen, not at root
  - Paths are resolved client-side by whoever already holds the GroupGraph (not a data-layer call)
  - A component can have N paths when ancestor groups (below the current location) are symlinked
  - GroupSearchViewModel.buildResolvedPaths walks the graph once from the current group, recording every placement under a ComponentKey(type, id)
  - ResolvedPath pairs the navigable GroupDescentPath with a slash-formatted displayString for the symlink disambiguation dialog
  - SearchResultItem.paths.size drives UI branching — 1 navigates directly, >1 opens SymlinksDialog in tap-to-navigate mode
  - DeepLinkNavigator is provided via LocalDeepLinkNavigator CompositionLocal (matches LocalTopBarController precedent)
  - DeepLinkNavigatorImpl is not @Singleton — it captures the NavBackStack which is per-composition
  - applyGroupDescentPath APPENDS onto the current back stack — pushes one GroupNavKey per id; last entry carries scrollToGroupItemId
  - Empty groupIds → top entry is replaced with a copy carrying the scroll hint, no nav transition
  - Pressing back returns the user to their original location, not to root — a direct consequence of append semantics
  - GroupNavKey.scrollToGroupItemId is consumed once by GroupScreen via a rememberSaveable flag keyed on the target id
  - Scroll must run in composition scope — lazyGridState.animateScrollToItem needs a MonotonicFrameClock that viewModelScope does not provide
  - Search must hide before navigating — the nav back-stack only covers nav3 destinations, not the in-place search overlay
keywords: [deep-link, DeepLink, DeepLinkNavigator, DeepLinkNavigatorImpl, LocalDeepLinkNavigator, GroupDescentPath, ResolvedPath, SearchResultItem, applyGroupDescentPath, descent-relative, append-semantics, scrollToGroupItemId, GroupNavKey, NavBackStack, CompositionLocal, staticCompositionLocalOf, navigation3, symlink, disambiguation, SymlinksDialog, SymlinksDialogContent, onPathClick, search, SearchScreen, ComponentKey, buildResolvedPaths, walkPaths, rememberSaveable, MonotonicFrameClock, animateScrollToItem, LaunchedEffect, hideSearch]
---

# Deep-link navigation

End-to-end pipeline: UI tap → caller resolves a `GroupDescentPath` from the data it already holds → `LocalDeepLinkNavigator.current.navigate(DeepLink.ToGroupItem(descent))` → `applyGroupDescentPath` appends onto the current back stack → the landing `GroupNavKey` carries `scrollToGroupItemId` → `ScrollToGroupItemEffect` (inside `GroupScreenContent`) scrolls the grid.

## Paths are descent-relative, anchored at the user's current location

`GroupDescentPath.groupIds` is the chain from the user's *current* `GroupScreen` down to the destination's parent group; it excludes both the anchor and the final component (identified by `groupItemId`). An empty `groupIds` means the target lives directly in the current group.

An earlier design had the path go root-to-component. That's fragile when the user is inside a group that itself has multiple parents via symlinks — there's no single correct prefix from root to reconstruct. A descent path sidesteps the problem entirely: we never need to know how the user got to where they are, we just append onto whatever stack they built. A useful side effect: pressing back returns them to the original location rather than jumping to root.

## Why path resolution is client-side, not a data-layer call

A single component can be reachable via more than one descent path when any ancestor group **between the current location and the component** is symlinked. If group `A` is reachable from the current group via both `Sub1/A` and `Sub2/A`, then a tracker `T` inside `A` has two distinct descents even though `T` itself is placed only once. Any API that returns "the path" has to either pick one (losing information) or return multiple.

An earlier attempt put a `getGroupPathForGroupItem` in the data layer that picked a deterministic placement by lowest group-item id and returned a single `List<Group>`. That silently papered over the ambiguity and hid it from the UI — fine for breadcrumbs, wrong for deep-link tap behaviour where the user ought to be asked "open in which location?".

The current design moves the walk into whichever caller already holds the relevant `GroupGraph`:

- **Search (`GroupSearchViewModel.buildResolvedPaths`)** walks its `getGroupGraphSync(...)` result once at `showSearch()` time, starting from the current group. Every component encountered is recorded under a `ComponentKey(type, id)` with a `ResolvedPath(descent, displayString)` for that placement. A single DFS touches every reachable placement of every component — including the extra placements implied by symlinked ancestors — in one pass.
- **`walkPaths` cycle guard**: `visitedGroupIds` tracks the current descent frame only, not every group ever seen, so a group with multiple parents is still walked once per parent. Only a group appearing as its own ancestor (shouldn't happen per app invariants) is skipped. **Must be a mutable set** — defaulting to `emptySet()` crashes because `java.util.Collections.emptySet()` is immutable even when assigned to `MutableSet`.
- **Any future caller** that also holds the graph can build its own `ResolvedPath` list the same way. The data-layer API deliberately does not help here.

The downside is the O(paths) walk instead of O(depth), but paths are computed once per search session (stable for the duration), not per keystroke.

## DeepLink surface

- **`DeepLink` (data, `navigation/DeepLink.kt`)** — sealed interface. Only `ToGroupItem(descent: GroupDescentPath)` exists. External entry points (notifications, widgets, URI schemes) will add variants here. Keep this narrow — one funnel for all "navigate to thing" intents.
- **`GroupDescentPath` (data, `navigation/GroupDescentPath.kt`)** — `(groupIds: List<Long>, groupItemId: Long)`. Carries only ids so any caller can construct one with minimal information — no `Group` DTOs needed. Semantically a descent chain; see the section above.
- **`DeepLinkNavigator` (`navigation/DeepLinkNavigator.kt`)** — interface + CompositionLocal + impl. The impl is *not* `@Singleton` because it holds a reference to the `NavBackStack`, which is created per `MainScreen` composition via `rememberNavBackStack`.
- **`applyGroupDescentPath` (extension on `NavBackStack<NavKey>`)** — imperative glue. Lives next to the rest of the back-stack wiring conceptually (same module, same file as the navigator). Pushes one `GroupNavKey` per id (outer-to-inner); the last entry carries `scrollToGroupItemId`. Empty `groupIds` replaces the top entry with a `copy(scrollToGroupItemId = …)` so the current screen scrolls without a nav transition.

## Why CompositionLocal over callback plumbing

Navigation is genuinely ambient — any screen, any depth may want to fire a deep-link. Threading callbacks through `GroupScreen` → `SearchScreen` (and whatever comes next — breadcrumbs, notification landing screens) bloats every screen signature and leaks nav concerns. The app already uses this pattern for `LocalTopBarController` at the `MainScreen` level; `LocalDeepLinkNavigator` mirrors it exactly and is provided in the same `CompositionLocalProvider` call.

The navigator is constructed via `remember(backStack) { DeepLinkNavigatorImpl(backStack) }` so it rebuilds only if the back stack identity changes.

## Symlink disambiguation — dialog in tap-to-navigate mode

When a search result's `SearchResultItem.paths` has more than one entry, tapping opens `SymlinksDialogContent` with an `onPathClick: ((Int) -> Unit)?` that fires `DeepLink.ToGroupItem(descent)` for the chosen placement. Single-path results navigate directly. See `SymlinksDialog.kt` for the dual-mode API (VM-backed info-only vs stateless tap-to-navigate) and [search-feature.md](search-feature.md) for the full tap flow.

## Consume-once scroll

`GroupNavKey.scrollToGroupItemId: Long?` is part of the serializable nav key (so it survives process death). The scroll is driven by a dedicated `ScrollToGroupItemEffect` composable invoked from `GroupScreenContent` (search's `if` branch does not render the grid, so there's no point starting the effect there). It does **not** route through the ViewModel.

The effect (1) waits for the target placement to appear in `currentChildren`, (2) waits for the grid to lay out that index via a `snapshotFlow` on `lazyGridState.layoutInfo.totalItemsCount`, (3) delays 200 ms for the layout to settle, then (4) calls `animateScrollToItem` and sets a `rememberSaveable(targetId)` consumed flag.

**Why composable-scoped, not viewModelScope**: `LazyGridState.animateScrollToItem` requires a `MonotonicFrameClock` in the coroutine context. That clock is only installed for coroutines launched from composition — a `viewModelScope.launch { lazyGridState.animateScrollToItem(…) }` throws `IllegalStateException: A MonotonicFrameClock is not available in this CoroutineContext`. An earlier revision had a VM-side `init { viewModelScope.launch { … } }` collector driving the scroll and hit this exact crash at runtime. Any scroll animation must run in a composition-scoped coroutine (`LaunchedEffect`, `rememberCoroutineScope`, etc).

**Why `rememberSaveable` for consume-once**: the target id lives on the (serializable) NavKey, so recomposition or process-death restore would otherwise re-fire the scroll after the user has manually scrolled away. The saveable flag is keyed on the target id, so a *new* deep-link (different id on a new NavKey) starts fresh.

**Why wait for layout before scrolling**: `currentChildren` emits from the VM before the `LazyVerticalGrid` has composed and laid out those items. Calling `animateScrollToItem(index)` against a not-yet-laid-out grid either silently no-ops or snaps instantly with no animation. Polling `layoutInfo.totalItemsCount` via `snapshotFlow { }.first { it >= index + 1 }` gates the scroll on the grid actually knowing about the target row.

**Why the 200 ms settle delay**: even after `totalItemsCount` catches up, kicking off `animateScrollToItem` immediately makes the first animation frames get dropped and the scroll reads as instant. A small delay lets the grid finish its layout pass so the animation runs smoothly. If the number feels wrong later, this is the knob.

**If the target never appears** (renamed/deleted between resolve and navigate), the effect simply stays suspended on `first { }` and is cancelled when the user navigates away — the user still lands on the correct group, just no scroll.

## Search is in-place — must hide before deep-linking

Search is not a separate nav destination. `GroupScreen` conditionally renders `SearchScreen` *or* the normal content based on `GroupSearchViewModel.isSearchVisible` (see [search-feature.md](search-feature.md)). The root `GroupScreen`'s `searchViewModel` is a long-lived `hiltViewModel` tied to the root nav entry, so `isSearchVisible` survives deep-link navigation.

Without explicit handling, tapping a search result → deep-linking → pressing back would land the user back inside the still-open search. The search tap handler calls `searchViewModel.hideSearch()` *before* `navigator.navigate(...)`. Any future deep-link entry point that starts from a possibly-open search must do the same.

## Title flicker avoidance — GroupViewModel.groupName fallback

Deep-link navigation constructs `GroupNavKey(groupId = X)` without a `groupName` (we don't have it at resolve time). If the top-bar title only read `navArgs.groupName`, users would see an empty title until the viewmodel's `setGroup(X)` loaded the group. `GroupViewModel.groupName: StateFlow<String?>` exposes the VM-resolved name so `GroupTopBar` can render `navArgs.groupName ?: vmGroupName ?: ""`. Callers that already know the name (normal in-app navigation from `NavigationHost`) still pass it on the NavKey and get no flicker; deep-link landings get the VM fallback.

## Where to look first if something breaks

- **Deep link does nothing, no crash**: the caller is resolving to an empty `ResolvedPath` list. Check that the component actually appears in the caller's `GroupGraph`.
- **Wrong path / wrong group chain**: check `walkPaths` — is it prepending at the right level? Remember `groupIds` is the descent chain **below the current group and above the final component**, excluding both.
- **Crash on opening search with `UnsupportedOperationException` from `AbstractCollection.add`**: `walkPaths`'s `visitedGroupIds` parameter defaulted to `emptySet()` (an immutable `Collections.EMPTY_SET`). Must be `mutableSetOf()`.
- **Scroll doesn't happen after navigate**: `currentChildren` flow not emitting the target id (renamed? filtered out in drag mode?), the `LaunchedEffect` got cancelled before the item appeared, or the `ScrollToGroupItemEffect` is mounted on a branch that doesn't render the grid.
- **Scroll snaps instantly, no animation**: grid wasn't laid out when `animateScrollToItem` ran — check the `snapshotFlow { layoutInfo.totalItemsCount }.first { }` wait, and the 200 ms settle delay.
- **Scroll fires on every recompose / every nav**: `rememberSaveable` key is wrong (should be the target id, not `Unit`) so the consumed flag is reset. Or the scroll was moved back into viewModelScope — see the MonotonicFrameClock note.
- **Back from deep-link lands in search, not the group**: tap handler didn't call `searchViewModel.hideSearch()` before `navigator.navigate(...)`. Search is rendered in-place by `GroupScreen`, not a separate nav entry, so the flag must be cleared explicitly.
- **`IllegalStateException: A MonotonicFrameClock is not available`**: `lazyGridState.animateScrollToItem` is running in viewModelScope. It must run in a composition-scoped coroutine (LaunchedEffect, rememberCoroutineScope).
- **Top bar title empty for a moment after deep-link**: `GroupTopBar` reads only `navArgs.groupName` without the VM fallback — add `groupViewModel.groupName.collectAsStateWithLifecycle()`.
- **Preview / tests fail after adding a new MainScreen parameter**: `MainActivity` and any test harness must be updated to pass the new dependency through.
