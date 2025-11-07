#!/usr/bin/env lua
-- test_avg_aggregation.lua
-- Tests for the average aggregator

local aggregation = require("src.tng.aggregation")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test
local create_data_point = helpers.create_data_point

print("Testing average aggregator...\n")

-- Test avg aggregator creation
test("avg_aggregator creates aggregator instance", function()
	local avg_agg = aggregation.avg_aggregator()
	assert(avg_agg ~= nil)
	assert(type(avg_agg.push) == "function")
	assert(type(avg_agg.pop) == "function")
	assert(type(avg_agg.run) == "function")
end)

-- Test avg aggregator with single data point
test("avg_aggregator handles single data point", function()
	local avg_agg = aggregation.avg_aggregator()
	avg_agg:push(create_data_point(1000, 8.0))

	local result = avg_agg:run()
	assert(result.value == 8.0)
	assert(result.timestamp == 1000)
end)

-- Test avg aggregator with push and pop operations
test("avg_aggregator calculates average during push/pop operations", function()
	local avg_agg = aggregation.avg_aggregator()

	-- Add values: 10, 20, 30
	avg_agg:push(create_data_point(3000, 10.0))
	assert(avg_agg:run().value == 10.0) -- 10/1 = 10

	avg_agg:push(create_data_point(2000, 20.0))
	assert(avg_agg:run().value == 15.0) -- (10+20)/2 = 15

	avg_agg:push(create_data_point(1000, 30.0))
	assert(avg_agg:run().value == 20.0) -- (10+20+30)/3 = 20

	-- Remove values
	avg_agg:pop()                      -- Remove 10.0
	assert(avg_agg:run().value == 25.0) -- (20+30)/2 = 25

	avg_agg:pop()                      -- Remove 20.0
	assert(avg_agg:run().value == 30.0) -- 30/1 = 30
end)

-- Test avg aggregator throws error on empty window
test("avg_aggregator throws error on empty window", function()
	local avg_agg = aggregation.avg_aggregator()
	local ok, err = pcall(function()
		avg_agg:run()
	end)

	assert(ok == false)
	assert(err and string.find(tostring(err), "empty window") ~= nil)
end)

-- Summary
helpers.finish_tests()
