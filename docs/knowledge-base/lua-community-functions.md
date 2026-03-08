---
title: Writing Lua function scripts and contributing to the community catalog
description: How to write Lua function scripts (generator/iterator pattern, data point fields, 8 config types, multiple inputs, filtering); catalog contribution requirements (version field, 4-language localization, categories, tests).
topics:
  - Pattern: script → generator function(source, config) → iterator function → yields data points
  - Data point fields: timestamp (ms), value, label, note, offset
  - Config types (from tng.config): text, number, checkbox, enum, uint, duration, localtime, instant
  - Multiple inputs: inputCount > 1, first param becomes table of sources
  - Catalog mode: requires version, id, 4-language translations (en/de/es/fr), categories
  - Tests: config values are actual Lua types; sources is array of arrays; run with make run-community-functions-tests
keywords: [lua, function, script, generator, iterator, datapoint, config, community, catalog, localization, tests, tng.config, inputCount, version, categories]
---

# Lua Function Scripts

Function scripts transform data point streams in the node editor. This covers both custom scripts and contributing to the community catalog.

## The Simplest Function

```lua
return function(source)
    return function()
        local data_point = source.dp()
        if not data_point then return nil end
        data_point.value = math.abs(data_point.value)
        return data_point
    end
end
```

Core pattern:
1. Script returns a **generator function** accepting `source` (and optionally `config`)
2. Generator returns an **iterator function** yielding data points one at a time
3. Iterator calls `source.dp()` for next point (reverse chronological order)
4. Return `nil` when exhausted

## Data Point Fields

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | number | Unix timestamp in milliseconds |
| `value` | number | The numeric value |
| `label` | string | Optional label text |
| `note` | string | Optional note text |
| `offset` | number | Timezone offset in milliseconds |

## Adding Metadata

Return a table instead of a bare function:

```lua
return {
    id = "absolute-value",
    inputCount = 1,
    title = "Absolute Value",
    description = [[Converts each value to its absolute value.]],
    config = {},

    generator = function(source)
        return function()
            local dp = source.dp()
            if not dp then return nil end
            dp.value = math.abs(dp.value)
            return dp
        end
    end,
}
```

### Metadata Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string/table | No | Display name |
| `description` | string/table | No | Markdown description |
| `inputCount` | number | No | Input connections (default: 1) |
| `config` | table | No | Configuration parameters |
| `id` | string | No | Unique identifier |
| `version` | string | No | Semantic version (enables catalog mode) |

## Configuration Types

Import from `tng.config`:

```lua
local tng_config = require("tng.config")
local text = tng_config.text
local number = tng_config.number
local checkbox = tng_config.checkbox
local enum = tng_config.enum
local uint = tng_config.uint
local duration = tng_config.duration
local localtime = tng_config.localtime
local instant = tng_config.instant
```

**text** - String input:
```lua
text { id = "filter", name = "Filter Text", default = "" }
```

**number** - Floating point:
```lua
number { id = "threshold", name = "Threshold", default = 0.0 }
```

**checkbox** - Boolean:
```lua
checkbox { id = "enabled", name = "Enabled", default = true }
```

**enum** - Dropdown:
```lua
enum {
    id = "mode", name = "Mode",
    options = { { id = "add", name = "Add" }, { id = "subtract", name = "Subtract" } },
    default = "add"
}
```

**uint** - Positive integer:
```lua
uint { id = "count", name = "Count", default = 10 }
```

**duration** - Time duration (hours:minutes:seconds picker):
```lua
duration { id = "window", name = "Time Window", default = core.DURATION.HOUR }
-- Lua receives milliseconds
```

**localtime** - Time of day picker:
```lua
localtime { id = "start_time", name = "Start Time", default = 8 * core.DURATION.HOUR }
-- Lua receives milliseconds since midnight
```

**instant** - Date/time picker:
```lua
instant { id = "cutoff", name = "Cutoff Date", default = core.time().timestamp }
-- Lua receives epoch milliseconds
```

### Accessing Config

```lua
generator = function(source, config)
    local threshold = config and config.threshold or 0.0
    -- ...
end
```

## Multiple Inputs

Set `inputCount > 1`. First parameter becomes a table:

```lua
return {
    inputCount = 2,
    generator = function(sources, config)
        local source1 = sources[1]
        local source2 = sources[2]
        return function()
            local dp1 = source1.dp()
            local dp2 = source2.dp()
            if not dp1 or not dp2 then return nil end
            dp1.value = dp1.value + dp2.value
            return dp1
        end
    end,
}
```

## Filtering Data Points

Loop until you find a matching point or exhaust the source:

```lua
generator = function(source, config)
    local threshold = config and config.threshold or 0.0
    return function()
        while true do
            local dp = source.dp()
            if not dp then return nil end
            if dp.value > threshold then return dp end
        end
    end
end
```

## Available APIs

- `tng.core` - Time utilities, duration constants, date parsing
- `tng.config` - Configuration DSL
- `tng.graph` - Graph creation (for graph scripts only)

API source: [lua/src/tng/](https://github.com/SamAmco/track-and-graph/tree/master/lua/src/tng)

## Tips

- **Always handle nil**: Check `source.dp()` returns before using
- **Config fallbacks**: Always `config and config.x or default`
- **Date handling**: Use `core.date(data_point)` not `core.date(data_point.timestamp)`

---

# Contributing to the Community Catalog

## The Version Field

Adding `version` enables "catalog mode" - hides script preview, shows only config UI:

```lua
return {
    id = "my-function",
    version = "1.0.0",  -- Major = API compatibility level
    -- ...
}
```

## Localization

Catalog functions require translations in 4 languages (en, de, es, fr):

**Inline:**
```lua
title = {
    ["en"] = "Filter Greater Than",
    ["de"] = "Filtern größer als",
    ["es"] = "Filtrar mayor que",
    ["fr"] = "Filtrer supérieur à",
},
```

**Shared translations** (for common strings):
```lua
title = "_duration"  -- References shared-translations.lua
config = {
    enum {
        id = "period",
        name = "_period",
        options = { "_hours", "_days", "_weeks" },
        default = "_days",
    },
},
categories = { "_filter" },
```

Convention: Only `_` prefixed keys in shared-translations.lua.

## Function Structure (Full)

```lua
local tng_config = require("tng.config")
local number = tng_config.number

return {
    id = "function-name",
    version = "1.0.0",
    inputCount = 1,
    categories = { "_transform" },

    title = { ["en"] = "Title", ["de"] = "...", ["es"] = "...", ["fr"] = "..." },
    description = { ["en"] = "Description...", ... },

    config = {
        number { id = "multiplier", name = { ["en"] = "Multiplier", ... }, default = 1.0 },
    },

    generator = function(source, config)
        local multiplier = config and config.multiplier or 1.0
        return function()
            local dp = source.dp()
            if not dp then return nil end
            dp.value = dp.value * multiplier
            return dp
        end
    end
}
```

**Key Points:**
- **Version major** must match highest API level used
- **`deprecated = N`** (optional) phases out functions when `deprecated <= current_api_level`
- **Categories** use translation keys from shared-translations.lua

## Writing Tests

Test files export a table of test cases:

```lua
local M = {}
local core = require("tng.core")
local test = require("test.core")

M.test_multiply_basic = {
    config = { multiplier = 2.5 },  -- Actual Lua types, not strings
    sources = function()
        local now = core.time().timestamp
        return {{
            { timestamp = now - core.DURATION.DAY, value = 10.0, label = "test1" },
            { timestamp = now - core.DURATION.DAY * 2, value = 4.0, label = "test2" },
        }}
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(2, #result)
        test.assertEquals(25.0, result[1].value)
        test.assertEquals("test1", result[1].label)  -- Verify preservation
    end,
}

M.test_default_config = {
    config = {},
    sources = function()
        return {{ { timestamp = core.time().timestamp, value = 7.5, label = "test" } }}
    end,
    assertions = function(result)
        test.assertEquals(7.5, result[1].value)  -- Default multiplier = 1.0
    end,
}

return M
```

### Key Points

- **Config values are Lua types**: `multiplier = 2.5`, `enabled = true` (not strings)
- **Sources is array of arrays**: `{{ dp1, dp2 }}` for single input, `{{ src1 }, { src2 }}` for multi
- **Assertions receive array**: All data points that passed through

### Assertions

- `test.assert(message, condition)`
- `test.assertEquals(expected, actual)`
- `test.assertClose(expected, actual, tolerance)` (default: 0.01)

### Coverage

1. Basic functionality (happy path)
2. Edge cases (zero, negative, empty)
3. Default config (empty `{}`)
4. Data preservation (unchanged fields intact)

## Running Tests

```bash
make run-community-functions-tests
```

**API level gating**: Tests fail if function uses APIs above declared version.

## File Structure

```
lua/src/community/functions/your-function/
├── your-function.lua
└── test_your-function.lua
```

## Development Workflow

1. Create directory: `lua/src/community/functions/function-name/`
2. Write `function-name.lua`
3. Write `test_function-name.lua`
4. Run: `make run-community-functions-tests`
5. Validate: see [lua-tooling.md](lua-tooling.md)
