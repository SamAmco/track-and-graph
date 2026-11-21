---
title: Writing Lua graph scripts and contributing graph scripts
description: Graph scripts and packages, including legacy built-in results, custom prepare/render hooks, stateful configuration UI, normalized package storage, DataSource API, and graph testing.
topics:
  - Graph types: graph.text, graph.data_point, graph.line, graph.time_bar, graph.pie
  - Custom UI graphs: prepare_data, render_graph, and optional render_configuration hooks
  - DataSource API: sources["name"].dp() iterates data points in reverse chronological order
  - Script config: local variables modified directly by users; PREVIEW_START/END for collapsed view
  - core.DURATION (ms, arithmetic ok) vs core.PERIOD (variable length — NO arithmetic)
  - Graph tests DIFFER from function tests: config values are Lua code strings (regex replacement); sources are named table
  - File structure: lua/src/community/graphs/<category>/<script-name>/
  - Local package iteration uses external tng-lua-dev tooling and staged database replacement
keywords: [lua, graph, script, package, tng, ZIP, configuration, render_configuration, prepare_data, render_graph, JSON, state, visualization, line, pie, bar, text, data_point, custom-ui, DataSource, DURATION, PERIOD, tests, tng.graph, PREVIEW, community, sync-lua-to-docs]
---

# Lua Graph Scripts

Graph scripts create custom visualizations. Unlike function scripts (which transform data streams), graph scripts return graph data structures for rendering.

## The Simplest Graph

```lua
return require("tng.graph").text("Hello world!")
```

Returns a table that Track & Graph renders:
```lua
{ type = "TEXT", text = "Hello world!" }
```

## Graph Types

| Type | Function | Description |
|------|----------|-------------|
| Text | `graph.text(params)` | Text display |
| Data Point | `graph.data_point(dp)` | Single data point |
| Line Graph | `graph.line(params)` | Line chart |
| Time Bar Chart | `graph.time_bar(params)` | Bar chart (not histograms) |
| Pie Chart | `graph.pie(params)` | Pie chart |

See `tng/graph.lua` for full parameter definitions.

Custom UI is an additional graph result type with separate data preparation and deferred rendering phases. It can describe layouts and Compose Canvas drawing rather than selecting a built-in chart. See [Lua custom UI](lua-custom-ui.md) for that contract and its runtime constraints.

### Descriptors for built-in graphs

Built-in graph types can also return a descriptor with `prepare_data(params)` and an optional `render_configuration(params)`. Preparation receives `params.sources` (the named input sources) and `params.state` (saved per-instance configuration, or nil). There is no UI context or view mode during preparation. Return the ordinary result table for the descriptor's declared type, or nil to show no graph:

```lua
local graph = require("tng.graph")
return {
    type = graph.GRAPH_TYPE.TEXT,
    prepare_data = function(params)
        local point = params.sources.input.dp()
        if point == nil then return nil end
        local multiplier = params.state and params.state.multiplier or 1
        return graph.text(point.value * multiplier)
    end,
}
```

This works for text, data-point, line, pie and time-bar results, in either inline scripts or packages. Package identity fields are independent of this execution contract. Built-in descriptors do not need `render_graph`: Android renders the prepared result. `CUSTOM` retains its existing two-phase contract, where preparation returns intermediate data for `render_graph` instead. Preparation runs once and its result is parsed directly, not recursively treated as another descriptor. A mismatched result type or a failing preparation hook becomes a normal graph error.

Compatibility is selected by shape, not storage: a returned function still receives the original `sources` table directly, and a returned native result table without `prepare_data` is still parsed immediately. Do not wrap legacy function arguments in `params`. `NativeGraphDescriptorTests` pins both legacy shapes, all five native descriptor types, saved/nil state, errors and inline/package parity against the real engine; the community graph runner separately exercises the catalog scripts.

## Multi-file graph packages

The app imports multi-file Track & Graph packages as ZIP-compatible files with a `.tng` extension. A package has `main.lua` at its root. When imported as a graph, its returned descriptor must declare `purpose = "GRAPH"` plus stable `id` and semantic `version` strings. Package IDs use concise descriptive kebab-case names, matching community Lua function IDs; reverse-domain names are not the convention. Package identity is the `(id, version)` pair. Storage format does not determine capability: inline text and package `main.lua` are evaluated identically. A legacy returned function keeps the old graph behavior. A returned descriptor table may expose package metadata, custom graph hooks, and `render_configuration`. When that hook exists, its Lua UI replaces the source preview and feature inputs. See [configuration lifecycle](lua-custom-ui.md#stateful-configuration-ui) for navigation and reset behaviour.

Lua modules use the conventional mapping: `require("renderer")` loads `renderer.lua`, while `require("chart.renderer")` loads `chart/renderer.lua`. `<module>/init.lua` is also supported. Modules are decoded on demand through a package accessor, and each graph execution has an isolated module cache so retained custom-render closures continue to resolve the correct package.

Keep every translated string table in a package-root `strings.lua` module, keyed by its purpose, and reference those values from implementation modules. For example, `strings.lua` can return `{ title = { en = "My graph" } }`, while `main.lua` uses `local strings = require("strings")` and sets `title = strings.title`. `ui.text` also accepts these translation tables directly; locale selection remains an app responsibility. This keeps language data out of graph logic and gives translation tooling one conventional file per package to inspect and update.

Packages are stored as normalized BLOBs in SQLite and graph instances reference them. Existing single-file scripts remain supported and are not converted by the database migration. Package limits are 5 MiB compressed, 8 MiB expanded in total, and 256 entries; names must be relative safe paths and scripts must be UTF-8. There is no separate per-entry size limit.

Each graph instance separately stores nullable JSON configuration. Package rows contain only package identity/version and archive bytes, so multiple graphs can share one immutable package while retaining different state. See [Lua custom UI](lua-custom-ui.md#stateful-configuration-ui) for the renderer and serialization contract.

To build one with common ZIP tooling, run the command from inside the package directory so `main.lua` is at the archive root:

```bash
zip -r my-graph.tng main.lua data.lua renderer.lua strings.lua
```

### Local package iteration on a debuggable device

On this development machine, the external helper `~/.local/bin/tng-lua-dev` zips a package directory and replaces an already-imported package on the emulator. Use `--list` to discover graph IDs, then:

```bash
tng-lua-dev /absolute/path/to/package --graph-id <lua_graphs_table.id>
```

The ID is the Lua graph row ID, not `graph_stat_id`. Configuration and feature inputs are preserved by default; `--config-file /tmp/config.json` optionally changes configuration in the same transaction, but does not change feature mappings. The helper includes every regular package file, so new required modules are not accidentally omitted. It accepts `--serial` and `--app` for a different device or debuggable app.

This deliberately bypasses production first-write-wins package storage: replacing a shared `(id, version)` affects every graph referencing it. Keep identity/version unchanged during this local loop rather than bumping the version for each visual tweak. Native API changes still require rebuilding/installing the app. For ordinary imports without the helper, delete all graphs referencing that package so cleanup removes the stored archive before reimporting changed bytes at the same version.

The helper stops the app, preserves committed WAL data, makes an offline backup under `~/.local/state/tng-lua-dev/backups/`, edits a private copy, and stages it for restart using the [database restore lifecycle](database-backup-restore.md#restore-lifecycle). Never replace an open Room database directly. The helper is machine-local tooling, not a repository dependency. After reload, open the graph card to inspect full-screen mode and verify both portrait and landscape; installing successfully is not visual verification.

## Using Data Sources

Return a function to receive configured data sources:

```lua
return function(sources)
    local graph = require("tng.graph")
    local dp = sources["my_data_source"].dp()
    return graph.data_point(dp)
end
```

Track & Graph:
1. Executes script → gets function
2. Calls function with data sources
3. Renders returned graph data

### DataSource API

```lua
sources["name"].dp()  -- Next data point (reverse chronological)
```

Data points iterate until `dp()` returns nil.

## Legacy Script Configuration

Legacy single-file graph scripts use local variables for configuration:

```lua
local core = require("tng.core")
local graph = require("tng.graph")

-- Configuration
local period = core.PERIOD.WEEK
local show_labels = true

return function(sources)
    -- Use period, show_labels...
end
```

Users modify these variables directly in the script editor. Descriptor-based scripts can instead provide `render_configuration(params)` and return interactive Lua UI plus persistent state; packages are not required for that capability.

### Script Previews

Use comments to control what's shown in collapsed view:

```lua
--- PREVIEW_START
-- Script: Total This Period
local period = core.PERIOD.WEEK
--- PREVIEW_END
```

## Data Types Reference

### Durations (milliseconds)
```lua
core.DURATION.SECOND
core.DURATION.MINUTE
core.DURATION.HOUR
core.DURATION.DAY
core.DURATION.WEEK
```

Arithmetic allowed: `core.DURATION.MINUTE * 5`

### Periods (variable length)
```lua
core.PERIOD.DAY
core.PERIOD.WEEK
core.PERIOD.MONTH
core.PERIOD.YEAR
```

**No arithmetic** (months/days vary). Use separate multiplier:
```lua
local period = core.PERIOD.MONTH
local period_multiplier = 3
```

### Colors
```lua
local color = "#FF0000"  -- Hex
local color = core.COLOR.BLUE  -- Named constant
```

Named colors: `RED_DARK`, `RED`, `ORANGE_DARK`, `ORANGE`, `YELLOW`, `BLUE_LIGHT`, `BLUE_SKY`, `BLUE`, `BLUE_DARK`, `BLUE_NAVY`, `GREEN_LIGHT`, `GREEN_DARK`

---

# Contributing Graph Scripts

## File Structure

**Code and tests** in `lua/src/community/graphs/`:
```
lua/src/community/graphs/<category>/<script-name>/
├── <script-name>.lua
└── test_<script-name>.lua
```

**Documentation** in `docs/docs/lua/community/`:
```
docs/docs/lua/community/<category>/<script-name>/
├── script.lua    # Auto-generated via make sync-lua-to-docs
├── README.md     # Manual
└── image.png     # Screenshot
```

## Writing Tests

Graph script tests use **text-based config overrides** (different from function tests):

```lua
local M = {}
local core = require("tng.core")
local graph = require("tng.graph")
local test = require("test.core")

M.test_basic = {
    config = {
        -- Config values are STRINGS of Lua code (regex replacement)
        period = "core.PERIOD.WEEK",
        count_by_label = "true",
        numerator_labels = "{'A', 'B'}",
    },
    sources = function()
        local now = core.time().timestamp
        return {
            source1 = {  -- Named sources matching script
                { timestamp = now - core.DURATION.DAY, value = 10.0, label = "A" },
                { timestamp = now - core.DURATION.DAY * 2, value = 5.0, label = "B" },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(graph.GRAPH_TYPE.TEXT, result.type)
        test.assertEquals("15/30", result.text)
    end,
}

return M
```

### Key Differences from Function Tests

| Aspect | Function Tests | Graph Tests |
|--------|---------------|-------------|
| Config values | Actual Lua types | Strings of Lua code |
| Sources format | Array of arrays | Named table |
| Why | Direct injection | Regex replacement |

### Config Override Mechanism

Test harness uses regex to replace variable declarations:
```lua
-- Script has: local period = nil
-- Test config: { period = "core.PERIOD.WEEK" }
-- Result: local period = core.PERIOD.WEEK
```

### Running Tests

```bash
make run-community-graph-tests
```

## Debugging

- Use `print()` for debugging (remove before PR)
- Tables print as memory addresses - access fields directly
- Check config keys match script variable names exactly
- Verify config values are valid Lua code strings

## Syncing to Docs

After editing scripts:
```bash
make sync-lua-to-docs
```

This copies `*.lua` from `lua/src/community/graphs/` to `docs/docs/lua/community/`.
