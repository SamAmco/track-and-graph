# Total this period

Shows the total of a given input over a specified period of time **from the beginning of that period**. For example:
- "Total this week" shows data from Monday 00:00 to Sunday 23:59 of the current week
- "Total this month" shows data from the 1st day 00:00 to the last day 23:59 of the current month
- "Total this day" shows data from 00:00 to 23:59 of today

If multiple data sources are given then the total of each data source is shown separately, as well as the overall total.

<div style="text-align: center;">
    <img src="image.jpg" alt="Total this period" style="width: 400px; height: auto;">
</div>

## Configuration

This script accepts the following configuration parameters:

```lua
-- Period of data to be displayed e.g. core.PERIOD.WEEK to show data for this week
local period = core.PERIOD.WEEK

-- Multiplier for the period (e.g. 2 for this 2 weeks when period is WEEK)
local multiplier = 1

-- Text size (1=small, 2=medium, 3=large). If nil, uses smart defaults.
local text_size = nil
```

Available period units:
- `core.PERIOD.DAY` - Days
- `core.PERIOD.WEEK` - Weeks  
- `core.PERIOD.MONTH` - Months
- `core.PERIOD.YEAR` - Years

The multiplier allows you to extend periods beyond single units. For example:
- `period = core.PERIOD.WEEK, multiplier = 2` shows "this 2 weeks"
- `period = core.PERIOD.MONTH, multiplier = 3` shows "this 3 months"

**Smart defaults for text size:**
- Single data source: Large text (size 3)
- Multiple data sources: Medium text (size 2) for better readability
- Override: Use specified size for all cases

[Install via deeplink](trackandgraph://lua_inject_url?url=https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/docs/docs/lua/community/text/total-this-period/script.lua)

[Read the full script](./script.lua)

Author: [SamAmco](https://github.com/SamAmco)
