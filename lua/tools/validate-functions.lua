#!/usr/bin/env lua
-- validate-functions.lua
-- Validates all community Lua functions

local validation = require("tools.lib.validation")

-- Find all non-test lua files in a directory
local function find_lua_files(dir)
	local files = {}
	local handle = io.popen("find " .. dir .. " -type f -name '*.lua' ! -name 'test_*'")
	if not handle then
		error("Failed to scan directory: " .. dir)
	end

	for file in handle:lines() do
		table.insert(files, file)
	end
	handle:close()

	return files
end

-- Read entire file
local function read_file(path)
	local file = io.open(path, "r")
	if not file then
		error("Could not open file: " .. path)
	end
	local content = file:read("*all")
	file:close()
	return content
end

-- Validate a single function file
local function validate_file(file_path)
	local content = read_file(file_path)

	-- Load and execute the file to get the module
	local chunk, load_err = load(content, file_path, "t")
	if not chunk then
		return false, {"Failed to load: " .. load_err}
	end

	local success, module = pcall(chunk)
	if not success then
		return false, {"Failed to execute: " .. module}
	end

	-- Validate structure
	return validation.validate_function(module, file_path)
end

-- Main function
local function main()
	print("Validating community functions...")

	local files = find_lua_files("src/community/functions")

	if #files == 0 then
		print("✗ No function files found in src/community/functions")
		os.exit(1)
	end

	table.sort(files)

	local total_errors = 0
	local functions_for_uniqueness = {}

	-- Validate each file
	for _, file_path in ipairs(files) do
		local ok, errors = validate_file(file_path)

		if ok then
			print("✓ " .. file_path)

			-- Load module again for uniqueness check
			local content = read_file(file_path)
			local module = load(content, file_path, "t")()
			table.insert(functions_for_uniqueness, {
				id = module.id,
				title = module.title,
				file_path = file_path
			})
		else
			print("✗ " .. file_path)
			for _, err in ipairs(errors) do
				print("  " .. err)
			end
			total_errors = total_errors + #errors
		end
	end

	-- Check uniqueness across all functions
	if #functions_for_uniqueness > 1 then
		local ok, errors = validation.check_uniqueness(functions_for_uniqueness)
		if not ok then
			print("\nUniqueness violations:")
			for _, err in ipairs(errors) do
				print("  " .. err)
			end
			total_errors = total_errors + #errors
		end
	end

	print()
	if total_errors > 0 then
		print(string.format("ERROR: Validation failed with %d error(s)", total_errors))
		os.exit(1)
	else
		print(string.format("✓ All %d function(s) valid", #files))
		os.exit(0)
	end
end

-- Run with error handling
local success, err = pcall(main)
if not success then
	io.stderr:write("FATAL ERROR: " .. tostring(err) .. "\n")
	os.exit(1)
end
