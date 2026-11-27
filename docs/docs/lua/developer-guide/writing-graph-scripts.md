# Writing Lua Graph Packages

This guide is an outline; the contract reference and agent skill are still to be written.

## Start from examples and API source

- [Calendar heatmap package and tests](https://github.com/SamAmco/track-and-graph/tree/master/lua/src/community/graphs/calendar-heatmap): a working graph with configuration UI.
- [Lua API source and type annotations](https://github.com/SamAmco/track-and-graph/tree/master/lua/src/tng): reference existing Lua definitions rather than duplicate them here.

## Script and package structure

TODO: Briefly explain the entry-point return value, single-file versus ZIP packages, `main.lua`, local modules via `require`, and keeping translated strings in `strings.lua`. Link to the example for implementation details.

## Entry-point return contract

TODO: A compact table of supported top-level keys: key, type, required/optional, purpose, and link to the relevant definition. Distinguish native graph results from custom UI graphs without a step-by-step tutorial.

## Callback inputs and outputs

TODO: Compact contract tables for `prepare_data`, `render_graph`, and `render_configuration`: which input keys are available, what each callback returns, and when it runs. Clarify state/configuration, graph data, context, feature mappings, validation, and view mode.

Link to existing Lua class definitions wherever possible. Document bridge-provided structures here only where no Lua definition exists, with links to the corresponding implementation:

- [Graph script adapter](https://github.com/SamAmco/track-and-graph/blob/master/app/data/src/main/java/com/samco/trackandgraph/data/lua/graphadapters/LuaGraphScriptAdapter.kt)
- [UI script adapter](https://github.com/SamAmco/track-and-graph/blob/master/app/data/src/main/java/com/samco/trackandgraph/data/lua/LuaUiScriptAdapter.kt)

## Compatibility and testing

TODO: Short notes on targeting an app/API level, versioning, and running the example's tests. Link to the harness and API specifications rather than reproduce their documentation.

## Write a graph with an AI assistant

TODO: Link to a downloadable graph-writing `SKILL.md`, alongside the existing function-writing skill. Keep the skill focused on finding version-appropriate examples, code, contracts, and rules, then testing and packaging the result—not duplicating an API manual.

## Recover from a broken script

Long-press the app launcher icon and select **Launch Lua disabled**. You can then edit or delete the offending graph before restarting normally.

## Older single-file graphs

- [Deprecated graph scripting guide](../deprecated/developer-guide/writing-graph-scripts.md)
- [Deprecated graph tutorial: installation and manual configuration](../deprecated/tutorial/lua-graphs.md)
