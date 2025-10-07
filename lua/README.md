# Lua Project Structure

This directory contains Lua source code, tooling, and build artifacts for Track & Graph's Lua integration.

## Directory Structure

```
lua/
├── src/                          # Runtime source code
│   ├── tng/                      # Track & Graph API (bundled with app)
│   ├── test/                     # Test utilities
│   └── community/                # Community-contributed code
│       ├── graphs/               # Graph scripts (bar charts, line graphs, etc.)
│       └── functions/            # Function scripts (data transformations)
├── tools/                        # Build and packaging tools
│   └── pack-functions.lua        # Packages community functions into catalog
├── catalog/                      # Generated catalogs (committed to git)
│   └── community-functions.lua   # Packaged community functions
└── README.md                     # This file
```

## Community Functions

Community functions are Lua scripts that transform data point streams. They live in `src/community/functions/` with each function in its own directory:

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

Each function must export a table with:
- `id` (string): Unique identifier for the function
- `version` (string): Semantic version (e.g., "1.0.0")
- `generator` (function): The function implementation

## Building the Function Catalog

The function catalog is a single Lua file that bundles all community functions for distribution. The Android app downloads this catalog at runtime to discover available functions.

### Prerequisites

Install required Lua dependencies:

```bash
luarocks install serpent
```

### Running the Packer

From the `lua/` directory, run:

```bash
lua tools/pack-functions.lua
```

This will:
1. Scan `src/community/functions/` for all `.lua` files (excluding `test_*` files)
2. Validate each function has required fields (`id`, `version`, `generator`)
3. Check for duplicate id/version pairs
4. Validate version strings are valid semver
5. Bundle all functions into `catalog/community-functions.lua`

The output file should be committed to version control.

### Catalog Format

The generated catalog is a Lua table with this structure:

```lua
return {
  functions = {
    {
      id = "multiply",
      version = "1.0.0",
      script = "-- full script content..."
    },
    {
      id = "override-label",
      version = "1.0.0",
      script = "-- full script content..."
    },
    -- ...
  }
}
```

## Development Workflow

### Adding a New Function

1. Create a new directory in `src/community/functions/<function-name>/`
2. Create `<function-name>.lua` with your function implementation
3. Create `test_<function-name>.lua` with tests (optional but recommended)
4. Run `lua tools/pack-functions.lua` to rebuild the catalog
5. Commit both the source file and updated `catalog/community-functions.lua`

### Testing Functions

Community function tests are run via the Android project's test suite. See the main project README for details on running tests.

## Runtime Distribution

The `catalog/community-functions.lua` file is served via GitHub raw content URLs. The Android app downloads this catalog to discover and load community functions at runtime. This allows functions to be updated without releasing a new app version.

**Important:** Any changes to `catalog/community-functions.lua` should be committed and pushed to ensure users get the latest functions.
