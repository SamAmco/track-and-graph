-- validation.lua
-- Validation functions for community Lua functions

local M = {}

-- Required language codes
local REQUIRED_LANGUAGES = {"en", "de", "es", "fr"}

--- Parse semantic version string
-- @param version_str string: Version string to parse
-- @return table|nil: { major, minor, patch, raw } or nil if invalid
function M.parse_semver(version_str)
	if type(version_str) ~= "string" then
		return nil
	end

	local major, minor, patch, _ = version_str:match("^(%d+)%.(%d+)%.(%d+)(.*)$")
	if not major then
		return nil
	end

	return {
		major = tonumber(major),
		minor = tonumber(minor),
		patch = tonumber(patch),
		raw = version_str
	}
end

--- Validate that a field contains all required translations
-- @param field table: The field to validate
-- @param field_name string: Name of the field for error messages
-- @param file_path string: File path for error messages
-- @return boolean: true if valid
-- @return table: Array of missing language codes or empty
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
-- @param config table: The config array to validate
-- @param file_path string: File path for error messages
-- @return boolean: true if valid
-- @return table: Array of error messages
function M.validate_config(config, file_path)
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

		::continue::
	end

	return #errors == 0, errors
end

--- Validate a function module structure
-- @param module table: The function module to validate
-- @param file_path string: File path for error messages
-- @return boolean: true if valid
-- @return table: Array of error messages
function M.validate_function(module, file_path)
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
		local parsed = M.parse_semver(module.version)
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

	if type(module.generator) ~= "function" then
		table.insert(errors, file_path .. " - 'generator' must be a function, got " .. type(module.generator))
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
	ok, trans_errors = M.validate_config(module.config, file_path)
	if not ok then
		for _, err in ipairs(trans_errors) do
			table.insert(errors, err)
		end
	end

	return #errors == 0, errors
end

--- Check uniqueness of titles for a specific language
-- @param functions table: Array of {id, title, file_path} tables
-- @param lang string: Language code (e.g., "en", "de")
-- @return table: Array of error messages for this language
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
-- @param functions table: Array of {id, title, file_path} tables
-- @return boolean: true if all unique
-- @return table: Array of error messages
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

return M
