---
title: Lua custom UI — deferred rendering, retained VM state, and Compose mapping
description: Custom graph data/render split, opaque affinity-bound Lua values, Lua UI context, declarative components and modifiers, deferred Canvas instruction recording, theme tokens, runtime error handling, and compatibility boundaries.
topics:
  - CUSTOM graph descriptor separates data preparation from view-dependent rendering
  - Lua values and callbacks remain opaque and bound to their originating VM affinity
  - LuaUiContext supplies root constraints, density, font scale, layout direction, and light/dark mode
  - tng.ui describes layout; tng.canvas records instructions for later Compose DrawScope replay
  - Theme values use stable tokens resolved by the active TnG Compose theme
  - Render and callback failures use the existing graph error presentation
keywords: [lua, custom-ui, CUSTOM, tng.ui, tng.canvas, tng.theme, LuaUiContent, LuaUiContext, LuaValueAccessor, LuaValueReference, affinity, render, data, canvas, Compose, constraints, density, theme, modifier, callback, state, error]
---

# Lua Custom UI

The custom UI path is a general declarative Lua-to-Compose boundary. Graphs are its first consumer, but the contract deliberately separates UI content from graph-specific rendering so it can later support screens such as Lua-driven graph configuration.

## Graph Contract and Lifecycle

A custom graph returns a descriptor with `type = "CUSTOM"`, a `data` function, and a `render` function:

```lua
local graph = require("tng.graph")
local ui = require("tng.ui")

return {
    type = graph.GRAPH_TYPE.CUSTOM,
    data = function(sources)
        return { values = read_values(sources["source"]) }
    end,
    render = function(data, context)
        return ui.text({ text = tostring(#data.values) })
    end,
}
```

The split matches the existing graph architecture:

1. The data/view-model phase invokes `data(sources)` while sampled data sources are available.
2. Its result stays as an opaque Lua value; Kotlin does not decode it into an intermediate domain model.
3. The view phase invokes `render(data, context)` after Compose knows the root constraints.
4. The returned UI table is read through `LuaValueAccessor` and mapped directly to Compose components.

The original render closure and data result are retained as `LuaValueReference`s tied to their originating `LuaVMAffinity`. Each render or UI callback reacquires a short-lived execution lock for that affinity and releases it in `finally`. This preserves Lua closures and VM-local state without holding one of the pooled VMs locked for the lifetime of a screen. See [Lua functions architecture](lua-architecture.md#lua-vm-affinity-and-execution-locks) for lease invariants.

## Render Context

`render` receives a context containing:

- `constraints`: root `min_width`, `max_width`, `min_height`, and `max_height` in physical pixels. These are constraints, not a promised final size.
- `density`: physical pixels per dp.
- `font_scale`: the user's text scale.
- `layout_direction`: `LTR` or `RTL`.
- `theme`: `LIGHT` or `DARK`.

Theme mode comes from the explicit `TnGComposeTheme` selection, not from guessing based on color luminance or independently reading the system setting. Visual colors should normally use `tng.theme.color` tokens so the app resolves them against the current theme. The mode string is for behavior that genuinely differs between light and dark modes.

The initial context fields are intended to be dependable. Future optional fields must be additive; scripts targeting them should nil-check so they can degrade gracefully on older app versions.

## Declarative UI Boundary

`tng.ui` API level 2 currently describes rows, columns, boxes, spacers, text, surfaces, and canvases. Dimensions carry a `PX`, `DP`, or `SP` unit. Ordered modifier tables cover sizing, weight, constraints, aspect ratio, padding, background, border, clipping, elevation, and child alignment. `tng.theme` supplies stable color, spacing, and shape tokens rather than exposing Compose objects to Lua.

Stable component keys preserve identity across renders and become especially important once components contain state or actions. The DSL currently has no clickable modifier or general input/action component; do not document planned interactive APIs as already supported.

Kotlin deliberately parses the returned table in place through `LuaValueAccessor` instead of copying the entire UI tree into DTOs. Accessors use raw Lua table lookup, which avoids triggering the `__index` metatables used by API compatibility tests. Lua arrays retain one-based indexing at the accessor boundary and are converted to Kotlin lists only where the renderer needs iteration.

`LuaValueAccessor` is a retained, read-only view rather than an execution capability. Plain values and tables that the script treats as immutable may be traversed after releasing the VM lock, as the Compose renderer already does. A function obtained with `asFunction()` is instead represented by an affinity-bound `LuaValueReference`; invoking it requires reacquiring a short-lived lock for that same affinity. Do not read a retained table concurrently with Lua code that may mutate it.

## Deferred Canvas Rendering

A canvas cannot know its exact bounds during root UI generation. `ui.canvas` therefore stores a Lua callback rather than immediate instructions. After Compose measures that canvas:

1. Kotlin calls the retained callback with `context.bounds.width` and `height` in pixels.
2. `tng.canvas` creates a Lua `CanvasDrawScope` containing the exact `size` and `center`. Bare numbers are physical pixels; its `dp` and `sp` helpers convert those units to numeric physical pixels for canvas arithmetic. The host supplies Compose density plus a font-scale conversion table so `sp` follows Android's non-linear accessibility scaling rather than assuming `density * fontScale`.
3. Draw calls append plain instruction tables. The color-based API supports lines, rectangles, rounded rectangles, circles, ovals, arcs, paths, points, and measured text. Nested callbacks support translation, rotation, scaling, insets, and rectangle/path clipping.
4. The callback returns the completed instruction list.
5. Kotlin parses and replays that list into the Compose `DrawScope` in paint order.

There is no Lua/Compose ping-pong for individual drawing operations. Lua records a complete instruction list, then Compose draws it. The draw callback is a closure in the same Lua environment as `render`, so it can retain state and values from the render context; canvas-specific data should remain limited to facts only known after measurement, such as exact bounds.

Standard linear, horizontal, vertical, radial, and sweep gradients are represented as serializable brush descriptions. The same boundary models Compose blend modes, tint/color-matrix/lighting filters, common path effects, and scoped or batched transforms. Paths support ordinary segment construction plus non-returning additions and mutations; operations that synchronously return computed geometry are not exposed.

Canvas text uses Compose's native horizontal `TextAlign`. Track & Graph additionally provides `vertical_align` for positioning measured text at the top, center, or bottom of an explicitly supplied `size` rectangle. It has no effect when `size` is absent because there is no vertical region to align within.

The bridge intentionally does not yet expose image drawing, graphics layers, arbitrary shader brushes, or raw `drawIntoCanvas`. Images first need a package-asset contract, while stateful or live Compose objects cannot cross the retained-data boundary.

## Errors and Cancellation

Initial graph execution errors already flow through `LuaGraphResult` and the normal graph error view. Failures thrown later by `render` or a retained canvas callback are converted to `LuaScriptException` and routed to the same presentation rather than silently producing an empty component. Coroutine cancellation is rethrown: constraint/content changes may cancel and restart `produceState`, and that control flow must not appear as a script failure.

The worked contract example is a multi-file package under `lua/src/examples/canvas-basic-graph/`. The graph result type belongs to `tng.graph`; `tng.ui` only describes the returned layout. API compatibility is declared in the `tng.graph`, `tng.ui`, `tng.canvas`, and `tng.theme` API spec files; update those specs whenever public Lua symbols change.
