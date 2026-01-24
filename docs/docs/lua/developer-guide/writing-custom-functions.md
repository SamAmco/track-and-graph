# Writing Custom Functions

This guide covers writing your own Lua functions for the node editor. If you're looking for how to use the visual node editor with existing functions, see [Functions](../../tutorial/functions.md).

## What Are Custom Functions?

When you long-press in the node editor, you can select functions from the catalog. But you can also write your own! A custom function is a Lua script that transforms data points - it takes data in, processes it, and outputs transformed data.

## The Simplest Function

Here's the generator from the "Absolute Value" function, stripped down to show the minimum structure:

```lua
return function(source)
    return function()
        local data_point = source.dp()
        if not data_point then
            return nil
        end

        data_point.value = math.abs(data_point.value)

        return data_point
    end
end
```

This demonstrates the core pattern:

1. Your script returns a **generator function** that accepts `source` (and optionally `config`)
2. The generator returns an **iterator function** that yields data points one at a time
3. The iterator calls `source.dp()` to get the next data point (in reverse chronological order)
4. Return `nil` when there are no more data points

To use this:

1. Create a new Function in the app
2. Long-press to add a node and scroll to the bottom for "Custom Lua Script"
3. Paste your script (or load from a file)
4. Connect a data source to the input and the output to your Output Node

## Data Point Fields

Each data point has these fields you can read and modify:

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | number | Unix timestamp in milliseconds |
| `value` | number | The numeric value |
| `label` | string | Optional label text |
| `note` | string | Optional note text |
| `offset` | number | Timezone offset in milliseconds |

## Adding Metadata

To add metadata like a title and description, return a table instead of a bare function. The table contains a `generator` field with your function, plus any metadata fields you want. Here's the full "Absolute Value" function:

```lua
return {
    id = "absolute-value",
    inputCount = 1,
    title = "Absolute Value",
    description = [[
Converts each data point's value to its absolute value (removes negative sign).
]],
    config = {},

    generator = function(source)
        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            data_point.value = math.abs(data_point.value)

            return data_point
        end
    end,
}
```

Now your function shows a title and description in the UI. You can tap the info button on the node card to see the description, which is rendered as markdown.

### Available Metadata Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string or table | No | Display name for the function |
| `description` | string or table | No | Detailed description (supports markdown) |
| `inputCount` | number | No | Number of input connections (default: 1) |
| `config` | table | No | Configuration parameters (see below) |
| `id` | string | No | Unique identifier |
| `version` | string | No | Semantic version - see [Contributing](#contributing-to-the-community-catalog) |

## Adding Configuration

Make your function configurable with the config DSL. Here's the "Multiply Values" function:

```lua
local number = require("tng.config").number

return {
    id = "multiply",
    inputCount = 1,
    title = "Multiply Values",
    description = [[
Multiplies all incoming data point values by a specified multiplier.

Configuration:
- **Multiplier**: The number to multiply all values by (default: 1.0)
]],
    config = {
        number {
            id = "multiplier",
            name = "Multiplier",
        }
    },

    generator = function(source, config)
        local multiplier = config and config.multiplier or 1.0

        return function()
            local data_point = source.dp()
            if not data_point then return nil end

            data_point.value = data_point.value * multiplier

            return data_point
        end
    end
}
```

The app will show an input field for "Multiplier" when you add this node.

### Configuration Types

Import config helpers from `tng.config`:

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

**enum** - Dropdown selection:
```lua
enum {
    id = "mode",
    name = "Mode",
    options = {
        { id = "add", name = "Add" },
        { id = "subtract", name = "Subtract" }
    },
    default = "add"
}
```

**uint** - Positive integer:
```lua
uint { id = "count", name = "Count", default = 10 }
```

**duration** - Time duration (shown as hours:minutes:seconds picker):
```lua
local core = require("tng.core")
duration { id = "window", name = "Time Window", default = core.DURATION.HOUR }
-- Your function receives milliseconds
```

**localtime** - Time of day (shown as time picker):
```lua
local core = require("tng.core")
localtime { id = "start_time", name = "Start Time", default = 8 * core.DURATION.HOUR }
-- Your function receives milliseconds since midnight
```

**instant** - Date and time (shown as date/time picker):
```lua
local core = require("tng.core")
instant { id = "cutoff", name = "Cutoff Date", default = core.time().timestamp }
-- Your function receives epoch milliseconds
```

## Filtering Data Points

Functions can filter by returning only some data points. Here's "Filter Greater Than":

```lua
local tng_config = require("tng.config")
local number = tng_config.number
local checkbox = tng_config.checkbox

return {
    id = "filter-greater-than",
    inputCount = 1,
    title = "Filter Greater Than",
    description = [[
Filters data points by value. Only data points with values greater than the threshold will pass through.

Configuration:
- **Threshold**: The minimum value (exclusive by default)
- **Include Equal**: Also include values equal to the threshold (default: false)
]],
    config = {
        number {
            id = "threshold",
            name = "Threshold",
        },
        checkbox {
            id = "include_equal",
            name = "Include Equal",
        },
    },

    generator = function(source, config)
        local threshold = config and config.threshold or 0.0
        local include_equal = config and config.include_equal or false

        return function()
            while true do
                local data_point = source.dp()
                if not data_point then
                    return nil
                end

                local passes
                if include_equal then
                    passes = data_point.value >= threshold
                else
                    passes = data_point.value > threshold
                end

                if passes then
                    return data_point
                end
            end
        end
    end,
}
```

## Multiple Inputs

Set `inputCount` greater than 1 to accept multiple data sources. The first parameter becomes a table of sources. Here's a simplified version of "Pair and Operate":

```lua
return {
    id = "pair-and-operate",
    inputCount = 2,
    title = "Pair and Operate",

    generator = function(sources, config)
        local source1 = sources[1]
        local source2 = sources[2]

        return function()
            local dp1 = source1.dp()
            local dp2 = source2.dp()

            if not dp1 or not dp2 then return nil end

            -- Add values from both sources
            dp1.value = dp1.value + dp2.value
            return dp1
        end
    end,
}
```

## Available APIs

The Track & Graph Lua environment provides several modules you can import:

- `tng.core` - Time utilities, duration constants, date parsing, data source helpers
- `tng.config` - Configuration DSL for defining user inputs
- `tng.graph` - Graph creation (for graph scripts, not function scripts)

The API source files are well-documented with comments. Browse them on GitHub: **[lua/src/tng/](https://github.com/SamAmco/track-and-graph/tree/master/lua/src/tng)**

The [Writing Graph Scripts](writing-graph-scripts.md) guide also covers these APIs in the context of graph scripts.

## Tips

- **Always handle nil**: Check if `source.dp()` returns nil before using the data point
- **Config fallbacks**: Always provide defaults like `config and config.x or default_value`
- **Use core.date() with full data points**: Pass the entire data point, not just the timestamp, to get correct timezone handling

## Contributing to the Community Catalog

If you've created a useful function and want to share it with the community, you can submit a pull request to include it in the catalog.

### The Version Field

When you add a `version` field to your function, the UI changes: the script preview and Paste/File buttons are hidden, showing only the configuration fields. This is the "catalog mode" - users interact with your function through its config UI rather than seeing the code.

```lua
return {
    id = "my-function",
    version = "1.0.0",  -- Adding this enables catalog mode
    -- ...
}
```

The major version number declares API compatibility. If your function uses API features from level 1, your version should be "1.x.x".

### Localization

Community catalog functions must provide translations for all text in four languages: English (en), German (de), Spanish (es), and French (fr).

**Inline translations** - provide a table with all four languages:

```lua
title = {
    ["en"] = "Filter Greater Than",
    ["de"] = "Filtern größer als",
    ["es"] = "Filtrar mayor que",
    ["fr"] = "Filtrer supérieur à",
},
```

**Shared translations** - for common strings, use a translation key (prefixed with `_`) that references [shared-translations.lua](https://github.com/SamAmco/track-and-graph/blob/master/lua/src/community/shared-translations.lua):

```lua
config = {
    duration {
        id = "threshold",
        name = "_time_threshold",  -- References shared translation
    },
    enum {
        id = "operation",
        name = "_operation",
        options = { "_addition", "_subtraction", "_multiplication", "_division" },  -- All reference shared translations
        default = "_addition",
    },
},
categories = { "_filter" },  -- Also uses shared translations
```

This is especially useful for enum options where the same values (like days of the week, aggregation types, etc.) appear in multiple functions. Check the shared translations file for available keys before adding new inline translations.

### Testing

Functions must include tests. Test files live alongside your function in the same directory with names prefixed with `test_`.

#### The Testing Approach

Function scripts return a Lua table with metadata and a `generator` field. The test harness loads this table directly and can override config values before calling the generator.

#### Test File Structure

Test files export a table of test cases:

```lua
local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_multiply_basic = {
    config = {
        -- Config values are actual Lua types, not strings
        multiplier = 2.5,
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {  -- First input source (array of data points)
                {
                    timestamp = now - (DDAY * 1),
                    value = 10.0,
                    label = "test1",
                },
                {
                    timestamp = now - (DDAY * 2),
                    value = 4.0,
                    label = "test2",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)

        -- Result is an array of transformed data points
        test.assertEquals(2, #result)
        test.assertEquals(25.0, result[1].value)  -- 10.0 * 2.5
        test.assertEquals(10.0, result[2].value)  -- 4.0 * 2.5

        -- Verify other fields are preserved
        test.assertEquals("test1", result[1].label)
    end,
}

M.test_with_default_config = {
    config = {},  -- Test default behavior
    sources = function()
        local now = core.time().timestamp
        return {
            {
                { timestamp = now, value = 7.5, label = "test" },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(1, #result)
        test.assertEquals(7.5, result[1].value)  -- Default multiplier is 1.0
    end,
}

return M
```

#### Key Points

- **Config values are actual Lua types**: Use `multiplier = 2.5` (number), `enabled = true` (boolean), not strings like `"2.5"` or `"true"`.
- **Sources is an array of arrays**: Each inner array represents one input source. For single-input functions, return `{ { dp1, dp2, ... } }`. For multi-input functions, return `{ { source1_points }, { source2_points } }`.
- **Assertions receive an array of data points**: All the data points that passed through your function.

#### Test Assertions

The `test.core` module provides:

- `test.assert(message, condition)` - Assert condition is truthy
- `test.assertEquals(expected, actual)` - Assert values are equal
- `test.assertClose(expected, actual, tolerance)` - Assert numbers are close (default tolerance: 0.01)

#### Recommended Test Coverage

For each function, write tests for:

1. **Basic functionality** - Happy path with typical inputs
2. **Edge cases** - Zero, negative, empty values as relevant
3. **Default config** - Behavior when config is empty `{}`
4. **Data preservation** - Verify unchanged fields (label, note, timestamp) remain intact

#### Running Tests

From the repository root:

```bash
make run-community-functions-tests
```

#### Debugging Tests

If a test fails:

1. Check that config keys match your function's config item IDs exactly
2. Verify you're using actual Lua types, not strings (e.g., `true` not `"true"`)
3. Remember sources is an array of arrays, even for single-input functions
4. Add `print()` statements to your generator to debug data flow (visible in test output)

### File Structure

Community functions live in `lua/src/community/functions/`:

```
lua/src/community/functions/your-function/
  ├── your-function.lua      # Main script
  └── test_your-function.lua # Test file(s)
```

### Learning More

For comprehensive documentation on contributing to the catalog:

- **[Community Functions README](https://github.com/SamAmco/track-and-graph/blob/master/lua/src/community/functions/README.md)** - Full guide covering structure, testing, shared translations, and the config DSL
- **[Example Functions](https://github.com/SamAmco/track-and-graph/tree/master/lua/src/community/functions)** - Browse existing functions to see patterns and conventions

## Next Steps

- See [Writing Graph Scripts](writing-graph-scripts.md) for creating custom visualizations (a different type of Lua script)
