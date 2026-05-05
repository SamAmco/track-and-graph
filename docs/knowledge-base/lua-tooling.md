---
title: Lua tooling, build process, publishing, and adding new config types
description: Directory structure for lua/, building the function catalog (pack-functions.lua), validation and inspection tools, debug/prod catalog publishing, and the complete list of 7 implementation + 5 test files to update when adding a new configuration type.
topics:
  - Directory: lua/src/community/functions/, lua/catalog/, lua/tools/, lua/src/tng/
  - Build catalog: lua tools/pack-functions.lua → catalog/community-functions.lua
  - Validation tools: verify-api-specs, validate-functions, validate-function-categories, detect-changes
  - Catalog publish: make lua-publish-debug / make lua-publish-prod rebuild before signing; direct Lua publish scripts only sign existing catalog
  - Adding config types: 7 implementation files + 5 test files must ALL be updated
keywords: [lua, tooling, build, pack-functions, catalog, luarocks, serpent, validation, config-types, debug-publish, prod-publish, publish-functions-prod, api-specs, make]
---

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

Prefer the `make` targets when publishing. `make lua-publish-debug` and `make lua-publish-prod` depend on `lua-pack-functions`, so they rebuild `lua/catalog/community-functions.lua` before signing. Running `lua tools/publish-functions-debug.lua` or `lua tools/publish-functions-prod.lua` directly only signs/copies the existing catalog file; if you skip packing first, you can publish stale catalog content.

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

## Catalog Publishing

Debug publishing signs the catalog with a fresh keypair and copies it into debug assets:
```bash
make lua-publish-debug
```

Creates in `app/app/src/debug/assets/functions-catalog/`:
- `community-functions.lua` - The catalog
- `community-functions.sig.json` - Signature metadata
- `debug-<timestamp>.pub` - Public key

Because these are Android assets, rebuild/reinstall the debug app before expecting a running APK to see the changed debug catalog.

Production publishing signs `lua/catalog/community-functions.lua` with the production key:
```bash
make lua-publish-prod
```

This expects `lua/.private-key-path` to contain the absolute path to the password-protected production private key. It writes:
- `lua/catalog/community-functions.lua`
- `lua/catalog/community-functions.sig.json`

Commit the catalog and signature together. Production clients fetch those files from the URLs in `configuration/remote-configuration.json`; currently they point at the `master` branch raw GitHub URLs for `lua/catalog/community-functions.lua` and `lua/catalog/community-functions.sig.json`. No app release is required for a catalog-only update as long as the existing production public key remains valid.

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
