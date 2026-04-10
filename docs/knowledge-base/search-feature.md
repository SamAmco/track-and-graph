---
title: Search feature — animated top-app-bar field, TextFieldState threading, and in-place screen swap
description: How search entry animates the top app bar into a text field, why the query is a TextFieldState threaded from the ViewModel, the AnimatedContent contentKey trick that prevents per-keystroke retriggering, and the GroupScreen in-place screen swap that underlies it all.
topics:
  - GroupSearchViewModel owning a TextFieldState (not a StateFlow<String>)
  - AppBarConfig.searchBarText drives an animated top-bar title swap
  - AppBarSearchField — Material3 TextField wrapper in ui/compose/ui
  - AnimatedContent contentKey trick — animate only on search enter/exit, not on keystrokes
  - actions slot uses animateContentSize instead of AnimatedContent
  - In-place screen swap in GroupScreen (if/else replaces top bar + content)
  - Why SearchScreen publishes its own clear-button action
keywords: [search, GroupSearchViewModel, SearchScreen, TextFieldState, clearText, AppBarConfig, searchBarText, AppBarSearchField, AnimatedContent, contentKey, animateContentSize, SizeTransform, appBarPinned, in-place, screen-swap, overrideBackNavigationAction, BackHandler, GroupScreen, GroupTopBar, onSearchClick, cursor-position]
---

# Search Feature

## Status

The search feature is still a **stub** for results — `SearchScreen` renders an empty `Box`. What is fully implemented is the UI affordance: the group top app bar animates into a search field, tracks input, and animates back on dismiss. All the pieces needed to wire real results into the empty `Box` are in place.

## TextFieldState, not String

`GroupSearchViewModelImpl.searchQuery` is a `TextFieldState` (not a `StateFlow<String>` / `String` + callback pair). The `TextFieldState` is owned by the ViewModel and threaded all the way down:

```
GroupSearchViewModel.searchQuery: TextFieldState
    → AppBarConfig.searchBarText: TextFieldState?
    → AppBarSearchField(textFieldState = ...)
    → Material3 TextField(state = ...)
```

**Why TextFieldState and not a string+callback:** the first cut used `query: String` + `onQueryChange: (String) -> Unit`, backed by a ViewModel `MutableStateFlow<String>`. Typing anywhere except at the end of the text caused the cursor to jump to the last position — the round-trip (keystroke → callback → VM flow → recomposition → new string prop → `TextField`) loses selection/composition info because string equality is all that survives. `TextFieldState` bundles text + selection + IME composition region into a single observable unit owned outside composition, which is the supported Compose 1.7+ pattern for this. Cursor position is preserved because the state object identity is stable across recompositions.

**Bonus:** because the VM owns the `TextFieldState` directly, it can mutate the query programmatically (e.g. `searchQuery.clearText()` in `hideSearch()`) without any extra plumbing.

## Animated top app bar

`AppBarConfig` has an optional field:

```kotlin
val searchBarText: TextFieldState? = null
```

When non-null, the top bar's title slot renders an `AppBarSearchField` (Material3 `TextField` with transparent container + transparent indicator colors so it blends into the bar). When null, it renders the normal `HeaderTitle`. The swap is wrapped in `AnimatedContent` inside `MainScreen.AppBar`'s `title` slot.

### The contentKey trick (important)

```kotlin
AnimatedContent(
    targetState = config.searchBarText,
    contentKey = { it != null },   // <-- critical
    transitionSpec = { ... },
    ...
) { searchBarText -> ... }
```

`contentKey = { it != null }` makes `AnimatedContent` only run a transition when the *nullness* of `searchBarText` changes — i.e. when search is entered or left. Without it, every keystroke would retrigger the animation, because:

- `SearchScreen` publishes a fresh `AppBarConfig` on every keystroke (the actions lambda closure captures mutable state and compares as a new reference).
- `topBarController.Set` stores `config = newConfig`, so `config.searchBarText` is technically a new state each frame from Compose's perspective — even though the underlying `TextFieldState` object identity is stable.

The `contentKey` reduces that to a single boolean transition.

### Transition spec

```kotlin
fadeIn(tween(220, delayMillis = 90)) togetherWith
    fadeOut(tween(90)) using SizeTransform(false)
```

This is a **staggered** swap, not a cross-fade: the outgoing content fades out quickly (90ms), *then* the incoming content fades in (220ms, delayed 90ms). Chosen over a straight cross-fade because the two contents differ in both width and shape (header text → text field) and overlapping them looks muddy. `SizeTransform(clip = false)` prevents the animating slot size from clipping the contents during the width change.

## Actions slot — `animateContentSize`, not `AnimatedContent`

The `actions` `Row` inside `AppBar` uses `slotInset.animateContentSize()` rather than its own `AnimatedContent`. The search-mode and non-search-mode contents both live in `config.actions`, and the search screen provides a clear button that appears only when the query is non-empty. Wrapping the whole thing in `AnimatedContent` was an earlier iteration; `animateContentSize` is lighter and handles both width changes (search entry/exit *and* clear-button appear/disappear) with one modifier. Trade-off: children pop in/out without individual fades, but combined with the width animation it reads fine.

**Do not use `Modifier.fillMaxSize()` on the inner content of the nav/actions slots** — the Material3 `TopAppBar` gives title slot = `layoutWidth - navWidth - actionsWidth`, and if nav or actions greedily fill width the title collapses to zero and the header text disappears. Use `Modifier.fillMaxHeight()` or rely on the fixed-height `slotInset`. This bug was hit during the initial implementation and is easy to regress.

## SearchScreen publishes its own clear-button action

`SearchTopBarContent` in `SearchScreen.kt` constructs the `AppBarConfig`:

```kotlin
AppBarConfig(
    backNavigationAction = true,
    appBarPinned = true,                    // don't collapse the bar while searching
    overrideBackNavigationAction = onBack,
    searchBarText = searchViewModel.searchQuery,
    actions = {
        if (searchViewModel.searchQuery.text.isNotEmpty()) {
            IconButton(onClick = { searchViewModel.searchQuery.clearText() }) {
                Icon(Icons.Filled.Close, contentDescription = null)
            }
        }
    }
)
```

Two things worth understanding:

1. **The app bar has no knowledge of search semantics.** It just renders whatever `AppBarConfig.actions` provides. The clear button lives in `SearchScreen`, not in `AppBar`. This is the right layering: anyone wanting an in-bar search just publishes a `searchBarText` and whatever actions make sense for their flow.

2. **Reading `searchQuery.text.isNotEmpty()` inside the `actions` lambda is efficient.** The snapshot read happens *when the lambda is invoked*, which is inside `AppBar`'s `Row` trailing lambda. Only that Row resubscribes to the text, not the whole `SearchTopBarContent`. On keystrokes the `Row` recomposes, `animateContentSize` animates the width change, and the `AppBarSearchField` (which is a sibling slot with a stable `TextFieldState` identity) does not even recompose.

3. `appBarPinned = true` switches `MainScreen` to the pinned scroll behavior for the duration of the search, so the bar doesn't collapse away while you're looking at results.

## In-place screen swap (unchanged from before)

`GroupScreen` still conditionally renders `SearchScreen` *or* the normal group content based on `searchViewModel.isSearchVisible`. Both branches publish their own `AppBarConfig` via `topBarController.Set`. When search opens, the config swap triggers the `AnimatedContent` transition in the title slot; the whole top bar animates in place with no navigation. `BackHandler` + `overrideBackNavigationAction` both route to `hideSearch()`, which also calls `searchQuery.clearText()` so the next entry starts fresh.

## AppBarSearchField — ui/compose/ui

The reusable piece lives in `ui/compose/ui/AppBarSearchField.kt`. It's a thin wrapper over Material3 `TextField` (state-based overload):

- Transparent container + transparent indicator colors, so only cursor and text remain visible over the app bar.
- `titleMedium` typography (smaller than the header's `headlineSmall` — important for visual balance).
- `TextFieldLineLimits.SingleLine`.
- `ImeAction.Done` — search-as-you-type does the work; the enter key just dismisses the keyboard.
- Auto-focus via `FocusRequester` + `LaunchedEffect(Unit)` on appearance.

**Placeholder is hardcoded to `R.string.search`.** Intentional — the component is currently group-specific. If a second in-bar search shows up, add an optional `placeholder` parameter rather than copy-pasting.

## What to look at first if something breaks

- **Cursor jumps back / typing at the end inserts in the middle**: something replaced `TextFieldState` with a `String` + `onValueChange` round-trip. Don't.
- **Header text vanishes when there's no search**: something set `fillMaxSize()` on the inner content of the nav or actions slot; the title got 0 width. See the warning above.
- **Title animation retriggers on every keystroke**: `contentKey = { it != null }` got removed from the `AnimatedContent` call.
- **Search bar collapses when scrolling results**: `appBarPinned = true` missing from the search `AppBarConfig`.
- **Clear button doesn't refresh with typing**: the `actions` lambda isn't reading `searchQuery.text` directly — it's probably reading a captured snapshot. Read the state *inside* the lambda body so the snapshot read happens at invocation time.

## Conflict-resolution note

When rebasing search work on top of symlinks work (or vice versa), the main conflict point is the `if (isSearchVisible)` wrapper in `GroupScreen`. Any new parameters on `GroupTopBarContent` / `createTopBarActions` must land inside the `else` branch of that wrapper, and must appear in all three places: the `GroupTopBarContent` signature, the `createTopBarActions` signature, and the `createTopBarActions` call site.
