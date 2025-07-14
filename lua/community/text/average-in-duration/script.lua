local core = require("tng.core")
local graph = require("tng.graph")

--- PREVIEW_START
-- Script: Text - Average In Duration
-- Duration of data to average over prior to now (moving average)
-- e.g. core.DURATION.DAY * 7 for average over last 7 days
local duration = core.DURATION.DAY * 7
--- PREVIEW_END

return function(sources)
	local now = core.time().timestamp
	local cutoff = core.get_cutoff({
		period = duration,
		period_multiplier = 1
	}, now)

	local all_data_points = {}
	for name, source in pairs(sources) do
		local data_points = source.dpafter(cutoff)
		for _, dp in ipairs(data_points) do
			table.insert(all_data_points, dp)
		end
	end

	if #all_data_points == 0 then
		return nil
	end

	local total = 0
	for _, dp in ipairs(all_data_points) do
		total = total + dp.value
	end
	
	local average = total / #all_data_points
	local rounded = math.floor(average * 100 + 0.5) / 100
	return graph.text(tostring(rounded))
end
