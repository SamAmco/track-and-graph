# Lua graph packages

Each graph has its own directory, with ZIP contents under `package/` (`main.lua` at the package root) and tests and fixtures under the sibling `test/` directory. Only `package/` belongs in the distributable archive. Calendar heatmap is the first package; signed catalog publishing is not yet implemented.

Run `make run-community-graph-tests` from the repository root. Each `test/test_*.lua` returns named test functions receiving the real-engine harness; other files in `test/` are not discovered as suites. See [the harness documentation](../../../../docs/knowledge-base/lua-graph-scripts.md#real-engine-graph-harness) and calendar heatmap's tests for examples. The version major declares the required API level; exercised API exports are checked against that level, including configuration and deferred drawing callbacks.

The older single-file scripts are retained only in [`docs/docs/lua/deprecated/community/`](../../../../docs/docs/lua/deprecated/community/index.md). They are permanent website downloads, not generated copies or an active source catalog. Installed scripts remain supported by the app and its runtime compatibility tests.
