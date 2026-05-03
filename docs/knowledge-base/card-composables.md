---
title: Card composables — Tracker, Group, Function, GraphStatCardView API shape
description: Shared opt-in pattern for the four group-screen card composables — `onClick` and a `ContextMenuCallbacks` data class are both nullable top-level params; a null `contextMenuCallbacks` hides the context-menu icon and a null `onClick` makes the card non-clickable. For `Tracker`, `onAdd`/`onPlayTimer`/`onStopTimer` are deliberately kept outside the context-menu object so callers can enable them independently — search currently uses this to provide tap + add + play/stop-timer without surfacing a context menu.
topics:
  - Two-slot API: `onClick` for the card body tap, `contextMenuCallbacks` for the dropdown
  - Each `XContextMenuCallbacks` data class lives alongside its composable (e.g. `TrackerContextMenuCallbacks` in `Tracker.kt`)
  - Null context-menu callbacks → no menu icon renders; null `onClick` → no `clickable` modifier
  - Tracker-specific nullable extras: `onAdd`, `onPlayTimer`, `onStopTimer` — top-level, not in context menu
  - Why search results get null context menus but non-null tracker-action callbacks
keywords: [Tracker, Group, Function, GraphStatCardView, TrackerContextMenuCallbacks, GroupContextMenuCallbacks, FunctionContextMenuCallbacks, GraphStatContextMenuCallbacks, context-menu, card-onClick, onPlayTimer, onStopTimer, onAdd, SearchScreen, search, GroupScreen, TrackerClickListeners, GraphStatClickListeners, GroupClickListeners, FunctionClickListeners]
---

# Card composables

The four cards rendered inside a group's grid — `Tracker`, `Group`, `Function`, `GraphStatCardView` — share an API shape:

```kotlin
onClick: ((T) -> Unit)? = null
contextMenuCallbacks: XContextMenuCallbacks? = null
```

- `onClick == null` → the card is not `clickable`.
- `contextMenuCallbacks == null` → the three-dot menu icon (and its dropdown) is not rendered at all.

Each `XContextMenuCallbacks` is a non-nullable-field data class (Edit/Delete/MoveTo/Symlinks + any type-specific items like Duplicate for Function/Graph and Description for Tracker). When callers want the menu, they must provide every action — there is no partial-menu mode.

## Why `onClick` is separate

Earlier, the click-through to open a tracker's history / graph's detail view was bundled into `GraphStatClickListener` / the equivalent wrappers. That made it impossible for a caller to want "card tap works, but no context menu" without constructing a bogus listener with no-op delete/edit lambdas — which in turn caused bugs where the card appeared actionable (menu icon visible) but the actions were all no-ops. Splitting the two means callers express the two intentions independently.

## Why Tracker's action buttons are outside the context-menu object

`Tracker` exposes `onAdd`, `onPlayTimer`, `onStopTimer` as individual nullable top-level params, **not** as fields on `TrackerContextMenuCallbacks`. Each button renders only if its callback is non-null.

This is deliberate: the search results grid wants the card tap (`onClick`) to deep-link to the component **and** the add-data-point / start/stop-timer buttons enabled, but no context menu. Keeping these three callbacks out of the context-menu bundle lets those two decisions be made independently.

`onDescription` stays inside `TrackerContextMenuCallbacks` because it only makes sense from the dropdown menu.

`LoadingTracker` also accepts a nullable `onClick: (() -> Unit)?` so search can deep-link from tracker placeholders before the `DisplayTracker` has loaded. It deliberately does not expose add/timer/context-menu actions, because those require the full `DisplayTracker`.

## GroupScreen wiring

`GroupScreen.kt` has per-type `*ClickListeners` aggregate data classes (`TrackerClickListeners`, `GraphStatClickListeners`, `GroupClickListeners`, `FunctionClickListeners`). These live at the screen level — they pre-date this split and collect all the actions the screen knows how to perform. The per-item wrapper composables (`TrackerItem`, `GroupItem`, `GraphStatItem`, `FunctionItem` — all in `GroupScreen.kt`) are the thin adapter layer that assembles a per-item `XContextMenuCallbacks` from the aggregate listeners + per-item closures (like the `onDelete` closure that needs the `groupItemId`) and passes `onClick` / the tracker-action lambdas as top-level params.

Don't try to unify `GraphStatClickListeners` and `GraphStatContextMenuCallbacks` — they live at different layers. The screen-level aggregate carries `onClick` because the screen wires that up; the card-level data class intentionally does not, because per-card `onClick` is a card-level concern.

### Hoisted tracker actions + dialog

The three tracker action lambdas (`onTrackerAdd`, `onTrackerPlayTimer`, `onTrackerStopTimer`) and the `AddDataPointsDialog` render are declared at the outer `GroupScreen` level, above the `if (isSearchVisible)` branch. The same lambdas are passed into both `SearchScreen` and `GroupScreenContent`. Rationale:

- **Shared "use default vs. open dialog" logic.** `onTrackerAdd` branches on `tracker.hasDefaultValue && useDefault` → `groupViewModel.addDefaultTrackerValue(tracker)` vs. `addDataPointsDialogViewModel.showAddDataPointDialog(...)`. Defining it once avoids duplicating that branch in two places.
- **Dialog persists across search open/close.** `AddDataPointsDialog` is rendered once at the outer level, outside the if/else, so opening or closing search doesn't tear down and re-create the dialog state. The dialog's ViewModel (`AddDataPointsViewModelImpl` obtained via `hiltViewModel<...>()`) is stable across the in-place screen swap for the same reason.
- Notification-permission requester is hoisted alongside because `onTrackerPlayTimer` calls it.

## SearchScreen wiring

Cards in `SearchResultsGrid` are built with `onClick = { onResultClick(item) }` and `contextMenuCallbacks = null` (no menu icon). Tracker loading placeholders get the same tap-to-navigate handler, but not add/timer actions. Loaded tracker cards additionally get `onAdd`, `onPlayTimer`, `onStopTimer` wired to the hoisted lambdas from `GroupScreen`. See [search-feature.md](search-feature.md) for the tap-handling logic.

Tracker card state in search results is **live** — `GroupSearchViewModelImpl.trackerDataMap` is a `MutableStateFlow<Map<Long, DisplayTracker>>` that's plumbed into the `displayResults` combine and refreshed targetedly on `DataUpdateType.DataPoint` events while search is open. So tapping `+` (or play / stop on a timer) updates the card's last-value / timestamp / timer display in place, the same way the group-screen cards do. Graph cards in the result set also recompute via `DataUpdateType.GraphOrStatUpdated`. Structural changes (renames, new/deleted components, new symlinks) are NOT reflected — close and reopen search to pick those up. See [search-feature.md](search-feature.md#live-updates-to-tracker-and-graph-display-data).

## Where bugs are likely

- **Card looks actionable but does nothing**: a wrapper is passing `contextMenuCallbacks = SomeCallbacks(onEdit = {}, onDelete = {}, ...)` with no-op lambdas instead of `null`. The menu icon renders but every item is a no-op. Pass `null` instead.
- **Tracker add / timer button does nothing in search**: the three tracker action lambdas are hoisted in the outer `GroupScreen` and passed to `SearchScreen`. If they aren't threaded through `SearchScreenContent` → `SearchResultsGrid` → the `Tracker(...)` call, the buttons still render (non-null) but closure state is wrong. Check the param chain.
- **Add-data-point dialog disappears when you open/close search**: the dialog must be rendered at the outer `GroupScreen` level, not inside `GroupScreenContent` (otherwise it gets torn down when the if/else swaps branches).
- **Minimum card height changes** when context menu is hidden: `Tracker` and `Function` reserve a `buttonSize` box when `contextMenuCallbacks == null` so name/date layout doesn't shift. If you remove that spacer, cards in search will jump vertically relative to the grid cards.
