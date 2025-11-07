#!/usr/bin/env lua
-- test_catalog_encoding.lua
-- Tests for catalog encoding functionality

local catalog_encoding = require("tools.lib.catalog-encoding")

-- Test function counter
local tests_run = 0
local tests_passed = 0

-- Helper function to run a test
local function test(name, test_func)
    tests_run = tests_run + 1
    print("Running: " .. name)

    local ok, err = pcall(test_func)
    if ok then
        tests_passed = tests_passed + 1
        print("  ✓ PASS")
    else
        print("  ✗ FAIL: " .. tostring(err))
    end
end

-- Helper function to assert equality
local function assert_eq(expected, actual, message)
    if expected ~= actual then
        error((message or "Assertion failed") .. string.format("\n  Expected: %s\n  Actual: %s",
            tostring(expected), tostring(actual)))
    end
end

-- Test basic value encoding
test("encode simple values", function()
    local catalog = {
        published_at = "2024-01-01T00:00:00Z",
        count = 42,
        active = true,
        empty = nil
    }

    local encoded = catalog_encoding.encode_catalog(catalog)

    -- Should be valid Lua that we can load
    local loaded_func = load(encoded)
    assert_eq(type(loaded_func), "function", "Should produce valid Lua")

    local result = loaded_func()
    assert_eq(result.published_at, "2024-01-01T00:00:00Z", "String should match")
    assert_eq(result.count, 42, "Number should match")
    assert_eq(result.active, true, "Boolean should match")
end)

-- Test nested table encoding
test("encode nested tables", function()
    local catalog = {
        translations = {
            {
                _id = "_test_key",
                en = "Test",
                de = "Test"
            }
        }
    }

    local encoded = catalog_encoding.encode_catalog(catalog)
    local result = load(encoded)()

    assert_eq(result.translations[1]._id, "_test_key", "Nested table access should work")
    assert_eq(result.translations[1].en, "Test", "Nested string should match")
end)

-- Test script encoding with long strings
test("encode lua script with long strings", function()
    local lua_script = [[return {
    id = "test-function",
    generator = function()
        return "hello [==[world]==]"
    end
}]]

    local catalog = {
        functions = {
            ["test-func"] = {
                script = lua_script,
                version = "1.0.0"
            }
        }
    }

    local encoded = catalog_encoding.encode_catalog(catalog)
    local result = load(encoded)()

    assert_eq(result.functions["test-func"].script, lua_script, "Script should be preserved exactly")
    assert_eq(result.functions["test-func"].version, "1.0.0", "Version should match")
end)

-- Test special characters and escaping
test("encode strings with special characters", function()
    local catalog = {
        text = "Line 1\nLine 2\tTabbed\"Quoted\"\\Backslash"
    }

    local encoded = catalog_encoding.encode_catalog(catalog)
    local result = load(encoded)()

    assert_eq(result.text, "Line 1\nLine 2\tTabbed\"Quoted\"\\Backslash", "Special characters should be preserved")
end)

-- Test minimal catalog structure similar to real catalog
test("encode minimal realistic catalog", function()
    local script = [[-- Simple function
return {
    id = "simple-func",
    version = "1.0.0",
    generator = function(source)
        return function()
            return source.dp()
        end
    end
}]]
    local description = [[
A string with the word return and a 
newline that will match the heuristic for a lua script.
    ]]

    local catalog = {
        published_at = "2024-01-01T00:00:00Z",
        translations = {
            {
                _id = "_test",
                en = "Test",
                de = "Test"
            }
        },
        functions = {
            ["simple-func"] = {
                version = "1.0.0",
                description = description,
                script = script
            }
        }
    }

    local encoded = catalog_encoding.encode_catalog(catalog)

    -- Verify it produces valid Lua
    local loaded_func = load(encoded)
    assert_eq(type(loaded_func), "function", "Should produce valid Lua")

    local result = loaded_func()

    -- Check structure
    assert_eq(result.published_at, "2024-01-01T00:00:00Z", "Published timestamp should match")
    assert_eq(type(result.translations), "table", "Should have translations table")
    assert_eq(result.translations[1]._id, "_test", "Translation should be preserved")
    assert_eq(type(result.functions), "table", "Should have functions table")
    assert_eq(result.functions["simple-func"].version, "1.0.0", "Function version should match")
    assert_eq(result.functions["simple-func"].description, description, "Function description should match")
    assert_eq(result.functions["simple-func"].script, script, "Function script should match")
end)

-- Test that encoded output matches expected hardcoded string format
test("encode output format matches expected structure", function()
    local catalog = {
        functions = {
            ["test"] = {
                script = "return { id = \"test\" }",
                version = "1.0.0"
            }
        },
        published_at = "2024-01-01",
        translations = {}
    }

    local encoded = catalog_encoding.encode_catalog(catalog)

    -- Should start with "return {" and end with "}"
    assert_eq(encoded:sub(1, 8), "return {", "Should start with 'return {'")
    assert_eq(encoded:sub(-1), "}", "Should end with '}'")

    -- Should contain the key parts
    assert_eq(encoded:find("functions=") ~= nil, true, "Should contain functions key")
    assert_eq(encoded:find("published_at=") ~= nil, true, "Should contain published_at key")
    assert_eq(encoded:find("translations=") ~= nil, true, "Should contain translations key")
end)

-- Print test summary
print("\n=== Test Results ===")
print(string.format("Tests run: %d", tests_run))
print(string.format("Tests passed: %d", tests_passed))
print(string.format("Tests failed: %d", tests_run - tests_passed))

if tests_passed == tests_run then
    print("✓ All tests passed!")
    os.exit(0)
else
    print("✗ Some tests failed!")
    os.exit(1)
end
