package.path = package.path .. ";src/?.lua;src/?/init.lua"

local traversal = require("tools.lib.file-traversal")
local json = require("tools.lib.json")

local mode = arg[1]
local requested = arg[2]
if mode ~= "functions" and mode ~= "shared" then
	error("Usage: lua tools/export-translatable-copy.lua functions [FUNCTION_ID] | shared [KEY]")
end

local function english(value, context)
	if type(value) == "string" then
		if value:match("^_") then return nil end
		return value
	end
	if type(value) ~= "table" or type(value.en) ~= "string" or value.en == "" then
		error(context .. " must be a shared key, English string, or translation table with English")
	end
	return value.en
end

local output = { kind = mode, items = {} }
if mode == "shared" then
	local records = require("community.shared-translations-data")
	for _, record in ipairs(records) do
		if not requested or record._id == requested then
			table.insert(output.items, { id = record._id, source = english(record, record._id), markdown = false, existing = record })
		end
	end
	if requested and #output.items == 0 then error("Unknown shared translation key: " .. requested) end
else
	local files = traversal.find_scripts(traversal.SCRIPT_TYPE.FUNCTIONS)
	table.sort(files)
	for _, path in ipairs(files) do
		local ok, fn = traversal.read_and_load(path)
		if not ok then error(fn) end
		if not requested or fn.id == requested then
			local fields = {
				{ id = "title", source = english(fn.title, fn.id .. " title"), markdown = false },
				{ id = "description", source = english(fn.description, fn.id .. " description"), markdown = true },
			}
			for index, config in ipairs(fn.config or {}) do
				local source = english(config.name, fn.id .. " config name " .. index)
				if source then
					local config_id = config.id or tostring(index)
					table.insert(fields, { id = "config/" .. config_id .. "/name", source = source, markdown = false })
				end
			end
			table.insert(output.items, { id = fn.id, path = path, fields = fields })
		end
	end
	if requested and #output.items == 0 then error("Unknown function: " .. requested) end
end

io.write(json.encode(output), "\n")
