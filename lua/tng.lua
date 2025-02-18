local tng = {}

--- Duration enum: All timestamps are in milliseconds.
--- @enum tng.DURATION
tng.DURATION = {
    SECOND = 1000, -- One second in milliseconds
    MINUTE = 60 * 1000, -- One minute in milliseconds
    HOUR = 60 * 60 * 1000, -- One hour in milliseconds
    DAY = 24 * 60 * 60 * 1000, -- One day in milliseconds
    WEEK = 7 * 24 * 60 * 60 * 1000, -- One week in milliseconds
}

--- Period enum: Constants for use with addperiod function.
--- @enum tng.PERIOD
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
--- @field offset integer (optional): The offset from UTC in seconds.
--- @field zone string (optional): A zone id from the IANA time zone database.

--- @class date
--- @field year  integer: four digits
--- @field month integer: 1-12
--- @field day   integer: 1-31
--- @field hour  integer (optional): 0-23
--- @field min   integer (optional): 0-59
--- @field sec   integer (optional): 0-61
--- @field wday  integer (optional): weekday, 1–7, Monday is 1
--- @field yday  integer (optional): day of the year, 1–366
--- @field zone  string (optional): the IANA time zone id to use for the timestamp. Defaults to the local time zone.

--- Returns the given date as a timestamp. If no date is provided, the current time will be used.
--- @param date date (optional): The date to use for the timestamp. If not provided, the current time will be used.
--- @return timestamp: A table containing the time at the given date or the current time.
tng.time.time = function(date) end

--- Returns the given timestamp as a date. If no timestamp is provided, the current time will be used.
--- @param timestamp (timestamp|integer) (optional): The timestamp to use for the date.
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
--- @param unit (tng.DURATION|tng.PERIOD): The units to shift by. Can be a duration in milliseconds (e.g. tng.DURATION.DAY) or a period string (e.g. tng.PERIOD.DAY).
--- @param amount integer (optional): Multiplier for the units. Defaults to 1. Useful if you are passing a period string.
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
--- @enum tng.COLOR
tng.COLOR = {
    RED_DARK = 1,    -- #A50026
    RED = 2,         -- #D73027
    ORANGE_DARK = 3, -- #F46D43
    ORANGE = 4,      -- #FDAE61
    YELLOW = 5,      -- #FEE090
    BLUE_LIGHT = 6,  -- #E0F3F8
    BLUE_SKY = 7,    -- #ABD9E9
    BLUE = 8,        -- #74ADD1
    BLUE_DARK = 9,   -- #4575B4
    BLUE_NAVY = 10,  -- #313695
    GREEN_LIGHT = 11,-- #54D931
    GREEN_DARK = 12  -- #1B8200
}

--- @alias color (tng.COLOR|string): Can be a value from tng.COLOR enum or a hex string e.g. "#00FF00"

--- Graph types enum
--- @enum tng.GRAPH_TYPE
tng.GRAPH_TYPE = {
    DATAPOINT = "datapoint",
    TEXT = "text",
    PIECHART = "piechart",
    TIME_BARCHART = "time_barchart",
    LINEGRAPH = "linegraph",
    BARCHART = "barchart"
}

--- Data class for tng.GRAPH_TYPE.DATAPOINT
--- @class datapoint_graphtype_data
--- @field timestamp timestamp: The timestamp of the data point.
--- @field featureId string: The database ID of the feature (aka data source).
--- @field value number: The value of the data point.
--- @field label string: The label of the data point.
--- @field note string: The note of the data point.
--- @field isduration boolean: Whether the data point is a duration.

--- Data class for tng.GRAPH_TYPE.TEXT
--- @class text_graphtype_data (you can also just return a string or number for this graph type)
--- @field text string: The text to display.
--- @field size integer (optional): 1-3 The size of the text small, medium or large. Defaults to large.
--- @field align string (optional): start, centre, or end The alignment of the text. Defaults to centre.

--- Data class for tng.GRAPH_TYPE.PIECHART
--- Pie chart data is just a table of piechart_segment.
--- @class piechart_segment
--- @field label string: The label of the segment.
--- @field value number: The value of the segment. This does not need to be normalised in any way.
--- @field color color (optional): The color of the segment. If not provided, a color will be chosen from the default palette.

--- Data class for tng.GRAPH_TYPE.TIME_BARCHART
--- @class time_barchart_graphtype_data
--- @field bar_duration integer (optional): The duration of each bar in milliseconds. You must provide either this or the bar_period.
--- @field bar_period string (optional): The period of each bar e.g. tng.PERIOD.DAY. You must provide either this or the bar_duration.
--- @field bar_period_multiple integer (optional): The number of the given bar_period units one bar represents. Defaults to 1.
--- @field duration_based_range boolean: If true, the y-axis represents time in milliseconds.
--- @field bars time_bar[]: A table of time_bar.
--- @field y_max integer (optional): The top extent of the y-axis. If not provided, the maximum value of the bars will be used.

--- @class time_bar
--- @field timestamp timestamp: The timestamp of the bar.
--- @field segments bar_segment[]: A table of bar_segment items.

--- @class bar_segment
--- @field value number: The value of the bar segment.
--- @field label string (optional): The label of the bar segment (shown in the legend).
--- @field color color (optional): The color of the bar segment. If not provided, a color will be chosen from the default palette.

--- Data class for tng.GRAPH_TYPE.LINEGRAPH
--- @class linegraph_graphtype_data
--- @field duration_based_range boolean: Whether the range is based on duration.
--- @field y_min integer (optional): The minimum y-value.
--- @field y_max integer (optional): The maximum y-value.
--- @field lines line[]: A table of line items.

--- @class line
--- @field line_color color (optional): The color of the line.
--- @field point_style string (optional): The style of the points on the line.
--- @field line_points line_point[]: A table of line_point items.

--- @class line_point
--- @field timestamp integer: The timestamp of the line point.
--- @field value number: The Y value of the line point.

--- @class barchart_graphtype_data
--- @field bars barchar_bat[]: A table of barchart_bar items.
--- @field y_max number (optional): The maximum value of the y-axis. If not provided, the maximum value of the bars will be used.
--- @field y_labels string[] (optional): A table of strings to use as labels on the y-axis. If provided the Y axis will be equally sub-divided by the given number of items and labelled in order from bottom to top. If not provided, the y-axis will be automatically divided and labelled. 
--- @field time_based_range boolean (optional): If true, and y_labels are not provided, the y-axis represents time in milliseconds, and will use time based labels.
--- @field legend legend[] (optional): A table of legend items.

--- @class barchart_bar 
--- @field segments barchart_bar_segment[]: A table of barchart_bar_segment items.
--- @field label string (optional): The X-axis label of the bar.

--- @alias barchart_bar_segment (number | colored_barchart_bar_segment)

--- @class colored_barchart_bar_segment
--- @field value number: The value of the bar segment.
--- @field color color (optional): The color of the bar segment. If not provided, a color will be chosen from the default palette.

--- @class legend
--- @field label string: The label of the legend item.
--- @field color color: The color of the legend item.

--- Returns a list of all the data sources available to this graph.
--- @return string[]: A table of strings containing the names of all the data sources.
tng.graph.sources = function() end

--- Fetches the next data point from the data source.
--- Data points are iterated in reverse chronological order.
--- @param name string: The name of the data source.
--- @return datapoint
tng.graph.dp = function(name) end

--- Fetches the next group of data points from the data source.
--- Data points are iterated in reverse chronological order.
--- @param name string: The name of the data source.
--- @param count integer: The number of data points to retrieve.
--- @return datapoint[]: A table containing the requested data points.
tng.graph.dpbatch = function(name, count) end

--- Fetches all data points from the data source.
--- Data points are iterated in reverse chronological order.
--- @param name string: The name of the data source.
--- @return datapoint[]: A table containing all data points.
tng.graph.dpall = function(name) end

--- Fetches all data points from the data source that are newer than the given timestamp.
--- Data points are iterated in reverse chronological order.
--- After this operation the data source will contain only datapoints that are before the given timestamp
--- @param name string: The name of the data source.
--- @param datetime (timestamp|date): The timestamp to compare against.
--- @return datapoint[]: A table containing all data points that are newer than the given timestamp.
tng.graph.dpafter = function(name, datetime) end

return tng
