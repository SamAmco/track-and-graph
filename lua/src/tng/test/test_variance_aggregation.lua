#!/usr/bin/env lua
-- test_variance_aggregation.lua
-- Tests for the variance aggregator

local aggregation = require("src.tng.aggregation")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test
local create_data_point = helpers.create_data_point

print("Testing variance aggregator...\n")

-- Test variance aggregator creation
test("variance_aggregator creates aggregator instance", function()
	local var_agg = aggregation.variance_aggregator()
	assert(var_agg ~= nil)
	assert(type(var_agg.push) == "function")
	assert(type(var_agg.pop) == "function")
	assert(type(var_agg.run) == "function")
end)

-- Test variance aggregator with single data point
test("variance_aggregator handles single data point", function()
	local var_agg = aggregation.variance_aggregator()
	var_agg:push(create_data_point(1000, 5.0))

	local result = var_agg:run()
	assert(result.value == 0) -- variance of single value is 0
	assert(result.timestamp == 1000)
end)

-- Test variance aggregator with known variance
test("variance_aggregator calculates correct variance", function()
	local var_agg = aggregation.variance_aggregator()

	-- Values: 1, 3, 5 (mean = 3, variance = ((1-3)² + (3-3)² + (5-3)²)/3 = (4+0+4)/3 = 8/3)
	var_agg:push(create_data_point(1000, 1.0))
	var_agg:push(create_data_point(2000, 3.0))
	var_agg:push(create_data_point(3000, 5.0))

	local result = var_agg:run()
	local expected_variance = 8.0 / 3.0
	assert(math.abs(result.value - expected_variance) < 0.0001)
end)

-- Test variance aggregator with push and pop operations
test("variance_aggregator maintains variance during push/pop operations", function()
	local var_agg = aggregation.variance_aggregator()

	-- Add values: 2, 4, 6
	var_agg:push(create_data_point(1000, 2.0))
	var_agg:push(create_data_point(2000, 4.0))
	var_agg:push(create_data_point(3000, 6.0))

	-- Mean = 4, variance = ((2-4)² + (4-4)² + (6-4)²)/3 = (4+0+4)/3 = 8/3
	local result1 = var_agg:run()
	assert(math.abs(result1.value - 8.0 / 3.0) < 0.0001)

	-- Remove first value (2.0), now we have 4, 6
	var_agg:pop()

	-- Mean = 5, variance = ((4-5)² + (6-5)²)/2 = (1+1)/2 = 1
	local result2 = var_agg:run()
	assert(math.abs(result2.value - 1.0) < 0.0001)
end)

-- Test variance aggregator throws error on empty window
test("variance_aggregator throws error on empty window", function()
	local var_agg = aggregation.variance_aggregator()
	local ok, err = pcall(function()
		var_agg:run()
	end)

	assert(ok == false)
	assert(err and string.find(tostring(err), "empty window") ~= nil)
end)

-- Summary
helpers.finish_tests()
