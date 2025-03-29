local core = require("tng.core")
local M = {}

--- Graph types enum
--- @since v5.1.0
--- @enum GRAPH_TYPE
M.GRAPH_TYPE = {
	DATA_POINT = "DATA_POINT",
	TEXT = "TEXT",
	PIE_CHART = "PIE_CHART",
	TIME_BARCHART = "TIME_BARCHART",
	LINE_GRAPH = "LINE_GRAPH",
}

--- @since v5.1.0
--- @class datapoint_graphtype_data
--- @field type GRAPH_TYPE.DATA_POINT: The type of the graph.
--- @field datapoint datapoint: The data point to display.
--- @field isduration boolean: Whether the data point is a duration.

--- @since v5.1.0
--- @class text_graphtype_data (you can also just return a string or number for this graph type)
--- @field type GRAPH_TYPE.TEXT
--- @field text string: The text to display.
--- @field size? integer: 1-3 The size of the text small, medium or large. Defaults to large.
--- @field align? string: start, centre, or end The alignment of the text. Defaults to centre.

--- @since v5.1.0
--- @class piechart_graphtype_data
--- @field type GRAPH_TYPE.PIE_CHART
--- @field segments piechart_segment[]: A table of piechart_segment items.

--- @since v5.1.0
--- @class linegraph_graphtype_data
--- @field type GRAPH_TYPE.LINE_GRAPH
--- @field lines line[]: A table of line items.
--- @field duration_based_range? boolean: Whether the range is based on duration.
--- @field range_bounds? range_bounds: The range of the y-axis.

--- @since v5.1.0
--- @class time_barchart_graphtype_data
--- @field type GRAPH_TYPE.TIME_BARCHART
--- @field bars time_bar[]: A table of time_bar sorted in reverse chronological order. Each bar is bar_duration or bar_period in length and ends that amount of time before the previous bar.
--- @field end_time (timestamp|integer): The end time of the graph. Either a timestamp or an integer representing the Unix epoch millisecond timestamp.
--- @field bar_duration? integer: The duration of each bar in milliseconds. You must provide either this or the bar_period.
--- @field bar_period? string: The period of each bar e.g. PERIOD.DAY. You must provide either this or the bar_duration.
--- @field bar_period_multiple? integer: The number of the given bar_period units one bar represents. Defaults to 1.
--- @field duration_based_range? boolean: If true, the y-axis represents time in milliseconds.
--- @field y_max? integer: The top extent of the y-axis. If not provided, the maximum value of the bars will be used.

--- @since v5.1.0
--- @class piechart_segment
--- @field label string: The label of the segment.
--- @field value number: The value of the segment. This does not need to be normalised in any way.
--- @field color? color: The color of the segment. If not provided, a color will be chosen from the default palette.

--- @since v5.1.0
--- @alias time_bar (number|time_barchart_bar_segment|time_barchart_bar_segment[]): A table of bar_segment items.

--- @since v5.1.0
--- @alias time_barchart_bar_segment (number|time_barchart_segment)

--- @since v5.1.0
--- @class time_barchart_segment
--- @field value number: The value of the bar segment.
--- @field label? string: The label of the bar segment (shown in the legend).
--- @field color? color: The color of the bar segment. If not provided, a color will be chosen from the default palette.

--- @since v5.1.0
---@class range_bounds
---@field min number: The minimum value of the range.
---@field max number: The maximum value of the range.

--- @since v5.1.0
--- @class line
--- @field line_points line_point[]: A table of line_point items. Line points should be sorted in reverse chronological order by timestamp.
--- @field line_color? color: The color of the line.
--- @field point_style? line_point_style: The style of the points on the line.
--- @field label? string: The label of the line. Will be displayed in the legend.

--- @since v5.1.0
--- @enum line_point_style
M.LINE_POINT_STYLE = {
	NONE = "none",
	CIRCLE = "circle",
	CIRCLE_VALUE = "circle_value",
}

--- @since v5.1.0
--- @class line_point
--- @field timestamp integer: The timestamp of the line point.
--- @field value number: The Y value of the line point.

--- Applies a moving average to a series of data points in place.
---
--- This function takes a table of data points (each must have at a minimum a timestamp and a value) and applies a moving average over a specified duration.
--- It updates each datapoint's value in the table to be the average of values within the specified duration preceding it.
---
--- @since v5.1.0
--- @param datapoints datapoint[]: The data points to apply the moving average to.
--- @param averaging_duration integer?: The duration over which to average the data points, in milliseconds.
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

--- Calculates the total value of data points for each period starting with the period containing the first data point.
---
--- This function takes a table of data points and calculates the total value for each period defined by the given period and multiplier.
--- It returns a new table of data points, each representing the total value for a specific period.
---
--- @since v5.1.0
--- @param datapoints datapoint[]: The data points to calculate totals for.
--- @param period string: The period to calculate totals over (e.g., core.PERIOD.DAY).
--- @param multiplier integer?: The multiplier for the period. Allowing for periods like 2 days, 3 weeks, etc.
--- @param zone_override? string: The timezone to use for the period. If not provided, the default timezone is used.
--- @return datapoint[]: A table of new data points, each representing the total value for a specific period.
M.calculate_period_totals = function(datapoints, period, multiplier, zone_override)
	if not period then
		return datapoints
	end

	local last_date = core.get_end_of_period(period, datapoints[1].timestamp, zone_override)
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

--- Finds the most recent data point in a table of data points.
---
--- This function iterates through a table of data points and returns the one with the highest timestamp
--- along with its corresponding key in the table.
---
--- @since v5.1.0
--- @param data_points table: A table of data points, where each data point should have a timestamp field.
--- @return datapoint|nil: The data point with the highest timestamp, or nil if no valid data points were found.
--- @return any|nil: The key of the data point with the highest timestamp, or nil if no valid data points were found.
M.find_latest_data_point = function(data_points)
	local latest_data_point = nil
	local latest_key = nil

	for key, data_point in pairs(data_points) do
		local valid = data_point and data_point.timestamp
		if valid and (not latest_data_point or data_point.timestamp > latest_data_point.timestamp) then
			latest_data_point = data_point
			latest_key = key
		end
	end

	return latest_data_point, latest_key
end

--- Merges data points from multiple sources in chronological order.
---
--- This function collects data points from multiple sources and organizes them in chronological order
--- (most recent first) until a specified cutoff time is reached.
---
--- @since v5.1.0
--- @param sources table: A table of sources, where each source has a dp() method that returns a data point.
--- @param cutoff_params cutoff_params|number: Parameters for calculating the cutoff time, or the cutoff time itself as a Unix epoch millisecond timestamp.
--- @param from_now boolean?: If true, uses the current time as the end time; otherwise uses the most recent data point's time. You don't need to provide this if you're using a numeric cutoff.
--- @return datapoint[]: A table of data points merged from all sources, sorted by timestamp in descending order.
M.merge_sources = function(sources, cutoff_params, from_now)
	local all_data = {}
	local last_data_points = {}

	-- Initialize the last data points table
	for key, source in pairs(sources) do
		last_data_points[key] = source.dp() or nil
	end

	local latest_data_point, latest_key = M.find_latest_data_point(last_data_points)

	local cutoff
	if type(cutoff_params) == "number" then
		cutoff = cutoff_params
	else
		local end_time = from_now and core.time() or latest_data_point
		cutoff = core.get_cutoff(cutoff_params, end_time)
	end

	while true do
		-- If no latest data point is found, break the loop
		if not latest_data_point or not latest_key then
			break
		end

		if cutoff and latest_data_point.timestamp < cutoff then
			break
		end

		-- Add the latest data point to all_data
		table.insert(all_data, latest_data_point)

		-- Fetch the next data point from the source that provided the latest data point
		last_data_points[latest_key] = sources[latest_key].dp() or nil

		latest_data_point, latest_key = M.find_latest_data_point(last_data_points)
	end

	return all_data
end

--- Collects all given data points into time period bars for drawing on a time bar chart.
---
--- This function takes a table of data points and groups them into time_bar segments,
--- calculating one segment corresponding to the sum of each label within each period.
---
--- The input datapoints should be in reverse chronological order. The resultant bars will also be in reverse chronological order. The segments in each bar will be sorted such that the segment with the greatest total value over the time period is alway first (at the bottom of the graph).
---
--- @since v5.1.0
--- @param datapoints datapoint[]: The data points to process (in reverse chronological order).
--- @param totalling_period string: The period to group by (e.g., core.PERIOD.DAY).
--- @param totalling_period_multiplier? integer: Optional multiplier for the period.
--- @param count_by_label boolean?: If true, counts occurrences of labels instead of summing values. Defaults to false
--- @param end_time integer?: The end time of the graph. An integer representing the Unix epoch millisecond timestamp. If not provided then the timestamp of the last datapoint in the datapoints is used.
--- @param label_colors color?: A table of colors for each label. The keys are the labels and the values are color values.
--- @return time_bar[]: A table of bars, each containing segments for the period.
M.collect_to_bars = function(
		datapoints,
		totalling_period,
		totalling_period_multiplier,
		count_by_label,
		end_time,
		label_colors
)
	local bars = {}

	if not datapoints or #datapoints == 0 then
		return bars
	end

	local index = 1
	local multiplier = totalling_period_multiplier or 1
	local resolved_end_time = end_time or datapoints[index].timestamp
	local current_bar_start = core.time(core.get_end_of_period(totalling_period, resolved_end_time))
	current_bar_start = core.shift(current_bar_start, totalling_period, -multiplier).timestamp

	local running_totals = {}

	while index <= #datapoints do
		local period_totals = {}

		-- Process all data points that belong to the current period
		while index <= #datapoints and datapoints[index].timestamp > current_bar_start do
			local dp = datapoints[index]
			local key = dp.label or ""
			local value = count_by_label and 1 or dp.value
			period_totals[key] = (period_totals[key] or 0) + value
			index = index + 1
		end

		-- Create segments for the current period
		local segments = {}
		for label, value in pairs(period_totals) do
			running_totals[label] = (running_totals[label] or 0) + value
			table.insert(segments, {
				label = label,
				value = value,
				color = label_colors and label_colors[label] or nil,
			})
		end

		table.insert(bars, segments)

		-- Move to the next period
		current_bar_start = core.shift(current_bar_start, totalling_period, -multiplier).timestamp
	end

	for _, bar in ipairs(bars) do
		table.sort(bar, function(a, b)
			return running_totals[a.label] > running_totals[b.label]
		end)
	end

	return bars
end

--- Collects all given data points into pie chart segments by their label
--- @since v5.1.0
--- @param datapoints datapoint[]: The data points to process.
--- @param count_by_label boolean?: If true, counts occurrences of labels instead of summing values. Defaults to false
--- @param label_colors color?: A table of colors for each label. The keys are the labels and the values are color values.
--- @return piechart_segment[]
M.collect_to_segments = function(datapoints, count_by_label, label_colors)
	local segments = {}
	local totals = {}

	for _, dp in ipairs(datapoints) do
		local key = dp.label or ""
		local value = count_by_label and 1 or dp.value
		totals[key] = (totals[key] or 0) + value
	end

	for label, value in pairs(totals) do
		table.insert(segments, {
			label = label,
			value = value,
			color = label_colors and label_colors[label] or nil,
		})
	end

	table.sort(segments, function(a, b)
		return a.label < b.label
	end)

	return segments
end

return M
