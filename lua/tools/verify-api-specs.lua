#!/usr/bin/env lua
-- verify-api-specs.lua
-- Validates that all exported symbols from tng modules have API spec entries

-- Add src to package path so we can require tng modules
package.path = package.path .. ";src/?.lua;src/?/init.lua"

-- Find all module files in tng directory (excluding .apispec.lua files)
local function find_module_files()
	local modules = {}
	local handle = io.popen("find src/tng -name '*.lua' ! -name '*.apispec.lua' -type f")
	if not handle then
		error("Failed to scan tng directory")
	end

	for file in handle:lines() do
		-- Extract module name from path: src/tng/core.lua -> core
		local module_name = file:match("src/tng/([^/]+)%.lua$")
		if module_name then
			table.insert(modules, module_name)
		end
	end
	handle:close()

	return modules
end

-- Load a module and return its exports table
local function load_module(module_name)
	local ok, module = pcall(require, "tng." .. module_name)
	if not ok then
		return nil, "Failed to load module: " .. module
	end
	return module, nil
end

-- Load an API spec file
local function load_api_spec(module_name)
	local spec_path = "src/tng/" .. module_name .. ".apispec.lua"
	local file = io.open(spec_path, "r")
	if not file then
		return nil, "API spec file not found: " .. spec_path
	end

	local content = file:read("*all")
	file:close()

	local chunk, load_err = load(content, spec_path, "t")
	if not chunk then
		return nil, "Failed to load API spec: " .. load_err
	end

	local ok, spec = pcall(chunk)
	if not ok then
		return nil, "Failed to execute API spec: " .. spec
	end

	if type(spec) ~= "table" then
		return nil, "API spec must return a table"
	end

	return spec, nil
end

-- Split a string on delimiter
local function split(str, delimiter)
	local result = {}
	for part in string.gmatch(str, "([^" .. delimiter .. "]+)") do
		table.insert(result, part)
	end
	return result
end

-- Validate that all exports have spec entries and all spec values are valid
local function validate_module(module_name, module, spec)
	local errors = {}
	local export_count = 0

	-- Check all module exports have spec entries (including nested methods)
	for export_name, export_value in pairs(module) do
		export_count = export_count + 1
		if not spec[export_name] then
			table.insert(errors, string.format("  Missing spec for: %s", export_name))
		end

		-- If export is a table, check for nested methods
		if type(export_value) == "table" then
			for method_name, _ in pairs(export_value) do
				local nested_key = export_name .. "." .. method_name
				if not spec[nested_key] then
					table.insert(errors, string.format("  Missing spec for: %s", nested_key))
				end
			end
		end
	end

	-- Check all spec values are positive integers
	for spec_name, api_level in pairs(spec) do
		if type(api_level) ~= "number" then
			table.insert(errors, string.format("  Invalid API level for '%s': must be a number, got %s",
				spec_name, type(api_level)))
		elseif api_level % 1 ~= 0 then
			table.insert(errors, string.format("  Invalid API level for '%s': must be an integer, got %s",
				spec_name, tostring(api_level)))
		elseif api_level < 1 then
			table.insert(errors, string.format("  Invalid API level for '%s': must be positive, got %d",
				spec_name, api_level))
		end

		-- Validate that spec entry exists in module (including nested paths)
		if spec_name:find("%.") then
			-- Handle nested path like "DataSource.dp"
			local parts = split(spec_name, ".")
			local obj = module[parts[1]]
			for i = 2, #parts do
				if type(obj) ~= "table" or obj[parts[i]] == nil then
					table.insert(errors, string.format("  Spec entry '%s' not found in module", spec_name))
					break
				end
				obj = obj[parts[i]]
			end
		else
			-- Regular export
			if module[spec_name] == nil then
				table.insert(errors, string.format("  Spec entry '%s' not found in module", spec_name))
			end
		end
	end

	return export_count, errors
end

-- Main function
local function main()
	print("Verifying API specs...")

	local modules = find_module_files()
	if #modules == 0 then
		print("✗ No modules found in src/tng/")
		os.exit(1)
	end

	table.sort(modules)

	local total_errors = 0
	local total_exports = 0

	for _, module_name in ipairs(modules) do
		-- Load module
		local module, err = load_module(module_name)
		if not module then
			print(string.format("✗ %s.lua - %s", module_name, err))
			total_errors = total_errors + 1
			goto continue
		end

		-- Load API spec
		local spec, err = load_api_spec(module_name)
		if not spec then
			print(string.format("✗ %s.lua - %s", module_name, err))
			total_errors = total_errors + 1
			goto continue
		end

		-- Validate
		local export_count, errors = validate_module(module_name, module, spec)
		total_exports = total_exports + export_count

		if #errors > 0 then
			print(string.format("✗ %s.lua - %d errors:", module_name, #errors))
			for _, error_msg in ipairs(errors) do
				print(error_msg)
			end
			total_errors = total_errors + #errors
		else
			print(string.format("✓ %s.lua - all %d exports have specs", module_name, export_count))
		end

		::continue::
	end

	print()
	if total_errors > 0 then
		print(string.format("ERROR: API spec validation failed with %d error(s)", total_errors))
		os.exit(1)
	else
		print(string.format("✓ All API specs valid (%d exports across %d modules)",
			total_exports, #modules))
		os.exit(0)
	end
end

-- Run with error handling
local success, err = pcall(main)
if not success then
	io.stderr:write("FATAL ERROR: " .. tostring(err) .. "\n")
	os.exit(1)
end
