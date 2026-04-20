---
title: Card composables — Tracker, Group, Function, GraphStatCardView API shape
description: Shared opt-in pattern for the four group-screen card composables — `onClick` and a `ContextMenuCallbacks` data class are both nullable top-level params; a null `contextMenuCallbacks` hides the context-menu icon and a null `onClick` makes the card non-clickable. For `Tracker`, `onAdd`/`onPlayTimer`/`onStopTimer` are deliberately kept outside the context-menu object so search results (and future consumers) can enable them independently.
topics:
  - Two-slot API: `onClick` for the card body tap, `contextMenuCallbacks` for the dropdown
  - Each `XContextMenuCallbacks` data class lives alongside its composable (e.g. `TrackerContextMenuCallbacks` in `Tracker.kt`)
  - Null context-menu callbacks → no menu icon renders; null `onClick` → no `clickable` modifier
  - Tracker-specific nullable extras: `onAdd`, `onPlayTimer`, `onStopTimer` — top-level, not in context menu
  - Why search results get null context menus and null tracker-action callbacks
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

This is deliberate: in the search results grid we want the card tap (`onClick`) to deep-link to the component, and initially nothing else. Later we want to re-enable the add-data-point and start/stop-timer buttons on search-result tracker cards without also re-enabling the context menu. Keeping these callbacks out of the context-menu bundle keeps those two decisions independent.

`onDescription` stays inside `TrackerContextMenuCallbacks` because it only makes sense from the dropdown menu.

## GroupScreen wiring

`GroupScreen.kt` has per-type `*ClickListeners` aggregate data classes (`TrackerClickListeners`, `GraphStatClickListeners`, `GroupClickListeners`, `FunctionClickListeners`). These live at the screen level — they pre-date this split and collect all the actions the screen knows how to perform. The per-item wrapper composables (`TrackerItem`, `GroupItem`, `GraphStatItem`, `FunctionItem` — all in `GroupScreen.kt`) are the thin adapter layer that assembles a per-item `XContextMenuCallbacks` from the aggregate listeners + per-item closures (like the `onDelete` closure that needs the `groupItemId`) and passes `onClick` / the tracker-action lambdas as top-level params.

Don't try to unify `GraphStatClickListeners` and `GraphStatContextMenuCallbacks` — they live at different layers. The screen-level aggregate carries `onClick` because the screen wires that up; the card-level data class intentionally does not, because per-card `onClick` is a card-level concern.

## SearchScreen wiring

Each card in `SearchResultsGrid` is constructed with only `onClick = { onResultClick(item) }`. Context menu callbacks are null (no menu icon on search result cards) and the tracker action callbacks are null (no add / timer buttons on tracker cards in search). See [search-feature.md](search-feature.md) for the tap-handling logic.

## Where bugs are likely

- **Card looks actionable but does nothing**: a wrapper is passing `contextMenuCallbacks = SomeCallbacks(onEdit = {}, onDelete = {}, ...)` with no-op lambdas instead of `null`. The menu icon renders but every item is a no-op. Pass `null` instead.
- **Tracker card shows add button in search**: `onAdd` was accidentally wired up. Search must pass only `onClick`.
- **Minimum card height changes** when context menu is hidden: `Tracker` and `Function` reserve a `buttonSize` box when `contextMenuCallbacks == null` so name/date layout doesn't shift. If you remove that spacer, cards in search will jump vertically relative to the grid cards.
