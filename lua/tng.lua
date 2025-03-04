local tng = {}

--- Duration enum: All timestamps are in milliseconds.
--- @enum duration
tng.DURATION = {
	SECOND = 1000, -- One second in milliseconds
	MINUTE = 60 * 1000, -- One minute in milliseconds
	HOUR = 60 * 60 * 1000, -- One hour in milliseconds
	DAY = 24 * 60 * 60 * 1000, -- One day in milliseconds
	WEEK = 7 * 24 * 60 * 60 * 1000, -- One week in milliseconds
}

--- Period enum: Constants for use with addperiod function.
--- @enum period
tng.PERIOD = {
	DAY = "P1D", -- One day period
	WEEK = "P1W", -- One week period
	MONTH = "P1M", -- One month period
	YEAR = "P1Y", -- One year period
}

tng.time = {}

--- Timestamp structure:
--- @class timestamp
--- @field timestamp integer: The Unix epoch millisecond timestamp.
--- @field offset? integer: The offset from UTC in seconds.
--- @field zone? string: A zone id from the IANA time zone database.

--- @class date
--- @field year  integer: four digits
--- @field month integer: 1-12
--- @field day   integer: 1-31
--- @field hour?  integer: 0-23
--- @field min?   integer: 0-59
--- @field sec?   integer: 0-61
--- @field wday?  integer: weekday, 1–7, Monday is 1
--- @field yday?  integer: day of the year, 1–366
--- @field zone?  string: the IANA time zone id to use for the timestamp. Defaults to the local time zone.

--- Returns the given date as a timestamp. If no date is provided, the current time will be used.
--- @param date? date: The date to use for the timestamp. If not provided, the current time will be used.
--- @return timestamp: A table containing the time at the given date or the current time.
tng.time.time = function(date) end

--- Returns the given timestamp as a date. If no timestamp is provided, the current time will be used.
--- @param timestamp? (timestamp|integer): The timestamp to use for the date.
--- If it is an integer, it will mean the timestamp in milliseconds since the epoch.
--- If not provided, the current time will be used.
--- @return date: A table containing the current date.
tng.time.date = function(timestamp) end

--- Shifts the given date or time by the given amount of units.
---
--- examples:
--- tng.time.shift(timestamp, tng.DURATION.SECOND) -- shifts the timestamp by one second
--- tng.time.shift(timestamp, tng.PERIOD.DAY) -- shifts the timestamp by one day
--- tng.time.shift(timestamp, tng.PERIOD.DAY, -2) -- shifts the timestamp backwards by two days
--- tng.time.shift(timestamp, tng.DURATION.HOUR * 3) -- shifts the timestamp forwards by 3 hours
---
--- Using periods will respect daylight savings time and other time zone changes. Where as using a duration will just move the timestamp by that amount of milliseconds.
---
--- @param datetime (timestamp|date): Any table with at least the field timestamp. Offset, and zone are optional.
--- @param unit (duration|period): The units to shift by. Can be a duration in milliseconds (e.g. tng.DURATION.DAY) or a period string (e.g. tng.PERIOD.DAY).
--- @param amount? integer: Multiplier for the units. Defaults to 1. Useful if you are passing a period string.
--- @return table: A table with the same data as the input table but with the timestamp and offset shifted.
--- If zone and offset are not in the input table they will be added to the output table.
tng.time.shift = function(datetime, unit, amount) end

--- Formats the given datetime using the given format string.
--- See the ofPattern Java/Kotlin date/time formatting functionality here: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
--- @param datetime (timestamp|date): The datetime to format.
--- @return string: The formatted datetime.
tng.time.format = function(datetime, format) end

--- Data point structure:
--- @class datapoint
--- @field timestamp integer: The Unix epoch millisecond timestamp of the data point.
--- @field offset integer: The offset from UTC in seconds. This allows you to know what the local time was when the data point was recorded.
--- @field featureId string: The database ID of the feature (aka data source).
--- @field value number: The value of the data point.
--- @field label string: The label of the data point.
--- @field note string: The note of the data point.

tng.graph = {}

--- Graph colors enum
--- @enum tng_color
tng.COLOR = {
	RED_DARK = 0, -- #A50026
	RED = 1, -- #D73027
	ORANGE_DARK = 2, -- #F46D43
	ORANGE = 3, -- #FDAE61
	YELLOW = 4, -- #FEE090
	BLUE_LIGHT = 5, -- #E0F3F8
	BLUE_SKY = 6, -- #ABD9E9
	BLUE = 7, -- #74ADD1
	BLUE_DARK = 8, -- #4575B4
	BLUE_NAVY = 9, -- #313695
	GREEN_LIGHT = 10, -- #54D931
	GREEN_DARK = 11, -- #1B8200
}

--- @alias color (tng_color|string): Can be a value from tng.COLOR enum or a hex string e.g. "#00FF00"

--- Graph types enum
--- @enum tng.GRAPH_TYPE
tng.GRAPH_TYPE = {
	DATA_POINT = "DATA_POINT",
	TEXT = "TEXT",
	PIE_CHART = "PIE_CHART",
	TIME_BARCHART = "TIME_BARCHART",
	LINE_GRAPH = "LINE_GRAPH",
}

--- @class datapoint_graphtype_data
--- @field type tng.GRAPH_TYPE.DATA_POINT: The type of the graph.
--- @field datapoint datapoint: The data point to display.
--- @field isduration boolean: Whether the data point is a duration.

--- @class text_graphtype_data (you can also just return a string or number for this graph type)
--- @field type tng.GRAPH_TYPE.TEXT
--- @field text string: The text to display.
--- @field size? integer: 1-3 The size of the text small, medium or large. Defaults to large.
--- @field align? string: start, centre, or end The alignment of the text. Defaults to centre.

--- @class piechart_graphtype_data
--- @field type tng.GRAPH_TYPE.PIE_CHART
--- @field segments piechart_segment[]: A table of piechart_segment items.

--- @class linegraph_graphtype_data
--- @field type tng.GRAPH_TYPE.LINE_GRAPH
--- @field duration_based_range? boolean: Whether the range is based on duration.
--- @field range_bounds? range_bounds: The range of the y-axis.
--- @field lines line[]: A table of line items.

--- @class time_barchart_graphtype_data
--- @field type tng.GRAPH_TYPE.TIME_BARCHART
--- @field bar_duration? integer: The duration of each bar in milliseconds. You must provide either this or the bar_period.
--- @field bar_period? string: The period of each bar e.g. tng.PERIOD.DAY. You must provide either this or the bar_duration.
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
tng.LINE_POINT_STYLE = {
	NONE = "none",
	CIRCLE = "circle",
	CIRCLE_VALUE = "circle_value",
}

--- @class line_point
--- @field timestamp integer: The timestamp of the line point.
--- @field value number: The Y value of the line point.

--- @class datasource
--- @field name string: The name of the data source.
local datasource = {}

--- Fetches the next data point from the data source.
--- Data points are iterated in reverse chronological order.
--- @return datapoint
function datasource:dp() end

--- Fetches the next group of data points from the data source.
--- Data points are iterated in reverse chronological order.
--- @param count integer: The number of data points to retrieve.
--- @return datapoint[]: A table containing the requested data points.
function datasource:dpbatch(count) end

--- Fetches all data points from the data source.
--- Data points are iterated in reverse chronological order.
--- @return datapoint[]: A table containing all data points.
function datasource:dpall() end

--- Fetches all data points from the data source that are newer than the given timestamp.
--- Data points are iterated in reverse chronological order.
--- After this operation the data source will contain only datapoints that are before the given timestamp
--- @param datetime (timestamp|date): The timestamp to compare against.
--- @return datapoint[]: A table containing all data points that are newer than the given timestamp.
function datasource:dpafter() end

return tng
