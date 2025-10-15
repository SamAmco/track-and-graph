#!/usr/bin/env lua
-- validate-function-categories.lua
-- Validates that all function categories are defined and all defined categories are used

local validation = require("tools.lib.validation")
local traversal = require("tools.lib.file-traversal")

local CATEGORY_FILE = "src/community/function-categories.lua"

-- Load categories
--- @return table Set of defined categories
local function load_categories()
	local content = traversal.read_file(CATEGORY_FILE)
	local ok, categories = traversal.load_module(content, CATEGORY_FILE)
	if not ok or type(categories) ~= "table" then
		error("Failed to load categories: " .. categories)
	end
	return categories
end

-- Main function
local function main()
	print("Validating function categories...\n")

	-- Load categories
	local categories = load_categories()
	local category_count = 0
	for _ in pairs(categories) do
		category_count = category_count + 1
	end
	print("Loaded " .. category_count .. " defined categor" .. (category_count == 1 and "y" or "ies"))

	-- Load all functions
	local files = traversal.find_scripts(traversal.SCRIPT_TYPE.FUNCTIONS)

	if #files == 0 then
		error("✗ No function files found")
	end

	print("Found " .. #files .. " function file(s)\n")

	-- Load function modules (just need categories field)
	local functions = {}
	for _, file_path in ipairs(files) do
		local ok, module = traversal.read_and_load(file_path)
		if ok then
			table.insert(functions, module)
		else
			io.stderr:write("Warning: Failed to load " .. file_path .. ": " .. tostring(module) .. "\n")
		end
	end

	-- Validate categories
	local undefined_categories = validation.collect_undefined_categories(functions, categories)
	local unused_categories = validation.collect_unused_categories(categories, functions)

	local has_errors = false

	-- Report undefined categories
	if #undefined_categories > 0 then
		has_errors = true
		print("✗ Undefined categories found:")
		for _, category in ipairs(undefined_categories) do
			print("  • " .. category)
		end
		print("\nAdd these to" .. CATEGORY_FILE .. "\n")
	end

	-- Report unused categories
	if #unused_categories > 0 then
		has_errors = true
		print("✗ Unused categories found:")
		for _, category in ipairs(unused_categories) do
			print("  • " .. category)
		end
		print("\nRemove these from src/community/function-categories.lua\n")
	end

	-- Summary
	if has_errors then
		local error_count = #undefined_categories + #unused_categories
		print(string.format("ERROR: Category validation failed with %d issue(s)", error_count))
		os.exit(1)
	else
		print("✓ All categories valid")
		print(string.format("  • %d categor%s defined", category_count, category_count == 1 and "y" or "ies"))
		print(string.format("  • All categories are used"))
		print(string.format("  • All functions define valid categories"))
		os.exit(0)
	end
end

-- Run with error handling
local success, err = pcall(main)
if not success then
	io.stderr:write("FATAL ERROR: " .. tostring(err) .. "\n")
	os.exit(1)
end
