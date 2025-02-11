-- GENERATED FILE: DO NOT EDIT MANUALLY

Tng = {}

Tng.time = {}
--- Useful time constants. All timestamps are in milliseconds.
Tng.time.D_SECOND = 1000                   -- One second in milliseconds
Tng.time.D_MINUTE = 60 * Tng.time.D_SECOND -- One minute in milliseconds
Tng.time.D_HOUR = 60 * Tng.time.D_MINUTE   -- One hour in milliseconds
Tng.time.D_DAY = 24 * Tng.time.D_HOUR      -- One day in milliseconds
Tng.time.D_WEEK = 7 * Tng.time.D_DAY       -- One week in milliseconds

--- Period constants for use with addperiod function.
Tng.time.P_DAY = "P1D"   -- One day period
Tng.time.P_WEEK = "P1W"  -- One week period
Tng.time.P_MONTH = "P1M" -- One month period
Tng.time.P_YEAR = "P1Y"  -- One year period

--- Data point structure:
--- @class datapoint
--- @field timestamp number: The Unix epoch millisecond timestamp of the data point.
--- @field offset number: The offset from UTC in seconds. This allows you to know what the local time was when the data point was recorded.
--- @field featureId string: The database ID of the feature (aka data source).
--- @field value number: The value of the data point.
--- @field label string: The label of the data point.
--- @field note string: The note of the data point.

--- Timestamp structure:
--- @class timestamp
--- @field timestamp number: The Unix epoch millisecond timestamp.
--- @field offset number: The offset from UTC in milliseconds.

--- Accepts any table with a timestamp field and returns a copy with the timestamp field adjusted by the given time in milliseconds.
--- Using durations is faster than using periods, but it will not respect daylight savings time changes and will not be accurate for periods that do not have a constant number of milliseconds (e.g. months, years). See addperiod for this.
--- @param timestamp table: The table containing a timestamp field to be adjusted.
--- @param milliseconds number: The number of milliseconds to adjust the timestamp by.
--- @return table: A new table with the adjusted timestamp.
Tng.time.addduration = function(timestamp, milliseconds)
  -- Copy the datapoint and add milliseconds to the timestamp if it exists
  local newDatapoint = {}
  for k, v in pairs(timestamp) do
    if k == "timestamp" then
      newDatapoint[k] = v + milliseconds
    else
      newDatapoint[k] = v
    end
  end
  return newDatapoint
end

--- Accepts any table with timestamp and offset fields and returns a copy with the timestamp/offset adjusted by the given period. This function is useful for adjusting timestamps by periods that do not have a constant number of milliseconds (e.g. months, years).
--- @class addperiodparams
--- @field timestamp table: The table containing a timestamp field to be adjusted.
--- @field period string: The period to adjust the timestamp by. Supported values are Tng.time.P_DAY, Tng.time.P_WEEK, Tng.time.P_MONTH, and Tng.time.P_YEAR.
--- @field amount number (optional): The number of periods to adjust the timestamp by. Can be negative to subtract periods. Defaults to 1
--- @field zone string (optional): The time zone to use for the adjustment. Defaults to the local time zone. If you provide a zone, the offset will be ignored.
--- @param params table: The parameters for the function.
Tng.time.addperiod = function(params) end

--- Returns the current time as a timestamp in milliseconds.
--- @return timestamp: The current time as a timestamp.
Tng.time.now = function() end

--- Takes any table and a timestamp as parameters and returns a copy of that table with the timestamp and offset fields overridden (or added if they don't exist).
--- @param tbl table: The table to be copied and modified.
--- @param timestamp number (optional): The Unix epoch millisecond
--- @param offset number (optional): The offset from UTC in milliseconds.
--- @return table: A new table with the timestamp and offset fields set.
Tng.time.withtime = function(tbl, timestamp, offset) end

Tng.graph = {}

--- Graph types
Tng.graph.DATAPOINT = "datapoint"

--- Fetches the next data point from the data source.
--- @param name string: The name of the data source.
--- @see datapoint
--- @return datapoint
Tng.graph.nextdp = function(name) end

--- Fetches the next group of data points from the data source.
--- @param name string: The name of the data source.
--- @param count number: The number of data points to retrieve.
--- @see datapoint
--- @return table: A table containing the requested data points.
Tng.graph.nextdpbatch = function(name, count) end

return Tng
