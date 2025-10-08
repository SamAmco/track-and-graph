#!/usr/bin/env lua
-- test_validation.lua
-- Simple tests for the validation library

local validation = require("tools.lib.validation")

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

print("Testing validation library...\n")

-- Test validate_translations
test("validate_translations accepts valid translations", function()
	local ok, errors = validation.validate_translations(
		{en="English", de="Deutsch", es="Español", fr="Français"},
		"test",
		"test.lua"
	)
	assert(ok == true)
	assert(#errors == 0)
end)

test("validate_translations rejects missing language", function()
	local ok, errors = validation.validate_translations(
		{en="English", de="Deutsch", es="Español"},
		"test",
		"test.lua"
	)
	assert(ok == false)
	assert(#errors > 0)
end)

test("validate_translations rejects empty string", function()
	local ok, errors = validation.validate_translations(
		{en="English", de="", es="Español", fr="Français"},
		"test",
		"test.lua"
	)
	assert(ok == false)
end)

test("validate_translations rejects non-table", function()
	local ok, errors = validation.validate_translations("not a table", "test", "test.lua")
	assert(ok == false)
end)

-- Test validate_config
test("validate_config accepts valid config", function()
	local config = {
		{
			id = "param1",
			type = "number",
			name = {
				en = "Parameter 1",
				de = "Parameter 1",
				es = "Parámetro 1",
				fr = "Paramètre 1"
			}
		}
	}
	local ok, errors = validation.validate_config(config, "test.lua")
	assert(ok == true)
	assert(#errors == 0)
end)

test("validate_config accepts nil config", function()
	local ok, errors = validation.validate_config(nil, "test.lua")
	assert(ok == true)
end)

test("validate_config rejects missing id", function()
	local config = {
		{
			type = "number",
			name = {en="A", de="B", es="C", fr="D"}
		}
	}
	local ok, errors = validation.validate_config(config, "test.lua")
	assert(ok == false)
end)

test("validate_config rejects missing translations in name", function()
	local config = {
		{
			id = "param1",
			type = "number",
			name = {en="A", de="B"}
		}
	}
	local ok, errors = validation.validate_config(config, "test.lua")
	assert(ok == false)
end)

-- Test validate_function
test("validate_function accepts valid module", function()
	local module = {
		id = "test-function",
		version = "1.0.0",
		inputCount = 1,
		title = {en="Test", de="Test", es="Test", fr="Test"},
		description = {en="Desc", de="Desc", es="Desc", fr="Desc"},
		generator = function() end
	}
	local ok, errors = validation.validate_function(module, "test.lua")
	assert(ok == true, "Expected valid, got errors: " .. table.concat(errors, ", "))
end)

test("validate_function rejects missing id", function()
	local module = {
		version = "1.0.0",
		inputCount = 1,
		title = {en="Test", de="Test", es="Test", fr="Test"},
		description = {en="Desc", de="Desc", es="Desc", fr="Desc"},
		generator = function() end
	}
	local ok, errors = validation.validate_function(module, "test.lua")
	assert(ok == false)
end)

test("validate_function rejects invalid semver", function()
	local module = {
		id = "test",
		version = "1.0",
		inputCount = 1,
		title = {en="Test", de="Test", es="Test", fr="Test"},
		description = {en="Desc", de="Desc", es="Desc", fr="Desc"},
		generator = function() end
	}
	local ok, errors = validation.validate_function(module, "test.lua")
	assert(ok == false)
end)

test("validate_function rejects missing title translations", function()
	local module = {
		id = "test",
		version = "1.0.0",
		inputCount = 1,
		title = {en="Test", de="Test"},
		description = {en="Desc", de="Desc", es="Desc", fr="Desc"},
		generator = function() end
	}
	local ok, errors = validation.validate_function(module, "test.lua")
	assert(ok == false)
end)

test("validate_function rejects non-function generator", function()
	local module = {
		id = "test",
		version = "1.0.0",
		inputCount = 1,
		title = {en="Test", de="Test", es="Test", fr="Test"},
		description = {en="Desc", de="Desc", es="Desc", fr="Desc"},
		generator = "not a function"
	}
	local ok, errors = validation.validate_function(module, "test.lua")
	assert(ok == false)
end)

-- Test check_uniqueness
test("check_uniqueness accepts unique functions", function()
	local functions = {
		{
			id = "func1",
			title = {en = "Function 1"},
			file_path = "func1.lua"
		},
		{
			id = "func2",
			title = {en = "Function 2"},
			file_path = "func2.lua"
		}
	}
	local ok, errors = validation.check_uniqueness(functions)
	assert(ok == true)
	assert(#errors == 0)
end)

test("check_uniqueness rejects duplicate IDs", function()
	local functions = {
		{
			id = "func1",
			title = {en = "Function 1"},
			file_path = "func1.lua"
		},
		{
			id = "func1",
			title = {en = "Function 2"},
			file_path = "func2.lua"
		}
	}
	local ok, errors = validation.check_uniqueness(functions)
	assert(ok == false)
	assert(#errors > 0)
end)

test("check_uniqueness rejects duplicate titles", function()
	local functions = {
		{
			id = "func1",
			title = {en = "Same Title"},
			file_path = "func1.lua"
		},
		{
			id = "func2",
			title = {en = "Same Title"},
			file_path = "func2.lua"
		}
	}
	local ok, errors = validation.check_uniqueness(functions)
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
