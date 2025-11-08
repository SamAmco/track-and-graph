local core = require("tng.core")
local enum = require("tng.config").enum
local uint = require("tng.config").uint

local get_aggregator_factory = function(config)
  local aggregation = require("tng.aggregation")
  local type = config.aggregation_type or error("aggregation_type required")
  local aggregator

  if type == "_min" then
    aggregator = aggregation.running_min_aggregator
  elseif type == "_max" then
    aggregator = aggregation.running_max_aggregator
  elseif type == "_average" then
    aggregator = aggregation.avg_aggregator
  elseif type == "_sum" then
    aggregator = aggregation.sum_aggregator
  elseif type == "_variance" then
    aggregator = aggregation.variance_aggregator
  elseif type == "_standard_deviation" then
    aggregator = aggregation.stdev_aggregator
  elseif type == "_count" then
    aggregator = aggregation.count_aggregator
  else
    error("Unknown aggregation_type " .. tostring(type))
  end

  return aggregator
end

local get_window = function(config)
  if type(config.window) ~= "string" then
    error("config.window is not a string")
  end

  if config.window == "_seconds" then
    return core.DURATION.SECOND
  elseif config.window == "_minutes" then
    return core.DURATION.MINUTE
  elseif config.window == "_hours" then
    return core.DURATION.HOUR
  elseif config.window == "_days" then
    return core.PERIOD.DAY
  elseif config.window == "_weeks" then
    return core.PERIOD.WEEK
  elseif config.window == "_months" then
    return core.PERIOD.MONTH
  elseif config.window == "_years" then
    return core.PERIOD.YEAR
  else
    error("Unknown window: " .. tostring(config.window))
  end
end

return {
  id = "adaptve-clustering-aggregation",
  version = "3.0.0",
  inputCount = 1,
  title = {
    ["en"] = "Adaptive Clustering",
  },
  categories = { "_aggregation" },
  description = {
    ["en"] = [[

    ]]
  },
  config = {
    enum {
      id = "aggregation_type",
      name = "_aggregation",
      options = { "_min", "_max", "_average", "_sum", "_variance", "_standard_deviation", "_count" },
      default = "_average"
    },
    uint {
      id = "multiplier",
      name = "_multiplier",
      default = 1
    },
    enum {
      id = "window",
      name = "_window_size",
      options = { "_seconds", "_minutes", "_hours", "_days", "_weeks", "_months", "_years", },
      default = "_weeks"
    },
  },
  generator = function(source, config)
    local agg_factory = get_aggregator_factory(config)
    local window = get_window(config)
    local multiplier = config.multiplier
    local carry = nil

    return function()
      local aggregator = agg_factory()

      local cutoff

      if carry ~= nil then
        aggregator:push(carry)
        cutoff = core.shift(carry, window, -multiplier)
        carry = nil
      else
        local ref = source.dp()

        if ref == nil then
          return nil
        end

        aggregator:push(ref)
        cutoff = core.shift(ref, window, -multiplier)
      end

      while true do
        local next_dp = source.dp()

        if next_dp == nil then break end

        if next_dp.timestamp >= cutoff.timestamp then
          aggregator:push(next_dp)
          cutoff = core.shift(next_dp, window, -multiplier)
        else
          carry = next_dp
          break
        end
      end

      return aggregator:run()
    end
  end
}
