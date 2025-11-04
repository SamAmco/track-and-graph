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

local function string_list(lst)
	if not lst or #lst == 0 then
		return "none"
	end
	return table.concat(lst, ", ")
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
	print("CATEGORIES: " .. string_list(func.categories))
	print()
	print("DESCRIPTION:")
	print(description)
	print()
end

-- Parse command line arguments
local function parse_args()
	local api_level = nil

	if arg[1] then
		api_level = tonumber(arg[1])
		if not api_level or api_level < 1 or api_level % 1 ~= 0 then
			io.stderr:write("Error: API level must be a positive integer\n")
			io.stderr:write("Usage: lua tools/print-catalog.lua [api_level]\n")
			os.exit(1)
		end
	end

	return api_level
end

-- Filter functions by API level compatibility
local function filter_by_api_level(functions, api_level)
	if not api_level then
		return functions
	end

	local filtered = {}
	for _, func in ipairs(functions) do
		local semver = require("tools.lib.semver")
		local version = semver.parse(func.version)

		if version and version.major <= api_level then
			-- Check if deprecated at this level
			local is_deprecated = func.deprecated ~= nil and func.deprecated <= api_level
			if not is_deprecated then
				table.insert(filtered, func)
			end
		end
	end

	return filtered
end

-- Main function
local function main()
	local api_level = parse_args()

	print("\n")
	print_separator()
	print("TRACK & GRAPH - COMMUNITY FUNCTIONS CATALOG")
	if api_level then
		print("FILTERED BY API LEVEL: " .. api_level)
	end
	print_separator()
	print()

	local files = traversal.find_scripts(traversal.SCRIPT_TYPE.FUNCTIONS)

	if #files == 0 then
		print("No function files found")
		os.exit(1)
	end

	-- Load all functions
	local all_functions = {}
	for _, file_path in ipairs(files) do
		local ok, module = traversal.read_and_load(file_path)
		if ok and module then
			table.insert(all_functions, module)
		else
			io.stderr:write("Warning: Failed to load " .. file_path .. ": " .. tostring(module) .. "\n")
		end
	end

	-- Filter by API level if specified
	local functions = filter_by_api_level(all_functions, api_level)

	sort_by_title(functions)

	print("Total functions loaded: " .. #all_functions)
	if api_level then
		print("Functions compatible with API level " .. api_level .. ": " .. #functions)
		local filtered_out = #all_functions - #functions
		if filtered_out > 0 then
			print("  (" .. filtered_out .. " filtered out due to version/deprecation)")
		end
	else
		print("  (no API level filter applied)")
	end
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
