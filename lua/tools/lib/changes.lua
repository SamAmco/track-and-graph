-- changes.lua
-- Change detection and version enforcement for community functions

local semver = require("tools.lib.semver")

local M = {}

--- Compare old catalog with new functions to detect changes
--- @param old_catalog table|nil: Previous catalog with {functions = [...]} or nil if first run
--- @param new_functions table: Array of {id, version, script, file_path}
--- @return table: ChangeReport = {new = [], modified = [], unchanged = []}
function M.compare_functions(old_catalog, new_functions)
	local report = {
		new = {},
		modified = {},
		unchanged = {}
	}

	-- If no old catalog, everything is new
	if not old_catalog or not old_catalog.functions then
		for _, func in ipairs(new_functions) do
			table.insert(report.new, {
				id = func.id,
				version = func.version,
				script = func.script,
				file_path = func.file_path
			})
		end
		return report
	end

	-- Build map of old functions by id
	local old_by_id = {}
	for _, old_func in ipairs(old_catalog.functions) do
		old_by_id[old_func.id] = old_func
	end

	-- Compare each new function with old
	for _, new_func in ipairs(new_functions) do
		local old_func = old_by_id[new_func.id]

		if not old_func then
			-- New function
			table.insert(report.new, {
				id = new_func.id,
				version = new_func.version,
				script = new_func.script,
				file_path = new_func.file_path
			})
		elseif old_func.script ~= new_func.script then
			-- Modified function
			table.insert(report.modified, {
				id = new_func.id,
				old_version = old_func.version,
				new_version = new_func.version,
				old_script = old_func.script,
				new_script = new_func.script,
				file_path = new_func.file_path
			})
		else
			-- Unchanged function
			table.insert(report.unchanged, {
				id = new_func.id,
				version = new_func.version,
				old_version = old_func.version,
				file_path = new_func.file_path
			})
		end
	end

	return report
end

--- Validate that version increments are correct for modified functions
--- @param change_report table: ChangeReport from compare_functions
--- @return boolean: true if all valid
--- @return table: Array of error messages
function M.validate_version_increments(change_report)
	local errors = {}

	-- Check modified functions have version increments
	for _, func in ipairs(change_report.modified) do
		local cmp = semver.compare(func.old_version, func.new_version)

		if cmp == nil then
			table.insert(errors, string.format(
				"%s: Invalid semver comparison between '%s' and '%s'",
				func.file_path, func.old_version, func.new_version
			))
		elseif cmp == 0 then
			table.insert(errors, string.format(
				"%s: Script changed but version unchanged (%s). Version must increment when script changes.",
				func.file_path, func.old_version
			))
		elseif cmp > 0 then
			table.insert(errors, string.format(
				"%s: Version decreased from %s to %s. Version must increase.",
				func.file_path, func.old_version, func.new_version
			))
		end
		-- cmp < 0 is good - version incremented
	end

	return #errors == 0, errors
end

return M
