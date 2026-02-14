--- This module provides various aggregators for computing aggregate statistics over a sliding window of data points. The aggregators are designed to be used for data points iterated in reverse chronological order (newest to oldest) where each data point is pushed to the aggregator instance as it enters the sliding window and popped as it exits the window. You can run the aggregator at any time to compute the current aggregate value over the data points in the window.

local M = {}

--- @class Aggregator
--- @field window DataPoint[] Array of data points in the sliding window
--- @field placement string|nil The position in the window for output data points ("start", "mid", or "end"). Defaults to "mid" for backwards compatibility.
--- @field push fun(self: Aggregator, data_point: DataPoint) Add a data point to the window
--- @field pop fun(self: Aggregator) Remove the oldest data point from the window
--- @field run fun(self: Aggregator): DataPoint Calculate the aggregate value for the current window and return as DataPoint
--- @field window_point fun(self: Aggregator, value: number): DataPoint Create a DataPoint with given value at the configured position in the window
local Aggregator = {}
Aggregator.__index = Aggregator

--- Add a data point to the window
--- @param data_point DataPoint The data point to add
function Aggregator:push(data_point)
  table.insert(self.window, data_point)
end

--- Remove the oldest data point from the window
function Aggregator:pop()
  if #self.window > 0 then
    table.remove(self.window, 1)
  end
end

--- Create a DataPoint with the given value at the configured position in the window.
--- The position is determined by the placement parameter: "start" uses the oldest data point,
--- "mid" uses the temporal midpoint, and "end" uses the newest data point.
--- @param value number The value to assign to the DataPoint
--- @return DataPoint: A new DataPoint at the configured position in the window
function Aggregator:window_point(value)
  if #self.window == 0 then
    error("Cannot compute window_point of empty window")
  end

  local newest_dp = self.window[1]
  local oldest_dp = self.window[#self.window]

  local ref_dp
  if self.placement == "start" then
    ref_dp = oldest_dp
  elseif self.placement == "mid" then
    local time_diff = newest_dp.timestamp - oldest_dp.timestamp
    local half_diff = math.floor(time_diff / 2)
    return {
      timestamp = oldest_dp.timestamp + half_diff,
      offset = oldest_dp.offset,
      value = value,
      label = newest_dp.label,
      note = newest_dp.note,
    }
  else -- "end" (default)
    ref_dp = newest_dp
  end

  return {
    timestamp = ref_dp.timestamp,
    offset = ref_dp.offset,
    value = value,
    label = newest_dp.label,
    note = newest_dp.note,
  }
end

--- Create a new aggregator with a custom aggregation function
--- @param on_aggregate fun(self: Aggregator): DataPoint Function that computes the aggregate value
--- @param placement string|nil The position in the window for output data points ("start", "mid", or "end"). Defaults to "mid" for backwards compatibility.
--- @return Aggregator: A new aggregator instance
local function new_aggregator(on_aggregate, placement)
  local self = setmetatable({
    window = {},
    placement = placement or "mid",
  }, Aggregator)
  self.run = on_aggregate
  return self
end

--- Create a monotonic aggregator (min/max) using a sliding window maximum/minimum algorithm
--- @param compare fun(a: number, b: number): boolean Comparison function that returns true if existing value `a` should be removed when new value `b` is added
--- @param placement string|nil The position in the window for output data points ("start", "mid", or "end"). Defaults to "mid" for backwards compatibility.
--- @return Aggregator: A new monotonic aggregator instance
local function monotonic_aggregator(compare, placement)
  -- compare(a, b) should return true if the existing/back value `a`
  -- should be removed when compared to the incoming/new value `b`.
  -- For a min-aggregator: compare = function(a,b) return a > b end
  -- For a max-aggregator: compare = function(a,b) return a < b end

  -- deque will hold indices into self.window
  local deque = {}

  local aggregator = new_aggregator(function(self)
    if #self.window == 0 then
      error("Cannot compute aggregate of empty window")
    end
    -- front of deque points to the index of the chosen element
    local idx = deque[1]
    return self:window_point(self.window[idx].value)
  end, placement)

  function aggregator:push(data_point)
    -- append to the end of the window (older datapoint)
    table.insert(self.window, data_point)
    local new_index = #self.window

    -- remove from back while existing back value should be removed
    while #deque > 0 do
      local back_idx = deque[#deque]
      if compare(self.window[back_idx].value, data_point.value) then
        table.remove(deque) -- pop back
      else
        break
      end
    end

    table.insert(deque, new_index)
  end

  function aggregator:pop()
    -- remove the first element of the window
    if #self.window == 0 then
      return
    end

    table.remove(self.window, 1) -- shift window left

    -- shift all deque indices left by 1 since window elements moved down
    for i = 1, #deque do
      deque[i] = deque[i] - 1
    end

    -- if the deque's head became 0 it was the removed element -> drop it
    if deque[1] == 0 then
      table.remove(deque, 1)
    end
  end

  return aggregator
end

--- Create a minimum value aggregator optimised for the running aggregation case where a sliding window is used. The current min is kept track of using a deque as data points enter and exit the window.
--- @param placement string|nil The position in the window for output data points ("start", "mid", or "end"). Defaults to "mid" for backwards compatibility.
--- @return Aggregator: An aggregator that computes the minimum value in the sliding window
--- @since API level 4
M.running_min_aggregator = function(placement)
  return monotonic_aggregator(function(a, b)
    return a > b
  end, placement)
end

--- Create a maximum value aggregator optimised for the running aggregation case where a sliding window is used. The current max is kept track of using a deque as data points enter and exit the window.
--- @param placement string|nil The position in the window for output data points ("start", "mid", or "end"). Defaults to "mid" for backwards compatibility.
--- @return Aggregator An aggregator that computes the maximum value in the sliding window
--- @since API level 4
M.running_max_aggregator = function(placement)
  return monotonic_aggregator(function(a, b)
    return a < b
  end, placement)
end

--- Create a simple min aggregator that iterates all data points in the current window to find the minimum value whenever run() is called. This is marginally more efficient than the running min aggregator if you will not be sliding the window over a data set.
--- @param placement string|nil The position in the window for output data points ("start", "mid", or "end"). Defaults to "mid" for backwards compatibility.
--- @return Aggregator: An aggregator that computes the minimum value in the sliding window
--- @since API level 4
M.simple_min_aggregator = function(placement)
  local aggregator = new_aggregator(function(self)
    if #self.window == 0 then
      error("Cannot compute minimum of empty window")
    end
    local min_value = self.window[1].value
    for i = 2, #self.window do
      if self.window[i].value < min_value then
        min_value = self.window[i].value
      end
    end
    return self:window_point(min_value)
  end, placement)

  return aggregator
end

--- Create a simple max aggregator that iterates all data points in the current window to find the maximum value whenever run() is called. This is marginally more efficient than the running max aggregator if you will not be sliding the window over a data set.
--- @param placement string|nil The position in the window for output data points ("start", "mid", or "end"). Defaults to "mid" for backwards compatibility.
--- @return Aggregator: An aggregator that computes the maximum value in the sliding window
--- @since API level 4
M.simple_max_aggregator = function(placement)
  local aggregator = new_aggregator(function(self)
    if #self.window == 0 then
      error("Cannot compute maximum of empty window")
    end
    local max_value = self.window[1].value
    for i = 2, #self.window do
      if self.window[i].value > max_value then
        max_value = self.window[i].value
      end
    end
    return self:window_point(max_value)
  end, placement)
  return aggregator
end

--- Create an average value aggregator that returns the arithmetic mean of values in the sliding window
--- @param placement string|nil The position in the window for output data points ("start", "mid", or "end"). Defaults to "mid" for backwards compatibility.
--- @return Aggregator: An aggregator that computes the arithmetic mean of values in the sliding window
--- @since API level 4
M.avg_aggregator = function(placement)
  local current_count = 0
  local current_sum = 0

  local aggregator = new_aggregator(function(self)
    if current_count == 0 then
      error("Cannot compute average of empty window")
    end
    local avg_value = current_sum / current_count
    return self:window_point(avg_value)
  end, placement)

  function aggregator:push(data_point)
    table.insert(self.window, data_point)
    current_count = current_count + 1
    current_sum = current_sum + data_point.value
  end

  function aggregator:pop()
    if #self.window > 0 then
      local removed_dp = table.remove(self.window, 1)
      current_count = current_count - 1
      current_sum = current_sum - removed_dp.value
    end
  end

  return aggregator
end

--- Create a sum aggregator that returns the sum of all values in the sliding window
--- @param placement string|nil The position in the window for output data points ("start", "mid", or "end"). Defaults to "mid" for backwards compatibility.
--- @return Aggregator: An aggregator that computes the sum of all values in the sliding window
--- @since API level 4
M.sum_aggregator = function(placement)
  local current_sum = 0

  local aggregator = new_aggregator(function(self)
    return self:window_point(current_sum)
  end, placement)

  function aggregator:push(data_point)
    table.insert(self.window, data_point)
    current_sum = current_sum + data_point.value
  end

  function aggregator:pop()
    if #self.window > 0 then
      local removed_dp = table.remove(self.window, 1)
      current_sum = current_sum - removed_dp.value
    end
  end

  return aggregator
end

--- Create a variance aggregator that returns the population variance of values in the sliding window
--- @param placement string|nil The position in the window for output data points ("start", "mid", or "end"). Defaults to "mid" for backwards compatibility.
--- @return Aggregator: An aggregator that computes the population variance of values in the sliding window
--- @since API level 4
M.variance_aggregator = function(placement)
  local sum_x = 0
  local sum_x2 = 0
  local count = 0

  local agg = new_aggregator(function(self)
    if #self.window == 0 then error("Cannot compute variance of empty window") end
    local mean = sum_x / count
    local variance = math.max(0, (sum_x2 / count) - (mean * mean))
    return self:window_point(variance)
  end, placement)

  function agg:push(dp)
    table.insert(self.window, dp)
    sum_x = sum_x + dp.value
    sum_x2 = sum_x2 + (dp.value * dp.value)
    count = count + 1
  end

  function agg:pop()
    if #self.window > 0 then
      local removed = table.remove(self.window, 1)
      sum_x = sum_x - removed.value
      sum_x2 = sum_x2 - (removed.value * removed.value)
      count = count - 1
    end
  end

  return agg
end

--- Create a standard deviation aggregator
--- @param placement string|nil The position in the window for output data points ("start", "mid", or "end"). Defaults to "mid" for backwards compatibility.
--- @return Aggregator: An aggregator that computes the population standard deviation of values in the sliding window
--- @since API level 4
M.stdev_aggregator = function(placement)
  local var_agg = M.variance_aggregator(placement)
  local old_run = var_agg.run
  function var_agg:run()
    local var_dp = old_run(self)
    var_dp.value = math.sqrt(var_dp.value)
    return var_dp
  end

  return var_agg
end

--- Create a count aggregator that returns the number of data points in the sliding window
--- @param placement string|nil The position in the window for output data points ("start", "mid", or "end"). Defaults to "mid" for backwards compatibility.
--- @return Aggregator: An aggregator that counts the number of data points in the sliding window
--- @since API level 4
M.count_aggregator = function(placement)
  return new_aggregator(function(self)
    return self:window_point(#self.window)
  end, placement)
end


return M
