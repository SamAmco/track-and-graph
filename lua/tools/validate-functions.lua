#!/usr/bin/env lua
-- validate-functions.lua
-- Validates all community Lua functions

local validation = require("tools.lib.validation")
local traversal = require("tools.lib.file-traversal")

-- Validate a single function file
local function validate_file(file_path)
	local ok, module = traversal.read_and_load(file_path)
	if not ok then
		return false, {module}  -- module contains error message
	end

	-- Validate structure
	return validation.validate_function(module, file_path)
end

-- Main function
local function main()
	print("Validating community functions...")

	local files = traversal.find_scripts(traversal.SCRIPT_TYPE.FUNCTIONS)

	if #files == 0 then
		print("✗ No function files found")
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
			local _, module = traversal.read_and_load(file_path)
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
