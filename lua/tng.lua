local tng = {}

tng.time = {}
--- Useful time constants. All timestamps are in milliseconds.
tng.time.D_SECOND = 1000 -- One second in milliseconds
tng.time.D_MINUTE = 60 * tng.time.D_SECOND -- One minute in milliseconds
tng.time.D_HOUR = 60 * tng.time.D_MINUTE -- One hour in milliseconds
tng.time.D_DAY = 24 * tng.time.D_HOUR -- One day in milliseconds
tng.time.D_WEEK = 7 * tng.time.D_DAY -- One week in milliseconds

--- Period constants for use with addperiod function.
tng.time.P_DAY = "P1D" -- One day period
tng.time.P_WEEK = "P1W" -- One week period
tng.time.P_MONTH = "P1M" -- One month period
tng.time.P_YEAR = "P1Y" -- One year period

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
--- @see date
--- @see timestamp
tng.time.time = function(date) end

--- Returns the given timestamp as a date. If no timestamp is provided, the current time will be used.
--- @param timestamp (timestamp|integer) (optional): The timestamp to use for the date.
--- If it is an integer, it will mean the timestamp in milliseconds since the epoch.
--- If not provided, the current time will be used.
--- @return date: A table containing the current date.
--- @see date
--- @see timestamp
tng.time.date = function(timestamp) end

--- Shifts the given date or time by the given amount of units.
---
--- examples:
--- tng.time.shift(timestamp, tng.time.D_SECOND) -- shifts the timestamp by one day
--- tng.time.shift(timestamp, tng.time.P_DAY) -- shifts the timestamp by one day
--- tng.time.shift(timestamp, tng.time.P_DAY, -2) -- shifts the timestamp backwards by two days
--- tng.time.shift(timestamp, tng.time.D_HOUR * 3) -- shifts the timestamp forwards by 3 hours
---
--- Using periods will respect daylight savings time and other time zone changes. Where as using a duration will just move the timestamp by that amount of milliseconds.
---
--- @param datetime (timestamp|date): Any table with at least the field timestamp. Offset, and zone are optional.
--- @param units (integer|string): The units to shift by. Can be a duration in milliseconds (e.g. tng.time.D_DAY) or a period string (e.g. tng.time.P_DAY).
--- @param amount integer (optional): Multiplier for the units. Defaults to 1. Useful if you are passing a period string.
--- @return table: A table with the same data as the input table but with the timestamp and offset shifted.
--- If zone and offset are not in the input table they will be added to the output table.
tng.time.shift = function(datetime, units, amount) end

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

--- Graph types
tng.graph.DATAPOINT = "datapoint"
--- @class datapoint_graphtype_data
--- @field timestamp timestamp: The timestamp of the data point.
--- @field featureId string: The database ID of the feature (aka data source).
--- @field value number: The value of the data point.
--- @field label string: The label of the data point.
--- @field note string: The note of the data point.
--- @field isduration boolean: Whether the data point is a duration.

tng.graph.TEXT = "text"
--- @class text_graphtype_data (you can also just return a string or number for this graph type)
--- @field text string: The text to display.
--- @field size integer (optional): 1-3 The size of the text small, medium or large. Defaults to large.
--- @field align string (optional): start, centre, or end The alignment of the text. Defaults to centre.

--- Returns a list of all the data sources available to this graph.
--- @return table: A table of strings containing the names of all the data sources.
tng.graph.sources = function() end

--- Fetches the next data point from the data source.
--- Data points are iterated in reverse chronological order.
--- @param name string: The name of the data source.
--- @see datapoint
--- @return datapoint
--- @see datapoint
tng.graph.dp = function(name) end

--- Fetches the next group of data points from the data source.
--- Data points are iterated in reverse chronological order.
--- @param name string: The name of the data source.
--- @param count integer: The number of data points to retrieve.
--- @return table: A table containing the requested data points.
--- @see datapoint
tng.graph.dpbatch = function(name, count) end

--- Fetches all data points from the data source.
--- Data points are iterated in reverse chronological order.
--- @param name string: The name of the data source.
--- @return table: A table containing all data points.
--- @see datapoint
tng.graph.dpall = function(name) end

--- Fetches all data points from the data source that are newer than the given timestamp.
--- Data points are iterated in reverse chronological order.
--- After this operation the data source will contain only datapoints that are before the given timestamp
--- @param name string: The name of the data source.
--- @param datetime (timestamp|date): The timestamp to compare against.
--- @return table: A table containing all data points that are newer than the given timestamp.
--- @see datapoint
--- @see timestamp
tng.graph.dpafter = function(name, datetime) end

return tng
