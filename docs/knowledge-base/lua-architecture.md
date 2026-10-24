---
title: Lua functions — app architecture and data model
description: Functions are derived data sources built in a visual node editor; covers FunctionGraph DTO, node types (FeatureNode, LuaScriptNode, OutputNode), configuration value types (8 types), serialization pipeline, LuaEngine execution and retained-value APIs, and Lua VM lease ownership/cancellation rules.
topics:
  - Functions: derived data sources using Lua; stored as Features in features_table
  - Visual node editor: FeatureNode → LuaScriptNode → OutputNode; data flows left to right
  - FunctionGraph DTO and JSON serialization via FunctionGraphSerializer (kotlinx.serialization)
  - Config value types: text, number, checkbox, enum, uint, duration (→ms), localtime (→ms), instant
  - LuaEngine execution APIs, including generic package entry-point evaluation through runLuaScript
  - Serialization pipeline: Node/Edge (VM) ↔ FunctionGraph (DTO) ↔ JSON (DB)
  - Lua VM affinity, validated execution-lock ownership, and cancellation cleanup
keywords: [lua, function, node-editor, FunctionGraph, LuaScriptNode, FeatureNode, OutputNode, configuration, serialization, LuaEngine, runLuaGraph, runLuaScript, callLuaFunction, LuaValueAccessor, LuaValueSnapshot, LuaValueHandle, overload, Mockito, architecture, FunctionGraphSerializer, FunctionGraphBuilder, FunctionGraphDecoder, LuaVMProvider, LuaVMAffinity, LuaVMLock, owner-token, FunctionGraphDataSample, DataSampler, cancellation, dispose, lease, LuaEngineSwitch, GuardedLuaEngineWrapper]
---

# Lua Functions Architecture

## What Are Functions?

A **Function** is a derived data source that transforms and combines data from trackers or other functions. Once created, functions can be used like trackers: added to graphs, viewed as history, or used as inputs to other functions.

Functions are built in a **visual node editor** where data flows from inputs (trackers/functions) through processing nodes (Lua scripts) to an output.

```
┌─────────────┐     ┌─────────────┐     ┌────────────┐
│ FeatureNode │────▶│ LuaScript   │────▶│ OutputNode │
│ (tracker)   │     │ Node        │     │ (result)   │
└─────────────┘     └─────────────┘     └────────────┘
```

## User Interaction

### Creating Functions
1. Tap + icon in a group → select "Function"
2. The **Output Node** appears with name/description fields and a "Duration" checkbox
3. **Long-press empty space** to open the Function Catalog
4. **Drag from output to input connectors** to connect nodes

### Node Connections
- Multiple outputs can connect to one input (data merges in reverse chronological order)
- Cycles are not allowed
- Always drag from output connector to input connector

### The Function Catalog
Long-press opens a categorized list of available functions. Tap the 'i' icon on any function to see its documentation (rendered as markdown).

### Custom Lua Scripts
Scroll to bottom of catalog → "Custom Lua Script" to write your own. You can paste code or load from a file.

### Reminders with Functions
"Time Since Last" reminders work with functions but track upstream dependencies. **Important**: Only deterministic functions (same input → same output) work reliably with reminders.

### Disabling Lua
If a function crashes the app: long-press app launcher → "Launch Lua disabled" to delete problematic functions.

## Database Storage

**Table:** `functions_table`
```
id: Long (PrimaryKey)
feature_id: Long (ForeignKey → features_table)
function_graph: String (JSON-serialized FunctionGraph)
```

Functions are stored as Features (enabling composition). The `function_graph` column contains the entire node graph as JSON.

## Data Model

### FunctionGraph DTO
`app/data/.../database/dto/FunctionGraph.kt`

```kotlin
data class FunctionGraph(
    val nodes: List<FunctionGraphNode>,
    val outputNode: FunctionGraphNode.OutputNode,
    val isDuration: Boolean  // Whether output represents duration
)
```

### Node Types (sealed class)

**FeatureNode** - Data source input:
```kotlin
data class FeatureNode(
    val id: Int,
    val featureId: Long,  // References features_table
    val x: Float, val y: Float
)
```

**LuaScriptNode** - Processing node:
```kotlin
data class LuaScriptNode(
    val id: Int,
    val script: String,
    val inputConnectorCount: Int,
    val configuration: List<LuaScriptConfigurationValue>,
    val dependencies: List<NodeDependency>,  // Edges from upstream nodes
    val catalogFunctionId: String?,  // If from library
    val catalogVersion: Version?,
    val translations: Map<String, SerializableTranslatedString>?,
    val x: Float, val y: Float
)
```

**OutputNode** - Result collector:
```kotlin
data class OutputNode(
    val id: Int,
    val dependencies: List<NodeDependency>,
    val x: Float, val y: Float
)
```

### NodeDependency (Edge)
```kotlin
data class NodeDependency(
    val connectorIndex: Int,  // Which input slot (0-based)
    val nodeId: Int           // Source node ID
)
```

## Configuration Value Types

Stored in `LuaScriptNode.configuration`, serialized with `@SerialName` discriminator:

| Type | Database Storage | Lua Receives |
|------|-----------------|--------------|
| Text | `String` | string |
| Number | `Double` | number |
| Checkbox | `Boolean` | boolean |
| Enum | `String` (option ID) | string |
| UInt | `Int` | number |
| Duration | `Double` (seconds) | number (milliseconds) |
| LocalTime | `Int` (minutes since midnight) | number (milliseconds) |
| Instant | `Long` (epoch ms) | number (epoch ms) |

Duration and LocalTime convert to milliseconds for Lua compatibility with `core.DURATION` constants.

## Serialization Pipeline

### Saving (UI → Database)

```
Node/Edge (ViewModel)
    ↓ FunctionGraphBuilder
FunctionGraph (DTO)
    ↓ FunctionGraphSerializer (kotlinx.serialization)
JSON String
    ↓
functions_table.function_graph
```

### Loading (Database → UI)

```
functions_table.function_graph
    ↓ FunctionGraphSerializer
FunctionGraph (DTO)
    ↓ FunctionGraphDecoder
Node/Edge (ViewModel)
    ↓ LuaScriptNodeProvider (analyzes scripts)
Complete UI state with metadata
```

## Runtime Execution

Every user script executes in a fresh environment table whose `__index` falls back to the pooled
VM's globals and whose `_G` points back to that fresh table. Ordinary global assignments therefore
cannot leak into later work on the same VM, while standard Lua functions and installed `tng.*`
modules remain available. Functions returned by the script retain that environment, so intentional
state in locals or execution-local globals survives for the lifetime of the returned closure.
Accessor-backed package entry points additionally install a package-aware `require` in the fresh
environment; plain scripts inherit the built-in-module `require` from the VM globals.

### LuaEngine Interface
`app/data/.../lua/LuaEngine.kt`

Primary modes:
1. `runLuaFunction()` - Parse script → metadata
2. `runLuaFunctionGenerator()` - Execute with data → `Sequence<DataPoint>`
3. `runLuaGraph()` - Execute either an inline graph script or an accessor-backed package entry point → render result
4. `runLuaCatalogue()` - Parse function library
5. `runLuaScript()` - Execute an inline or accessor-backed entry point → live `LuaValueHandle`
6. `renderLuaUi()` - Invoke a retained UI renderer with context, state, and optional affinity-bound arguments → the complete returned `LuaValueHandle`
7. `callLuaFunction()` - Invoke an affinity-bound `LuaValueHandle` with zero or more accessor-shaped arguments → live `LuaValueHandle`

`runLuaGraph` is overloaded on its second argument (`String` versus `LuaGraphScriptAccessor`). In Mockito-Kotlin tests, three untyped `any()` matchers make calls and stubs ambiguous. Use named arguments such as `runLuaGraph(vmLock = any(), script = any(), params = any())` or name `scriptAccessor` to select the intended overload explicitly.

### Execution Flow

```
LuaEngine.runLuaFunctionGenerator(script, dataSources, config)
    ↓ LuaFunctionDataSourceAdapter (wraps data sources)
    ↓ ConfigurationValueParser (converts stored values, Duration/LocalTime → ms)
    ↓ Generator function called with sources + config
    ↓ Iterator consumed as lazy Sequence<DataPoint>
```

### Lua Contract

Functions receive:
- **source**: Single data source (if `inputCount=1`) or table of sources
- **config**: Table with string keys matching config IDs

Return: Iterator function yielding data points until nil.

### Lua VM Affinity and Execution Locks

`LuaVMAffinity` is the stable identity of a Lua runtime. It can be retained alongside Lua tables and functions without keeping that runtime locked. `LuaVMLock` is short-lived exclusive access to the runtime identified by its `affinity`. `LuaEngine.acquireVM()` chooses an available runtime; `LuaEngine.acquireVM(affinity)` waits for that particular runtime. Both return an execution lock that must be released with `LuaEngine.releaseVM(lock)`.

Each acquired lock is a one-use lease with its own mutex owner token. Engine entry points verify that the lease has not been released and that its token still owns the runtime mutex. Release atomically invalidates the lease before unlocking. Consequently, double release and use-after-release fail immediately, and an old lease cannot accidentally unlock a newer holder of the same runtime. Do not manufacture, cache for reuse, or share a released `LuaVMLock`; retain `LuaVMAffinity` when long-lived runtime identity is required.

Function and graph execution can be recursive: a Lua function can read a function-backed data source, which may execute more Lua. To avoid deadlocks and excess VM consumption, a recursive operation should pass the same execution lock down through `DataSampler.getRawDataSampleForFeatureId(..., vmLock)` / `getDataSampleForFeatureId(..., vmLock)`. Stateful UI sessions may retain the affinity between calls, but must release the execution lock after each call and reacquire against that affinity for the next call.

Lua-shaped values use one traversal contract and two representations. `LuaValueAccessor` defines only read/traversal operations and makes no promise about origin or mutability. `LuaValueSnapshot` is an immutable, detached, JSON-compatible accessor suitable for DTOs and persistence. `LuaValueHandle` is a live accessor tied to a `LuaVMAffinity`; it can additionally identify functions and be passed back to that VM. A custom graph retains both the result of `prepare_data` and the original `render_graph` closure as handles; it does not reload the script before rendering. `RetainedLuaUiContent` binds renderer, live state, and optional arguments into a generic session, reacquires an execution lock using the renderer's affinity, and invokes those handles. Configuration UI uses the same session with `render_configuration` and no graph-data argument. Canvas and click callbacks follow the same affinity rule.

Database DTOs expose stored configuration as `Lazy<LuaValueSnapshot>` so unrelated reads do not decode it. The first UI render can pass that snapshot directly to Lua; later renders reuse the live state handle returned by the prior render. There is no separate retained-state copy or `retainLuaValue` conversion. `LuaEngine` returns the renderer's complete top-level value without interpreting its UI schema. In the app layer, `RetainedLuaUiContent` reads `state` to maintain the next-render argument and `LuaUiHost` reads `ui` for display. The configuration ViewModel wraps handle-to-snapshot conversion in `Lazy`, and JSON encoding occurs only when the graph DTO is converted for persistence. Never pass a handle to an arbitrary execution lock; the data-layer adapter rejects mismatched affinities.

See [Lua custom UI](lua-custom-ui.md) for the graph contract and view-layer callback lifecycle built on this affinity model.

If `DataSampler` is called with `vmLock = null` for a function-backed feature, `FunctionGraphDataSample.create()` owns the acquired lease and releases it from `dispose()`. That ownership starts before the sample object is returned, so construction must be cancellation-safe: if building nested input samples throws or is cancelled, any partially-created input samples must be disposed and the owned VM lease must be released in the construction catch path. This matters most in search and graph previews, where many function-backed graph calculations may be started and cancelled in quick succession.

`LuaEngineSwitch` / `GuardedLuaEngineWrapper` block new Lua work when Lua is disabled, but `releaseVM(lock)` is cleanup for an already-acquired execution lock and must still release unconditionally. Do not guard release on the enabled flag: if the flag is flipped after acquisition but before a `finally` block runs, throwing from release would leave the underlying VM mutex locked.

`LuaVMProvider` currently waits on a specific round-robin VM when the pool is at capacity. A leaked or never-released lease can therefore do more than reduce parallelism: a waiter may suspend behind the leaked VM even if another VM becomes free later. Do not rely on "some VM is still available" as proof that lease leaks are harmless; repeated cancellation can still cause graph batches to stall.

## ViewModel Layer

### Node Types (UI)
```kotlin
sealed class Node {
    data class Output(val id: Int, val position: Offset)
    data class DataSource(val id: Int, val featureId: Long, ...)
    data class LuaScript(val id: Int, val script: String,
                         val configInputs: Map<String, LuaScriptConfigurationInput>, ...)
}
```

### Edge & Connector
```kotlin
data class Edge(val from: Connector, val to: Connector)
data class Connector(val nodeId: Int, val type: ConnectorType, val connectorIndex: Int)
```

## Key Files

| Layer | File |
|-------|------|
| Entity | `app/data/.../database/entity/Function.kt` |
| DTO | `app/data/.../database/dto/FunctionGraph.kt` |
| Serializer | `app/data/.../serialization/FunctionGraphSerializer.kt` |
| Lua Engine | `app/data/.../lua/LuaEngineImpl.kt` |
| Config Parser | `app/data/.../lua/apiimpl/ConfigurationValueParser.kt` |
| ViewModel | `app/app/.../functions/node_editor/viewmodel/FunctionsScreenViewModel.kt` |
| Builder | `app/app/.../functions/node_editor/viewmodel/FunctionGraphBuilder.kt` |
| Decoder | `app/app/.../functions/node_editor/viewmodel/FunctionGraphDecoder.kt` |

## Translation Keys in Catalog Functions

Community catalog functions use `_`-prefixed strings as shared translation keys (e.g. `name = "_period"`, `options = { "_days", "_weeks" }`). These are resolved by `LuaFunctionMetadataAdapter` via a `LocalizationsTable` passed to `runLuaFunction`.

### How translations flow

1. **Catalog → node**: when a function is added from the catalog, the engine resolves all `_` keys against `shared-translations.lua`. The resolved `usedTranslations` are stored in `LuaScriptNode.translations` (serialized to JSON in the DB).
2. **Load existing node**: `FunctionGraphDecoder` reads `graphNode.translations`, deserializes it, and passes it back to `runLuaFunction` — so translations still resolve correctly after save/reload.
3. **Custom paste (no translations)**: if a user pastes a catalog script into a custom Lua Script node, `translations` is null. `_`-prefixed config names display as raw keys (e.g. `_period`). Enum options that are bare `_` strings must still be added as `EnumOption(key, TranslatedString.Simple(key))` — the adapter used to silently drop them, which is a bug.

Lua translation tables are parsed without a locale allowlist. The UI resolves
their BCP 47 keys against the app's current locale preferences using
language-and-script matching (for example `zh-CN` to `zh-Hans`) and falls back
explicitly to English. Do not rely on Lua table or Kotlin map iteration order
for fallback selection.

The persisted `translations` map is stored per Lua script node inside the
function graph JSON. It contains only shared translation keys used by that
node, but each key retains its complete locale map. Inline title, description,
and config-name tables remain in the persisted Lua script itself. Consequently,
an existing node retains the catalog copy and shared translations captured when
it was created until a future catalog-update mechanism replaces them.

### `version` field and catalog mode

If a script sets `version`, `LuaScriptNodeProvider` sets `showEditTools = false`, hiding the script editor. This is intentional for catalog functions (users interact via config UI only), but means a pasted catalog script also hides its own editor.

### Key files

- `LuaFunctionMetadataAdapter` — parses script metadata; handles translation lookup and fallback
- `TranslatedStringParser.parseWithLookup` — resolves a string against the translations table, falls back to `TranslatedString.Simple` if not found
- `LuaScriptNode.translations` (DTO field) — serialized snapshot of `usedTranslations`; passed back on decode to keep catalog nodes rendering correctly

## Tests

- `LuaScriptConfigurationEncoderTest` - All 8 config types
- `FunctionGraphBuilderTest` - Graph structure and dependencies
- `FunctionGraphSerializerTest` - JSON round-trip
- `LuaFunctionTests` - Generator execution
- `LuaScriptNodeProviderTest` - Script analysis
