#!/usr/bin/env lua
-- test_semver.lua
-- Tests for the semver library

local semver = require("tools.lib.semver")

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

print("Testing semver library...\n")

-- Test parse
test("parse handles valid semver", function()
	local v = semver.parse("1.2.3")
	assert(v.major == 1)
	assert(v.minor == 2)
	assert(v.patch == 3)
	assert(v.raw == "1.2.3")
end)

test("parse handles pre-release", function()
	local v = semver.parse("1.2.3-alpha.1")
	assert(v.major == 1)
	assert(v.minor == 2)
	assert(v.patch == 3)
end)

test("parse handles build metadata", function()
	local v = semver.parse("1.2.3+build.123")
	assert(v.major == 1)
end)

test("parse rejects invalid version", function()
	assert(semver.parse("1.2") == nil)
	assert(semver.parse("abc") == nil)
	assert(semver.parse("") == nil)
end)

test("parse rejects non-string", function()
	assert(semver.parse(123) == nil)
	assert(semver.parse(nil) == nil)
	assert(semver.parse({}) == nil)
end)

-- Test compare
test("compare returns 0 for equal versions", function()
	assert(semver.compare("1.2.3", "1.2.3") == 0)
end)

test("compare returns -1 when v1 < v2 (major)", function()
	assert(semver.compare("1.0.0", "2.0.0") == -1)
end)

test("compare returns 1 when v1 > v2 (major)", function()
	assert(semver.compare("2.0.0", "1.0.0") == 1)
end)

test("compare returns -1 when v1 < v2 (minor)", function()
	assert(semver.compare("1.0.0", "1.1.0") == -1)
end)

test("compare returns 1 when v1 > v2 (minor)", function()
	assert(semver.compare("1.2.0", "1.1.0") == 1)
end)

test("compare returns -1 when v1 < v2 (patch)", function()
	assert(semver.compare("1.0.0", "1.0.1") == -1)
end)

test("compare returns 1 when v1 > v2 (patch)", function()
	assert(semver.compare("1.0.2", "1.0.1") == 1)
end)

test("compare returns nil for invalid versions", function()
	assert(semver.compare("1.2", "1.2.3") == nil)
	assert(semver.compare("1.2.3", "abc") == nil)
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
