---
title: Lua tooling, build process, publishing, and adding new config types
description: Directory structure for lua/, building the function catalog (pack-functions.lua), validation and inspection tools, debug/prod catalog publishing, and the complete list of 7 implementation + 5 test files to update when adding a new configuration type.
topics:
  - Directory: lua/src/community/functions/, lua/catalog/, lua/tools/, lua/src/tng/
  - Build catalog: lua tools/pack-functions.lua → catalog/community-functions.lua
  - Validation tools: verify-api-specs, validate-functions, pack-functions, detect-changes
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

Translation tooling operates on the individual files under
`lua/src/community/functions/`, never on this generated catalog.
`lua/tools/export-translatable-copy.lua` executes source modules and exports
their inline copy. `scripts/translations/translate_lua_catalog.py` regenerates
one explicitly selected function at a time, reusing the shared language
manifest, domain brief, and provider adapter, then emits validated drafts under
`build/translation-drafts/lua`. Generation is paid but does not edit source.
Apply a reviewed, complete draft separately with
`make lua-translations-apply DRAFT=…`; partial language suites and partial
functions are rejected.

Shared copy is stored as editable records in
`lua/src/community/shared-translations-data.lua`. The normal
`shared-translations.lua` wrapper indexes and strictly validates those records.
This separation lets draft tooling inspect a newly added English-only record
without weakening runtime or release validation.

Prefer the `make` targets when publishing. `make lua-publish-debug` and `make lua-publish-prod` depend on `lua-pack-functions`, so they rebuild `lua/catalog/community-functions.lua` before signing. Running `lua tools/publish-functions-debug.lua` or `lua tools/publish-functions-prod.lua` directly only signs/copies the existing catalog file; if you skip packing first, you can publish stale catalog content.

## Validation Tools

Run from `lua/` directory:

| Tool | Purpose |
|------|---------|
| `lua tools/verify-api-specs.lua` | Ensures TNG API exports have API level specs |
| `lua tools/get-max-api-level.lua` | Returns highest API level across specs |
| `lua tools/validate-functions.lua` | Validates required fields, inline translation structure, and every shared translation reference |
| `lua tools/pack-functions.lua` | Performs publish-time validation, including resolving category, config-name, and enum-option shared translation keys |
| `lua tools/detect-changes.lua` | Compares functions against published catalog |

`make validate-all` is the release gate. Keep new validation and its tests
reachable from this target; a check that only runs from an ad-hoc command can
otherwise be missed before publishing.

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
