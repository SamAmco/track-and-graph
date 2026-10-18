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

return function(sources)
	return { values = read_values(first_source(sources)) }
end
