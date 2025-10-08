#!/usr/bin/env lua
-- test_catalog_versioning.lua
-- Tests for the catalog versioning library

local versioning = require("tools.lib.catalog-versioning")

local test_count = 0
local passed_count = 0

local function test(name, fn)
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

print("Testing catalog-versioning library...\n")

-- Test generate_timestamp format
test("generate_timestamp returns ISO 8601 format", function()
	local timestamp = versioning.generate_timestamp()
	assert(type(timestamp) == "string")
	-- Check format: YYYY-MM-DDTHH:MM:SSZ
	assert(timestamp:match("^%d%d%d%d%-%d%d%-%d%dT%d%d:%d%d:%d%dZ$") ~= nil)
end)

-- Test should_write_catalog with no changes
test("should_write_catalog returns false for no changes", function()
	local report = {
		new = {},
		modified = {},
		unchanged = {{id = "func1"}}
	}
	local should_write, reason = versioning.should_write_catalog(report)
	assert(should_write == false)
	assert(reason == "No changes detected")
end)

-- Test should_write_catalog with new functions
test("should_write_catalog returns true for new functions", function()
	local report = {
		new = {{id = "func1"}},
		modified = {},
		unchanged = {}
	}
	local should_write, reason = versioning.should_write_catalog(report)
	assert(should_write == true)
	assert(reason:match("1 new"))
end)

-- Test should_write_catalog with modified functions
test("should_write_catalog returns true for modified functions", function()
	local report = {
		new = {},
		modified = {{id = "func1"}},
		unchanged = {}
	}
	local should_write, reason = versioning.should_write_catalog(report)
	assert(should_write == true)
	assert(reason:match("1 modified"))
end)

-- Test should_write_catalog with both new and modified
test("should_write_catalog describes both new and modified", function()
	local report = {
		new = {{id = "func1"}, {id = "func2"}},
		modified = {{id = "func3"}},
		unchanged = {}
	}
	local should_write, reason = versioning.should_write_catalog(report)
	assert(should_write == true)
	assert(reason:match("2 new"))
	assert(reason:match("1 modified"))
end)

-- Summary
print("\n" .. string.rep("=", 40))
print(string.format("Tests: %d/%d passed", passed_count, test_count))

if passed_count == test_count then
	print("✓ All tests passed!")
	os.exit(0)
else
	print("✗ Some tests failed")
	os.exit(1)
end
