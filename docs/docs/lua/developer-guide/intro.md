# Lua Developer Guide

## Introduction

This guide is for people who are interested in writing their own Lua graph scripts. If you just want a reference for how to use the Lua graph feature in the app then check out [the tutorial here.](../../tutorial/graphs/lua-graphs/lua-graphs.md)

Throughout this guide I will assume some basic experience with programming. There are plenty of resources out there to help you get started if you need to build that foundation first.

If you don't know Lua, don't worry - it is a very simple and easy language to learn. If you already have some experience with programming and just want a quick guide to the syntax, then you can read the guide over at [learnxinyminutes.com/lua](https://learnxinyminutes.com/docs/lua/) in around 15-20 minutes. For more comprehensive learning, see the [official Lua documentation](https://www.lua.org/docs.html) (however please keep in mind the [limitations below](#scripting-limitations)). If you are comfortable with Lua you may want to just take a look at the examples in the [community scripts](../community/index.md). Each one has a link to the source code and they are generally quite short and readable.

## Setting up your environment

Before you can start writing any scripts you need to set up a development environment. You can use any text editor but I recommend finding an IDE that you like that supports Lua. Lua supports [LSP](https://en.wikipedia.org/wiki/Language_Server_Protocol) so there are [ many IDE's ](http://lua-users.org/wiki/LuaIntegratedDevelopmentEnvironments) out there that work well out of the box.

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

(This command is ultimately just getting all of the [ files and folders here ](https://github.com/SamAmco/track-and-graph/tree/master/lua/src) and copying them to your current directory.)

Now create a new Lua script file in the root of your workspace directory to write your code in e.g.

```bash
nvim my_script.lua
```

Your IDE should now recognise the Track & Graph API and give you code completion hints when you type something like: 

```lua
require("tng.core").
```

## Writing your first script

To get started let's write and save a super simple script: 

```lua
return require("tng.graph").text("Hello world!")
```

Now is a good time to figure out your development flow for testing the script on your device. If you have an easy way to copy and paste text between devices you can use the paste button in the Lua graph configuration. You can also save your workspace to a shared cloud directory or send the file to your device some other way and access it using the file button.

If you load that script into the Lua graph configuration you should see a simple text graph with the words "Hello world!" in it like this: 

!["Hello world text demo"](./hello_world_card.jpg){ width="400" }

### Understanding the script
Let's break down what is going on here. First we are using the `require` function to load the `tng.graph` module. This module contains all of the functions that you can use to create graphs. The `text` function creates a simple text graph. Let's take a look at the definition of that function:

```lua
--- Creates a text graph.
--- @since v5.1.0
--- @param params (string|number|TextGraphParams): Either the text to display or the parameters for the text graph.
--- @return table: A table representing the text graph.
M.text = function(params)
	if type(params) ~= "table" then
		return {
			type = M.GRAPH_TYPE.TEXT,
			text = params,
		}
	end
	return {
		type = M.GRAPH_TYPE.TEXT,
		text = params.text,
		size = params.size,
		align = params.align,
	}
end
```

Essentially this function takes a string, number, or a table of parameters and returns a table that represents the graph. The table contains a `type` field that tells the app what type of graph it is, and then any other parameters that are needed to create the graph. In this case we are just passing in a string, so the function creates a table with the `type` field set to `GRAPH_TYPE.TEXT` and the `text` field set to the string we passed in.

So our script ultimately returned a table that looks like this:

```lua
{
  type = "TEXT",
  text = "Hello world!",
}
```

Notice you could get the same result by writing your script like this:

```lua
local graph = require("tng.graph")

return {
  type = graph.GRAPH_TYPE.TEXT,
  text = "Hello world!",
}
```

Whenever Track & Graph needs to generate the view data for your Lua graph it will call the script and use the returned table to create the graph. For example your script is run to generate the preview in the Lua graph configuration screen, and whenever you open the group that contains your Lua graph.

## What graphs can I create?

Returning to the tng/graph.lua file notice the params object references the class name TextGraphParams. If you look further up the file you will find the definition of that class:

```lua
--- @since v5.1.0
--- @class TextGraphParams
--- @field text (string|number): The text to display.
--- @field size? integer: 1-3 The size of the text small, medium or large. Defaults to large.
--- @field align? string: start, centre, or end The alignment of the text. Defaults to centre.
```

Note that a parameter with a `?` after it means that it is optional. If you don't provide a value for that parameter, the function will use a default value. 

You can also see the other types of graphs you can return and the parameters they expect in this file. For further inspiration I recommend checking out the [community scripts](../community/index.md).

## Using data sources

A Lua graph script can actually return one of two things. It can return a table with the data in it as we've seen above, or it can return a function which accepts a table of named data sources. Track & Graph will then:

1. Execute the script to get it's return value (in this case a function).
2. Realise that the return value is a function and call it, passing in the data sources configured in the app.
3. Use the return value of that function to create the graph.

For example if you were to configure your Lua graph with a data source that contains at least one data point like this:

!["data_source"](./data_source_configuration.jpg){ width="300" }

Then you could use the following script to return the most recent data point from that data source:

```lua
return function(sources)
  return {
    type = require("tng.graph").GRAPH_TYPE.DATA_POINT,
    datapoint = sources["my_data_source"].dp(),
  }
end
```

Here `sources["my_data_source"]` is actually returning a `DataSource` object. Take a look at the `tng/core.lua` file to see the definition of this class and what other functions are available on it. Notice the definition of this function is empty:

```lua
--- Fetches the next data point from the data source.
--- Data points are iterated in reverse chronological order.
--- @since v5.1.0
--- @return DataPoint
function DataSource:dp() end
```

That's because the implementation of this data source is actually written in the kotlin code of the android app. [ You can see that here ](https://github.com/SamAmco/track-and-graph/blob/master/app/src/main/java/com/samco/trackandgraph/lua/apiimpl/LuaDataSourceProviderImpl.kt) if you are interested but it is not necessary to read the kotlin code.

## Scripting limitations

There are a few limitations to be aware of when writing Lua scripts: 

- Lua graph scripts are single files and they can not require or use symbols from other files except those that are provided by the Track & Graph API.
- The Lua engine under the hood is LuaK which is a Kotlin port of LuaJ based on Lua 5.2.x so language features should be compatible with that version of Lua.
- Some of the core api's have been stripped out for safety. For example:
    - `dofile`
    - `loadfile`
    - `pcall`
    - `xpcall`
    - `package`

In general your scripts should not try to access the OS to run commands, network requests, or access the file system.

## Script previews

If you have downloaded any of the community scripts you may have noticed that only the configuration parameters of the script are immediately visible. This is because the script preview will only show the code between the comments:

```lua
--- PREVIEW_START
```

and 

```lua
--- PREVIEW_END
```

You can use this to make the most important information about your script immediately visible like the name of the script and any configuration parameters e.g.

```lua
--- PREVIEW_START
-- Script: Text - Total This Period
-- Period of data to be displayed e.g. core.PERIOD.WEEK to show data for this week
local period = core.PERIOD.WEEK
--- PREVIEW_END
```

## Contributing to the community scripts

You already have all the tools you need to write and run your own scripts. If you create something you think is very useful and you want to share it with the community, you can create a PR into the Track & Graph repository. However community scripts will require a few more things before they can be committed to the repository. To give you an idea of what's required you can see the [ other community scripts and their accompanying files here](https://github.com/SamAmco/track-and-graph/tree/master/docs/docs/lua/community). In general you will need 4 things: 

1. A README.md file that describes the script and how to use it
2. A screenshot of the graph that the script generates
3. A test file, or multiple test files, that contain the test cases for the script
4. The script file that contains the code


## Writing tests

Writing tests is important not only to ensure your code works as expected (particularly around edge cases) but also to make sure that your script continues to work as the Track & Graph API evolves. If your script relies on some implementation detail or method call in the Track & Graph Lua API, and that function changes in a future version, then your script may be left broken without anyone noticing except disgruntled users.

### The testing framework

Track & Graph provides a testing framework for Lua scripts that runs your tests in the same Lua environment that's used in the production app. This gives greater assurances that if your tests pass, your script will work correctly in the actual app.

You can create as many test files as you like for your script as long as each one starts with the word test and ends with .lua. For example, if your script is called `my_script.lua`, you could create a test file called `test_my_script.lua` or just `test.lua`. The test files should be placed in the same directory as your script. A test file is a Lua module that returns a table of test cases. Each test case specifies:

1. Configuration overrides
2. A set of named mock data sources
3. Assertions to validate the script's output

Here's a simple example of a test file:

```lua
local M = {}
local test = require("test.core")

M.test_simple_case = {
  -- Configuration overrides for your script
  config = {
    period = "core.PERIOD.WEEK",
  },
  
  -- Function that returns mock data sources
  sources = function()
    return {
      my_source = {
        { timestamp = 1620000000, value = 10 },
        { timestamp = 1620086400, value = 20 },
      }
    }
  end,
  
  -- Function that validates the result
  assertions = function(result)
    test.assertEquals(30, result.text)
  end,
}

return M
```

### Creating test cases

Each test should be defined as a field in your module's table. The value should be a table with the following fields:

#### 1. `config` (table)

The `config` table contains values that will override variables in your script. For example, if your script defines `local period = core.PERIOD.MONTH`, your test can override it with `local period = core.PERIOD.WEEK`. Notice that the configuration overrides always use strings and not references to the values themselves. e.g. `"core.PERIOD.WEEK"` rather than `core.PERIOD.WEEK`. That's because the testing framework actually just uses a regex to overwrite the first declaration matching the config name in your script before executing it.

#### 2. `sources` (function)

The `sources` function should return a table of mock data sources that will be passed to your script. Each source is a table of data points with `timestamp` and `value` fields:

```lua
sources = function()
  local core = require("tng.core")
  local now = core.time().timestamp
  
  return {
    -- First data source named "steps"
    steps = {
      { timestamp = now - 86400, value = 5000 },
      { timestamp = now - 172800, value = 7500 },
    },
    -- Second data source named "calories"
    calories = {
      { timestamp = now - 86400, value = 300 },
      { timestamp = now - 172800, value = 450 },
    }
  }
end
```

#### 3. `assertions` (function)

The `assertions` function receives the result of your script and should validate that it's correct using the test utility functions:

```lua
assertions = function(result)
  local test = require("test.core")
  test.assertEquals(12500, result.text)
  test.assert(result.type == require("tng.graph").GRAPH_TYPE.TEXT)
end
```

Currently there are only two assertion functions available: `assertEquals` and `assert`. The assertEquals function will convert the left and right side to strings and compare string equality. 

It is also valid to use the error function to throw an error if the test fails. For example:

```lua
assertions = function(result)
  if result.text ~= "Hello world!" then
    error("Expected 'Hello world!', got '" .. result.text .. "'")
  end
end
```

### Running tests

Since the tests run in the actual Track & Graph Lua engine, you need an Android development environment to run them. You should be ok with just the CLI tools, or you can install Android Studio if you prefer. [ You can download those things here. ](https://developer.android.com/studio)

You will also need Track & Graph cloned locally:

```bash
git clone git@github.com:SamAmco/track-and-graph.git
cd track-and-graph
```

The tests are run using the [ CommunityTestRunner.kt ](https://github.com/SamAmco/track-and-graph/blob/master/app/src/test/java/com/samco/trackandgraph/lua/CommunityTestRunner.kt) in the Android project. You can run them using the gradlew file inside the repo with:

```bash
./gradlew :app:testDebugUnitTest --tests "com.samco.trackandgraph.lua.CommunityTestRunner"
```

### Debugging tests

Debugging scripts and tests can be a bit tricky since you will generally have to work without a breakpoint debugger in your Lua code. 

You can use the `error` and `print` functions to help narrow down issues. The `print` function takes any set of arguments and tries to convert them to strings before printing them to the test output. The following are all valid uses of `print`:

```lua
print("Hello world!") -- prints "Hello world!"
print(123) -- prints "123"
print({ key = "value" }) -- prints e.g. "table: 1ddd3478"
print("Hello ", "world!") -- prints "Hello world!"
print("Hello ", 123) -- prints "Hello 123"
```

Notice that only primitives like strings and numbers will print readably. Tables will not print their contents but instead their memory address, so you need to access their fields to print them effectively.

You can use `print` and `error` anywhere in your script or test to narrow down issues, but please make sure to remove any `print` statements before you submit your PR.
