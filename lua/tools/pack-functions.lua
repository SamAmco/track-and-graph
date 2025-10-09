#!/usr/bin/env lua
-- pack-functions.lua
-- Packs community Lua functions into a single distributable catalog

local serpent = require("serpent")
local validation = require("tools.lib.validation")
local semver = require("tools.lib.semver")
local changes = require("tools.lib.changes")
local versioning = require("tools.lib.catalog-versioning")
local api_specs = require("tools.lib.api-specs")

-- Configuration
local FUNCTIONS_DIR = "src/community/functions"
local CATALOG_PATH = "catalog/community-functions.lua"

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

-- Write content to file
local function write_file(path, content)
	local file = io.open(path, "w")
	if not file then
		error("Could not open output file: " .. path)
	end
	file:write(content)
	file:close()
end

-- Verify API specs and print results
local function verify_and_print_api_specs()
	local ok, result = api_specs.verify()

	if not ok and result.error then
		print("✗ " .. result.error)
		error("API spec verification failed")
	end

	-- Print results for each module
	for _, module_result in ipairs(result.modules) do
		if #module_result.errors > 0 then
			print(string.format("✗ %s.lua - %d errors:", module_result.name, #module_result.errors))
			for _, error_msg in ipairs(module_result.errors) do
				print(error_msg)
			end
		else
			print(
				string.format("✓ %s.lua - all %d exports have specs", module_result.name, module_result.export_count)
			)
		end
	end

	if result.total_errors > 0 then
		error(string.format("API spec validation failed with %d error(s)", result.total_errors))
	end

	print(
		string.format("✓ All API specs valid (%d exports across %d modules)", result.total_exports, #result.modules)
	)
end

-- Get max API level
local function get_max_api_level()
	local level, err = api_specs.get_max_level()
	if not level then
		error("Failed to get max API level: " .. err)
	end
	return level
end

-- Load and validate a single function
local function load_and_validate_function(file_path, max_api_level)
	local content = read_file(file_path)

	-- Load module
	local chunk, load_err = load(content, file_path, "t")
	if not chunk then
		return false, "Failed to load: " .. load_err
	end

	local success, module = pcall(chunk)
	if not success then
		return false, "Failed to execute: " .. module
	end

	-- Validate structure
	local ok, errors = validation.validate_function(module, file_path)
	if not ok then
		return false, table.concat(errors, "\n")
	end

	-- Check API level compatibility
	local version = semver.parse(module.version) or error("Invalid semver in " .. file_path)
	if version.major < 1 or version.major > max_api_level then
		return false,
			string.format(
				"%s: version major %d must be between 1 and %d (max API level)",
				file_path,
				version.major,
				max_api_level
			)
	end

	return true,
		{
			id = module.id,
			version = module.version,
			script = content,
			file_path = file_path,
			title = module.title,
		}
end

-- Print change summary
local function print_change_summary(report)
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
	end
end

-- Main execution
local function main()
	print("==> Phase 1: API Spec Verification")
	verify_and_print_api_specs()
	local max_api_level = get_max_api_level()
	print("Max API level: " .. max_api_level)

	print("\n==> Phase 2: Loading Functions")
	local files = find_lua_files(FUNCTIONS_DIR)

	if #files == 0 then
		error("No function files found in " .. FUNCTIONS_DIR)
	end

	print("Found " .. #files .. " function file(s)")

	local functions = {}
	local all_errors = {}

	for _, file_path in ipairs(files) do
		local ok, func_or_err = load_and_validate_function(file_path, max_api_level)
		if ok then
			table.insert(functions, func_or_err)
			print("  ✓ " .. file_path)
		else
			print("  ✗ " .. file_path)
			print("    " .. func_or_err)
			table.insert(all_errors, func_or_err)
		end
	end

	if #all_errors > 0 then
		error(string.format("\nValidation failed with %d error(s)", #all_errors))
	end

	-- Check uniqueness
	local ok, errors = validation.check_uniqueness(functions)
	if not ok then
		print("\nUniqueness errors:")
		for _, err in ipairs(errors) do
			print("  " .. err)
		end
		error("Uniqueness validation failed")
	end

	print("\n==> Phase 3: Change Detection")
	local old_catalog = versioning.load_catalog(CATALOG_PATH)

	if old_catalog then
		print("Loaded existing catalog (published: " .. (old_catalog.published_at or "unknown") .. ")")
	else
		print("No existing catalog found (first run)")
	end

	local change_report = changes.compare_functions(old_catalog, functions)
	print_change_summary(change_report)

	-- Validate version increments for modified functions
	if #change_report.modified > 0 then
		print("\nValidating version increments...")
		local ok, errors = changes.validate_version_increments(change_report)
		if not ok then
			print("\nVersion errors:")
			for _, err in ipairs(errors) do
				print("  " .. err)
			end
			error("Version validation failed")
		end
		print("✓ All version increments valid")
	end

	print("\n==> Phase 4: Catalog Generation")
	local should_write, reason = versioning.should_write_catalog(change_report)

	if not should_write then
		print("✓ " .. reason)
		print("✓ No catalog changes needed")
		return
	end

	print("Building new catalog: " .. reason)

	-- Build function list for catalog (sorted by id)
	local catalog_functions = {}
	for _, func in ipairs(functions) do
		table.insert(catalog_functions, {
			id = func.id,
			version = func.version,
			script = func.script,
		})
	end
	table.sort(catalog_functions, function(a, b)
		return a.id < b.id
	end)

	-- Create catalog
	local catalog = {
		published_at = versioning.generate_timestamp(),
		functions = catalog_functions,
	}

	-- Create catalog directory if it doesn't exist
	os.execute("mkdir -p catalog")

	-- Serialize and write
	local output_content = "return "
		.. serpent.block(catalog, {
			comment = false,
			sortkeys = true,
			compact = true,
			fatal = true,
			nocode = true,
			nohuge = true,
		})

	write_file(CATALOG_PATH, output_content)

	print("\n✓ Successfully published catalog")
	print("  Published at: " .. catalog.published_at)
	print("  Total functions: " .. #catalog.functions)
	print("  Output: " .. CATALOG_PATH)
end

-- Run with error handling
local success, err = pcall(main)
if not success then
	io.stderr:write("\nERROR: " .. tostring(err) .. "\n")
	os.exit(1)
end
