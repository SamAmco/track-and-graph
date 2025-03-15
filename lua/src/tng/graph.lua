local core = require("tng.core")
local M = {}

--- Graph types enum
--- @enum GRAPH_TYPE
M.GRAPH_TYPE = {
	DATA_POINT = "DATA_POINT",
	TEXT = "TEXT",
	PIE_CHART = "PIE_CHART",
	TIME_BARCHART = "TIME_BARCHART",
	LINE_GRAPH = "LINE_GRAPH",
}

--- @class datapoint_graphtype_data
--- @field type GRAPH_TYPE.DATA_POINT: The type of the graph.
--- @field datapoint datapoint: The data point to display.
--- @field isduration boolean: Whether the data point is a duration.

--- @class text_graphtype_data (you can also just return a string or number for this graph type)
--- @field type GRAPH_TYPE.TEXT
--- @field text string: The text to display.
--- @field size? integer: 1-3 The size of the text small, medium or large. Defaults to large.
--- @field align? string: start, centre, or end The alignment of the text. Defaults to centre.

--- @class piechart_graphtype_data
--- @field type GRAPH_TYPE.PIE_CHART
--- @field segments piechart_segment[]: A table of piechart_segment items.

--- @class linegraph_graphtype_data
--- @field type GRAPH_TYPE.LINE_GRAPH
--- @field lines line[]: A table of line items.
--- @field duration_based_range? boolean: Whether the range is based on duration.
--- @field range_bounds? range_bounds: The range of the y-axis.

--- @class time_barchart_graphtype_data
--- @field type GRAPH_TYPE.TIME_BARCHART
--- @field bars time_bar[]: A table of time_bar sorted in reverse chronological order. Each bar is bar_duration or bar_period in length and ends that amount of time before the previous bar.
--- @field end_time (timestamp|integer): The end time of the graph. Either a timestamp or an integer representing the Unix epoch millisecond timestamp.
--- @field bar_duration? integer: The duration of each bar in milliseconds. You must provide either this or the bar_period.
--- @field bar_period? string: The period of each bar e.g. PERIOD.DAY. You must provide either this or the bar_duration.
--- @field bar_period_multiple? integer: The number of the given bar_period units one bar represents. Defaults to 1.
--- @field duration_based_range? boolean: If true, the y-axis represents time in milliseconds.
--- @field y_max? integer: The top extent of the y-axis. If not provided, the maximum value of the bars will be used.

--- @class piechart_segment
--- @field label string: The label of the segment.
--- @field value number: The value of the segment. This does not need to be normalised in any way.
--- @field color? color: The color of the segment. If not provided, a color will be chosen from the default palette.

--- @alias time_bar (number|time_barchart_bar_segment|time_barchart_bar_segment[]): A table of bar_segment items.
--- @alias time_barchart_bar_segment (number|time_barchart_segment)

--- @class time_barchart_segment
--- @field value number: The value of the bar segment.
--- @field label? string: The label of the bar segment (shown in the legend).
--- @field color? color: The color of the bar segment. If not provided, a color will be chosen from the default palette.

---@class range_bounds
---@field min number: The minimum value of the range.
---@field max number: The maximum value of the range.

--- @class line
--- @field line_points line_point[]: A table of line_point items. Line points should be sorted in reverse chronological order by timestamp.
--- @field line_color? color: The color of the line.
--- @field point_style? line_point_style: The style of the points on the line.
--- @field label? string: The label of the line. Will be displayed in the legend.

--- @enum line_point_style
M.LINE_POINT_STYLE = {
	NONE = "none",
	CIRCLE = "circle",
	CIRCLE_VALUE = "circle_value",
}

--- @class line_point
--- @field timestamp integer: The timestamp of the line point.
--- @field value number: The Y value of the line point.

--- @class cutoff_params
--- @field period string?: The period for the cutoff.
--- @field period_multiplier? integer: The multiplier for the period.
--- @field from_now boolean: Whether the cutoff is from the current time.
--- @field timestamp? integer: The starting timestamp for the cutoff.

--- @param params cutoff_params: The parameters for calculating the cutoff.
--- @return integer|nil: The cutoff timestamp obtained by subtracting the given period from the specified end time (as a Unix epoch millisecond) or the current time, or nil if the period is not provided.
M.get_cutoff = function(params)
	if not params.period then
		return nil
	end

	local multiplier = params.period_multiplier or 1

	if params.from_now then
		return core.shift(core.time(), params.period, -multiplier).timestamp
	end

	local start_time = params.timestamp or core.time()
	return core.shift(start_time, params.period, -multiplier).timestamp
end

--- Applies a moving average to a series of data points in place.
---
--- This function takes a table of data points (each must have at a minimum a timestamp and a value) and applies a moving average over a specified duration.
--- It updates each datapoint's value in the table to be the average of values within the specified duration preceding it.
---
--- @param datapoints datapoint[]: The data points to apply the moving average to.
--- @param averaging_duration integer: The duration over which to average the data points, in milliseconds.
M.apply_moving_averaging = function(datapoints, averaging_duration)
	if averaging_duration == nil then
		return datapoints
	end

	if type(averaging_duration) ~= "number" or averaging_duration % 1 ~= 0 then
		error("averaging_duration must be an integer")
	end

	if #datapoints < 2 then
		return datapoints
	end

	local current_window = {}
	-- the exclusive end of the window
	local window_end = 1
	local sum = 0
	local count = 0

	for i = 1, #datapoints, 1 do
		if i > 1 then
			local removed = table.remove(current_window, 1)
			sum = sum - removed
			count = count - 1
		end

		-- Add the new data point to the window if it's not already in the window
		if window_end < (i + 1) then
			window_end = i + 1
			table.insert(current_window, datapoints[i].value)
			sum = sum + datapoints[i].value
			count = count + 1
		end

		-- Add any other data points that are within the duration
		while
			window_end <= #datapoints
			and datapoints[i].timestamp - datapoints[window_end].timestamp < averaging_duration
		do
			table.insert(current_window, datapoints[window_end].value)
			sum = sum + datapoints[window_end].value
			count = count + 1
			window_end = window_end + 1
		end

		-- Calculate the average
		datapoints[i].value = sum / count
	end
end


--- Calculates the end of a given period for a specific timestamp.
---
--- This function determines the end of a specified period (e.g., day, week, month, year) for a given timestamp.
--- It adjusts the date to the start of the next period and sets the time to midnight.
---
--- @param period string: The period to calculate the end for (e.g., core.PERIOD.DAY, core.PERIOD.WEEK).
--- @param timestamp integer: The timestamp to calculate the end of the period for.
--- @param zone_override? string: An optional timezone override. If not provided, the default timezone is used.
--- @return date: A date representing the end of the specified period with the time set to midnight.
M.get_end_of_period = function(period, timestamp, zone_override)
	local zone = zone_override or core.date().zone
	local date = core.date(timestamp)
	if period == core.PERIOD.DAY then
		date = core.date(core.shift(date, core.PERIOD.DAY, 1))
	elseif period == core.PERIOD.WEEK then
		date = core.date(core.shift(date, core.PERIOD.WEEK, 1))
		date.wday = 1
	elseif period == core.PERIOD.MONTH then
		date = core.date(core.shift(date, core.PERIOD.MONTH, 1))
		date.day = 1
		date.wday = nil
		date.yday = nil
	elseif period == core.PERIOD.YEAR then
		date = core.date(core.shift(date, core.PERIOD.YEAR, 1))
		date.month = 1
		date.day = 1
	end
	date.hour = 0
	date.min = 0
	date.sec = 0
	date.zone = zone
	return date
end

--- Calculates the total value of data points for each period starting with the period containing the first data point.
---
--- This function takes a table of data points and calculates the total value for each period defined by the given period and multiplier.
--- It returns a new table of data points, each representing the total value for a specific period.
---
--- @param datapoints datapoint[]: The data points to calculate totals for.
--- @param period string: The period to calculate totals over (e.g., core.PERIOD.DAY).
--- @param multiplier integer: The multiplier for the period. Allowing for periods like 2 days, 3 weeks, etc.
--- @param zone_override? string: The timezone to use for the period. If not provided, the default timezone is used.
--- @return datapoint[]: A table of new data points, each representing the total value for a specific period.
M.calculate_period_totals = function(datapoints, period, multiplier, zone_override)
	if not period then
		return datapoints
	end

	local last_date = M.get_end_of_period(period, datapoints[1].timestamp, zone_override)
	local period_totals = {}
	local period_end = core.time(last_date)
	local mult = -(multiplier or 1)
	local period_start = core.shift(period_end, period, mult)
	local sum = 0

	for i = 1, #datapoints do
		while datapoints[i].timestamp < period_start.timestamp do
			table.insert(period_totals, {
				timestamp = period_end.timestamp - 1,
				offset = period_end.offset,
				zone = period_end.zone,
				value = sum,
			})
			period_end = period_start
			period_start = core.shift(period_start, period, mult)
			sum = 0
		end
		sum = sum + datapoints[i].value
	end

	-- Add the last period total
	table.insert(period_totals, {
		timestamp = period_end.timestamp - 1,
		offset = period_end.offset,
		zone = period_end.zone,
		value = sum,
	})

	return period_totals
end

return M
