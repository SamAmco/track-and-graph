#!/usr/bin/env lua
-- get-max-api-level.lua
-- Returns the highest API level defined across all API specs

local api_specs = require("tools.lib.api-specs")

local function main()
	local max_level, err = api_specs.get_max_level()

	if not max_level then
		io.stderr:write("ERROR: " .. err .. "\n")
		os.exit(1)
	end

	print(max_level)
end

-- Run with error handling
local success, err = pcall(main)
if not success then
	io.stderr:write("FATAL ERROR: " .. tostring(err) .. "\n")
	os.exit(1)
end
