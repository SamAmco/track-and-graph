local graph = require("tng.graph")

--- PREVIEW_START
-- The threshold below which the last value is returned
local threshold = 0
--- PREVIEW_END

return function(sources)
  -- Track current datapoint for each source
  local current_points = {}
  local active_sources = {}
  local last_point_below = nil

  -- Initialize with first datapoint from each source
  for name, source in pairs(sources) do
    local dp = source:dp()
    if dp then
      current_points[name] = dp
      active_sources[name] = source
    end
  end

  -- Continue while we have active sources
  while next(active_sources) do
    -- Find most recent point below threshold from current batch
    for name, dp in pairs(current_points) do
      if dp.value < threshold then
        if not last_point_below or dp.timestamp > last_point_below.timestamp then
          last_point_below = dp
        end
      end

      -- Get next point from this source
      local next_dp = active_sources[name]:dp()
      if next_dp then
        current_points[name] = next_dp
      else
        -- Remove source if exhausted
        current_points[name] = nil
        active_sources[name] = nil
      end
    end

    -- Check if we found a point below threshold
    if last_point_below then
      return {
        type = graph.GRAPH_TYPE.DATA_POINT,
        datapoint = last_point_below,
      }
    end
  end

  return nil
end
