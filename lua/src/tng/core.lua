local M = {}

--- Timestamp structure:
--- @since v5.1.0
--- @class Timestamp
--- @field timestamp integer: The Unix epoch millisecond timestamp.
--- @field offset? integer: The offset from UTC in seconds.
--- @field zone? string: A zone id from the IANA time zone database.

--- @since v5.1.0
--- @class Date
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
--- @since v5.1.0
--- @class DataPoint
--- @field timestamp integer: The Unix epoch millisecond timestamp of the data point.
--- @field offset integer: The offset from UTC in seconds. This allows you to know what the local time was when the data point was recorded.
--- @field value number: The value of the data point.
--- @field label string: The label of the data point.
--- @field note string: The note of the data point.

--- Returns the given date as a timestamp. If no date is provided, the current time will be used.
--- @since v5.1.0
--- @param date? Date: The date to use for the timestamp. If not provided, the current time will be used.
--- @return Timestamp: A table containing the time at the given date or the current time.
M.time = function(date) end

--- Returns the given timestamp as a date. If no timestamp is provided, the current time will be used.
--- @since v5.1.0
--- @param timestamp? (Timestamp|integer): The timestamp to use for the date.
--- If it is an integer, it will mean the timestamp in milliseconds since the epoch.
--- If not provided, the current time will be used.
--- @return Date: A table containing the current date.
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
--- @since v5.1.0
--- @param datetime (Timestamp|Date|integer): Any table with at least the field timestamp. Offset, and zone are optional.
--- @param unit (DURATION|PERIOD): The units to shift by. Can be a duration in milliseconds (e.g. DURATION.DAY) or a period string (e.g. PERIOD.DAY).
--- @param amount? integer: Multiplier for the units. Defaults to 1. Useful if you are passing a period string.
--- @return Timestamp: The shifted timestamp.
M.shift = function(datetime, unit, amount) end

--- Formats the given datetime using the given format string.
--- See the ofPattern Java/Kotlin date/time formatting functionality here: https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
--- @since v5.1.0
--- @param datetime (Timestamp|Date): The datetime to format.
--- @return string: The formatted datetime.
M.format = function(datetime, format) end

--- Colors enum
--- @since v5.1.0
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

--- @since v5.1.0
--- @alias Color (COLOR|string): Can be a value from COLOR enum or a hex string e.g. "#00FF00"

--- @since v5.1.0
--- @enum DURATION All durations are in milliseconds.
M.DURATION = {
	SECOND = 1000,                 -- One second in milliseconds
	MINUTE = 60 * 1000,            -- One minute in milliseconds
	HOUR = 60 * 60 * 1000,         -- One hour in milliseconds
	DAY = 24 * 60 * 60 * 1000,     -- One day in milliseconds
	WEEK = 7 * 24 * 60 * 60 * 1000, -- One week in milliseconds
}

--- @since v5.1.0
--- @enum PERIOD
M.PERIOD = {
	DAY = "P1D",  -- One day period
	WEEK = "P1W", -- One week period
	MONTH = "P1M", -- One month period
	YEAR = "P1Y", -- One year period
}

--- @since v5.1.0
--- @class DataSource
--- @field name string: The name of the data source.
--- @field index integer: The index of the data source in the list of data sources configured by the user.
local DataSource = {}

--- Fetches the next data point from the data source.
--- Data points are iterated in reverse chronological order.
--- @since v5.1.0
--- @return DataPoint
function DataSource:dp() end

--- Fetches the next group of data points from the data source.
--- Data points are iterated in reverse chronological order.
--- @since v5.1.0
--- @param count integer: The number of data points to retrieve.
--- @return DataPoint[]: A table containing the requested data points.
function DataSource:dpbatch(count) end

--- Fetches all data points from the data source.
--- Data points are iterated in reverse chronological order.
--- @since v5.1.0
--- @return DataPoint[]: A table containing all data points.
function DataSource:dpall() end

--- Fetches all data points from the data source that are newer than the given timestamp.
--- Data points are iterated in reverse chronological order.
--- After this operation the data source will contain only datapoints that are before the given timestamp
--- @since v5.1.0
--- @param datetime (Timestamp|Date)?: The timestamp to compare against. If nil then the behaviour is the same as dpall
--- @return DataPoint[]: A table containing all data points that are newer than the given timestamp.
function DataSource:dpafter(datetime) end

--- @since v5.1.0
--- @class CutoffParams
--- @field period (DURATION|PERIOD)?: The period or duration for the cutoff.
--- @field period_multiplier? integer: The multiplier for the period.

--- Calculates a timestamp by subtracting the given period*multiplier from the specified end time or now.
--- @since v5.1.0
--- @param params CutoffParams: The parameters for calculating the cutoff.
--- @param end_time (Timestamp|Date|integer)?: The end time for the cutoff. If not provided, the current time will be used. If an integer is provided, it will be treated as a timestamp in milliseconds since the epoch.
--- @return integer|nil: The cutoff timestamp obtained by subtracting the given period from the specified end time (as a Unix epoch millisecond) or the current time, or nil if the period is not provided.
M.get_cutoff = function(params, end_time)
	if not params.period then
		return nil
	end

	local multiplier = params.period_multiplier or 1

	local end_or_now = end_time or M.time()
	return M.shift(end_or_now, params.period, -multiplier).timestamp
end

--- Calculates the end of a given period for a specific timestamp.
---
--- This function determines the end of a specified period (e.g., day, week, month, year) for a given timestamp.
--- It adjusts the date to the start of the next period and sets the time to midnight.
---
--- @since v5.1.0
--- @param period string: The period to calculate the end for (e.g., PERIOD.DAY, PERIOD.WEEK).
--- @param timestamp integer: The timestamp to calculate the end of the period for.
--- @param zone_override? string: An optional timezone override. If not provided, the default timezone is used.
--- @return Date: A date representing the end of the specified period with the time set to midnight.
M.get_end_of_period = function(period, timestamp, zone_override)
	local zone = zone_override or M.date().zone
	local date = M.date(timestamp)
	if period == M.PERIOD.DAY then
		date = M.date(M.shift(date, M.PERIOD.DAY, 1))
	elseif period == M.PERIOD.WEEK then
		date = M.date(M.shift(date, M.PERIOD.WEEK, 1))
		date.wday = 1
	elseif period == M.PERIOD.MONTH then
		date = M.date(M.shift(date, M.PERIOD.MONTH, 1))
		date.day = 1
		date.wday = nil
		date.yday = nil
	elseif period == M.PERIOD.YEAR then
		date = M.date(M.shift(date, M.PERIOD.YEAR, 1))
		date.month = 1
		date.day = 1
	else
		error("Invalid period: " .. period)
	end
	date.hour = 0
	date.min = 0
	date.sec = 0
	date.zone = zone
	return date
end

return M
