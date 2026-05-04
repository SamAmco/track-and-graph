---
title: Search feature — animated top-app-bar field, TextFieldState threading, in-place screen swap, fuzzy search, and multi-path disambiguation
description: How search entry animates the top app bar into a text field; why the query is a TextFieldState threaded from the ViewModel; the AnimatedContent contentKey trick; lazy flat-list build on search open; ranked fuzzy matching via FuzzyMatcher; SearchResultProcessor streams progressively populated results and listens for live data updates; each SearchResultItem carries every ResolvedPath to its component so multi-path (symlinked) results open a disambiguation dialog on tap; FeatureHistoryScreen and NotesScreen use the same top-bar search affordance but filter chronologically with case-insensitive substring matching.
topics:
  - GroupSearchViewModel owning a TextFieldState (not a StateFlow<String>)
  - AppBarConfig.searchBarText drives an animated top-bar title swap
  - AppBarSearchField — Material3 TextField wrapper in ui/compose/ui
  - AnimatedContent contentKey trick — animate only on search enter/exit, not on keystrokes
  - actions slot uses animateContentSize instead of AnimatedContent
  - In-place screen swap in GroupScreen (if/else replaces top bar + content)
  - Why SearchScreen publishes its own clear-button action
  - SearchableItem flat list built once on showSearch() — structural snapshot only (paths, names, GroupGraphItem)
  - SearchResultProcessor owns per-session tracker/graph display-data cache and streams Flow<List<SearchResultItem>>
  - Processor emits placeholders immediately, then populates trackers/graphs in rank-order batches with trackers first
  - Query switching depends on cooperative cancellation in graph view-data calculation
  - Processor starts DataUpdateType listening immediately; versioned cache writes stop stale initial batch results overwriting fresher event refreshes
  - Structural changes (new/deleted/renamed components, new symlinks) are intentionally ignored during an open search session
  - FuzzyMatcher — ranked subsequence matching with DP alignment
  - SearchResultItem pairs a rendered GroupChild with every ResolvedPath (one per placement when ancestors are symlinked)
  - Tap handler branches on paths.size — 1 navigates directly, >1 opens SymlinksDialogContent in tap-to-navigate mode
  - SymlinksDialogContent has optional onPathClick — dual-mode dialog (info vs tap-to-navigate)
keywords: [search, GroupSearchViewModel, SearchResultProcessor, SearchResultCache, SearchScreen, NotesScreen, TextFieldState, clearText, AppBarConfig, searchBarText, AppBarSearchField, AnimatedContent, contentKey, animateContentSize, SizeTransform, appBarPinned, in-place, screen-swap, overrideBackNavigationAction, BackHandler, GroupScreen, GroupTopBar, FeatureHistoryScreen, onSearchClick, cursor-position, GroupGraph, groupItemId, debounce, FuzzyMatcher, matches, contains, substring, SearchableItem, SearchResultItem, RankedItem, ResolvedPath, score, fuzzy, ranked, chronological, filter, decimal, DataPoint, getDisplayValue, DP, displayTracker, reactive, live-updates, DataUpdateType, GraphOrStatUpdated, tryGetTrackerByFeatureId, featureId, collectSearchableItems, scoreItem, buildResolvedPaths, walkPaths, ComponentKey, SymlinksDialog, SymlinksDialogContent, onPathClick, disambiguation, cancellation, cooperative-cancellation, LineGraphDataFactory, BarChartDataFactory, LuaFunctionDataSourceAdapter]
---

# Search Feature

## Search Data Flow

`GroupSearchViewModelImpl` builds a flat `List<SearchableItem>` lazily — only when `showSearch()` is called. The group graph is fetched from the DB, the tree is walked once, and each node becomes a `SearchableItem` with pre-extracted name/description strings, a pre-computed `List<ResolvedPath>` (see below), and the `GroupGraphItem` needed for rendering/fetching display data later. On `hideSearch()` the list is cleared and the processor cache is disposed.

The UI-facing output is `SearchResultItem(child: GroupChild, paths: List<ResolvedPath>)`. `child.groupItemId` is the list key (see [group-hierarchy.md](group-hierarchy.md) for why entity IDs cannot be used as unique keys). `paths` is consumed by the tap handler — see "Tapping a result" below.

## Chronological Filter Search

`FeatureHistoryScreen` and `NotesScreen` reuse the same app-bar search affordance (`AppBarConfig.searchBarText`, `TextFieldState`, clear action, pinned app bar, back override), but intentionally do **not** reuse the group search result model. These searches are chronological filters, not relevance-ranked navigation:

- Blank query leaves the normal history list visible.
- Non-blank query filters the existing `DateScrollData.items` in their current order; it does not sort by fuzzy score.
- Empty filtered results show the existing `no_results` message, while genuinely empty history keeps the normal empty-history message.
- On feature history, search is only available outside multi-select mode. While search is open, long-press multi-select entry is suppressed so the top bar cannot enter both modes at once.

Feature history searches the displayed value, label, and note. The displayed value should come from the same formatting used by the card/dialog (`DataPoint.getDisplayValue(isDuration)`) so duration values and labels match what the user sees. Feature history also adds decimal-separator variants of the displayed value (`.` and `,`) so users can find numeric values regardless of regional decimal notation.

Notes search must respect the global-note/data-point-note toggles before applying the query filter. It searches note text, feature name, and feature path.

Because the result order is chronological and no score is displayed, use case-insensitive substring matching (`target.contains(query, ignoreCase = true)`) rather than fuzzy subsequence matching. Fuzzy matching works well when weak matches can be pushed down by relevance ranking; in chronological filters, weak fuzzy matches stay in date order and tend to add noise.

## Paths are resolved once, per-component, and are multi-valued

A single component can be reachable by more than one path through the group DAG — not only when the component itself is symlinked, but also when any of its **ancestors** are. `buildResolvedPaths` does one DFS of the `GroupGraph` at search-open time and records every placement it sees under a `ComponentKey(type, id)`. `walkPaths` uses a `visitedGroupIds` set scoped to the **current descent**, not a global seen-set, so a symlinked group is still walked once per parent — that's the whole point.

Each `ResolvedPath` bundles a `GroupDescentPath` (the navigable value — see [deep-link-navigation.md](deep-link-navigation.md)) with a slash-formatted `displayString` for the disambiguation dialog. The display string is **descent-relative** — anchored at the user's current group, not at root — so a direct child of the current group renders as `/Leaf`, a nested match as `/SubGroup/Leaf`. This matches the navigable path exactly (no implicit root prefix the user can't see), and the dialog becomes a "pick which descent" picker in multi-path cases.

### Display state is a sealed class, not a list + boolean pair

`displayResults` is a `StateFlow<SearchDisplayState>` with three variants: `Empty` (blank query — show "type to search"), `Loading` (work in flight — show spinner), `Results(items)` (done — show grid, or "no results" if the list is empty). The UI does a single `when` and there is no way to end up in an ambiguous state like "not loading, empty list, empty query".

Earlier versions had a separate `isSearchLoading: StateFlow<Boolean>` and a `displayResults: StateFlow<List<GroupChild>>`. That produced visibly wrong sequencing: opening search flashed a spinner (graph still loading) → empty list (graph loaded but no query) → no-results text (query typed, scoring not complete). The correct sequence is "type to search" on open, spinner only while a non-empty query is being scored, then results.

### flatMapLatest for scoring and progressive result processing

```
queryText (snapshotFlow of searchQuery.text)
  └─ flatMapLatest { query ->         // RxJava switchMap equivalent
       if (query.isBlank()) flowOf(SearchDisplayState.Empty)
       else flow {
         emit(SearchDisplayState.Loading)  // immediate — before the debounce
         delay(150)                     // debounce inside the flow
         val items = searchState.filterIsInstance<Ready>().first().items
         val ranked = score + sort
         emitAll(processor.process(ranked).map { SearchDisplayState.Results(it) })
       }
     }
```

**Why flatMapLatest upstream:** a new query must cancel the in-flight scoring+debounce for the previous query, so only the latest query's result can ever reach the UI. Debouncing is done with `delay(150)` inside the flow (not an upstream `.debounce()`) because we want `Loading` to be emitted *before* the delay — the UI flips to the spinner on the first keystroke without waiting 150 ms.

`SearchResultProcessor.process(ranked)` also runs inside the `flatMapLatest` body. It returns a `Flow<List<SearchResultItem>>`, so a new query cancels both the previous query's scoring and its in-flight result population/listener work. The public `SearchDisplayState` only has what the UI needs: `Empty`, `Loading`, or `Results(items)`.

### Query switching waits for cancellation to complete

`flatMapLatest` starts the new query branch only after the previous inner flow has cancelled. That means the UI can still look stale after a new query has been observed if the old `SearchResultProcessor` flow is stuck in a slow non-cooperative graph calculation. This can be misread as slow scoring, but the symptom is different: logs show the new query arriving, then a gap before the previous branch completes and the new branch emits `Loading`.

The processor intentionally limits concurrent graph work to the current batch, so the expected cancellation cost is "cancel the active graph calculations", not "cancel hundreds of jobs". If stale results linger, inspect graph view-data calculation first:

- `LineGraphDataFactory` and `BarChartDataFactory` should rethrow `CancellationException` from broad `catch (Throwable)` blocks.
- Long synchronous loops in graph calculators should check `currentCoroutineContext().ensureActive()` periodically.
- A remaining hard case is a single `DataSample.iterator().next()` call that performs substantial function/Lua work before returning. Cancellation checks around the iterator boundary cannot interrupt work inside Lua's generator call or data-source helper loops; target the function/Lua data-source pipeline if logs show delays concentrated there.

During the search responsiveness work, the useful diagnostic shape was: query-observed timestamp, branch start/completion, processor finish/cancel, graph batch start, and per-graph start/cancel/finally with graph id/type/name. The important measurement is the time from "new query observed" to "old branch completed"; that is the user-visible delay before the new query can show its `Loading` state.

## Why a Flat List (not re-traversing the tree)

The group graph tree structure is only needed once (to build the flat list). Re-traversing the tree on every debounced keystroke is wasted work — flattening up-front means each keystroke is a simple linear scan of the list. The flat list is also the snapshot that supports the "no structural changes while open" behaviour described under "Live updates" below — paths, set of items, and names are frozen at `showSearch()` time.

## Tracker and graph display state — processor-owned cache

`SearchableItem` is a structural snapshot — it carries the component's `name`, `description`, `paths`, `typeBonus`, and `GroupGraphItem`. It does NOT carry a `DisplayTracker` or calculated graph view data. After scoring, each hit is converted into a `RankedItem` and handed to `SearchResultProcessor`.

`SearchResultProcessor` owns the per-session display-data cache. The first processor emission renders the full ranked list immediately, using cache hits where available and loading placeholders for missing trackers/graphs. It then processes missing data in rank-order batches (`BATCH_SIZE = 12`): trackers in the batch are fetched first and emitted, then graphs are calculated and emitted. This keeps the first visible results ahead of lower-ranked work and avoids a slow graph blocking tracker cards in the same batch.

On `hideSearch()`, `processor.dispose()` clears this cache. Repeated queries during the same open search can reuse completed tracker/graph data, but closing search releases that memory.

Because batches are sequential, a graph calculation that never completes in batch N blocks all later batches, including trackers in batch N+1. If search results show both graph cards and display trackers stuck on loading, inspect the active graph batch first rather than assuming tracker fetches are failing. Function-backed graphs are a common stress case because rapid query changes can cancel many `FunctionGraphDataSample` constructions; see [lua-architecture.md](lua-architecture.md#lua-vm-lease-ownership) for the VM lease ownership rule.

## Live updates to tracker and graph display data

While the processor flow for a non-empty query is active, it subscribes to `dataInteractor.getDataUpdateEvents()` immediately after emitting the initial placeholder list. This listener runs concurrently with the initial rank-order batch population:

- `DataUpdateType.DataPoint` and `DataUpdateType.TrackerUpdated` refresh the matching tracker by `featureId`, update the processor cache, replace that one list item, and re-emit the current list.

- `DataUpdateType.GraphOrStatUpdated` invalidates the matching graph cache entry, emits the graph loading placeholder for that item, recalculates its view data, then replaces that one list item and re-emits.

The graph job deliberately does NOT listen for `DataPoint`. The data layer already fans data-point writes out to one `GraphOrStatUpdated(graphStatId)` per dependent graph via `DependencyAnalyser`, so subscribing to `DataPoint` here would duplicate work. See [helper-classes.md](helper-classes.md#data-point-writes-fan-out-to-graphorstatupdated-via-dependencyanalyser) for the fan-out contract.

Because event listening overlaps with initial batch work, the cache tracks a simple version per tracker/graph. Initial batch work captures the version before it starts; event invalidation increments the version. A completed batch result is only applied if the version is still current, preventing stale work from overwriting a fresher event-triggered refresh.

### Structural changes are intentionally ignored while search is open

New / deleted / renamed components, new symlinks, group renames — none of these are observed during an open session. The `SearchableItem` list (paths, names, item set) is a snapshot taken at `showSearch()` time. The user closes and re-opens search to pick up structural changes. This keeps the implementation small (no path-rebuilding, no fuzzy-score re-ranking on structural events) and matches the "search is a transient overlay" interaction model. `TrackerUpdated` is targeted by `featureId` when available; other structural updates still require closing and reopening search.

## FuzzyMatcher

`FuzzyMatcher.score(query, target)` in `util/FuzzyMatcher.kt` returns `Double?` — null means no match, non-null is the match quality score (higher = better). Matching is case-insensitive subsequence: all query characters must appear in the target in order, with any characters between them.

### Scoring

Uses dynamic programming to find the **best-scoring alignment** of query characters into the target (not the first valid alignment). This matters: "abc" in "aXbXcXabc" should prefer the trailing consecutive run. Score components (all additive):

- +10 per matched character (base)
- +15 consecutive run bonus (each char that immediately follows the previous match)
- +10 word-boundary bonus (match lands at start-of-string, after space/dash/underscore, or at a camelCase lowercase→uppercase transition)
- +20 prefix bonus (first query char lands at position 0)
- +50 exact match bonus (query equals target, case-insensitive, trimmed)
- +10 case-exact bonus (target contains query with original casing)

### Multi-field Scoring

Items with both a name and description (Tracker, Function) are scored twice and the higher score wins, with a 0.8 multiplier applied to the description score:

```
val nameScore = FuzzyMatcher.score(query, item.name)
val descScore = item.description?.let { FuzzyMatcher.score(query, it) }?.times(0.8)
val baseScore = listOfNotNull(nameScore, descScore).maxOrNull() ?: return null  // null if neither matches
```

Groups and Graphs only have names — single call, no multiplier.

### Type Bonus

Trackers receive a small additive bonus (`TYPE_BONUS_TRACKER = 5.0`) applied after the best-of-fields score. Additive (not multiplicative) so a perfect graph match can still beat a mediocre tracker match. The intent is to tip the tie when match quality is otherwise equal.

### What Was Considered and Rejected

**`FuzzyMatchTarget` pre-processing** (pre-compute lowercase string + word-boundary boolean array per candidate): the idea was to avoid re-lowercasing strings and re-computing boundaries on every keystroke. Rejected as premature — for typical string lengths and item counts the DP is negligible and the debounce already throttles it. Profile with Perfetto before adding this complexity.

**`groupItemId → GroupChild` result cache** (avoid re-mapping display data on every query change): would skip re-processing items that appear in both the previous and current result set. Not implemented — not obviously the bottleneck. The next obvious optimization target after profiling.

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

## Tapping a result — deep-link navigation with disambiguation

Each card in `SearchResultsGrid` is rendered with `onClick = { onResultClick(item) }` and `contextMenuCallbacks = null` (no menu icon on search result cards). Tracker loading placeholders also get the tap-to-navigate handler because `SearchResultItem.paths` is available before the `DisplayTracker` is fetched. Loaded tracker cards additionally get `onAdd` / `onPlayTimer` / `onStopTimer` wired through from `SearchScreen`'s params — the same lambdas `GroupScreen` uses, hoisted to the outer level so the `AddDataPointsDialog` persists across search open/close. See [card-composables.md](card-composables.md) for the shared card API and why the tracker-action slots sit outside the context-menu object.

The handler lives in `SearchScreen`:

- `item.paths.size == 1` — call the navigator directly with the single `ResolvedPath.descent`.
- `item.paths.size > 1` — set a `disambiguation: SearchResultItem?` state to the item. `SymlinksDialogContent` then renders with `onPathClick = { i -> navigate(item.paths[i].descent) }` — the dialog doubles as a "pick which placement" picker.
- `item.paths.size == 0` — no-op. Shouldn't happen in practice (every indexed component was reached by the graph walk) but we're defensive rather than crashing.

The navigator is read from `LocalDeepLinkNavigator.current`. No navigation callback is threaded through `GroupScreen`. See [deep-link-navigation.md](deep-link-navigation.md) for the full pipeline.

`SymlinksDialogContent` is `internal` and takes an optional `onPathClick: ((Int) -> Unit)?`. When null the rows are static text (the existing symlinks-info use case from group context menus); when non-null each row is `clickable`. This is the one piece of shared UI between the two features — keep the dual-mode API rather than forking the composable.

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
