---
title: Lua custom UI — deferred rendering, retained VM state, and Compose mapping
description: Custom graph preparation/rendering, stateful configuration UI sessions, affinity-bound Lua values, responsive declarative layout, deferred Canvas drawing, theme and translation tokens, testing boundaries, and runtime errors.
topics:
  - CUSTOM graph descriptor separates data preparation from view-dependent rendering
  - Lua values and callbacks remain opaque and bound to their originating VM affinity
  - LuaUiContext supplies root constraints, density, font scale, layout direction, and light/dark mode
  - tng.ui describes responsive layout; tng.graphui composes host-free graph conventions from it
  - tng.canvas records instructions for later Compose DrawScope replay
  - Theme values and data-visualization colors resolve consistently across declarative UI and Canvas
  - Render and callback failures use the existing graph error presentation
  - Configuration renderers retain JSON-serializable Lua table state across interactions and graph execution
keywords: [lua, custom-ui, CUSTOM, prepare_data, render_graph, render_configuration, configuration, JSON, state, on_click, RetainedLuaUiContent, tng.ui, tng.graphui, tng.canvas, tng.theme, LuaUiContent, LuaUiContext, LuaValueAccessor, LuaValueReference, affinity, render, data, canvas, DrawScope, Compose, constraints, responsive, density, font-scale, dp, sp, FlowRow, legend, data-vis, translation, TextAlign, vertical-align, Path, pathFactory, testing, theme, modifier, callback, error]
---

# Lua Custom UI

The custom UI path is a general declarative Lua-to-Compose boundary shared by graph rendering and Lua-driven graph configuration. The contract deliberately separates UI content from graph-specific data preparation.

## Graph Contract and Lifecycle

A custom graph returns a descriptor with `type = "CUSTOM"`, `prepare_data`, and `render_graph`. It may additionally expose `render_configuration`:

```lua
local graph = require("tng.graph")
local ui = require("tng.ui")

return {
    type = graph.GRAPH_TYPE.CUSTOM,
    prepare_data = function(sources, state)
        return { values = read_values(sources["source"]) }
    end,
    render_graph = function(context, state, data)
        return {
            ui = ui.text({ text = tostring(#data.values) }),
            state = state,
        }
    end,
}
```

The split matches the existing graph architecture:

1. The data/view-model phase decodes the graph instance's nullable JSON configuration and invokes `prepare_data(sources, state)` while sampled data sources are available.
2. Its result stays as an opaque Lua value; Kotlin does not decode it into an intermediate domain model.
3. The view phase invokes `render_graph(context, state, data)` after Compose knows the root constraints.
4. Every UI renderer returns `{ ui = <component>, state = <table-or-nil> }`. The returned UI is read through `LuaValueAccessor`; returned state becomes the session's state for the next render.

The original render closure and data result are retained as `LuaValueReference`s tied to their originating `LuaVMAffinity`. Each render or UI callback reacquires a short-lived execution lock for that affinity and releases it in `finally`. This preserves Lua closures and VM-local state without holding one of the pooled VMs locked for the lifetime of a screen. See [Lua functions architecture](lua-architecture.md#lua-vm-affinity-and-execution-locks) for lease invariants.

## Render Context

A UI renderer receives a context containing:

- `constraints`: root `min_width`, `max_width`, `min_height`, and `max_height` in physical pixels. These are constraints, not a promised final size.
- `density`: physical pixels per dp.
- `font_scale`: the user's text scale.
- `layout_direction`: `LTR` or `RTL`.
- `theme`: `LIGHT` or `DARK`.

Theme mode comes from the explicit `TnGComposeTheme` selection, not from guessing based on color luminance or independently reading the system setting. Visual colors should normally use `tng.theme.color` tokens so the app resolves them against the current theme. The mode string is for behavior that genuinely differs between light and dark modes.

The initial context fields are intended to be dependable. Future optional fields must be additive; scripts targeting them should nil-check so they can degrade gracefully on older app versions.

For coarse responsive decisions such as changing a graph's orientation, compare `constraints.max_width / density` to a dp breakpoint during `render`. The constraints choose the component tree; the measured canvas callback remains the source of truth for exact drawing geometry. Do not treat root constraints as the final canvas bounds.

## Declarative UI Boundary

`tng.ui` API level 2 currently describes rows, wrapping flow rows, columns, boxes, spacers, text, surfaces, and canvases. Dimensions carry a `PX`, `DP`, or `SP` unit. Ordered modifier tables cover sizing, weight, constraints, aspect ratio, padding, background, border, clipping, elevation, and child alignment. `flow_row` maps Compose's horizontal and vertical arrangements, independent inter-item and inter-row spacing, item vertical alignment, maximum items per row, maximum lines, and `fill_max_row_height` child behavior. Explicit spacing combines with start/center/end alignment; space-between/around/evenly arrangements define their own gaps, so explicit spacing is ignored for those modes. Keep FlowRow-specific child modifiers in the same ordered modifier list as ordinary modifiers.

`tng.theme` supplies stable color, spacing, shape, and text-style tokens rather than exposing Compose objects to Lua. Its text styles cover the complete Material display/headline/title/body/label set plus Track & Graph's code style. A color accepted by Lua UI or Canvas may be a theme token, a hex string, or a zero-based `tng.core.COLOR` data-visualization palette index; both rendering paths deliberately share the same resolver.

`tng.graphui` is a pure-Lua convenience layer built only from `tng.ui` and `tng.theme`; it introduces no Android bridge types. Its legend helpers reproduce the native graph convention—a centered, wrapping full-width FlowRow containing 20dp circular swatches, a 2dp label gap, body-medium text, and 10dp item spacing. Put reusable graph presentation conventions here so native and packaged graphs do not drift visually.

Stable component keys preserve identity across renders and are especially important for interactive trees. `ui.on_click(callback)` is valid on every node through its modifier list. The host calls the callback and rerenders the root UI before releasing the one VM execution lock acquired for that interaction. This makes the callback mutation and resulting tree/state snapshot one serialized Lua operation; do not add a separate UI mutex around it.

`ui.text` accepts either a direct string or the same `{ language_tag = text }` translation table used by package metadata. Kotlin resolves translation tables against the current Android locale preferences, including English fallback. Keep package translations in `strings.lua` and pass the table through unchanged; renderers should not choose a locale themselves.

Kotlin deliberately parses the returned table in place through `LuaValueAccessor` instead of copying the entire UI tree into DTOs. Accessors use raw Lua table lookup, which avoids triggering the `__index` metatables used by API compatibility tests. Lua arrays retain one-based indexing at the accessor boundary and are converted to Kotlin lists only where the renderer needs iteration.

`LuaValueAccessor` is a retained, read-only view rather than an execution capability. Plain values and tables that the script treats as immutable may be traversed after releasing the VM lock, as the Compose renderer already does. A function obtained with `asFunction()` is instead represented by an affinity-bound `LuaValueReference`; invoking it requires reacquiring a short-lived lock for that same affinity. Do not read a retained table concurrently with Lua code that may mutate it.

## Stateful Configuration UI

The graph configuration screen evaluates either an inline script or package `main.lua` through the same script-accessor boundary. A table containing `render_configuration` replaces all source and feature controls below the graph-type selector with its Lua UI. Back returns to those source controls. Legacy scripts that return a function, and descriptor tables without this hook, keep the old configuration experience.

`render_configuration(context, state)` uses the same renderer contract and retained session as `render_graph`. On first render, `state` is the graph instance's decoded configuration table or nil. Click handlers normally capture and mutate that table. After each root render, the session retains the real state table as a `LuaValueReference` for the next interaction and exposes a read-only `LuaValueAccessor` over that same table to the configuration ViewModel. Rendering does not traverse or encode the state. The ViewModel wraps `accessor.snapshot()` in `Lazy`, so detachment happens only if a consumer requests the configuration; JSON encoding happens only at the create/update persistence boundary. Unsupported values and functions are omitted; numbers must be finite; dense integer-keyed tables encode as arrays, string-keyed tables as objects, and cyclic branches are omitted. An empty table is encoded as an object.

The graph instance owns configuration; it is not package metadata and is not stored in the normalized package row. `prepare_data(sources, state)` and `render_graph(context, state, data)` receive independently decoded values from that same persisted JSON when the graph is displayed. Source replacement clears prior configuration so state from one script cannot leak into another.

`LuaValueData` is the DTO-level, data-only state contract. It is an immutable, detached snapshot containing only JSON-compatible Lua-shaped data; it never refers to a Lua VM or exposes functions. This deliberately differs from `LuaValueAccessor`, which inspects a real value produced by a VM, and `LuaValueReference`, which retains an opaque real value for later use on that same VM. Room configuration is exposed as `Lazy<LuaValueData>`, so listing or searching graphs does not parse unused JSON. Live configuration is also converted lazily through `LuaValueAccessor.snapshot()`, and entity conversion forces and JSON-encodes it when the graph is saved.

`RetainedLuaUiContent` is the generic app-layer session used by both graph rendering and configuration. It retains the renderer, current state, optional extra arguments, and VM affinity. `interact` acquires one affinity lock, calls the handler synchronously, rerenders, updates the retained state, and releases in `finally`. Canvas callbacks also reacquire the owning VM but do not rerender the root.

## Deferred Canvas Rendering

A canvas cannot know its exact bounds during root UI generation. `ui.canvas` therefore stores a Lua callback rather than immediate instructions. After Compose measures that canvas:

1. Kotlin calls the retained callback with `context.bounds.width` and `height` in pixels.
2. `tng.canvas` creates a Lua `CanvasDrawScope` containing the exact `size` and `center`. Bare numbers are physical pixels; its `dp` and `sp` helpers convert those units to numeric physical pixels for canvas arithmetic. The host samples Compose's real `sp` conversion at several sizes and supplies those points with the density. Lua interpolates between them, preserving Android's non-linear accessibility scaling while keeping conversion synchronous and usable in arithmetic. It falls back to `density * fontScale` only when the sampled conversion table is absent.
3. Draw calls append plain instruction tables. The color-based API supports lines, rectangles, rounded rectangles, circles, ovals, arcs, paths, points, and measured text. Nested callbacks support translation, rotation, scaling, insets, and rectangle/path clipping.
4. The callback returns the completed instruction list.
5. Kotlin parses and replays that list into the Compose `DrawScope` in paint order.

There is no Lua/Compose ping-pong for individual drawing operations. Lua records a complete instruction list, then Compose draws it. The draw callback is a closure in the same Lua environment as `render`, so it can retain state and values from the render context; canvas-specific data should remain limited to facts only known after measurement, such as exact bounds.

Standard linear, horizontal, vertical, radial, and sweep gradients are represented as serializable brush descriptions. The same boundary models Compose blend modes, tint/color-matrix/lighting filters, common path effects, and scoped or batched transforms. Paths support ordinary segment construction plus non-returning additions and mutations; operations that synchronously return computed geometry are not exposed.

Canvas text uses Compose's native horizontal `TextAlign`. When an instruction supplies `size`, the bridge fixes the measured paragraph width to `size.width`; merely applying a maximum width leaves short labels at their intrinsic width and makes center/end alignment appear left-aligned. Height is intentionally different: `size.height` remains only the maximum measurement height so the result retains its natural text height.

Track & Graph's additional `vertical_align` then positions that naturally measured text at the top, center, or bottom of the requested height. It has no effect when `size` is absent because there is no vertical region to align within. Do not force the minimum text height to `size.height`: doing so makes the measured height equal the container height, leaves no remainder to offset, and breaks this vertical-alignment abstraction.

The bridge intentionally does not yet expose image drawing, graphics layers, arbitrary shader brushes, or raw `drawIntoCanvas`. Images first need a package-asset contract, while stateful or live Compose objects cannot cross the retained-data boundary.

### Canvas testing boundary

Test the Lua instruction recorder in `lua/src/tng/test/test_canvas.lua`, and test Kotlin instruction-to-Compose mapping in `LuaCanvasInstructionParserTest`. Compose's Android-backed `Path` methods throw "not mocked" in ordinary local JVM tests. The parser therefore accepts an internal `pathFactory`; parser tests should supply a mocked `Path` and verify the mapped calls rather than requiring Robolectric merely to exercise path commands. This seam also needs to be passed recursively when testing `add_path`.

## Errors and Cancellation

Initial graph execution errors already flow through `LuaGraphResult` and the normal graph error view. Failures thrown later by `render` or a retained canvas callback are converted to `LuaScriptException` and routed to the same presentation rather than silently producing an empty component. Coroutine cancellation is rethrown: constraint/content changes may cancel and restart `produceState`, and that control flow must not appear as a script failure.

The worked stateful example is the multi-file calendar heatmap package under `lua/src/examples/`; it configures its data range, layout, and legend. Examples are disposable demonstrations, not runtime dependencies. The graph result type belongs to `tng.graph`; `tng.ui` only describes the returned layout. API compatibility is declared in the `tng.graph`, `tng.ui`, `tng.graphui`, `tng.canvas`, and `tng.theme` API spec files; update those specs whenever public Lua symbols change.
