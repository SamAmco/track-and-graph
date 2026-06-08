---
title: Graph rendering — renderer-neutral view data and Vico bar charts
description: Incremental AndroidPlot migration architecture; bar-chart factories emit plain Kotlin series and bounds, while the Compose UI owns Vico models, styling, axes, zoom, and marker interactions.
topics:
  - Renderer-neutral graph view-data contracts
  - Vico stacked bar-chart rendering
  - Shared contract for database and Lua time-bar graphs
  - Selection, time markers, pan, zoom, axes, and performance
keywords: [graph, chart, rendering, AndroidPlot, Vico, Compose, bar-chart, BarChartView, IBarChartViewData, BarChartSeries, marker, zoom, pan, Lua, migration]
---

# Graph Rendering

Graph rendering is being migrated incrementally from AndroidPlot to native Compose. Do not make factories or view-data DTOs depend on the replacement renderer: renderer-specific models belong in the UI.

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

The bar-chart axes, ticks, and plot-area guidelines use `onSurface` at 70% opacity with a `0.5.dp` thickness. The shared Vico theme uses the same subdued line color. This is intentionally a visual match for the legacy AndroidPlot graphs rather than an attempt to duplicate AndroidPlot's physical-pixel rendering.

## Further migrations

AndroidPlot remains in the line, histogram, and pie paths. When migrating another graph type, first replace AndroidPlot objects in its view-data contract with domain values, update both standard and Lua producers where applicable, then introduce renderer models only inside the Compose UI.
