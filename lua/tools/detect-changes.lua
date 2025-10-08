#!/usr/bin/env lua
-- detect-changes.lua
-- Detects changes in community functions compared to the published catalog

local changes = require("tools.lib.changes")
local versioning = require("tools.lib.catalog-versioning")

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

-- Load a function file
local function load_function(file_path)
	local content = read_file(file_path)
	local chunk, load_err = load(content, file_path, "t")
	if not chunk then
		error("Failed to load " .. file_path .. ": " .. load_err)
	end

	local success, module = pcall(chunk)
	if not success then
		error("Failed to execute " .. file_path .. ": " .. module)
	end

	return {
		id = module.id,
		version = module.version,
		script = content,
		file_path = file_path
	}
end

-- Main function
local function main()
	print("Detecting changes in community functions...\n")

	-- Load existing catalog
	local old_catalog = versioning.load_catalog("catalog/community-functions.lua")
	if old_catalog then
		print("Loaded existing catalog:")
		print("  Published: " .. (old_catalog.published_at or "unknown"))
		print("  Functions: " .. #old_catalog.functions)
	else
		print("No existing catalog found (first run)")
	end

	-- Load all current functions
	local files = find_lua_files("src/community/functions")
	local new_functions = {}

	for _, file_path in ipairs(files) do
		local func = load_function(file_path)
		table.insert(new_functions, func)
	end

	print("\nCurrent functions: " .. #new_functions)

	-- Compare
	local report = changes.compare_functions(old_catalog, new_functions)

	-- Print report
	print("\n" .. string.rep("=", 60))
	print("Change Report")
	print(string.rep("=", 60))

	if #report.new > 0 then
		print(string.format("\nNew functions: %d", #report.new))
		for _, func in ipairs(report.new) do
			print(string.format("  + %s v%s", func.id, func.version))
		end
	end

	if #report.modified > 0 then
		print(string.format("\nModified functions: %d", #report.modified))
		for _, func in ipairs(report.modified) do
			print(string.format("  ~ %s v%s → v%s", func.id, func.old_version, func.new_version))
		end
	end

	if #report.unchanged > 0 then
		print(string.format("\nUnchanged functions: %d", #report.unchanged))
		for _, func in ipairs(report.unchanged) do
			print(string.format("  = %s v%s", func.id, func.version))
		end
	end

	-- Validate version increments
	if #report.modified > 0 then
		print("\n" .. string.rep("-", 60))
		print("Validating version increments...")
		local ok, errors = changes.validate_version_increments(report)

		if not ok then
			print("\nErrors:")
			for _, error in ipairs(errors) do
				print("  ✗ " .. error)
			end
			print("\n✗ Version validation failed")
			os.exit(1)
		else
			print("✓ All version increments valid")
		end
	end

	-- Summary
	print("\n" .. string.rep("=", 60))
	local should_write, reason = versioning.should_write_catalog(report)
	if should_write then
		print("✓ Catalog should be regenerated: " .. reason)
	else
		print("✓ " .. reason)
	end
	print(string.rep("=", 60))
end

-- Run with error handling
local success, err = pcall(main)
if not success then
	io.stderr:write("ERROR: " .. tostring(err) .. "\n")
	os.exit(1)
end
