local core = require("tng.core")
local graph = require("tng.graph")
-- Optional period of data to be displayed e.g. core.PERIOD.WEEK to only show 1 week of data
local period = core.PERIOD.WEEK

local function multi_input_text_output(totals)
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

	return {
		type = graph.GRAPH_TYPE.TEXT,
		text = text,
	}
end

return function(sources)
	local next_end = core.get_end_of_period(period, core.time().timestamp)
	local next_start = core.shift(next_end, period, -1)

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
		return {
			type = graph.GRAPH_TYPE.TEXT,
			text = totals[1].total,
		}
	else
		return multi_input_text_output(totals)
	end
end
