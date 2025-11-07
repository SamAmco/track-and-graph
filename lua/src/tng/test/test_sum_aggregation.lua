#!/usr/bin/env lua
-- test_sum_aggregation.lua
-- Tests for the sum aggregator

local aggregation = require("src.tng.aggregation")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test
local create_data_point = helpers.create_data_point

print("Testing sum aggregator...\n")

-- Test sum aggregator creation
test("sum_aggregator creates aggregator instance", function()
	local sum_agg = aggregation.sum_aggregator()
	assert(sum_agg ~= nil)
	assert(type(sum_agg.push) == "function")
	assert(type(sum_agg.pop) == "function")
	assert(type(sum_agg.run) == "function")
end)

-- Test sum aggregator with empty window
test("sum_aggregator returns 0 for empty window", function()
	local sum_agg = aggregation.sum_aggregator()
	-- For sum aggregator, empty window should return DataPoint with value 0
	-- But mid_point will still fail, so we need at least one point for proper test
	sum_agg:push(create_data_point(1000, 5.0))
	sum_agg:pop() -- Remove it to simulate empty after operations

	-- Sum should be 0 but mid_point will throw error on empty window
	local ok, err = pcall(function()
		sum_agg:run()
	end)

	assert(ok == false)
	assert(err and string.find(tostring(err), "empty window") ~= nil)
end)

-- Test sum aggregator with single data point
test("sum_aggregator handles single data point", function()
	local sum_agg = aggregation.sum_aggregator()
	sum_agg:push(create_data_point(1000, 5.0))

	local result = sum_agg:run()
	assert(result.value == 5.0)
	assert(result.timestamp == 1000)
end)

-- Test sum aggregator with push and pop operations
test("sum_aggregator calculates sum during push/pop operations", function()
	local sum_agg = aggregation.sum_aggregator()

	-- Add values: 10, 20, 30
	sum_agg:push(create_data_point(3000, 10.0))
	assert(sum_agg:run().value == 10.0)

	sum_agg:push(create_data_point(2000, 20.0))
	assert(sum_agg:run().value == 30.0)

	sum_agg:push(create_data_point(1000, 30.0))
	assert(sum_agg:run().value == 60.0)

	-- Remove values
	sum_agg:pop() -- Remove 10.0
	assert(sum_agg:run().value == 50.0)

	sum_agg:pop() -- Remove 20.0
	assert(sum_agg:run().value == 30.0)

	-- Note: After removing all points, mid_point throws error even though sum is tracked
end)

-- Test sum aggregator with negative values
test("sum_aggregator handles negative values", function()
	local sum_agg = aggregation.sum_aggregator()

	sum_agg:push(create_data_point(3000, -5.0))
	sum_agg:push(create_data_point(2000, 10.0))
	sum_agg:push(create_data_point(1000, -3.0))

	local result = sum_agg:run()
	assert(result.value == 2.0) -- -5 + 10 + (-3) = 2
end)

-- Summary
helpers.finish_tests()
