-- catalog-versioning.lua
-- Catalog versioning and timestamp management

local M = {}

--- Load existing catalog from file
--- @param path string: Path to catalog file
--- @return table|nil: { published_at, functions } or nil if doesn't exist
function M.load_catalog(path)
	local file = io.open(path, "r")
	if not file then
		return nil
	end

	local content = file:read("*all")
	file:close()

	-- Execute catalog to get the table
	local chunk, load_err = load(content, path, "t")
	if not chunk then
		error("Failed to load catalog: " .. load_err)
	end

	local ok, catalog = pcall(chunk)
	if not ok then
		error("Failed to execute catalog: " .. catalog)
	end

	if type(catalog) ~= "table" then
		error("Catalog must return a table")
	end

	return catalog
end

--- Generate ISO 8601 timestamp in UTC
--- @return string|osdate: Timestamp like "2025-10-08T23:45:12Z"
function M.generate_timestamp()
	return os.date("!%Y-%m-%dT%H:%M:%SZ")
end

--- Determine if catalog should be written based on changes
--- @param change_report table: ChangeReport from changes.compare_functions
--- @return boolean: true if catalog should be written
--- @return string: Reason message
function M.should_write_catalog(change_report)
	local new_count = #change_report.new
	local modified_count = #change_report.modified

	if new_count == 0 and modified_count == 0 then
		return false, "No changes detected"
	end

	local parts = {}
	if new_count > 0 then
		table.insert(parts, string.format("%d new", new_count))
	end
	if modified_count > 0 then
		table.insert(parts, string.format("%d modified", modified_count))
	end

	return true, table.concat(parts, ", ")
end

return M
