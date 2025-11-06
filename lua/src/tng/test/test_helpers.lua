-- test_helpers.lua
-- Common test utilities for tng tests

local M = {}

-- Test state
local test_count = 0
local passed_count = 0

-- Run a single test
function M.test(name, fn)
	test_count = test_count + 1
	local ok, err = pcall(fn)
	if ok then
		passed_count = passed_count + 1
		print("✓ " .. name)
	else
		print("✗ " .. name)
		print("  Error: " .. tostring(err))
	end
end

-- Helper function to create test data points
function M.create_data_point(timestamp, value, offset, label, note)
	return {
		timestamp = timestamp or 0,
		value = value or 0,
		offset = offset or 0,
		label = label or "",
		note = note or ""
	}
end

-- Print test summary and exit with appropriate code
function M.finish_tests()
	print("\n" .. string.rep("=", 40))
	print(string.format("Tests: %d/%d passed", passed_count, test_count))
	if passed_count == test_count then
		print("✓ All tests passed!")
		os.exit(0)
	else
		print("✗ Some tests failed")
		os.exit(1)
	end
end

-- Reset test counters (useful for test runners)
function M.reset_counters()
	test_count = 0
	passed_count = 0
end

return M
