local graph = require("tng.graph")
local renderer = require("renderer")

local function first_source(sources)
	if sources[1] then
		return sources[1]
	end
	for _, source in pairs(sources) do
		return source
	end
	error("The canvas graph needs one data source")
end

local function read_values(source)
	local values = {}
	while true do
		local data_point = source.dp()
		if data_point == nil then
			break
		end
		table.insert(values, 1, data_point.value)
	end
	return values
end

return {
	id = "canvas-basic",
	version = "2.0.0",
	title = {
		en = "Basic canvas graph",
	},
	purpose = graph.TYPE,
	type = graph.GRAPH_TYPE.CUSTOM,
	data = function(sources)
		return { values = read_values(first_source(sources)) }
	end,
	render = renderer,
}
