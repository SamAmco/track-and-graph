#!/usr/bin/env lua
-- test_all.lua
-- Runs all test files in the test directory

-- Find all test files
local function find_test_files()
	local files = {}
	local handle = io.popen("find src/tng/test -name 'test_*.lua' ! -name 'test_all.lua' ! -name 'test_helpers.lua' -type f | sort")
	if not handle then
		error("Failed to scan test directory")
	end
	for file in handle:lines() do
		table.insert(files, file)
	end
	handle:close()

	return files
end

-- Run a single test file
local function run_test(file_path)
	print("\n" .. string.rep("=", 60))
	print("Running: " .. file_path)
	print(string.rep("=", 60))
	local exit_code = os.execute("lua " .. file_path)
	return exit_code == 0 or exit_code == true
end

-- Main
local function main()
	print("Running all tng tests...\n")
	local test_files = find_test_files()

	if #test_files == 0 then
		print("No test files found")
		os.exit(1)
	end

	local passed = 0
	local failed = 0

	for _, file in ipairs(test_files) do
		if run_test(file) then
			passed = passed + 1
		else
			failed = failed + 1
		end
	end

	print("\n" .. string.rep("=", 60))
	print(string.format("Test Suites: %d passed, %d failed, %d total",
		passed, failed, #test_files))
	print(string.rep("=", 60))

	if failed > 0 then
		os.exit(1)
	else
		print("\n✓ All test suites passed!")
		os.exit(0)
	end
end

-- Run
local success, err = pcall(main)
if not success then
	io.stderr:write("FATAL ERROR: " .. tostring(err) .. "\n")
	os.exit(1)
end
