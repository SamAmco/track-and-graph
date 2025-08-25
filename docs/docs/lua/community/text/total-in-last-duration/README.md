# Total in Last Duration

Shows the total of a given input over a **rolling time window** extending backwards from the current moment. For example:
- "Total in last 24 hours" shows data from exactly 24 hours ago until now
- "Total in last 7 days" shows data from exactly 7 days ago until now  

If multiple data sources are given then the total of each data source is shown separately, as well as the overall total.

<div style="text-align: center;">
    <img src="image.jpg" alt="Total in last duration" style="width: 400px; height: auto;">
</div>

## Configuration

This script accepts the following configuration parameters:

```lua
-- Duration of data to total over prior to now (rolling window)
-- e.g. core.DURATION.DAY for total in last day
local duration = core.DURATION.DAY

-- Multiplier for the duration (e.g. 7 for last 7 days when duration is DAY)
local multiplier = 7

-- Text size (1=small, 2=medium, 3=large). If nil, uses smart defaults.
local text_size = nil
```

Available duration units:
- `core.DURATION.HOUR` - Hours
- `core.DURATION.DAY` - Days  
- `core.DURATION.WEEK` - Weeks
- `core.DURATION.MONTH` - Months

**Smart defaults for text size:**
- Single data source: Large text (size 3)
- Multiple data sources: Medium text (size 2) for better readability
- Override: Use specified size for all cases

[Install via deeplink](trackandgraph://lua_inject_url?url=https://raw.githubusercontent.com/SamAmco/track-and-graph/refs/heads/master/docs/docs/lua/community/text/total-in-last-duration/script.lua)

[Read the full script](./script.lua)

Author: [SamAmco](https://github.com/SamAmco)
