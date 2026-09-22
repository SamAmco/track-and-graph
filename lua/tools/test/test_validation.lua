#!/usr/bin/env lua
-- test_validation.lua
-- Simple tests for the validation library

local validation = require("tools.lib.validation")
local required_languages = require("tools.lib.languages").codes()

local test_count = 0
local passed_count = 0

local function translations(value)
	local result = {}
	for _, language in ipairs(required_languages) do
		result[language] = value
	end
	return result
end

local function errors_contain(errors, expected)
	for _, error_message in ipairs(errors) do
		if error_message:find(expected, 1, true) then
			return true
		end
	end
	return false
end

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
		translations("Example"),
		"test",
		"test.lua"
	)
	assert(ok == true)
	assert(#errors == 0)
end)

test("validate_translations rejects missing language", function()
	local values = translations("Example")
	values[required_languages[#required_languages]] = nil
	local ok, errors = validation.validate_translations(
		values,
		"test",
		"test.lua"
	)
	assert(ok == false)
	assert(#errors > 0)
end)

test("validate_translations rejects empty string", function()
	local values = translations("Example")
	values[required_languages[#required_languages]] = ""
	local ok, errors = validation.validate_translations(
		values,
		"test",
		"test.lua"
	)
	assert(ok == false)
end)

test("validate_translations rejects non-table", function()
	local ok, errors = validation.validate_translations(123, "test", "test.lua")
	assert(ok == false)
end)

-- Test validate_config
test("validate_config accepts valid config", function()
	local config = {
		{
			id = "param1",
			type = "number",
			name = translations("Parameter 1")
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
			name = translations("A")
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

test("validate_config explains inline enum option restriction", function()
	local config = {{
		id = "mode",
		type = "enum",
		name = translations("Mode"),
		options = {{id="one", name={en="One", de="Eins", es="Uno", fr="Un"}}},
	}}
	local ok, errors = validation.validate_config(config, "test.lua")
	assert(ok == false)
	assert(errors_contain(errors, "Android app supports inline { id, name } option tables"))
end)

test("validate_config rejects undefined enum shared key", function()
	local config = {{
		id = "mode",
		type = "enum",
		name = translations("Mode"),
		options = {"_missing"},
	}}
	local ok, errors = validation.validate_config(config, "test.lua", {})
	assert(ok == false)
	assert(errors_contain(errors, "undefined shared translation key"))
end)

-- Test validate_function
test("validate_function accepts valid module", function()
	local module = {
		id = "test-function",
		version = "1.0.0",
		inputCount = 1,
		categories = {"_filter"},
		title = translations("Test"),
		description = translations("Desc"),
		generator = function() end
	}
	local ok, errors = validation.validate_function(module, "test.lua")
	assert(ok == true, "Expected valid, got errors: " .. table.concat(errors, ", "))
end)

test("validate_function rejects missing id", function()
	local module = {
		version = "1.0.0",
		inputCount = 1,
		title = translations("Test"),
		description = translations("Desc"),
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
		title = translations("Test"),
		description = translations("Desc"),
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
		description = translations("Desc"),
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
		title = translations("Test"),
		description = translations("Desc"),
		generator = "not a function"
	}
	local ok, errors = validation.validate_function(module, "test.lua")
	assert(ok == false)
end)

test("validate_function explains inline category restriction", function()
	local module = {
		id = "test",
		version = "1.0.0",
		categories = {{en="Filter", de="Filter", es="Filtro", fr="Filtre"}},
		title = translations("Test"),
		description = translations("Desc"),
		generator = function() end,
	}
	local ok, errors = validation.validate_function(module, "test.lua")
	assert(ok == false)
	assert(errors_contain(errors, "not supported by the Android category parser"))
end)

test("validate_function rejects undefined category shared key", function()
	local module = {
		id = "test",
		version = "1.0.0",
		categories = {"_missing"},
		title = translations("Test"),
		description = translations("Desc"),
		generator = function() end,
	}
	local ok, errors = validation.validate_function(module, "test.lua", {})
	assert(ok == false)
	assert(errors_contain(errors, "undefined shared translation key"))
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

test("check_uniqueness accepts duplicate titles with non-overlapping versions", function()
	local functions = {
		{
			id = "func1",
			title = {en = "Same Title"},
			file_path = "func1.lua",
			version = "3.0.0",
			deprecated = 4,
		},
		{
			id = "func2",
			title = {en = "Same Title"},
			file_path = "func2.lua",
			version = "4.0.0",
		}
	}
	local ok, errors = validation.check_uniqueness(functions)
	assert(ok == true)
	assert(#errors == 0)
end)

test("check_uniqueness rejects duplicate titles with overlapping versions", function()
	local functions = {
		{
			id = "func1",
			title = {en = "Same Title"},
			file_path = "func1.lua",
			version = "3.0.0",
			deprecated = 5,
		},
		{
			id = "func2",
			title = {en = "Same Title"},
			file_path = "func2.lua",
			version = "4.0.0",
			deprecated = 6,
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
