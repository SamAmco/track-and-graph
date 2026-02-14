-- validation.lua
-- Validation functions for community Lua functions

local semver = require("tools.lib.semver")

local M = {}

-- Required language codes
local REQUIRED_LANGUAGES = {"en", "de", "es", "fr"}

--- Validate that a field contains all required translations
--- @param field string|table: The field to validate (string for translation key lookup, table for inline translations)
--- @param field_name string: Name of the field for error messages
--- @param file_path string: File path for error messages
--- @param valid_translations table?: Optional table of valid translation keys (if provided, validates string keys exist)
--- @return boolean: true if valid
--- @return table: Array of error messages or empty
function M.validate_translations(field, field_name, file_path, valid_translations)
	-- If it's a string, check if it exists in valid_translations (if provided)
	if type(field) == "string" then
		if valid_translations and not valid_translations[field] then
			return false, {string.format("%s - '%s' references undefined translation key: %s",
				file_path, field_name, field)}
		end
		return true, {}
	end

	-- If it's a table, validate it has all required languages
	if type(field) ~= "table" then
		return false, {string.format("%s - '%s' must be a string or table, got %s",
			file_path, field_name, type(field))}
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
--- @param valid_translations table?: Optional table of valid translation keys (if provided, validates translation keys exist)
--- @return boolean: true if valid
--- @return table: Array of error messages
function M.validate_config(config, file_path, valid_translations)
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
		local ok, trans_errors = M.validate_translations(item.name, "config[" .. i .. "].name", file_path, valid_translations)
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
			elseif valid_translations then
				-- Validate each option exists in valid_translations (enum options are translation keys)
				for j, option in ipairs(item.options) do
					if type(option) ~= "string" then
						table.insert(errors, string.format("%s - config[%d].options[%d] must be a string", file_path, i, j))
					elseif not valid_translations[option] then
						table.insert(errors, string.format("%s - config[%d] undefined translation key for enum option '%s'", file_path, i, option))
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
--- @param valid_translations table?: Optional table of valid translation keys (if provided, validates translation keys exist)
--- @return boolean: true if valid
--- @return table: Array of error messages
function M.validate_function(module, file_path, valid_translations)
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

	-- Validate categories (required, must be non-empty array of strings)
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
			end
		end
	end

	-- Validate title translations
	local ok, trans_errors = M.validate_translations(module.title, "title", file_path, valid_translations)
	if not ok then
		for _, err in ipairs(trans_errors) do
			table.insert(errors, err)
		end
	end

	-- Validate description translations
	ok, trans_errors = M.validate_translations(module.description, "description", file_path, valid_translations)
	if not ok then
		for _, err in ipairs(trans_errors) do
			table.insert(errors, err)
		end
	end

	-- Validate config (if present)
	ok, trans_errors = M.validate_config(module.config, file_path, valid_translations)
	if not ok then
		for _, err in ipairs(trans_errors) do
			table.insert(errors, err)
		end
	end

	return #errors == 0, errors
end

--- Check if two version ranges overlap
--- @param f1 table: Function 1 {min, max}
--- @param f2 table: Function 2 {min, max}
--- @return boolean: True if they overlap
local function ranges_overlap(f1, f2)
	-- Default nil to infinity/negative infinity
	local min1 = f1.min or -math.huge
	local max1 = f1.max or math.huge
	local min2 = f2.min or -math.huge
	local max2 = f2.max or math.huge

	return max1 > min2 and max2 > min1
end

--- Format a version string for error messages
local function fmt_ver(f)
	local s = ""
	if f.min then s = s .. "min:" .. f.min else s = s .. "min:-inf" end
	s = s .. ", "
	if f.max then s = s .. "max:" .. f.max else s = s .. "max:inf" end
	return "(" .. s .. ")"
end

--- Check uniqueness of titles for a specific language, considering version overlaps
--- @param functions table: Array of {id, title, file_path, version, deprecated} tables
--- @param lang string: Language code (e.g., "en", "de")
--- @return table: Array of error messages for this language
local function check_title_uniqueness_for_language(functions, lang)
	local errors = {}
	local by_title = {}

	-- 1. Group functions by title
	for _, func in ipairs(functions) do
		local title = func.title and func.title[lang]
		if title then
			if not by_title[title] then
				by_title[title] = {}
			end
			table.insert(by_title[title], func)
		end
	end

	-- 2. Check for overlaps within each title group
	for title, entries in pairs(by_title) do
		-- We only care if there is more than one function with this title
		if #entries > 1 then
			-- Compare every entry against every other entry (O(N^2) for this specific title)
			for i = 1, #entries do
				for j = i + 1, #entries do
					local f1Ver = semver.parse(entries[i].version)
					local f2Ver = semver.parse(entries[j].version)
					local f1 = {
						file_path = entries[i].file_path,
						min = f1Ver and f1Ver.major,
						max = entries[i].deprecated,
					}
					local f2 = {
						file_path = entries[j].file_path,
						min = f2Ver and f2Ver.major,
						max = entries[j].deprecated,
					}

					if ranges_overlap(f1, f2) then
						table.insert(errors, string.format(
							"Overlap detected for title['%s'] '%s':\n  File 1: %s %s\n  File 2: %s %s",
							lang, title,
							f1.file_path, fmt_ver(f1),
							f2.file_path, fmt_ver(f2)
						))
					end
				end
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

--- Collect all undefined translation keys across functions
--- @param functions table: Array of function modules
--- @param valid_translations table: Table of valid translation keys
--- @return table: Array of undefined translation keys (unique, sorted)
function M.collect_undefined_translations(functions, valid_translations)
	local undefined = {}
	local seen = {}

	-- Helper to check and collect undefined translation key
	local function check_translation(value)
		if type(value) == "string" and not valid_translations[value] and not seen[value] then
			table.insert(undefined, value)
			seen[value] = true
		end
	end

	for _, func in ipairs(functions) do
		-- Check title
		check_translation(func.title)

		-- Check description
		check_translation(func.description)

		-- Check categories
		if func.categories then
			for _, category in ipairs(func.categories) do
				check_translation(category)
			end
		end

		-- Check config names and enum options
		if func.config then
			for _, config_item in ipairs(func.config) do
				check_translation(config_item.name)

				if config_item.type == "enum" and config_item.options then
					for _, option in ipairs(config_item.options) do
						check_translation(option)
					end
				end
			end
		end
	end

	table.sort(undefined)
	return undefined
end

--- Collect all unused translation keys
--- @param valid_translations table: Table of valid translation keys
--- @param functions table: Array of function modules
--- @return table: Array of unused translation keys (sorted)
function M.collect_unused_translations(valid_translations, functions)
	local used = {}

	-- Helper to mark translation as used
	local function mark_used(value)
		if type(value) == "string" then
			used[value] = true
		end
	end

	-- Collect all used translation keys
	for _, func in ipairs(functions) do
		-- Check title
		mark_used(func.title)

		-- Check description
		mark_used(func.description)

		-- Check categories
		if func.categories then
			for _, category in ipairs(func.categories) do
				mark_used(category)
			end
		end

		-- Check config names and enum options
		if func.config then
			for _, config_item in ipairs(func.config) do
				mark_used(config_item.name)

				if config_item.type == "enum" and config_item.options then
					for _, option in ipairs(config_item.options) do
						mark_used(option)
					end
				end
			end
		end
	end

	-- Find unused
	local unused = {}
	for translation_key in pairs(valid_translations) do
		if not used[translation_key] then
			table.insert(unused, translation_key)
		end
	end

	table.sort(unused)
	return unused
end

return M
