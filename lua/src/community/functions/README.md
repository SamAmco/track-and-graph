# Community Functions Guide

This guide covers everything you need to write and test new Lua functions for Track & Graph's node editor.

## Overview

Community functions are Lua scripts that transform data point streams. Each function lives in its own directory alongside its test cases:
- `function-name.lua` - The function implementation
- `test_function-name.lua` - Test cases

## Function Structure

A function exports a table with metadata and a generator function. Use the `tng.config` DSL to define configuration items:

```lua
local tng_config = require("tng.config").number
local number = tng_config.number
local checkbox = tng_config.checkbox

return {
    id = "function-name",
    version = "1.0.0",  -- Major version = API compatibility level
    inputCount = 1,      -- Number of input data sources
    categories = { "_transform" },  -- One or more category keys

    title = {
        ["en"] = "Function Title",
        ["de"] = "Funktionstitel",
        ["es"] = "Título de función",
        ["fr"] = "Titre de la fonction",
    },

    description = {
        ["en"] = [[Function description.

Use multi-line strings for detailed descriptions.
List configuration options with bullet points for complex functions.]],
        ["de"] = [[Funktionsbeschreibung...]],
        ["es"] = [[Descripción de la función...]],
        ["fr"] = [[Description de la fonction...]],
    },

    config = {
        number {
            id = "multiplier",
            name = {
                ["en"] = "Multiplier",
                ["de"] = "Multiplikator",
                ["es"] = "Multiplicador",
                ["fr"] = "Multiplicateur",
            },
            default = 1.0,
        },
        checkbox {
            id = "enabled",
            name = {
                ["en"] = "Enabled",
                ["de"] = "Aktiviert",
                ["es"] = "Habilitado",
                ["fr"] = "Activé",
            },
            default = true,
        },
    },

    generator = function(source, config)
        -- Access config values with fallbacks
        local multiplier = config and config.multiplier or 1.0
        local enabled = config and config.enabled or false

        -- Return iterator function
        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            -- Transform data_point...
            if enabled then
                data_point.value = data_point.value * multiplier
            end

            return data_point
        end
    end
}
```

### Key Points

- **Use the config DSL**: Import helpers from `tng.config` and use them to define configuration items
- **Version major number**: Must match the highest API level used by the function. If you call any API with a higher level, tests will fail. See the .apispec.lua files in the `lua/src/tng/` directory for details on api levels.
- **Deprecated (optional)**: Optionally specify `deprecated = N` (integer) to mark when a function becomes deprecated. Functions with `deprecated <= current_api_level` will be filtered out. Use this to phase out old functions.
- **Categories**: Array of category keys (e.g., `{"_arithmetic", "_filter"}`). Use translation keys from shared-translations.lua.
- **Config defaults**: Optionally specify `default` field to provide a default value. These defaults are used in the UI, but your generator should still handle missing values with fallbacks.
- **Config values**: In tests, pass actual Lua types (e.g., `true`/`false` for checkboxes, not strings)
- **Generator pattern**: Returns an iterator function that yields data points one at a time. The generator takes source(s) and config as arguments. The sources will be a single data source (see the core.lua api) if `inputCount` is 1, or a list of sources if `inputCount` > 1. The config will be a table of config values with keys matching the config ids and values matching the users inputs (or test inputs).
- **Data point fields**: `timestamp`, `value`, `label`, `note`, `offset`
- **Date/time handling**: Always pass the full data point to `core.date()`, not just the timestamp. The data point contains both `timestamp` and `offset` which are needed for correct timezone and DST handling. Use `local date = core.date(data_point)` not `core.date(data_point.timestamp)`.

## Shared Translations

Reusable translated strings are defined in `src/community/shared-translations.lua`. Functions can reference these translations by key instead of defining translations inline.

### How It Works

When the metadata parser encounters a **string** (not a table) for `title`, `description`, `config[].name`, or `enum options`:
1. Look up the string in shared-translations.lua
2. If found: replace with the translation table
3. If not found: use the string as a literal value

This allows you to write `title = "_duration"` and have it automatically replaced with `{en="Duration", de="Dauer", ...}`.

### Convention

**Only store `_` prefixed keys in shared-translations.lua** to avoid accidentally matching literal strings you didn't intend as lookups.

### Examples

**Using translation key**:
```lua
title = "_duration"  -- Looks up in shared-translations.lua, replaced with translation table
```

**Using literal string**:
```lua
title = "My Custom Function"  -- Not in translations table, used as Simple string
```

**Using inline translations**:
```lua
title = {  -- Table, used directly without lookup
    ["en"] = "My Function",
    ["de"] = "Meine Funktion"
}
```

Note: In the catalog all strings must have translations. Either they should define translations inline, or use a translation key that exists in shared-translations.lua.

### Using Translation Keys with Config DSL

```lua
local enum = require("tng.config").enum

config = {
    enum {
        id = "time_unit",
        name = "_time_unit",  -- Translation key
        options = { "_seconds", "_minutes", "_hours", "_days" },
        default = "_hours"
    }
}
```

### Adding New Translations

Edit `src/community/shared-translations.lua` and add your key (prefixed with `_`) with translations for all 4 languages (en, de, es, fr). The file validates itself to ensure all entries are complete and unique.

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

## Configuration DSL

Use the `tng.config` module for a cleaner, more ergonomic way to define configuration items:

```lua
local tng_config = require("tng.config")
local text = tng_config.text
local number = tng_config.number
local checkbox = tng_config.checkbox
local enum = tng_config.enum
local uint = tng_config.uint

return {
    config = {
        text {
            id = "label_filter",
            name = { ["en"] = "Label Filter" },
            default = "",
        },
        number {
            id = "multiplier",
            name = { ["en"] = "Multiplier" },
            default = 1.0,
        },
        checkbox {
            id = "enabled",
            name = { ["en"] = "Enabled" },
            default = true,
        },
        enum {
            id = "period",
            name = { ["en"] = "Period" },
            options = { "_hours", "_days", "_weeks" },
            default = "_days",
        },
        uint {
            id = "count",
            name = { ["en"] = "Count" },
            default = 10,
        },
    },
}
```

### Available Config Types

**text**: String input
```lua
config.text { id = "my_text", name = {...}, default = "value" }
```

**number**: Floating point number
```lua
config.number { id = "my_number", name = {...}, default = 1.0 }
```

**checkbox**: Boolean value
```lua
config.checkbox { id = "my_checkbox", name = {...}, default = true }
```

**enum**: Dropdown with predefined options
```lua
config.enum { id = "my_enum", name = {...}, options = {"_opt1", "_opt2"}, default = "_opt1" }
```

**uint**: Unsigned integer (non-negative whole number)
```lua
config.uint { id = "my_uint", name = {...}, default = 42 }
```

### Accessing Config Values in Generator

```lua
generator = function(source, config)
    local text_val = config and config.my_text
    local number_val = config and config.my_number or 1.0
    local checkbox_val = config and config.my_checkbox or false
    local enum_val = config and config.my_enum
    local uint_val = config and config.my_uint or 1

    -- Use values...
end
```

### Config in Tests

Pass actual Lua types (not strings):
```lua
config = {
    my_text = "test value",
    my_number = 42.0,
    my_checkbox = true,
    my_enum = "_hours",
    my_uint = 10,
}
```
