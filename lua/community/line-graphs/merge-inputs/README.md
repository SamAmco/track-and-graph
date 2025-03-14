# Merge Inputs

Plots all provided data sources in a single line where all datapoints are sorted by their timestamp.

<div style="text-align: center;">
    <img src="merge-inputs.jpg" alt="Merge Inputs" style="width: 400px; height: auto;">
</div>

## Configuration

This script accepts the following configuration parameters:

```lua
-- Optional period of data to be displayed e.g. core.PERIOD.WEEK to only show 1 week of data
local period = nil
-- Optional integer value used with period e.g. 5
local period_multiplier = 8
-- If from_now is false the end of the graph will be the last datapoint, otherwise it's the current date/time
local from_now = false
-- Optional color, e.g. "#FF00FF" or core.COLOR.BLUE_SKY
local line_color = nil
-- Optional point style e.g. graph.LINE_POINT_STYLE.CIRCLE
local line_point_style = nil
-- Optional string label for the line in the legend, e.g. "Data"
local line_label = nil
-- Optional integer value used to average data points over a certain duration e.g. core.DURATION.DAY * 30 for a 30 day moving average
local averaging_duration = nil
-- Optional totalling period used to calculate 'plot totals' e.g. core.PERIOD.WEEK
local totalling_period = nil
-- Optional totalling period multiplier used to calculate 'plot totals' e.g. 2
local totalling_period_multiplier = nil
```

[Install via deeplink](trackandgraph://lua_inject_url?url=https://www.github.com/SamAmco/track-and-graph/tree/master/lua/community/line-graphs/merge-inputs/script.lua)

[Read the full script](./script.lua)
