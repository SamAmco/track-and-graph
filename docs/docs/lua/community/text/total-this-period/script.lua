local core = require("tng.core")
local graph = require("tng.graph")

--- PREVIEW_START
-- Script: Text - Total This Period
-- Period of data to be displayed e.g. core.PERIOD.WEEK to show data for this week
local period = core.PERIOD.WEEK
-- Multiplier for the period (e.g. 2 for this 2 weeks when period is WEEK)
local multiplier = 1
-- Text size (1=small, 2=medium, 3=large). If nil, uses smart defaults.
local text_size = nil
--- PREVIEW_END

local function multi_input_text_output(totals, size)
	table.sort(totals, function(a, b)
		return a.total > b.total
	end)
	local total_of_all = 0
	for _, total in ipairs(totals) do
		total_of_all = total_of_all + total.total
	end

	local text = total_of_all .. "\n"
	for i, entry in ipairs(totals) do
		text = text .. entry.name .. ": " .. entry.total
		if i < #totals then
			text = text .. "\n"
		end
	end

	return graph.text({
		text = text,
		size = size
	})
end

return function(sources)
	local next_end = core.get_end_of_period(period, core.time().timestamp)
	local next_start = core.shift(next_end, period, -multiplier)

	local data = {}
	for name, source in pairs(sources) do
		local data_points = source.dpafter(next_start)
		local total = 0
		for _, dp in ipairs(data_points) do
			total = total + dp.value
		end
		data[name] = total
	end

	local totals = {}
	for name, total in pairs(data) do
		table.insert(totals, { name = name, total = total })
	end

	if #totals == 0 then
		return nil
	elseif #totals == 1 then
		local size = text_size or 2  -- Default to medium for single source
		return graph.text({
			text = totals[1].total,
			size = size
		})
	else
		local size = text_size or 1  -- Default to small for multiple sources
		return multi_input_text_output(totals, size)
	end
end
