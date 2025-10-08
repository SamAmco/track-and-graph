# Lua Project Structure

This directory contains Lua source code, tooling, and build artifacts for Track & Graph's Lua integration.

## Community Functions

Community functions are Lua scripts that transform data point streams for use in the node editor. They live in `src/community/functions/` with each function in its own directory:

```
src/community/functions/
├── multiply/
│   ├── multiply.lua
│   └── test_multiply.lua
├── override-label/
│   ├── override-label.lua
│   └── test_override-label.lua
└── filter-by-label/
    ├── filter-by-label.lua
    └── test_filter-by-label.lua
```

Each function in this directory tree must export a table with at least the following fields:
- `id` (string): Unique identifier for the function
- `version` (string): Semantic version (e.g., "1.0.0")
- `generator` (function): The function implementation

## Development Tools

### Validate API Specs
Ensures all TNG API exports have corresponding API level specifications:
```bash
lua tools/verify-api-specs.lua
```

### Get Max API Level
Returns the highest API level defined across all specs:
```bash
lua tools/get-max-api-level.lua
```

### Validate Functions
Validates all community functions have required fields and translations:
```bash
lua tools/validate-functions.lua
```

### Detect Changes
Compares current functions against the published catalog to detect changes:
```bash
lua tools/detect-changes.lua
```

## Building the Function Catalog

The function catalog is a single Lua file that bundles all community functions for distribution. The Android app downloads this catalog at runtime to discover available functions.

### Prerequisites

Install required Lua dependencies:

```bash
brew install lua luarocks
luarocks install serpent
```

### Running the Packer

From the `lua/` directory, run:

```bash
lua tools/pack-functions.lua
```

This will:
1. Crawl `src/community/functions/` for all `.lua` files (excluding `test_*` files)
2. Validate each function has required fields (`id`, `version`, `generator`)
3. Check for duplicate id/version pairs
4. Validate version strings are valid semver
5. Bundle all functions into `catalog/community-functions.lua`

The output file should be committed to version control.
