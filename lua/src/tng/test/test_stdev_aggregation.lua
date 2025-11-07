#!/usr/bin/env lua
-- test_stdev_aggregation.lua
-- Tests for the standard deviation aggregator

local aggregation = require("src.tng.aggregation")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test
local create_data_point = helpers.create_data_point

print("Testing standard deviation aggregator...\n")

-- Test stdev aggregator creation
test("stdev_aggregator creates aggregator instance", function()
	local stdev_agg = aggregation.stdev_aggregator()
	assert(stdev_agg ~= nil)
	assert(type(stdev_agg.push) == "function")
	assert(type(stdev_agg.pop) == "function")
	assert(type(stdev_agg.run) == "function")
end)

-- Test stdev aggregator with single data point
test("stdev_aggregator handles single data point", function()
	local stdev_agg = aggregation.stdev_aggregator()
	stdev_agg:push(create_data_point(1000, 5.0))

	local result = stdev_agg:run()
	assert(result.value == 0) -- stdev of single value is 0
	assert(result.timestamp == 1000)
end)

-- Test stdev aggregator with known standard deviation
test("stdev_aggregator calculates correct standard deviation", function()
	local stdev_agg = aggregation.stdev_aggregator()

	-- Values: 1, 3, 5 (mean = 3, variance = 8/3, stdev = sqrt(8/3))
	stdev_agg:push(create_data_point(1000, 1.0))
	stdev_agg:push(create_data_point(2000, 3.0))
	stdev_agg:push(create_data_point(3000, 5.0))

	local result = stdev_agg:run()
	local expected_stdev = math.sqrt(8.0 / 3.0)
	assert(math.abs(result.value - expected_stdev) < 0.0001)
end)

-- Test stdev aggregator with push and pop operations
test("stdev_aggregator maintains stdev during push/pop operations", function()
	local stdev_agg = aggregation.stdev_aggregator()

	-- Add values: 2, 4, 6
	stdev_agg:push(create_data_point(1000, 2.0))
	stdev_agg:push(create_data_point(2000, 4.0))
	stdev_agg:push(create_data_point(3000, 6.0))

	-- Variance = 8/3, so stdev = sqrt(8/3)
	local result1 = stdev_agg:run()
	assert(math.abs(result1.value - math.sqrt(8.0 / 3.0)) < 0.0001)

	-- Remove first value (2.0), now we have 4, 6
	stdev_agg:pop()

	-- Variance = 1, so stdev = 1
	local result2 = stdev_agg:run()
	assert(math.abs(result2.value - 1.0) < 0.0001)
end)

-- Test stdev aggregator throws error on empty window
test("stdev_aggregator throws error on empty window", function()
	local stdev_agg = aggregation.stdev_aggregator()
	local ok, err = pcall(function()
		stdev_agg:run()
	end)

	assert(ok == false)
	assert(err and string.find(tostring(err), "empty window") ~= nil)
end)

-- Summary
helpers.finish_tests()
