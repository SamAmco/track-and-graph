# Lua Graphs and Statistics

Lua graphs are more powerful and flexible than the other graph types. A lua graph is a script written in the lua programming language that returns a data structure that Track & Graph can render. The lua graph can be configured with named data sources that the script can read from when it is executed. For example we could re-create the Last Tracked statistic by adding a single data source and then using this script:

```lua
return function(sources)
  local _, first = next(sources)
  if not first then
    return nil
  end
  return require("tng.graph").data_point(first.dp())
end
```

Don't worry if that seems a bit complicated, there are many scripts already written for you over at [the community script hub](../../../lua/community/index.md). 

If you would like to learn how to write your own scripts check out [the developer guide](../../../lua/developer-guide/intro.md).

## Community Scripts

You can get directly there by tapping the "GitHub" button in the app. Once there if you find a script you want to use you can tap the "Install via deeplink" button and you will be returned to the app with the script already installed: 

!["lua_graph_flow"](./video.gif){ width="300" }

## Configuration

Most of the scripts have some configuration parameters near the top. Currently you will need to modify these manually to configure the script. Below is a guide for working with the different data types you'll encounter in Lua scripts.

### Basic Data Types

When configuring Lua scripts, you'll need to understand how to provide different types of values:

#### Numbers
```lua
local number = 42           -- integers
local decimal = 3.14        -- floating point numbers
local negative = -10        -- negative numbers
```

#### Strings
```lua
local string = "Hello"      -- double quotes
local string2 = 'World'     -- single quotes work too
```

#### Booleans
```lua
local truth = true          -- true value
local falsehood = false     -- false value
```

#### Tables/Lists
```lua
local emptyList = {}                           -- empty list
local numberList = {1, 2, 3, 4}                -- list of numbers
local stringList = {"small", "medium"}         -- list of strings
local colorList = {"#FF0000", core.COLOR.BLUE} -- list of colors
```

#### nil
```lua
local nothing = nil         -- represents the absence of a value
```

#### Comments
```lua
-- Anything after -- is a comment and will be ignored by the interpreter
--[[
  This is a multi-line comment
  It can span multiple lines
]]
```

### Durations 

A duration is just a number of milliseconds. There are constants defined for you for convenience:

```lua
core.DURATION.SECOND
core.DURATION.MINUTE
core.DURATION.HOUR
core.DURATION.DAY
core.DURATION.WEEK
```

It is also valid to perform arithmetic on a duration e.g.

```lua
(core.DURATION.MINUTE * 5 + 8 -2) / 3
```

### Periods

A period is slightly different to a duration because it can have a variable number of milliseconds. For example a month does not have a fixed number of days, and a day may have a different number of hours due to daylight savings. The period constants defined are:

```lua
core.PERIOD.DAY
core.PERIOD.WEEK
core.PERIOD.MONTH
core.PERIOD.YEAR
```

It is not valid to use arithmetic operations on a period. For example `core.PERIOD.MONTH * 5` is not valid. For this reason a script that uses a period in the config will often accept a multiplier for that period as a separate parameter. So if you want to represent something like 5 months you might provide the following configuration.

```lua
local period = core.PERIOD.MONTH
local period_multiplier = 5
```

### Colors

For colors you can use any hex value such as `#FF0000` or `#00FF00` like so:

```lua
local color = "#FF0000"
```

You can also use the following named colors from Track & Graph's default colour palette:

| Constant | Hex Value | Color Sample |
|----------|-----------|--------------|
| `core.COLOR.RED_DARK` | `#A50026` | <span style="display:inline-block;width:60px;height:30px;background-color:#A50026;border:1px solid #ccc"></span> |
| `core.COLOR.RED` | `#D73027` | <span style="display:inline-block;width:60px;height:30px;background-color:#D73027;border:1px solid #ccc"></span> |
| `core.COLOR.ORANGE_DARK` | `#F46D43` | <span style="display:inline-block;width:60px;height:30px;background-color:#F46D43;border:1px solid #ccc"></span> |
| `core.COLOR.ORANGE` | `#FDAE61` | <span style="display:inline-block;width:60px;height:30px;background-color:#FDAE61;border:1px solid #ccc"></span> |
| `core.COLOR.YELLOW` | `#FEE090` | <span style="display:inline-block;width:60px;height:30px;background-color:#FEE090;border:1px solid #ccc"></span> |
| `core.COLOR.BLUE_LIGHT` | `#E0F3F8` | <span style="display:inline-block;width:60px;height:30px;background-color:#E0F3F8;border:1px solid #ccc"></span> |
| `core.COLOR.BLUE_SKY` | `#ABD9E9` | <span style="display:inline-block;width:60px;height:30px;background-color:#ABD9E9;border:1px solid #ccc"></span> |
| `core.COLOR.BLUE` | `#74ADD1` | <span style="display:inline-block;width:60px;height:30px;background-color:#74ADD1;border:1px solid #ccc"></span> |
| `core.COLOR.BLUE_DARK` | `#4575B4` | <span style="display:inline-block;width:60px;height:30px;background-color:#4575B4;border:1px solid #ccc"></span> |
| `core.COLOR.BLUE_NAVY` | `#313695` | <span style="display:inline-block;width:60px;height:30px;background-color:#313695;border:1px solid #ccc"></span> |
| `core.COLOR.GREEN_LIGHT` | `#54D931` | <span style="display:inline-block;width:60px;height:30px;background-color:#54D931;border:1px solid #ccc"></span> |
| `core.COLOR.GREEN_DARK` | `#1B8200` | <span style="display:inline-block;width:60px;height:30px;background-color:#1B8200;border:1px solid #ccc"></span> |

## Disabling Lua

It should be hard to write a script that crashes the app, but if you do you can start the app without the lua engine enabled by long pressing on the app launcher and selecting "Launch Lua disabled". You will still see the cards for the Lua graphs you created so you can delete or edit them before restarting the app again.
