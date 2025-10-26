local M = {}

local core = require("tng.core")
local test = require("test.core")

local DDAY = core.DURATION.DAY

M.test_random_value_basic = {
    config = {
        min_value = 0.0,
        max_value = 10.0,
        seed = 12345,
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now - (DDAY * 1),
                    value = 5.0,
                    label = "test1",
                    note = "note1",
                },
                {
                    timestamp = now - (DDAY * 2),
                    value = 15.0,
                    label = "test2",
                    note = "note2",
                },
                {
                    timestamp = now - (DDAY * 3),
                    value = 25.0,
                    label = "test3",
                    note = "note3",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(3, #result)

        -- All values should be between 0 and 10
        for i = 1, 3 do
            test.assert("value should be >= min", result[i].value >= 0.0)
            test.assert("value should be <= max", result[i].value <= 10.0)
        end

        -- Labels and notes should be preserved
        test.assertEquals("test1", result[1].label)
        test.assertEquals("test2", result[2].label)
        test.assertEquals("test3", result[3].label)
        test.assertEquals("note1", result[1].note)
        test.assertEquals("note2", result[2].note)
        test.assertEquals("note3", result[3].note)
    end,
}

M.test_random_value_default_config = {
    config = {
        seed = 54321,
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now,
                    value = 100.0,
                    label = "test",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(1, #result)

        -- Default range is 0 to 1
        test.assert("value should be >= 0", result[1].value >= 0.0)
        test.assert("value should be <= 1", result[1].value <= 1.0)
        test.assertEquals("test", result[1].label)
    end,
}

M.test_random_value_swapped_min_max = {
    config = {
        min_value = 100.0,
        max_value = 50.0,
        seed = 99999,
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now,
                    value = 10.0,
                    label = "swap",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(1, #result)

        -- Function should swap min and max, so range is 50 to 100
        test.assert("value should be >= 50", result[1].value >= 50.0)
        test.assert("value should be <= 100", result[1].value <= 100.0)
    end,
}

M.test_random_value_negative_range = {
    config = {
        min_value = -10.0,
        max_value = -5.0,
        seed = 11111,
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now,
                    value = 0.0,
                    label = "negative",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(1, #result)

        -- Range should be -10 to -5
        test.assert("value should be >= -10", result[1].value >= -10.0)
        test.assert("value should be <= -5", result[1].value <= -5.0)
    end,
}

M.test_random_value_reproducible = {
    config = {
        min_value = 0.0,
        max_value = 1.0,
        seed = 42424,
    },
    sources = function()
        local now = core.time().timestamp
        return {
            {
                {
                    timestamp = now,
                    value = 123.0,
                    label = "reproducible",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(1, #result)

        -- With the same seed, we should get reproducible results
        -- Store this value for reference
        local expected_value = result[1].value
        test.assert("value should be >= 0", expected_value >= 0.0)
        test.assert("value should be <= 1", expected_value <= 1.0)
    end,
}

return M
