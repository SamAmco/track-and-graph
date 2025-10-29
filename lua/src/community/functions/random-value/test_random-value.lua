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

local reference_date = {
    year = 2025,
    month = 1,
    day = 2,
    hour = 3,
    min = 2,
    sec = 1,
    zone = "UTC",
}
local reference_time = core.time(reference_date).timestamp

M["test random value reproducible 1"] = {
    config = {
        min_value = 0.0,
        max_value = 1.0,
        seed = 42,
    },
    sources = function()
        return {
            {
                {
                    timestamp = reference_time - 1,
                    value = 100.0,
                    label = "test1",
                },
                {
                    timestamp = reference_time - 2,
                    value = 200.0,
                    label = "test2",
                },
                {
                    timestamp = reference_time - 3,
                    value = 300.0,
                    label = "test3",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(3, #result)

        local expected_values = {
            0.19737142,
            0.8292714,
            0.099706925,
        }

        for i = 1, #expected_values do
            test.assertEquals(expected_values[i], result[i].value)
        end

        -- Labels should be preserved
        test.assertEquals("test1", result[1].label)
        test.assertEquals("test2", result[2].label)
        test.assertEquals("test3", result[3].label)
    end,
}

-- In this second test time has moved forwards and we have all the same old data
-- points plus one new one. The random values should be the same for the old data points.
-- The new data point should get the next random value in the sequence.
M["test random value reproducible 2"] = {
    config = {
        min_value = 0.0,
        max_value = 1.0,
        seed = 42,
    },
    sources = function()
        return {
            {
                {
                    timestamp = reference_time,
                    value = 50.0,
                    label = "test1",
                },
                {
                    timestamp = reference_time - 1,
                    value = 100.0,
                    label = "test1",
                },
                {
                    timestamp = reference_time - 2,
                    value = 200.0,
                    label = "test2",
                },
                {
                    timestamp = reference_time - 3,
                    value = 300.0,
                    label = "test3",
                },
            },
        }
    end,
    assertions = function(result)
        test.assert("result was nil", result)
        test.assertEquals(4, #result)

        local expected_values = {
            0.4741889,
            0.19737142,
            0.8292714,
            0.099706925,
        }

        for i = 1, #expected_values do
            test.assertEquals(expected_values[i], result[i].value)
        end

        -- Labels should be preserved
        test.assertEquals("test1", result[1].label)
        test.assertEquals("test1", result[2].label)
        test.assertEquals("test2", result[3].label)
        test.assertEquals("test3", result[4].label)
    end,
}

return M
