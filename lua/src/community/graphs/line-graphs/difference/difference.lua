local core = require("tng.core")
local graph = require("tng.graph")
local graphext = require("tng.graphext")

--- PREVIEW_START
-- Script: Line Graphs - Difference
-- Optional period of data to be displayed e.g. core.PERIOD.WEEK to only show 1 week of data
local period = nil
-- Optional integer value used with period e.g. 5
local period_multiplier = nil
-- If from_now is false the end of the graph will be the last datapoint, otherwise it's the current date/time
local from_now = false
-- Optional colors list, e.g. {"#FF00FF", "#0000FF", core.COLOR.BLUE_SKY}
local line_colors = nil
-- Optional point style e.g. graph.LINE_POINT_STYLE.CIRCLE
local line_point_style = nil
-- Optional integer value used to average data points over a certain duration e.g. core.DURATION.DAY * 30 for a 30 day moving average
local averaging_duration = nil
-- Optional totalling period used to calculate 'plot totals' e.g. core.PERIOD.WEEK
local totalling_period = nil
-- Optional totalling period multiplier used to calculate 'plot totals' e.g. 2
local totalling_period_multiplier = nil
-- Optional if the y axis represents time
local duration_based_range = false
-- Optional bounds for the y axis e.g. { min = 0, max = 100 }
local range_bounds = nil
--- PREVIEW_END

local get_difference = function(datapoints)
	local difference = {}
	for i = 2, #datapoints, 1 do
		local datapoint = datapoints[i]
		difference[i - 1] = {
			timestamp = datapoint.timestamp,
			offset = datapoint.offset,
			value = datapoints[i - 1].value - datapoint.value,
			label = datapoint.label,
			note = datapoint.note,
		}
	end
	return difference
end

local function get_line_data(source)
	local latest_data_point = source.dp()

	if not latest_data_point then
		return nil
	end

	local cutoff_params = {
		period = period,
		period_multiplier = period_multiplier,
	}
	local cutoff_end = nil
	if not from_now then
		cutoff_end = latest_data_point.timestamp
	end
	local cutoff = core.get_cutoff(cutoff_params, cutoff_end)

	local datapoints = source.dpafter(cutoff)
	table.insert(datapoints, 1, latest_data_point)

	local all_data = {}
	if totalling_period == nil then
		all_data = datapoints
	elseif totalling_period ~= nil then
		all_data = graphext.calculate_period_totals(datapoints, totalling_period, totalling_period_multiplier)
	end

	local difference = get_difference(all_data)
	graphext.apply_moving_averaging(difference, averaging_duration)

	local line_color = line_colors and line_colors[source.index] or nil

	return {
		line_points = difference,
		line_color = line_color,
		point_style = line_point_style or graph.LINE_POINT_STYLE.NONE,
		label = source.name,
	}
end

return function(sources)
	table.sort(sources, function(a, b)
		return a.index < b.index
	end)

	local lines = {}
	for _, source in pairs(sources) do
		local line_data = get_line_data(source)
		if line_data then
			table.insert(lines, line_data)
		end
	end

	return graph.line_graph({
		lines = lines,
		duration_based_range = duration_based_range,
		range_bounds = range_bounds,
	})
end
