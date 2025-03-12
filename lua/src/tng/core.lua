local M = {}

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

--- Data point structure:
--- @class datapoint
--- @field timestamp integer: The Unix epoch millisecond timestamp of the data point.
--- @field offset integer: The offset from UTC in seconds. This allows you to know what the local time was when the data point was recorded.
--- @field value number: The value of the data point.
--- @field label string: The label of the data point.
--- @field note string: The note of the data point.

--- Returns the given date as a timestamp. If no date is provided, the current time will be used.
--- @param date? date: The date to use for the timestamp. If not provided, the current time will be used.
--- @return timestamp: A table containing the time at the given date or the current time.
M.time = function(date) end

--- Returns the given timestamp as a date. If no timestamp is provided, the current time will be used.
--- @param timestamp? (timestamp|integer): The timestamp to use for the date.
--- If it is an integer, it will mean the timestamp in milliseconds since the epoch.
--- If not provided, the current time will be used.
--- @return date: A table containing the current date.
M.date = function(timestamp) end

--- Shifts the given date or time by the given amount of units.
---
--- examples:
--- shift(timestamp, DURATION.SECOND) -- shifts the timestamp by one second
--- shift(timestamp, PERIOD.DAY) -- shifts the timestamp by one day
--- shift(timestamp, PERIOD.DAY, -2) -- shifts the timestamp backwards by two days
--- shift(timestamp, DURATION.HOUR * 3) -- shifts the timestamp forwards by 3 hours
---
--- Using periods will respect daylight savings time and other time zone changes. Where as using a duration will just move the timestamp by that amount of milliseconds.
---
--- @param datetime (timestamp|date): Any table with at least the field timestamp. Offset, and zone are optional.
--- @param unit (DURATION|PERIOD): The units to shift by. Can be a duration in milliseconds (e.g. DURATION.DAY) or a period string (e.g. PERIOD.DAY).
--- @param amount? integer: Multiplier for the units. Defaults to 1. Useful if you are passing a period string.
--- @return timestamp: The shifted timestamp.
M.shift = function(datetime, unit, amount) end

--- Formats the given datetime using the given format string.
--- See the ofPattern Java/Kotlin date/time formatting functionality here: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
--- @param datetime (timestamp|date): The datetime to format.
--- @return string: The formatted datetime.
M.format = function(datetime, format) end

--- Colors enum
--- @enum COLOR
M.COLOR = {
	RED_DARK = 0,    -- #A50026
	RED = 1,         -- #D73027
	ORANGE_DARK = 2, -- #F46D43
	ORANGE = 3,      -- #FDAE61
	YELLOW = 4,      -- #FEE090
	BLUE_LIGHT = 5,  -- #E0F3F8
	BLUE_SKY = 6,    -- #ABD9E9
	BLUE = 7,        -- #74ADD1
	BLUE_DARK = 8,   -- #4575B4
	BLUE_NAVY = 9,   -- #313695
	GREEN_LIGHT = 10, -- #54D931
	GREEN_DARK = 11, -- #1B8200
}

--- @alias color (COLOR|string): Can be a value from COLOR enum or a hex string e.g. "#00FF00"

--- @enum DURATION All durations are in milliseconds.
M.DURATION = {
	SECOND = 1000,                 -- One second in milliseconds
	MINUTE = 60 * 1000,            -- One minute in milliseconds
	HOUR = 60 * 60 * 1000,         -- One hour in milliseconds
	DAY = 24 * 60 * 60 * 1000,     -- One day in milliseconds
	WEEK = 7 * 24 * 60 * 60 * 1000, -- One week in milliseconds
}

--- @enum PERIOD
M.PERIOD = {
	DAY = "P1D",  -- One day period
	WEEK = "P1W", -- One week period
	MONTH = "P1M", -- One month period
	YEAR = "P1Y", -- One year period
}

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
function datasource:dpafter(datetime) end

return M
