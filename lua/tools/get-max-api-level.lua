#!/usr/bin/env lua
-- get-max-api-level.lua
-- Returns the highest API level defined across all API specs

-- Find all API spec files
local function find_api_spec_files()
	local specs = {}
	local handle = io.popen("find src/tng -name '*.apispec.lua' -type f")
	if not handle then
		error("Failed to scan tng directory")
	end

	for file in handle:lines() do
		table.insert(specs, file)
	end
	handle:close()

	return specs
end

-- Load an API spec file and return the table
local function load_api_spec(file_path)
	local file = io.open(file_path, "r")
	if not file then
		return nil, "Failed to open: " .. file_path
	end

	local content = file:read("*all")
	file:close()

	local chunk, load_err = load(content, file_path, "t")
	if not chunk then
		return nil, "Failed to load: " .. load_err
	end

	local ok, spec = pcall(chunk)
	if not ok then
		return nil, "Failed to execute: " .. spec
	end

	if type(spec) ~= "table" then
		return nil, "Must return a table"
	end

	return spec, nil
end

-- Find the maximum API level across all specs
local function find_max_api_level()
	local spec_files = find_api_spec_files()

	if #spec_files == 0 then
		io.stderr:write("ERROR: No API spec files found\n")
		os.exit(1)
	end

	local max_level = 0

	for _, spec_file in ipairs(spec_files) do
		local spec, err = load_api_spec(spec_file)
		if not spec then
			io.stderr:write(string.format("ERROR: %s - %s\n", spec_file, err))
			os.exit(1)
		end

		for _, api_level in pairs(spec) do
			if type(api_level) == "number" and api_level > max_level then
				max_level = api_level
			end
		end
	end

	return max_level
end

-- Main function
local function main()
	local max_level = find_max_api_level()
	print(max_level)
end

-- Run with error handling
local success, err = pcall(main)
if not success then
	io.stderr:write("FATAL ERROR: " .. tostring(err) .. "\n")
	os.exit(1)
end
