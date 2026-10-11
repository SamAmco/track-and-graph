---
name: write-track-and-graph-function
description: Write or revise a pasteable custom Lua function for the Track & Graph Android app. Use for data-stream transformations in the Function node editor; do not use for Lua graph scripts or community-catalog contributions.
---

# Write a Track & Graph custom function

Create a single-file Lua function that the user can paste into a **Custom Lua
Script** node in Track & Graph. Optimize for an individual editing and using
their own script, not for publication in the community catalog.

## Establish the behavior

Before writing code, determine the following from the request or make the
simplest reasonable assumptions:

- What each input stream represents and how many inputs are needed.
- Which settings should be editable in the node UI.
- Which fields must be preserved or changed: `timestamp`, `value`, `label`,
  `note`, and `offset`.
- For time-based behavior, whether the user means a fixed amount of elapsed
  time (such as exactly 24 hours) or a calendar-relative amount (such as the
  previous local day, which can be 23 or 25 hours across daylight-saving
  changes).
- Which point should supply the timestamp when several points contribute to an
  output.

Ask a concise question only when different answers would materially change the
result. State important assumptions in the response.

## Target the user's app version

Ask the user which version of Track & Graph they use; it is shown in the app's
About screen. APIs can differ between releases, so do not assume that the
current `master` branch describes their installed version.

Use the matching release tag from the
[Track & Graph tags](https://github.com/SamAmco/track-and-graph/tags), normally
named `v<app-version>` or `rc-v<app-version>`. Inspect the `lua/src/tng/` module
source and its matching `.apispec.lua` file at that tag before using an API.
For example, replace `<TAG>` and `<MODULE>` in:

```text
https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/tags/<TAG>/lua/src/tng/<MODULE>.lua
https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/tags/<TAG>/lua/src/tng/<MODULE>.apispec.lua
```

Each API spec maps an exported symbol to the API level in which it was
introduced. An app release supports the symbols present in its tagged module
source; the highest integer across its bundled API specs is that release's
maximum API level. A function's `version` major declares the highest API level
it requires for catalog compatibility, but adding `version` does not make an
unavailable API work in an older app.

If the user cannot identify their version, prefer long-established APIs and
say that the script targets the current release. Do not use a newer API merely
because it appears on `master` when the requested app version predates it.

## Use the custom-script shape

The absolute minimum is a script that returns a generator function:

```lua
return function(source)
	return function()
		local data_point = source.dp()
		if not data_point then
			return nil
		end

		data_point.value = math.abs(data_point.value)
		return data_point
	end
end
```

The outer function is called once to create an iterator. The iterator is then
called repeatedly. Each call must return one data-point table or `nil` when the
output is exhausted.

For a useful title, description, multiple inputs, or settings, return a metadata
table instead:

```lua
local number = require("tng.config").number

return {
	title = "Multiply values",
	description = [[Multiplies each value while preserving the other fields.]],
	inputCount = 1,
	config = {
		number {
			id = "multiplier",
			name = "Multiplier",
			default = 1,
		},
	},

	generator = function(source, config)
		local multiplier = config.multiplier

		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			data_point.value = data_point.value * multiplier
			return data_point
		end
	end,
}
```

Use plain strings in the user's language for `title`, `description`, config
names, and enum option names. A custom function does not need translations,
shared translation keys, categories, an ID, or catalog tests.

### Default to an editable script

Omit `version` by default. In Track & Graph, any non-nil `version` marks the
node as catalog-style and hides its clipboard, file, and script-editing
controls. Omitting it keeps the script easy for the user to inspect and repair
inside the app.

Some users may prefer a configuration-only node with its implementation
hidden. Explain the tradeoff and offer `version` as an explicit option when
appropriate, ideally after the script is working. Do not add it silently. If
the user opts in, set its semantic-version major to the highest API level the
script uses and warn that later code changes will require recreating or
otherwise replacing the node from an editable copy.

Do not copy `_`-prefixed shared translation keys from community functions.
Those keys are hydrated by catalog context and otherwise appear to the user as
raw strings such as `_period`.

## Runtime contract

Data sources are lazy and are traversed in **reverse chronological order**:
newest point first.

For one input (the default), the generator receives that data source directly:

```lua
generator = function(source, config)
```

For two or more inputs, set `inputCount` and receive a one-indexed table:

```lua
return {
	inputCount = 2,
	generator = function(sources, config)
		local left = sources[1]
		local right = sources[2]
		-- Return an iterator here.
	end,
}
```

Do not pair multiple streams merely by calling `dp()` once on each unless
position-based pairing is truly intended. Independently recorded streams often
have different timestamps; define the desired timestamp matching, tolerance,
or pass-through behavior.

`source.dp()` consumes the next point and is the fundamental streaming
operation. Before using batch, cutoff, or other data-source operations, inspect
the `DataSource` section of the version-appropriate `tng.core` source and API
spec. Do not reproduce an API from memory: its availability or contract may
differ in the user's version.

Prefer streaming only the data points you need to calculate the next output
data point. Keep per-run state inside the generator, not at script/module
scope.

Filtering iterators must keep consuming until they find a point to return or
reach the end:

```lua
return function()
	while true do
		local data_point = source.dp()
		if not data_point then
			return nil
		end
		if data_point.value >= threshold then
			return data_point
		end
	end
end
```

If an algorithm is naturally chronological (oldest to newest), account for the
input order explicitly. It may need to buffer the full input using an operation
available in the user's version, calculate from the end of that array toward
the beginning, and still emit results newest first.

## Data points and time

An input data point contains:

| Field | Type | Meaning |
| --- | --- | --- |
| `timestamp` | number | Unix epoch milliseconds; required on output |
| `offset` | number | UTC offset in **seconds** |
| `value` | number | Tracked numeric value |
| `label` | string | Label, normally an empty string when absent |
| `note` | string | Note, normally an empty string when absent |

Mutate and return the input table when only changing a field; this naturally
preserves the other fields. When constructing a new point, supply a valid
`timestamp`, and preserve or deliberately choose `offset`, `label`, and `note`.

For local calendar calculations, use the full point:

```lua
local core = require("tng.core")
local date = core.date(data_point)
```

Passing only `data_point.timestamp` loses the point's offset and interprets the
instant in UTC. In the Track & Graph API, `core.DURATION` represents an exact
number of elapsed milliseconds, like Java Time's `Duration`. `core.PERIOD`
represents calendar-relative days, weeks, months, or years, like Java Time's
`Period`; shifting by one local day can therefore span 23, 24, or 25 elapsed
hours. Use the version-appropriate `tng.core` documentation to confirm the
available constants and functions.

If output values represent elapsed time, remind the user to enable **Duration**
on the function's Output node; that setting is outside the Lua script.

## Configuration

Inspect the version-appropriate `tng.config` source and API spec before choosing
a configuration type. Do not assume that every type on `master` exists in the
user's installed release. A typical numeric configuration looks like:

```lua
local number = require("tng.config").number

number {
	id = "threshold",
	name = "Threshold",
	default = 0,
}
```

Confirm units in that tagged API source. For example, releases that provide
duration, local-time, and instant inputs pass them to the generator as duration
milliseconds, milliseconds since midnight, and epoch milliseconds
respectively—but the tagged source remains authoritative.

For a custom enum, always provide stable IDs and plain display names:

```lua
enum {
	id = "operation",
	name = "Operation",
	options = {
		{ id = "add", name = "Add" },
		{ id = "subtract", name = "Subtract" },
	},
	default = "add",
}
```

Do not use a bare string array for enum options; that form is intended for
catalog translation keys.

Declare defaults in the config specification. If defensive fallback logic is
also needed, do not accidentally replace a valid `false` value:

```lua
local enabled = true
if config and config.enabled ~= nil then
	enabled = config.enabled
end
```

`config and config.enabled or true` is wrong here because explicit `false`
would become `true`: in Lua, `false or true` evaluates to `true`. The explicit
nil check above does preserve `false`, because `false ~= nil` is true and the
assignment therefore sets `enabled` to `false`.

## Available APIs and environment

The app embeds a Lua 5.2-compatible LuaK/LuaJ environment. Scripts are single
files and cannot access the network, operating system, or filesystem. They may
load only the modules supplied by Track & Graph.

Use the release-tag lookup described above as the primary API reference. These
`master` links describe current development and are only the fallback when the
user is on the current release or their version is unknown:

- [`tng.core`](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/lua/src/tng/core.lua) — data sources, dates, time shifting, durations, periods, and colors.
- [`tng.config`](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/lua/src/tng/config.lua) — configuration input constructors.
- [`tng.aggregation`](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/lua/src/tng/aggregation.lua) — aggregate and sliding-window helpers.
- [`tng.random`](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/lua/src/tng/random.lua) — stable seeded random values.
- [Current community function examples](https://github.com/SamAmco/track-and-graph/tree/master/lua/src/community/functions) — useful algorithm examples, but strip catalog-only metadata, translations, and shared keys when adapting them.
- [Full custom-function guide](https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/docs/docs/lua/developer-guide/writing-custom-functions.md) — additional examples and app instructions.

Do not use `tng.graph`; graph scripts have a different contract and output
shape.

## Validate the result

Before delivering the script, reason through at least:

- An empty source.
- A single data point.
- Typical input with exact expected outputs.
- Boundary values and missing/empty labels or notes when relevant.
- Default and non-default configuration, including explicit `false`.
- Reverse-chronological ordering and the timestamp/offset of generated points.
- Exhaustion or mismatch behavior for every input in a multi-input function.

Check that every iterator path either returns a data point, continues consuming
input, or terminates with `nil`; avoid loops that can make no progress. Preserve
fields the user did not ask to change. Do not claim the script was tested in
Track & Graph unless it actually was.

## Deliver the function

Make the pasteable Lua script the primary result. Briefly explain its inputs,
configuration, output, ordering assumptions, and important edge cases. Avoid
community-catalog packaging, repository paths, translation work, and test
harness files unless the user explicitly asks to publish the function.

Tell the user how to install it:

1. Create or edit a Function in Track & Graph.
2. Long-press empty space in the node editor and choose **Custom Lua Script**.
3. Paste the script or load it from a file.
4. Connect the expected input and outputs nodes.
5. Adjust its configuration controls and save the Function.
