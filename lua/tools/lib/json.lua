local M = {}

local function escape_string(value)
	return value:gsub('[%z\1-\31\\"]', function(character)
		local escapes = { ['\\'] = '\\\\', ['"'] = '\\"', ['\b'] = '\\b', ['\f'] = '\\f', ['\n'] = '\\n', ['\r'] = '\\r', ['\t'] = '\\t' }
		return escapes[character] or string.format("\\u%04x", character:byte())
	end)
end

local function is_array(value)
	local count = 0
	for key in pairs(value) do
		if type(key) ~= "number" or key < 1 or key % 1 ~= 0 then return false end
		count = math.max(count, key)
	end
	for index = 1, count do
		if value[index] == nil then return false end
	end
	return true, count
end

local function encode(value)
	local kind = type(value)
	if kind == "nil" then return "null" end
	if kind == "boolean" or kind == "number" then return tostring(value) end
	if kind == "string" then return '"' .. escape_string(value) .. '"' end
	if kind ~= "table" then error("Cannot encode JSON value of type " .. kind) end

	local array, count = is_array(value)
	local parts = {}
	if array then
		for index = 1, count do parts[index] = encode(value[index]) end
		return "[" .. table.concat(parts, ",") .. "]"
	end
	for key, item in pairs(value) do
		if type(key) ~= "string" then error("JSON object keys must be strings") end
		table.insert(parts, encode(key) .. ":" .. encode(item))
	end
	table.sort(parts)
	return "{" .. table.concat(parts, ",") .. "}"
end

M.encode = encode
return M
