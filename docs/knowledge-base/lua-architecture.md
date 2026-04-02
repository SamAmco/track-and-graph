---
title: Lua functions — app architecture and data model
description: Functions are derived data sources built in a visual node editor; covers FunctionGraph DTO, node types (FeatureNode, LuaScriptNode, OutputNode), configuration value types (8 types), serialization pipeline, and LuaEngine execution flow with 4 modes.
topics:
  - Functions: derived data sources using Lua; stored as Features in features_table
  - Visual node editor: FeatureNode → LuaScriptNode → OutputNode; data flows left to right
  - FunctionGraph DTO and JSON serialization via FunctionGraphSerializer (kotlinx.serialization)
  - Config value types: text, number, checkbox, enum, uint, duration (→ms), localtime (→ms), instant
  - LuaEngine: 4 modes — runLuaFunction, runLuaFunctionGenerator, runLuaGraph, runLuaCatalogue
  - Serialization pipeline: Node/Edge (VM) ↔ FunctionGraph (DTO) ↔ JSON (DB)
keywords: [lua, function, node-editor, FunctionGraph, LuaScriptNode, FeatureNode, OutputNode, configuration, serialization, LuaEngine, architecture, FunctionGraphSerializer, FunctionGraphBuilder, FunctionGraphDecoder]
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

### LuaEngine Interface
`app/data/.../lua/LuaEngine.kt`

Four modes:
1. `runLuaFunction()` - Parse script → metadata
2. `runLuaFunctionGenerator()` - Execute with data → `Sequence<DataPoint>`
3. `runLuaGraph()` - Execute graph script → render result
4. `runLuaCatalogue()` - Parse function library

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
