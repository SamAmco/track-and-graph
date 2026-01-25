-- GENERATED FILE: DO NOT EDIT MANUALLY

local M = {}

--- Graph types enum
--- @since v5.1.0 (API level 1)
--- @enum GRAPH_TYPE
M.GRAPH_TYPE = {
	DATA_POINT = "DATA_POINT",
	TEXT = "TEXT",
	PIE_CHART = "PIE_CHART",
	TIME_BARCHART = "TIME_BARCHART",
	LINE_GRAPH = "LINE_GRAPH",
}

--- @since v5.1.0 (API level 1)
--- @enum LINE_POINT_STYLE
M.LINE_POINT_STYLE = {
	--- just lines
	NONE = "none",
	--- lines with circles at each point
	CIRCLE = "circle",
	--- lines with circles and values at each point
	CIRCLE_VALUE = "circle_value",
	--- @since v7.0.0 (API level 1) circles with no connecting lines
	CIRCLE_ONLY = "circles_only",
}

--- @since v5.1.0 (API level 1)
--- @class DataPointGraphParams
--- @field datapoint DataPoint: The data point to display.
--- @field isduration boolean: Whether the data point is a duration.
M.DataPointGraphParams = {}

--- @since v5.1.0 (API level 1)
--- @class TextGraphParams
--- @field text (string|number): The text to display.
--- @field size? integer: 1-3 The size of the text small, medium or large. Defaults to medium.
--- @field align? string: start, centre, or end The alignment of the text. Defaults to centre.
M.TextGraphParams = {}

--- @since v5.1.0 (API level 1)
--- @class PieChartGraphParams
--- @field segments PieChartSegment[]: A table of piechart segment items.
M.PieChartGraphParams = {}

--- @since v5.1.0 (API level 1)
--- @class LineGraphParams
--- @field lines Line[]: A table of line items.
--- @field duration_based_range? boolean: Whether the range is based on duration.
--- @field range_bounds? RangeBounds: The range of the y-axis.
M.LineGraphParams = {}

--- @since v5.1.0 (API level 1)
--- @class TimeBarChartGraphParams
--- @field bars TimeBar[]: A table of TimeBar sorted in reverse chronological order. Each bar is bar_duration or bar_period in length and ends that amount of time before the previous bar.
--- @field end_time (Timestamp|integer): The end time of the graph. Either a timestamp or an integer representing the Unix epoch millisecond timestamp.
--- @field bar_duration? integer: The duration of each bar in milliseconds. You must provide either this or the bar_period.
--- @field bar_period? string: The period of each bar e.g. PERIOD.DAY. You must provide either this or the bar_duration.
--- @field bar_period_multiple? integer: The number of the given bar_period units one bar represents. Defaults to 1.
--- @field duration_based_range? boolean: If true, the y-axis represents time in milliseconds.
--- @field y_max? integer: The top extent of the y-axis. If not provided, the maximum value of the bars will be used.
M.TimeBarChartGraphParams = {}

--- @since v5.1.0 (API level 1)
--- @class PieChartSegment
--- @field label string: The label of the segment.
--- @field value number: The value of the segment. This does not need to be normalised in any way.
--- @field color? Color: The color of the segment. If not provided, a color will be chosen from the default palette.
M.PieChartSegment = {}

--- @since v5.1.0 (API level 1)
--- @alias TimeBar (number|number[]|TimeBarChartSegment|TimeBarChartSegment[]): A table of bar_segment items.

--- @since v5.1.0 (API level 1)
--- @class TimeBarChartSegment
--- @field value number: The value of the bar segment.
--- @field label? string: The label of the bar segment (shown in the legend).
--- @field color? Color: The color of the bar segment. If not provided, a color will be chosen from the default palette.
M.TimeBarChartSegment = {}

--- @since v5.1.0 (API level 1)
---@class RangeBounds
---@field min number: The minimum value of the range.
---@field max number: The maximum value of the range.
M.RangeBounds = {}

--- @since v5.1.0 (API level 1)
--- @class Line
--- @field line_points LinePoint[]: A table of line_point items. Line points should be sorted in reverse chronological order by timestamp.
--- @field line_color? Color: The color of the line.
--- @field point_style? LINE_POINT_STYLE: The style of the points on the line.
--- @field label? string: The label of the line. Will be displayed in the legend.
M.Line = {}

--- @since v5.1.0 (API level 1)
--- @class LinePoint
--- @field timestamp integer: The timestamp of the line point.
--- @field value number: The Y value of the line point.
M.LinePoint = {}

--- Creates a data point graph.
--- @since v5.1.0 (API level 1)
--- @param params DataPointGraphParams: The parameters for the data point graph.
--- @return table: A table representing the data point graph.
M.data_point = function(params)
	return {
		type = M.GRAPH_TYPE.DATA_POINT,
		datapoint = params.datapoint,
		isduration = params.isduration,
	}
end

--- Creates a text graph.
--- @since v5.1.0 (API level 1)
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

--- Creates a pie chart graph.
--- @since v5.1.0 (API level 1)
--- @param params PieChartGraphParams: The parameters for the pie chart graph.
--- @return table: A table representing the pie chart graph.
M.pie_chart = function(params)
	return {
		type = M.GRAPH_TYPE.PIE_CHART,
		segments = params.segments,
	}
end

--- Creates a line graph.
--- @since v5.1.0 (API level 1)
--- @param params LineGraphParams: The parameters for the line graph.
--- @return table: A table representing the line graph.
M.line_graph = function(params)
	return {
		type = M.GRAPH_TYPE.LINE_GRAPH,
		lines = params.lines,
		duration_based_range = params.duration_based_range,
		range_bounds = params.range_bounds,
	}
end

--- Creates a time bar chart graph.
--- @since v5.1.0 (API level 1)
--- @param params TimeBarChartGraphParams: The parameters for the time bar chart graph.
--- @return table: A table representing the time bar chart graph.
M.time_barchart = function(params)
	return {
		type = M.GRAPH_TYPE.TIME_BARCHART,
		bars = params.bars,
		end_time = params.end_time,
		bar_duration = params.bar_duration,
		bar_period = params.bar_period,
		bar_period_multiple = params.bar_period_multiple,
		duration_based_range = params.duration_based_range,
		y_max = params.y_max,
	}
end

return M
