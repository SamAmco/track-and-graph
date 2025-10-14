#!/usr/bin/env lua
-- print-catalog.lua
-- Prints all community functions with their English titles and descriptions

local traversal = require("tools.lib.file-traversal")

-- Sort functions alphabetically by English title
local function sort_by_title(functions)
	table.sort(functions, function(a, b)
		local title_a = (a.title and a.title["en"]) or a.id or ""
		local title_b = (b.title and b.title["en"]) or b.id or ""
		return title_a < title_b
	end)
end

-- Print separator line
local function print_separator()
	print(string.rep("=", 80))
end

-- Print a single function's details
local function print_function(func)
	local title = (func.title and func.title["en"]) or func.id or "Unknown"
	local description = (func.description and func.description["en"]) or "No description available"

	print_separator()
	print("TITLE: " .. title)
	print("ID: " .. (func.id or "unknown"))
	print("VERSION: " .. (func.version or "unknown"))
	print()
	print("DESCRIPTION:")
	print(description)
	print()
end

-- Main function
local function main()
	print("\n")
	print_separator()
	print("TRACK & GRAPH - COMMUNITY FUNCTIONS CATALOG")
	print_separator()
	print()

	local files = traversal.find_scripts(traversal.SCRIPT_TYPE.FUNCTIONS)

	if #files == 0 then
		print("No function files found")
		os.exit(1)
	end

	-- Load all functions
	local functions = {}
	for _, file_path in ipairs(files) do
		local ok, module = traversal.read_and_load(file_path)
		if ok and module then
			table.insert(functions, module)
		else
			io.stderr:write("Warning: Failed to load " .. file_path .. ": " .. tostring(module) .. "\n")
		end
	end

	sort_by_title(functions)

	print("Total functions: " .. #functions)
	print()

	for _, func in ipairs(functions) do
		print_function(func)
	end

	print_separator()
	print("END OF CATALOG")
	print_separator()
	print()
end

-- Run with error handling
local success, err = pcall(main)
if not success then
	io.stderr:write("FATAL ERROR: " .. tostring(err) .. "\n")
	os.exit(1)
end
