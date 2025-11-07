#!/usr/bin/env lua
-- test_min_aggregation.lua
-- Tests for the min aggregator

local aggregation = require("src.tng.aggregation")
local helpers = require("src.tng.test.test_helpers")

local test = helpers.test
local create_data_point = helpers.create_data_point

-- Wrapper function to test both aggregator types
local function test_with_aggregators(test_name, test_func)
	test(test_name .. " (running_max_aggregator)", function()
		test_func(aggregation.running_min_aggregator)
	end)

	test(test_name .. " (simple_max_aggregator)", function()
		test_func(aggregation.simple_min_aggregator)
	end)
end


print("Testing min aggregator...\n")

-- Test min aggregator creation
test_with_aggregators("min_aggregator creates aggregator instance", function(aggregator)
	local min_agg = aggregator()
	assert(min_agg ~= nil)
	assert(type(min_agg.push) == "function")
	assert(type(min_agg.pop) == "function")
	assert(type(min_agg.run) == "function")
end)

-- Test min aggregator with single data point
test_with_aggregators("min_aggregator handles single data point", function(aggregator)
	local min_agg = aggregator()
	local dp = create_data_point(1000, 5.0)

	min_agg:push(dp)
	local result = min_agg:run()

	assert(result.value == 5.0)
	assert(result.timestamp == 1000)
end)

-- Test min aggregator with multiple data points
test_with_aggregators("min_aggregator finds minimum value", function(aggregator)
	local min_agg = aggregator()

	min_agg:push(create_data_point(1000, 10.0))
	min_agg:push(create_data_point(2000, 5.0))
	min_agg:push(create_data_point(3000, 15.0))
	min_agg:push(create_data_point(4000, 3.0))

	local result = min_agg:run()
	assert(result.value == 3.0)
	assert(result.timestamp == 2500)
end)

-- Test min aggregator with negative values
test_with_aggregators("min_aggregator handles negative values", function(aggregator)
	local min_agg = aggregator()

	min_agg:push(create_data_point(1000, -5.0))
	min_agg:push(create_data_point(2000, 10.0))
	min_agg:push(create_data_point(3000, -15.0))
	min_agg:push(create_data_point(4000, 3.0))

	local result = min_agg:run()
	assert(result.value == -15.0)
	assert(result.timestamp == 2500)
end)

-- Test min aggregator with duplicate minimum values
test_with_aggregators("min_aggregator handles duplicate minimum values", function(aggregator)
	local min_agg = aggregator()

	min_agg:push(create_data_point(4000, 5.0))
	min_agg:push(create_data_point(3000, 3.0))
	min_agg:push(create_data_point(2000, 3.0))
	min_agg:push(create_data_point(1000, 7.0))

	local result = min_agg:run()
	assert(result.value == 3.0)
	assert(result.timestamp == 2500)
end)

-- Test min aggregator midpoint calculation
test_with_aggregators("min_aggregator calculates correct midpoint timestamp", function(aggregator)
	local min_agg = aggregator()

	min_agg:push(create_data_point(5000, 10.0))
	min_agg:push(create_data_point(1000, 5.0))

	local result = min_agg:run()
	assert(result.value == 5.0)
	-- Midpoint between 1000 and 5000 should be 3000
	assert(result.timestamp == 3000)
end)

-- Test min aggregator pop functionality
test_with_aggregators("min_aggregator pop removes oldest element", function(aggregator)
	local min_agg = aggregator()

	min_agg:push(create_data_point(3000, 10.0))
	min_agg:push(create_data_point(2000, 5.0))
	min_agg:push(create_data_point(1000, 15.0))

	-- Before pop: min should be 5.0
	local result1 = min_agg:run()
	assert(result1.value == 5.0)

	-- Pop the oldest (10.0 at timestamp 1000)
	min_agg:pop()

	-- After pop: min should still be 5.0
	local result2 = min_agg:run()
	assert(result2.value == 5.0)

	-- window should contain two elements now
	assert(#min_agg.window == 2)
	assert(min_agg.window[1].value == 5.0)
	assert(min_agg.window[2].value == 15.0)
end)

-- Test min aggregator pop when minimum is removed
test_with_aggregators("min_aggregator pop updates minimum when minimum is removed", function(aggregator)
	local min_agg = aggregator()

	min_agg:push(create_data_point(3000, 3.0)) -- This will be the minimum
	min_agg:push(create_data_point(2000, 10.0))
	min_agg:push(create_data_point(1000, 7.0))

	-- Before pop: min should be 3.0
	local result1 = min_agg:run()
	assert(result1.value == 3.0)

	-- Pop the oldest (3.0 at timestamp 1000) - this removes the minimum
	min_agg:pop()

	-- After pop: min should now be 7.0
	local result2 = min_agg:run()
	assert(result2.value == 7.0)
end)

-- Test min aggregator with sliding window behavior
test_with_aggregators("min_aggregator maintains sliding window correctly", function(aggregator)
	local min_agg = aggregator()

	-- Add initial data points
	min_agg:push(create_data_point(6000, 8.0))
	min_agg:push(create_data_point(5000, 3.0))
	min_agg:push(create_data_point(4000, 12.0))

	assert(min_agg:run().value == 3.0)

	-- Pop oldest, add new
	min_agg:pop() -- Removes 8.0
	min_agg:push(create_data_point(3000, 2.0))

	assert(min_agg:run().value == 2.0)

	-- Pop oldest, add new
	min_agg:pop() -- Removes 3.0
	min_agg:push(create_data_point(2000, 15.0))

	assert(min_agg:run().value == 2.0)

	-- Pop oldest, add new
	min_agg:pop() -- Removes 12.0
	min_agg:push(create_data_point(1000, 1.0))

	assert(min_agg:run().value == 1.0)
end)

-- Test sliding window with fixed size over a data sequence
test_with_aggregators("min_aggregator handles sliding window size 3", function(aggregator)
	local min_agg = aggregator()
	local window_size = 3

	-- Test data sequence: [8, 3, 5, 4, 7, 6, 2, 1, 9]
	local data_sequence = { 8, 3, 5, 4, 7, 6, 2, 1, 9 }
	local timestamps = {}
	for i = 1, #data_sequence do
		timestamps[i] = i * 1000 -- 1000, 2000, 3000, etc.
	end

	-- Expected minimums for each window position:
	-- Window [8, 3, 5] -> min = 3
	-- Window [3, 5, 4] -> min = 3
	-- Window [5, 4, 7] -> min = 4
	-- Window [4, 7, 6] -> min = 4
	-- Window [7, 6, 2] -> min = 2
	-- Window [6, 2, 1] -> min = 1
	-- Window [2, 1, 9] -> min = 1
	local expected_mins = { 3, 3, 4, 4, 2, 1, 1 }

	local results = {}

	-- Fill initial window
	for i = 1, window_size do
		min_agg:push(create_data_point(timestamps[i], data_sequence[i]))
	end
	table.insert(results, min_agg:run().value)

	-- Slide the window over remaining data
	for i = window_size + 1, #data_sequence do
		min_agg:pop()                                                  -- Remove oldest
		min_agg:push(create_data_point(timestamps[i], data_sequence[i])) -- Add newest
		table.insert(results, min_agg:run().value)
	end

	-- Verify all results match expected minimums
	assert(#results == #expected_mins,
		string.format("Expected %d results, got %d", #expected_mins, #results))

	for i = 1, #expected_mins do
		assert(results[i] == expected_mins[i],
			string.format("Window %d: expected min=%d, got min=%d",
				i, expected_mins[i], results[i]))
	end
end)

test_with_aggregators("min_aggregator handles sliding window size 5", function(aggregator)
	local min_agg = aggregator()
	local window_size = 5

	-- Create a larger dataset with known minimum pattern
	-- Data: [10, 8, 6, 4, 2, 1, 3, 5, 7, 9, 11, 13]
	-- Windows and their mins:
	-- [10, 8, 6, 4, 2] -> min = 2
	-- [8, 6, 4, 2, 1]  -> min = 1
	-- [6, 4, 2, 1, 3]  -> min = 1
	-- [4, 2, 1, 3, 5]  -> min = 1
	-- [2, 1, 3, 5, 7]  -> min = 1
	-- [1, 3, 5, 7, 9]  -> min = 1
	-- [3, 5, 7, 9, 11] -> min = 3
	-- [5, 7, 9, 11, 13]-> min = 5
	local data_sequence = { 10, 8, 6, 4, 2, 1, 3, 5, 7, 9, 11, 13 }
	local expected_sequence = { 2, 1, 1, 1, 1, 1, 3, 5 }

	-- Fill initial window
	for i = 1, window_size do
		min_agg:push(create_data_point(i * 1000, data_sequence[i]))
	end

	local results = {}
	table.insert(results, min_agg:run().value)

	-- Slide window and collect results
	for i = window_size + 1, #data_sequence do
		min_agg:pop()
		min_agg:push(create_data_point(i * 1000, data_sequence[i]))
		table.insert(results, min_agg:run().value)
	end

	-- Verify results
	for i = 1, #expected_sequence do
		assert(results[i] == expected_sequence[i],
			string.format("Position %d: expected %d, got %d",
				i, expected_sequence[i], results[i]))
	end
end)

-- Test growing then shrinking window
test_with_aggregators("min_aggregator handles growing then shrinking window correctly", function(aggregator)
	local min_agg = aggregator()

	-- Values to push: [7, 3, 9, 1, 5]
	local values = { 7, 3, 9, 1, 5 }

	-- Expected mins during growing phase:
	-- After push 7: [7] -> min = 7
	-- After push 3: [7, 3] -> min = 3
	-- After push 9: [7, 3, 9] -> min = 3
	-- After push 1: [7, 3, 9, 1] -> min = 1
	-- After push 5: [7, 3, 9, 1, 5] -> min = 1
	local growing_expected = { 7, 3, 3, 1, 1 }

	-- Expected mins during shrinking phase (popping from left):
	-- After pop: [3, 9, 1, 5] -> min = 1
	-- After pop: [9, 1, 5] -> min = 1
	-- After pop: [1, 5] -> min = 1
	-- After pop: [5] -> min = 5
	local shrinking_expected = { 1, 1, 1, 5 }

	-- Growing phase: push values and check mins
	for i, value in ipairs(values) do
		min_agg:push(create_data_point(i * 1000, value))
		local result = min_agg:run()
		assert(result.value == growing_expected[i],
			string.format("Growing phase step %d: expected min=%d, got min=%d",
				i, growing_expected[i], result.value))
	end

	-- Shrinking phase: pop values and check mins
	for i, expected_min in ipairs(shrinking_expected) do
		min_agg:pop()
		local result = min_agg:run()
		assert(result.value == expected_min,
			string.format("Shrinking phase step %d: expected min=%d, got min=%d",
				i, expected_min, result.value))
	end
end)

-- Test error cases
test_with_aggregators("min_aggregator throws error on empty window", function(aggregator)
	local min_agg = aggregator()

	local ok, err = pcall(function()
		min_agg:run()
	end)

	assert(ok == false)
	assert(err and string.find(tostring(err), "empty window") ~= nil)
end)

-- Summary
helpers.finish_tests()
