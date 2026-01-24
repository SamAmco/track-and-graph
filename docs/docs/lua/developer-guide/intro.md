# Getting Started

Track & Graph exposes Lua interfaces for two purposes:

1. **Function Scripts** - Data transformers for the node editor that process streams of data points
2. **Graph Scripts** - Custom visualizations that return graph data (line graphs, pie charts, text displays, etc.)

If you don't know Lua, don't worry - it is a very simple and easy language to learn. If you already have some experience with programming and just want a quick guide to the syntax, then you can read the guide over at [learnxinyminutes.com/lua](https://learnxinyminutes.com/docs/lua/) in around 15-20 minutes. For more comprehensive learning, see the [official Lua documentation](https://www.lua.org/docs.html) (however please keep in mind the [limitations below](#scripting-limitations)). If you are comfortable with Lua you may want to just take a look at the examples in the [community scripts](../community/index.md). Each one has a link to the source code and they are generally quite short and readable.

## Getting Started

- [Writing Custom Functions](writing-custom-functions.md) - Create data transformers for the node editor
- [Writing Graph Scripts](writing-graph-scripts.md) - Create custom visualizations

## Setting Up Your Environment

Before you can start writing any scripts you need to set up a development environment. You can use any text editor but I recommend finding an IDE that you like that supports Lua. Lua supports [LSP](https://en.wikipedia.org/wiki/Language_Server_Protocol) so there are [many IDE's](http://lua-users.org/wiki/LuaIntegratedDevelopmentEnvironments) out there that work well out of the box.

Once you have an editor you need to create a new directory somewhere to work in e.g.

```bash
mkdir my_workspace
cd my_workspace
```

Then clone the API source code from the repository:
```bash
curl -L https://github.com/SamAmco/track-and-graph/archive/refs/heads/master.zip -o master.zip \
&& unzip master.zip "track-and-graph-master/lua/src/*" \
&& mv track-and-graph-master/lua/src/* . \
&& rm -rf track-and-graph-master master.zip
```

(This command is ultimately just getting all of the [files and folders here](https://github.com/SamAmco/track-and-graph/tree/master/lua/src) and copying them to your current directory.)

Now create a new Lua script file in the root of your workspace directory to write your code in e.g.

```bash
nvim my_script.lua
```

Your IDE should now recognise the Track & Graph API and give you code completion hints when you type something like:

```lua
require("tng.core").
```

## Scripting Limitations

There are a few limitations to be aware of when writing Lua scripts:

- Scripts are single files and cannot require or use symbols from other files except those provided by the Track & Graph API.
- The Lua engine under the hood is LuaK which is a Kotlin port of LuaJ based on Lua 5.2.x so language features should be compatible with that version of Lua.
- Some of the core APIs have been stripped out for safety. For example:
    - `dofile`
    - `loadfile`
    - `package`

In general your scripts should not try to access the OS to run commands, network requests, or access the file system.

## API Reference

The API source files are documented with comments. Browse them on GitHub:

- **[lua/src/tng/](https://github.com/SamAmco/track-and-graph/tree/master/lua/src/tng)** - All API modules (`core`, `config`, `graph`, etc.)

## Examples

The best way to learn is by reading existing scripts:

- **[Community Functions](https://github.com/SamAmco/track-and-graph/tree/master/lua/src/community/functions)** - Filters, transforms, aggregations
- **[Community Graph Scripts](https://github.com/SamAmco/track-and-graph/tree/master/lua/src/community/graphs)** - Line graphs, pie charts, text displays

You can also browse the [Community Scripts](../community/index.md) page in the app to install and inspect scripts.
