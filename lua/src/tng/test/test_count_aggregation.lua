#!/usr/bin/env lua
-- test_count_aggregation.lua
-- Tests for the count aggregator

local aggregation = require("src.tng.aggregation")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test
local create_data_point = helpers.create_data_point

print("Testing count aggregator...\n")

-- Test count aggregator creation
test("count_aggregator creates aggregator instance", function()
	local count_agg = aggregation.count_aggregator()
	assert(count_agg ~= nil)
	assert(type(count_agg.push) == "function")
	assert(type(count_agg.pop) == "function")
	assert(type(count_agg.run) == "function")
end)

-- Test count aggregator with empty window throws error (as per mid_point behavior)
test("count_aggregator throws error on empty window", function()
	local count_agg = aggregation.count_aggregator()
	local ok, err = pcall(function()
		count_agg:run()
	end)

	assert(ok == false)
	assert(err and string.find(tostring(err), "empty window") ~= nil)
end)

-- Test count aggregator with single data point
test("count_aggregator handles single data point", function()
	local count_agg = aggregation.count_aggregator()
	count_agg:push(create_data_point(1000, 5.0))

	local result = count_agg:run()
	assert(result.value == 1)
	assert(result.timestamp == 1000)
end)

-- Test count aggregator with push and pop operations
test("count_aggregator tracks count during push/pop operations", function()
	local count_agg = aggregation.count_aggregator()

	-- Add one point
	count_agg:push(create_data_point(1000, 10.0))
	assert(count_agg:run().value == 1)

	-- Add second point
	count_agg:push(create_data_point(2000, 20.0))
	assert(count_agg:run().value == 2)

	-- Add third point
	count_agg:push(create_data_point(3000, 30.0))
	assert(count_agg:run().value == 3)

	-- Remove one point
	count_agg:pop()
	assert(count_agg:run().value == 2)

	-- Remove another point
	count_agg:pop()
	assert(count_agg:run().value == 1)

	-- Remove last point
	count_agg:pop()
	-- After removing all points, calling run() should throw error due to empty window
	local ok, _ = pcall(function()
		count_agg:run()
	end)
	assert(ok == false)
end)

-- Summary
helpers.finish_tests()
