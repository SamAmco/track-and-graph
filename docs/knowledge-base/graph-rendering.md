---
title: Graph rendering — Compose line graphs and Vico bar charts
description: Renderer-neutral graph contracts; Compose Canvas line rendering and Vico bar rendering own their axes, measured labels, styling, interaction, and defensive validation in the UI.
topics:
  - Renderer-neutral graph view-data contracts
  - Compose Canvas line-graph rendering
  - Vico stacked bar-chart rendering
  - Shared contract for database and Lua time-bar graphs
  - Selection, time markers, pan, zoom, axes, and performance
keywords: [graph, chart, rendering, AndroidPlot, Vico, Compose, Canvas, line-graph, LineGraphView, LineGraphPerf, Logcat, performance, ILineGraphViewData, LineGraphPoint, bar-chart, BarChartView, IBarChartViewData, BarChartSeries, marker, axis, label, Roboto Mono, zoom, pan, Lua, migration, NaN, zero-range, finite]
---

# Graph Rendering

Graph rendering is being migrated incrementally from AndroidPlot to native Compose. Do not make factories or view-data DTOs depend on the replacement renderer: renderer-specific models belong in the UI.

## Line-chart boundary

Line charts are drawn directly with Compose Canvas. `ILineGraphViewData` exposes timestamp/value `LineGraphPoint` lists and optional configured fixed Y bounds; it must not expose AndroidPlot series, regions, or axis subdivisions. Both `LineGraphDataFactory` and `LineGraphLuaHelper` produce this same contract. They keep points in ascending timestamp order, and a line needs at least two points to be plottable.

`LineGraphView` owns all layout decisions because they depend on the rendered size and measured text. It calculates Y bounds and tick density from visible finite data and available height, preserving exact configured bounds for fixed-range graphs. For the interval selection itself it reuses `DataDisplayIntervalHelper`, passing a height-derived approximate line count. The helper prefers clean decimal divisions (and base-60 divisions for duration data), may use one more line than the approximation when that produces a clearer interval, and retains its legacy 6–12 line defaults for bar-chart callers that do not provide a count. This keeps fixed ranges such as 0–100 on stable 20-unit intervals in both list and full-screen layouts. The view measures every distinct axis label before setting the plot rectangle, which prevents wide or negative Y labels from being clipped at the left edge and means a monospaced font is unnecessary. Line and Vico bar axes share the proportional 12sp `graphAxisTextStyle`, plus common grid opacity and thickness values, from `GraphStatUICommon.kt`. Equal dynamic Y ranges are expanded defensively.

X ticks are selected from actual data-point timestamps, including irregular series, rather than fixed temporal subdivisions. Eligible ticks are anchored to their index in the complete, sorted timestamp sequence rather than whichever point is currently at the viewport edge. Tick strides use powers of two: panning moves the same labels through the viewport, while zooming in retains coarse labels and adds intermediate data-point labels when space permits. The selector measures one real candidate near the middle of the viewport to estimate capacity, then compares upper bounds for measuring every visible timestamp versus measuring only stride-eligible timestamps. Because that estimate is itself a real candidate, the direct path does not pay for a throwaway measurement. The chosen candidates then use their exact measured bounds in the final -28-degree overlap pass, so an imperfect initial estimate cannot cause overlaps. Formatting is based on the visible duration: seconds and minutes use clock formats, day-scale labels use `dd MMM`, and long ranges use the compact, unambiguous abbreviated-year form `MMM’yy` (for example, `Mar’26`). The named-month formatters use `Locale.ENGLISH` deliberately so every month occupies exactly three characters (`Sep`, never locale-dependent `Sept`). Recalculate both axes when the full-screen viewport changes.

List-mode line charts are static. Full-screen charts support one-finger horizontal pan and two-finger pan/zoom; keep the viewport clamped to the complete data extent. Do not use the general `detectTransformGestures` detector here: it consumes one-finger vertical drags and prevents the surrounding `FadingScrollColumn` from scrolling when the title plus graph exceed the screen height. Handle multi-touch transforms in the initial pointer pass, consume them for the graph, and use the orientation-aware horizontal drag detector for one-finger panning so vertical drags remain available to the parent. Maximum zoom is derived from the full epoch-millisecond range, allowing a minimum one-millisecond viewport instead of imposing an arbitrary zoom factor. Axis layout is replaced as the viewport changes, so the pointer-input coroutine must be keyed to stable inputs such as interaction mode and canvas size, not the calculated layout—otherwise the first recalculation cancels the active gesture. Yield one frame only before the initial layout/reveal; yielding on every viewport update makes the axes lag behind an active gesture. The initial complete graph—including axes and an optional time marker—fades in only after text measurement and layout finish. Viewport changes must not restart or cancel this reveal.

Every rendered line-graph instance emits one `LineGraphPerf` info-level Logcat entry in all build variants after its first completed Canvas draw. `firstRenderWorkMs` sums active preparation, merge/sort, initial layout/text measurement, and Canvas draw time; it intentionally excludes the deliberately yielded frame and other scheduling delay. The same entry includes line, input/finite/merged/visible-point, and X/Y tick counts so timings from real graphs can be compared meaningfully. Do not add per-frame logging to this path: Logcat traffic during pan, zoom, or reveal would distort the rendering performance being observed.

## Bar-chart boundary

Bar charts are the first Vico-based graph type. `IBarChartViewData` exposes:

- ascending bar-end dates
- `BarChartSeries` values, label, and app color specification
- explicit Y-axis bounds and subdivision count
- duration formatting metadata and the bar period

Both `BarChartDataFactory` and `TimeBarchartLuaHelper` produce this same plain Kotlin contract. Keep them aligned when changing bar-chart behavior. In particular, Lua segments with the same label but different colors remain distinct series.

`BarChartView` owns its Vico chart types. It creates a stacked `ColumnCartesianLayer`, fixes the Vico range to the factory-provided bounds, and updates a persistent `CartesianChartModelProducer` through transactions. This keeps model processing off the main thread and avoids leaking Vico into graph calculation tests. Shared Vico styling belongs in `ProvideGraphVicoTheme` in `GraphStatUICommon.kt` so future Vico graph types use the same Material colors.

Graph height policy also belongs in `GraphStatUICommon.kt`. `graphHeightFor` converts it to Compose `Dp`, while the legacy `setGraphHeight` applies it to Android View layout parameters; both share the same full-screen multipliers for graphs with and without legends.

## Preserved interaction behavior

Full-screen bar charts support horizontal pan and zoom. List-mode charts remain non-interactive. Both modes use `Zoom.Content` as the initial zoom so the complete X range is visible, including dense charts whose rendered bars must become thinner than their width at unit zoom. Full-screen users can then zoom in and pan. Do not use Vico's default `max(Zoom.fixed(), Zoom.Content)` initial zoom: its lower bound of `1f` prevents dense charts from initially fitting all bars. Vico's toggle-on-tap marker controller drives the existing details overlay, while a custom decoration draws the selected-bar highlight. A supplied graph time marker initializes the same selected index.

Do not implement the persistent highlight as a Vico persistent marker. Vico only exposes a column marker target while the column center is inside the layer bounds, which makes highlights pop in and out when a partially visible selected bar is panned across an edge. The decoration computes the selected column's viewport position directly and clips the highlight rectangle to the layer bounds. Draw the highlight over the chart layers so it also tints a bar that reaches the top of the plot; drawing it underneath can make such a selection invisible.

X-axis label spacing adapts to the visible range and uses powers of two, preserving the previous approximate ten-label limit during zoom. Date formatting still depends on the total graph duration. Y-axis labels use duration formatting when requested.

Bars retain borders for fewer than 60 time buckets; borders are suppressed for denser charts. Series remain ordered largest-total first so stacked colors and legends stay consistent with the previous renderer.

Vico requires finite chart values and a nonzero Y range. A range such as `0..0` makes its vertical-axis coordinate calculation produce `NaN`, which Compose rejects while drawing a rectangular guideline. The bar-chart UI is the final defensive boundary: it rejects non-finite values, inconsistent series lengths, inverted bounds, and expands equal finite bounds. Producers should still emit valid ranges; both database and Lua all-zero bar charts use `0..1`. Fixed database bar-chart maxima must be finite and positive.

The bar-chart axes, ticks, and plot-area guidelines use `onSurface` at 70% opacity with a `0.5.dp` thickness. The shared Vico theme uses the same subdued line color. This is intentionally a visual match for the legacy AndroidPlot graphs rather than an attempt to duplicate AndroidPlot's physical-pixel rendering.

## Further migrations

AndroidPlot remains in the histogram and pie paths. When migrating another graph type, first replace AndroidPlot objects in its view-data contract with domain values, update both standard and Lua producers where applicable, then introduce renderer models only inside the Compose UI.
