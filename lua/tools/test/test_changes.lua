#!/usr/bin/env lua
-- test_changes.lua
-- Tests for the changes detection library

local changes = require("tools.lib.changes")

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

print("Testing changes library...\n")

-- Test compare_functions with no old catalog
test("compare_functions treats all as new when no old catalog", function()
	local new_functions = {
		{id = "func1", version = "1.0.0", script = "script1", file_path = "func1.lua"}
	}
	local report = changes.compare_functions(nil, new_functions)
	assert(#report.new == 1)
	assert(#report.modified == 0)
	assert(#report.unchanged == 0)
end)

-- Test compare_functions with unchanged function
test("compare_functions detects unchanged functions", function()
	local old_catalog = {
		functions = {
			["func1"] = {version = "1.0.0", script = "script content"}
		}
	}
	local new_functions = {
		{id = "func1", version = "1.0.0", script = "script content", file_path = "func1.lua"}
	}
	local report = changes.compare_functions(old_catalog, new_functions)
	assert(#report.new == 0)
	assert(#report.modified == 0)
	assert(#report.unchanged == 1)
end)

-- Test compare_functions with modified function
test("compare_functions detects modified functions", function()
	local old_catalog = {
		functions = {
			["func1"] = {version = "1.0.0", script = "old script"}
		}
	}
	local new_functions = {
		{id = "func1", version = "1.1.0", script = "new script", file_path = "func1.lua"}
	}
	local report = changes.compare_functions(old_catalog, new_functions)
	assert(#report.new == 0)
	assert(#report.modified == 1)
	assert(#report.unchanged == 0)
	assert(report.modified[1].old_version == "1.0.0")
	assert(report.modified[1].new_version == "1.1.0")
end)

-- Test compare_functions with new function added
test("compare_functions detects new functions", function()
	local old_catalog = {
		functions = {
			["func1"] = {version = "1.0.0", script = "script1"}
		}
	}
	local new_functions = {
		{id = "func1", version = "1.0.0", script = "script1", file_path = "func1.lua"},
		{id = "func2", version = "1.0.0", script = "script2", file_path = "func2.lua"}
	}
	local report = changes.compare_functions(old_catalog, new_functions)
	assert(#report.new == 1)
	assert(#report.modified == 0)
	assert(#report.unchanged == 1)
end)

-- Test validate_version_increments accepts valid increment
test("validate_version_increments accepts version increment for modified function", function()
	local report = {
		new = {},
		modified = {
			{
				id = "func1",
				old_version = "1.0.0",
				new_version = "1.1.0",
				file_path = "func1.lua"
			}
		},
		unchanged = {}
	}
	local ok, errors = changes.validate_version_increments(report)
	assert(ok == true)
	assert(#errors == 0)
end)

-- Test validate_version_increments rejects same version for modified function
test("validate_version_increments rejects unchanged version for modified script", function()
	local report = {
		new = {},
		modified = {
			{
				id = "func1",
				old_version = "1.0.0",
				new_version = "1.0.0",
				file_path = "func1.lua"
			}
		},
		unchanged = {}
	}
	local ok, errors = changes.validate_version_increments(report)
	assert(ok == false)
	assert(#errors > 0)
end)

-- Test validate_version_increments rejects version decrease
test("validate_version_increments rejects version decrease", function()
	local report = {
		new = {},
		modified = {
			{
				id = "func1",
				old_version = "1.1.0",
				new_version = "1.0.0",
				file_path = "func1.lua"
			}
		},
		unchanged = {}
	}
	local ok, errors = changes.validate_version_increments(report)
	assert(ok == false)
	assert(#errors > 0)
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
