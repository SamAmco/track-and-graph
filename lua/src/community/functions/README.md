# Community Functions Guide

This guide covers everything you need to write and test new Lua functions for Track & Graph's node editor.

## Overview

Community functions are Lua scripts that transform data point streams. Each function lives in its own directory alongside its test cases:
- `function-name.lua` - The function implementation
- `test_function-name.lua` - Test cases

## Function Structure

A function exports a table with metadata and a generator function:

```lua
return {
    id = "function-name",
    version = "1.0.0",  -- Major version = API compatibility level
    inputCount = 1,      -- Number of input data sources

    title = {
        ["en"] = "Function Title",
        -- Add translations for de, es, fr
    },

    description = {
        ["en"] = [[Function description.

Use multi-line strings for detailed descriptions.
List configuration options with bullet points for complex functions.]],
    },

    config = {
        {
            id = "param_name",
            type = "text",  -- or "number", "checkbox"
            default = "default_value",  -- Optional: specify default value
            name = {
                ["en"] = "Parameter Name",
            }
        }
    },

    generator = function(source, config)
        -- Access config values
        local param = config and config.param_name

        -- Return iterator function
        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            -- Transform data_point...

            return data_point
        end
    end
}
```

### Key Points

- **Version major number**: Must match the highest API level used by the function. If you call any API with a higher level, tests will fail. See the .apispec.lua files in the `lua/src/tng/` directory for details on api levels.
- **Config types**: `text` (string), `number` (double), `checkbox` (boolean)
- **Config defaults**: Optionally specify `default` field in config spec to provide a default value. These defaults are used in the UI, but your generator should still handle missing values with fallbacks.
- **Config values**: In tests, pass actual Lua types (e.g., `true`/`false` for checkboxes, not strings as are used in the graph tests)
- **Generator pattern**: Returns an iterator function that yields data points one at a time. The generator takes source(s) and config as arguments. The sources will be a single data source (see the core.lua api) if `inputCount` is 1, or a list of sources if `inputCount` > 1. The config will be a table of config values with keys matching the config ids and values matching the users inputs (or test inputs).
- **Data point fields**: `timestamp`, `value`, `label`, `note`, `offset`
- **Date/time handling**: Always pass the full data point to `core.date()`, not just the timestamp. The data point contains both `timestamp` and `offset` which are needed for correct timezone and DST handling. Use `local date = core.date(data_point)` not `core.date(data_point.timestamp)`.

## Writing Tests

Test files export a table of test cases:

```lua
local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_case_name = {
    config = {
        param_name = value,  -- Use actual Lua types, not strings
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {  -- First input source
                {
                    timestamp = now - (DDAY * 1),
                    value = 10.0,
                    label = "test",
                    note = "note",
                },
                -- More data points...
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(expected, result[1].value)
        -- More assertions...
    end,
}

return M
```

Functions can accept multiple input sources. If the inputCount is greater than 1, the `sources` function should return a list of lists, one for each input source.

You can create multiple test_*.lua files for one function in the same directory if you want to organize tests by category.

### Test Assertions

- `test.assert(message, condition)` - Assert condition is truthy
- `test.assertEquals(expected, actual)` - Assert values are equal
- Result is an array of data points that passed through the function

### Minimal Test Coverage

For each function, write tests for:
1. **Basic functionality** - Happy path with typical inputs
2. **Edge cases** - Zero, negative, empty values as relevant
3. **No config** - Default behavior when config is empty
4. **Data preservation** - Verify unchanged fields remain intact

Keep tests concise and focused. Avoid redundancy.

## Running Tests

Tests run in a Kotlin test harness that uses the real Track & Graph APIs installed at runtime in the JVM. This ensures functions work correctly with actual API implementations.

From the repository root:

```bash
# Run all function tests
make run-community-functions-tests

# Run all tests (functions + graphs)
make run-community-tests
```

Tests execute with `--rerun-tasks` to always show output, even when cached.

### API Level Gating

Tests enforce API compatibility:
- The function's `version` major number declares its API compatibility level
- During test execution, if the function accesses any API above that level, the test fails
- This ensures functions don't accidentally use newer APIs than declared

Example: A function with `version = "1.2.3"` can only use API level 1 features.

## Development Workflow

1. **Create function directory**: `lua/src/community/functions/function-name/`
2. **Write function**: `function-name.lua`
3. **Write tests**: `test_function-name.lua` (3-5 concise tests)
4. **Run tests**: `make run-community-functions-tests`
5. **Validate and package**: See [lua/README.md](../../README.md) for packaging and validation tools

## Config Parameter Patterns

### Boolean Checkboxes
```lua
-- In function spec:
{
    id = "my_checkbox",
    type = "checkbox",
    default = false,  -- Optional: specify default
    name = { ["en"] = "My Checkbox" }
}

-- In tests:
config = { my_checkbox = true }

-- In generator:
local enabled = config and config.my_checkbox or false
```

### Optional Text
```lua
-- In function spec:
{
    id = "my_text",
    type = "text",
    default = "",  -- Optional: specify default
    name = { ["en"] = "My Text" }
}

-- In tests:
config = { my_text = "value" }

-- In generator:
local text = config and config.my_text  -- nil if not set
```

### Numbers with Defaults
```lua
-- In function spec:
{
    id = "my_number",
    type = "number",
    default = 1.0,  -- Optional: specify default
    name = { ["en"] = "My Number" }
}

-- In tests:
config = { my_number = 42.0 }

-- In generator:
local num = config and config.my_number or 1.0  -- Fallback if not set
```
