-- file-traversal.lua
-- Common utilities for traversing and loading Lua script files

local M = {}

-- Script types for traversal
M.SCRIPT_TYPE = {
	FUNCTIONS = "functions",
	GRAPHS = "graphs",
}

-- Base paths for different script types
local BASE_PATHS = {
	[M.SCRIPT_TYPE.FUNCTIONS] = "src/community/functions",
	[M.SCRIPT_TYPE.GRAPHS] = "src/community/graphs",
}

--- Find all lua files in a directory using the find command
--- @param dir string The directory to search
--- @param include_tests boolean? Whether to include test_ files (default: false)
--- @return table Array of file paths
function M.find_lua_files(dir, include_tests)
	include_tests = include_tests or false

	local files = {}
	local exclude_pattern = include_tests and "" or " ! -name 'test_*'"
	local command = string.format("find %s -type f -name '*.lua'%s", dir, exclude_pattern)

	local handle = io.popen(command)
	if not handle then
		error("Failed to scan directory: " .. dir)
	end

	for file in handle:lines() do
		table.insert(files, file)
	end
	handle:close()

	return files
end

--- Find lua files for a specific script type
--- @param script_type string One of M.SCRIPT_TYPE values
--- @param include_tests boolean? Whether to include test_ files (default: false)
--- @return table Array of file paths
function M.find_scripts(script_type, include_tests)
	local base_path = BASE_PATHS[script_type]
	if not base_path then
		error("Invalid script type: " .. tostring(script_type))
	end

	return M.find_lua_files(base_path, include_tests)
end

--- Read entire file content
--- @param path string Path to the file
--- @return string File content
function M.read_file(path)
	local file = io.open(path, "r")
	if not file then
		error("Could not open file: " .. path)
	end
	local content = file:read("*all")
	file:close()
	return content
end

--- Load a Lua module from file content
--- @param content string Lua source code
--- @param file_path string Path to file (used for error messages)
--- @return boolean success
--- @return table|string module or error message
function M.load_module(content, file_path)
	-- Set up package path for tng module dependencies
	package.path = package.path .. ";src/?.lua;src/?/init.lua"

	local chunk, load_err = load(content, file_path, "t")
	if not chunk then
		return false, "Failed to load: " .. load_err
	end

	local success, module = pcall(chunk)
	if not success then
		return false, "Failed to execute: " .. module
	end

	return true, module
end

--- Read and load a Lua module from a file path
--- @param file_path string Path to the Lua file
--- @return boolean success
--- @return table|string module or error message
function M.read_and_load(file_path)
	local content = M.read_file(file_path)
	return M.load_module(content, file_path)
end

return M
