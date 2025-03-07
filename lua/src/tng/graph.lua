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
--- @field duration_based_range? boolean: Whether the range is based on duration.
--- @field range_bounds? range_bounds: The range of the y-axis.
--- @field lines line[]: A table of line items.

--- @class time_barchart_graphtype_data
--- @field type GRAPH_TYPE.TIME_BARCHART
--- @field bar_duration? integer: The duration of each bar in milliseconds. You must provide either this or the bar_period.
--- @field bar_period? string: The period of each bar e.g. PERIOD.DAY. You must provide either this or the bar_duration.
--- @field bar_period_multiple? integer: The number of the given bar_period units one bar represents. Defaults to 1.
--- @field end_time (timestamp|integer): The end time of the graph. Either a timestamp or an integer representing the Unix epoch millisecond timestamp.
--- @field duration_based_range? boolean: If true, the y-axis represents time in milliseconds.
--- @field bars time_bar[]: A table of time_bar sorted in reverse chronological order. Each bar is bar_duration or bar_period in length and ends that amount of time before the previous bar.
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
--- @field line_color? color: The color of the line.
--- @field point_style? line_point_style: The style of the points on the line.
--- @field line_points line_point[]: A table of line_point items. Line points should be sorted in reverse chronological order by timestamp.
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

return M
