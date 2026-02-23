# Lua Tooling and Project Structure

## Directory Structure

```
lua/
├── src/community/functions/    # Community Lua functions
│   └── function-name/
│       ├── function-name.lua
│       └── test_function-name.lua
├── catalog/                    # Generated catalog output
├── tools/                      # Build and validation scripts
└── src/tng/                    # TNG API and .apispec.lua files
```

## Building the Function Catalog

The catalog bundles all community functions for distribution. The Android app downloads it at runtime.

**Prerequisites:**
```bash
brew install lua luarocks
luarocks install serpent
```

**Generate catalog** (from `lua/` directory):
```bash
lua tools/pack-functions.lua
```
Output: `catalog/community-functions.lua`

## Validation Tools

Run from `lua/` directory:

| Tool | Purpose |
|------|---------|
| `lua tools/verify-api-specs.lua` | Ensures TNG API exports have API level specs |
| `lua tools/get-max-api-level.lua` | Returns highest API level across specs |
| `lua tools/validate-functions.lua` | Validates required fields and translations |
| `lua tools/validate-function-categories.lua` | Validates category keys |
| `lua tools/detect-changes.lua` | Compares functions against published catalog |

## Catalog Inspection

```bash
# From repository root (all functions):
make lua-print-catalog

# From lua/ directory (with optional API level filter):
lua tools/print-catalog.lua          # All functions
lua tools/print-catalog.lua 1        # Only API level 1 compatible
```

## Debug Publishing

Signs catalog with fresh keypair for debug builds:
```bash
lua tools/publish-functions-debug.lua
```

Creates in `app/app/src/debug/assets/functions-catalog/`:
- `community-functions.lua` - The catalog
- `community-functions.sig.json` - Signature metadata
- `debug-<timestamp>.pub` - Public key

## Adding New Configuration Types

7 files + 5 test files to update when adding new config types (text, number, checkbox, enum, etc.):

**Core Implementation:**
1. `LuaFunctionConfigSpec` - Data DTO (what Lua defines)
2. `LuaScriptConfigurationValue` - Database serialization
3. `LuaScriptConfigurationInput` - ViewModel with mutable state
4. `LuaScriptConfigurationInputFactory` - Creates inputs from specs
5. `LuaScriptConfigurationEncoder` - ViewModel → Database
6. `ConfigurationValueParser` - Database → Lua VM
7. `ConfigurationInputField` - UI component
8. `LuaFunctionMetadataAdapter` - Parses Lua config items

**Tests (will fail via reflection if you miss types):**
- `LuaFunctionMetadataTests`
- `LuaScriptConfigurationInputFactoryTest`
- `LuaScriptConfigurationEncoderTest`
- `ConfigurationValueParserTest`
- `FunctionGraphSerializerTest`

**Naming:** PascalCase for `@SerialName`, snake_case in Lua.
