-- api-specs.lua
-- API specification validation and querying

local M = {}

-- Split a string on delimiter
local function split(str, delimiter)
	local result = {}
	for part in string.gmatch(str, "([^" .. delimiter .. "]+)") do
		table.insert(result, part)
	end
	return result
end

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
	-- Required for runtime: tng modules have internal dependencies
	package.path = package.path .. ";src/?.lua;src/?/init.lua"

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

-- Validate that all exports have spec entries and all spec values are valid
local function validate_module(module, spec)
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

--- Verify all API specs are valid
-- @return boolean: true if all valid
-- @return table: { total_exports = N, modules = { {name, export_count, errors} } }
function M.verify()
	local modules = find_module_files()
	if #modules == 0 then
		return false, {error = "No modules found in src/tng/"}
	end

	table.sort(modules)

	local result = {
		total_exports = 0,
		total_errors = 0,
		modules = {}
	}

	for _, module_name in ipairs(modules) do
		local module_result = {
			name = module_name,
			export_count = 0,
			errors = {}
		}

		-- Load module
		local module, err = load_module(module_name)
		if not module then
			table.insert(module_result.errors, err)
			result.total_errors = result.total_errors + 1
			table.insert(result.modules, module_result)
			goto continue
		end

		-- Load API spec
		local spec, err = load_api_spec(module_name)
		if not spec then
			table.insert(module_result.errors, err)
			result.total_errors = result.total_errors + 1
			table.insert(result.modules, module_result)
			goto continue
		end

		-- Validate
		local export_count, errors = validate_module(module, spec)
		module_result.export_count = export_count
		module_result.errors = errors

		result.total_exports = result.total_exports + export_count
		result.total_errors = result.total_errors + #errors

		table.insert(result.modules, module_result)

		::continue::
	end

	return result.total_errors == 0, result
end

--- Get the maximum API level across all specs
-- @return number|nil: Max API level or nil on error
-- @return string|nil: Error message if failed
function M.get_max_level()
	local handle = io.popen("find src/tng -name '*.apispec.lua' -type f")
	if not handle then
		return nil, "Failed to scan tng directory"
	end

	local spec_files = {}
	for file in handle:lines() do
		table.insert(spec_files, file)
	end
	handle:close()

	if #spec_files == 0 then
		return nil, "No API spec files found"
	end

	local max_level = 0

	for _, spec_file in ipairs(spec_files) do
		local spec, err = load_api_spec(spec_file:match("src/tng/([^/]+)%.apispec%.lua$"))
		if not spec then
			return nil, err
		end

		for _, api_level in pairs(spec) do
			if type(api_level) == "number" and api_level > max_level then
				max_level = api_level
			end
		end
	end

	return max_level, nil
end

return M
