local core = require("tng.core")
local graph = require("tng.graph")
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

local find_latest_data_point = function(data_points)
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

local get_line_data = function(sources)
	local all_data = {}
	local last_data_points = {}

	-- Initialize the last data points table
	for key, source in pairs(sources) do
		last_data_points[key] = source.dp() or nil
	end

	local latest_data_point, latest_key = find_latest_data_point(last_data_points)

	local cutoff_params = {
		period = period,
		period_multiplier = period_multiplier,
		from_now = from_now,
		timestamp = (latest_data_point and latest_data_point.timestamp) or nil,
	}
	local cutoff = graph.get_cutoff(cutoff_params)

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

		latest_data_point, latest_key = find_latest_data_point(last_data_points)
	end

	return all_data
end

return function(sources)
	local all_data = get_line_data(sources)

	graph.apply_moving_averaging(all_data, averaging_duration)
	local totalled_date = graph.calculate_period_totals(all_data, totalling_period, totalling_period_multiplier)

	local line = {
		line_points = totalled_date,
		line_color = line_color,
		point_style = line_point_style or graph.LINE_POINT_STYLE.NONE,
		label = line_label,
	}

	return {
		type = graph.GRAPH_TYPE.LINE_GRAPH,
		lines = { line },
	}
end
