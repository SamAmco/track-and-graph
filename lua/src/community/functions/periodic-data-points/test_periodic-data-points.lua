local M = {}

local core = require("tng.core")
local test = require("test.core")

local DHOUR = core.DURATION.HOUR
local DDAY = core.DURATION.DAY

M.test_daily_basic = {
    config = {
        period = "_day",
        period_multiplier = 1,
        cutoff = core.time().timestamp - (7 * DDAY), -- 7 days ago
    },
    sources = function()
        return {}
    end,
    assertions = function(result)
        test.assert("result was nil", result)

        -- Should have approximately 7-8 data points (depends on current time)
        test.assert("should have multiple data points", #result >= 7)
        test.assert("should not have too many data points", #result <= 8)

        -- All values should be 1.0
        for i = 1, #result do
            test.assertEquals(1.0, result[i].value)
            test.assertEquals("", result[i].label)
            test.assertEquals("", result[i].note)
        end

        -- Data points should be in reverse chronological order
        for i = 2, #result do
            test.assert("timestamps should decrease", result[i].timestamp < result[i - 1].timestamp)
        end

        -- First data point should be at or after cutoff
        local cutoff = core.time().timestamp - (7 * DDAY)
        test.assert("oldest timestamp should be at or after cutoff", result[#result].timestamp >= cutoff)
    end,
}

M.test_daily_with_multiplier = {
    config = {
        period = "_day",
        period_multiplier = 3,
        cutoff = core.time().timestamp - (30 * DDAY), -- 30 days ago
    },
    sources = function()
        return {}
    end,
    assertions = function(result)
        test.assert("result was nil", result)

        -- Should have approximately 10-11 data points (30 days / 3 = 10)
        test.assert("should have multiple data points", #result >= 9)
        test.assert("should not have too many data points", #result <= 11)

        -- Check 3-day spacing
        if #result >= 2 then
            local diff_ms = result[1].timestamp - result[2].timestamp
            local expected_ms = 3 * DDAY
            -- Allow some tolerance for DST
            test.assert("spacing should be ~3 days", math.abs(diff_ms - expected_ms) < DHOUR)
        end
    end,
}

M.test_weekly = {
    config = {
        period = "_week",
        period_multiplier = 1,
        cutoff = core.time().timestamp - (60 * DDAY), -- ~2 months ago
    },
    sources = function()
        return {}
    end,
    assertions = function(result)
        test.assert("result was nil", result)

        -- Should have approximately 9-10 data points (60 days / 7 = 8.5, starting at cutoff)
        test.assert("should have multiple data points", #result >= 8)
        test.assert("should not have too many data points", #result <= 10)

        -- All values should be 1.0
        for i = 1, #result do
            test.assertEquals(1.0, result[i].value)
        end
    end,
}

M.test_monthly = {
    config = {
        period = "_month",
        period_multiplier = 1,
        cutoff = core.time().timestamp - (180 * DDAY), -- ~6 months ago
    },
    sources = function()
        return {}
    end,
    assertions = function(result)
        test.assert("result was nil", result)

        -- Should have approximately 6-7 data points
        test.assert("should have multiple data points", #result >= 5)
        test.assert("should not have too many data points", #result <= 8)

        -- All values should be 1.0
        for i = 1, #result do
            test.assertEquals(1.0, result[i].value)
        end
    end,
}

M.test_yearly = {
    config = {
        period = "_year",
        period_multiplier = 1,
        cutoff = core.shift(core.time(), core.PERIOD.YEAR, -3).timestamp, -- 3 years ago
    },
    sources = function()
        return {}
    end,
    assertions = function(result)
        test.assert("result was nil", result)

        -- Should have approximately 3-4 data points
        test.assert("should have multiple data points", #result >= 3)
        test.assert("should not have too many data points", #result <= 5)

        -- All values should be 1.0
        for i = 1, #result do
            test.assertEquals(1.0, result[i].value)
        end
    end,
}

M.test_cutoff_boundary = {
    config = {
        period = "_day",
        period_multiplier = 1,
        cutoff = core.time().timestamp - (2 * DDAY), -- Very recent cutoff
    },
    sources = function()
        return {}
    end,
    assertions = function(result)
        test.assert("result was nil", result)

        -- Should have 2-3 data points only
        test.assert("should have data points", #result >= 2)
        test.assert("should respect cutoff", #result <= 3)

        -- All timestamps should be >= cutoff
        local cutoff = core.time().timestamp - (2 * DDAY)
        for i = 1, #result do
            test.assert("timestamp should be after cutoff", result[i].timestamp >= cutoff)
        end
    end,
}

return M
