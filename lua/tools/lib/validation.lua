-- validation.lua
-- Validation functions for community Lua functions

local semver = require("tools.lib.semver")

local M = {}

-- Required language codes
local REQUIRED_LANGUAGES = {"en", "de", "es", "fr"}

--- Validate that a field contains all required translations
--- @param field table: The field to validate
--- @param field_name string: Name of the field for error messages
--- @param file_path string: File path for error messages
--- @return boolean: true if valid
--- @return table: Array of missing language codes or empty
function M.validate_translations(field, field_name, file_path)
	if type(field) ~= "table" then
		return false, {"Field '" .. field_name .. "' must be a table"}
	end

	local missing = {}
	for _, lang in ipairs(REQUIRED_LANGUAGES) do
		if type(field[lang]) ~= "string" or field[lang] == "" then
			table.insert(missing, lang)
		end
	end

	if #missing > 0 then
		return false, {string.format("%s - '%s' missing translations: %s",
			file_path, field_name, table.concat(missing, ", "))}
	end

	return true, {}
end

--- Validate config array has proper structure and translations
--- @param config table: The config array to validate
--- @param file_path string: File path for error messages
--- @param valid_enums table?: Optional table of valid enum IDs (if provided, validates enum options exist)
--- @return boolean: true if valid
--- @return table: Array of error messages
function M.validate_config(config, file_path, valid_enums)
	if config == nil then
		return true, {}  -- config is optional
	end

	if type(config) ~= "table" then
		return false, {file_path .. " - 'config' must be an array"}
	end

	local errors = {}

	for i, item in ipairs(config) do
		if type(item) ~= "table" then
			table.insert(errors, string.format("%s - config[%d] must be a table", file_path, i))
			goto continue
		end

		if type(item.id) ~= "string" then
			table.insert(errors, string.format("%s - config[%d].id must be a string", file_path, i))
		end

		if type(item.type) ~= "string" then
			table.insert(errors, string.format("%s - config[%d].type must be a string", file_path, i))
		end

		-- Validate name translations
		local ok, trans_errors = M.validate_translations(item.name, "config[" .. i .. "].name", file_path)
		if not ok then
			for _, err in ipairs(trans_errors) do
				table.insert(errors, err)
			end
		end

		-- Validate enum options if type is enum
		if item.type == "enum" then
			if type(item.options) ~= "table" then
				table.insert(errors, string.format("%s - config[%d].options must be a table for enum type", file_path, i))
			elseif #item.options == 0 then
				table.insert(errors, string.format("%s - config[%d].options must contain at least one option", file_path, i))
			elseif valid_enums then
				-- Validate each option exists in valid_enums
				for j, option in ipairs(item.options) do
					if type(option) ~= "string" then
						table.insert(errors, string.format("%s - config[%d].options[%d] must be a string", file_path, i, j))
					elseif not valid_enums[option] then
						table.insert(errors, string.format("%s - config[%d] undefined enum option '%s'", file_path, i, option))
					end
				end
			end
		end

		::continue::
	end

	return #errors == 0, errors
end

--- Validate a function module structure
--- @param module table: The function module to validate
--- @param file_path string: File path for error messages
--- @param valid_categories table?: Optional table of valid category IDs (if provided, validates categories exist)
--- @param valid_enums table?: Optional table of valid enum IDs (if provided, validates enum options exist)
--- @return boolean: true if valid
--- @return table: Array of error messages
function M.validate_function(module, file_path, valid_categories, valid_enums)
	local errors = {}

	-- Check module is a table
	if type(module) ~= "table" then
		return false, {file_path .. " must return a table, got " .. type(module)}
	end

	-- Required fields
	if type(module.id) ~= "string" then
		table.insert(errors, file_path .. " - 'id' must be a string, got " .. type(module.id))
	end

	if type(module.version) ~= "string" then
		table.insert(errors, file_path .. " - 'version' must be a string, got " .. type(module.version))
	else
		-- Validate semver format
		local parsed = semver.parse(module.version)
		if not parsed then
			table.insert(errors, file_path .. " - 'version' is not valid semver: " .. module.version)
		end
	end

	-- inputCount is optional, defaults to 1
	if module.inputCount ~= nil then
		if type(module.inputCount) ~= "number" or module.inputCount % 1 ~= 0 then
			table.insert(errors, file_path .. " - 'inputCount' must be an integer")
		end
	end

	-- deprecated is optional, must be positive integer if present
	if module.deprecated ~= nil then
		if type(module.deprecated) ~= "number" then
			table.insert(errors, file_path .. " - 'deprecated' must be a number, got " .. type(module.deprecated))
		elseif module.deprecated % 1 ~= 0 then
			table.insert(errors, file_path .. " - 'deprecated' must be an integer")
		elseif module.deprecated < 1 then
			table.insert(errors, file_path .. " - 'deprecated' must be positive")
		end
	end

	if type(module.generator) ~= "function" then
		table.insert(errors, file_path .. " - 'generator' must be a function, got " .. type(module.generator))
	end

	-- Validate categories (required, must be non-empty array)
	if type(module.categories) ~= "table" then
		table.insert(errors, file_path .. " - 'categories' must be a table, got " .. type(module.categories))
	elseif #module.categories == 0 then
		table.insert(errors, file_path .. " - 'categories' must contain at least one category")
	else
		-- Validate each category is a string
		for i, category in ipairs(module.categories) do
			if type(category) ~= "string" then
				table.insert(errors, string.format("%s - categories[%d] must be a string, got %s",
					file_path, i, type(category)))
			elseif valid_categories and not valid_categories[category] then
				table.insert(errors, string.format("%s - undefined category '%s'", file_path, category))
			end
		end
	end

	-- Validate title translations
	local ok, trans_errors = M.validate_translations(module.title, "title", file_path)
	if not ok then
		for _, err in ipairs(trans_errors) do
			table.insert(errors, err)
		end
	end

	-- Validate description translations
	ok, trans_errors = M.validate_translations(module.description, "description", file_path)
	if not ok then
		for _, err in ipairs(trans_errors) do
			table.insert(errors, err)
		end
	end

	-- Validate config (if present)
	ok, trans_errors = M.validate_config(module.config, file_path, valid_enums)
	if not ok then
		for _, err in ipairs(trans_errors) do
			table.insert(errors, err)
		end
	end

	return #errors == 0, errors
end

--- Check uniqueness of titles for a specific language
--- @param functions table: Array of {id, title, file_path} tables
--- @param lang string: Language code (e.g., "en", "de")
--- @return table: Array of error messages for this language
local function check_title_uniqueness_for_language(functions, lang)
	local errors = {}
	local seen = {}

	for _, func in ipairs(functions) do
		local title = func.title and func.title[lang]
		if title then
			if seen[title] then
				table.insert(errors, string.format(
					"Duplicate title['%s'] '%s' in %s (already seen in %s)",
					lang, title, func.file_path, seen[title]
				))
			else
				seen[title] = func.file_path
			end
		end
	end

	return errors
end

--- Check uniqueness of IDs and titles (all languages)
--- @param functions table: Array of {id, title, file_path} tables
--- @return boolean: true if all unique
--- @return table: Array of error messages
function M.check_uniqueness(functions)
	local errors = {}
	local seen_ids = {}

	-- Check ID uniqueness
	for _, func in ipairs(functions) do
		if seen_ids[func.id] then
			table.insert(errors, string.format(
				"Duplicate id '%s' in %s (already seen in %s)",
				func.id, func.file_path, seen_ids[func.id]
			))
		else
			seen_ids[func.id] = func.file_path
		end
	end

	-- Check title uniqueness for each language independently
	for _, lang in ipairs(REQUIRED_LANGUAGES) do
		local lang_errors = check_title_uniqueness_for_language(functions, lang)
		for _, err in ipairs(lang_errors) do
			table.insert(errors, err)
		end
	end

	return #errors == 0, errors
end

--- Collect all undefined categories across functions
--- @param functions table: Array of function modules
--- @param valid_categories table: Table of valid category IDs
--- @return table: Array of undefined category IDs (unique, sorted)
function M.collect_undefined_categories(functions, valid_categories)
	local undefined = {}
	local seen = {}

	for _, func in ipairs(functions) do
		if func.categories then
			for _, category in ipairs(func.categories) do
				if not valid_categories[category] and not seen[category] then
					table.insert(undefined, category)
					seen[category] = true
				end
			end
		end
	end

	table.sort(undefined)
	return undefined
end

--- Collect all unused categories
--- @param valid_categories table: Table of valid category IDs
--- @param functions table: Array of function modules
--- @return table: Array of unused category IDs (sorted)
function M.collect_unused_categories(valid_categories, functions)
	local used = {}

	-- Collect all used categories
	for _, func in ipairs(functions) do
		if func.categories then
			for _, category in ipairs(func.categories) do
				used[category] = true
			end
		end
	end

	-- Find unused
	local unused = {}
	for category_id in pairs(valid_categories) do
		if not used[category_id] then
			table.insert(unused, category_id)
		end
	end

	table.sort(unused)
	return unused
end

--- Collect all undefined enum options across functions
--- @param functions table: Array of function modules
--- @param valid_enums table: Table of valid enum option IDs
--- @return table: Array of undefined enum option IDs (unique, sorted)
function M.collect_undefined_enums(functions, valid_enums)
	local undefined = {}
	local seen = {}

	for _, func in ipairs(functions) do
		if func.config then
			for _, config_item in ipairs(func.config) do
				if config_item.type == "enum" and config_item.options then
					for _, option in ipairs(config_item.options) do
						if not valid_enums[option] and not seen[option] then
							table.insert(undefined, option)
							seen[option] = true
						end
					end
				end
			end
		end
	end

	table.sort(undefined)
	return undefined
end

--- Collect all unused enum options
--- @param valid_enums table: Table of valid enum option IDs
--- @param functions table: Array of function modules
--- @return table: Array of unused enum option IDs (sorted)
function M.collect_unused_enums(valid_enums, functions)
	local used = {}

	-- Collect all used enum options
	for _, func in ipairs(functions) do
		if func.config then
			for _, config_item in ipairs(func.config) do
				if config_item.type == "enum" and config_item.options then
					for _, option in ipairs(config_item.options) do
						used[option] = true
					end
				end
			end
		end
	end

	-- Find unused
	local unused = {}
	for enum_id in pairs(valid_enums) do
		if not used[enum_id] then
			table.insert(unused, enum_id)
		end
	end

	table.sort(unused)
	return unused
end

return M
