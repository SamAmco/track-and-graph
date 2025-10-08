-- semver.lua
-- Semantic versioning utilities

local M = {}

--- Parse semantic version string
-- @param version_str string: Version string to parse
-- @return table|nil: { major, minor, patch, raw } or nil if invalid
function M.parse(version_str)
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

--- Compare two semantic versions
-- @param v1 string: First version
-- @param v2 string: Second version
-- @return number: -1 if v1 < v2, 0 if equal, 1 if v1 > v2, nil if invalid
function M.compare(v1, v2)
	local parsed1 = M.parse(v1)
	local parsed2 = M.parse(v2)

	if not parsed1 or not parsed2 then
		return nil
	end

	if parsed1.major ~= parsed2.major then
		return parsed1.major < parsed2.major and -1 or 1
	end

	if parsed1.minor ~= parsed2.minor then
		return parsed1.minor < parsed2.minor and -1 or 1
	end

	if parsed1.patch ~= parsed2.patch then
		return parsed1.patch < parsed2.patch and -1 or 1
	end

	return 0
end

return M
