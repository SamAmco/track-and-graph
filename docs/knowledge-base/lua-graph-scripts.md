---
title: Writing Lua graph scripts and contributing graph scripts
description: Graph scripts return graph data structures (not data streams); covers 5 graph types (text, data_point, line, time_bar, pie), DataSource API, script configuration with local variables, DURATION vs PERIOD distinction, and the graph test framework (config as Lua code strings, named sources).
topics:
  - Graph types: graph.text, graph.data_point, graph.line, graph.time_bar, graph.pie
  - DataSource API: sources["name"].dp() iterates data points in reverse chronological order
  - Script config: local variables modified directly by users; PREVIEW_START/END for collapsed view
  - core.DURATION (ms, arithmetic ok) vs core.PERIOD (variable length — NO arithmetic)
  - Graph tests DIFFER from function tests: config values are Lua code strings (regex replacement); sources are named table
  - File structure: lua/src/community/graphs/<category>/<script-name>/
keywords: [lua, graph, script, visualization, line, pie, bar, text, data_point, DataSource, DURATION, PERIOD, tests, tng.graph, PREVIEW, community, sync-lua-to-docs]
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

## Script Configuration

Graph scripts use local variables for configuration:

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

Users modify these variables directly in the script editor.

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
