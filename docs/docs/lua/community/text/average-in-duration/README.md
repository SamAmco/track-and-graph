# Average in Duration

Shows the average of all data points from all data sources combined over a specified duration prior to now. This is a moving average that calculates the mean of all values within the duration window, not bounded by calendar periods. For example, the average over the last 7 days, or the average over the last 30 days from the current moment.

<div style="text-align: center;">
    <img src="image.png" alt="Average in Duration" style="width: 400px; height: auto;">
</div>

## Configuration

This script accepts the following configuration parameters:

```lua
-- Duration of data to average over prior to now (moving average)
-- e.g. core.DURATION.DAY * 7 for average over last 7 days
local duration = core.DURATION.DAY * 7
```

## Examples

- `core.DURATION.DAY * 7` - Average over the last 7 days
- `core.DURATION.DAY * 30` - Average over the last 30 days  
- `core.DURATION.HOUR * 24` - Average over the last 24 hours
- `core.DURATION.DAY * 1` - Average over the last 1 day

## Key Features

- **Moving Average**: Calculates average from now backwards by the specified duration, not calendar-based periods
- **Multi-Source**: Combines all data points from all configured data sources into a single average
- **Precision**: Results are rounded to 2 decimal places for clean display

[Install via deeplink](trackandgraph://lua_inject_url?url=https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/docs/docs/lua/community/text/average-in-duration/script.lua)

[Read the full script](./script.lua)

Author: [SamAmco](https://github.com/SamAmco)
