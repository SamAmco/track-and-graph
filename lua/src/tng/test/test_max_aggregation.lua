#!/usr/bin/env lua
-- test_max_aggregation.lua
-- Tests for the max aggregator

local aggregation = require("src.tng.aggregation")
local helpers = require("src.tng.test.test_helpers")

local test = helpers.test
local create_data_point = helpers.create_data_point

print("Testing max aggregator...\n")

-- Wrapper function to test both aggregator types
local function test_with_aggregators(test_name, test_func)
	test(test_name .. " (running_max_aggregator)", function()
		test_func(aggregation.running_max_aggregator)
	end)

	test(test_name .. " (simple_max_aggregator)", function()
		test_func(aggregation.simple_max_aggregator)
	end)
end

-- Test max aggregator creation
test_with_aggregators("max_aggregator creates aggregator instance", function(aggregator)
	local max_agg = aggregator()
	assert(max_agg ~= nil)
	assert(type(max_agg.push) == "function")
	assert(type(max_agg.pop) == "function")
	assert(type(max_agg.run) == "function")
end)

-- Test max aggregator with single data point
test_with_aggregators("max_aggregator handles single data point", function(aggregator)
	local max_agg = aggregator()
	local dp = create_data_point(1000, 5.0)

	max_agg:push(dp)
	local result = max_agg:run()

	assert(result.value == 5.0)
	assert(result.timestamp == 1000)
end)

-- Test max aggregator with multiple data points
test_with_aggregators("max_aggregator finds maximum value", function(aggregator)
	local max_agg = aggregator()

	max_agg:push(create_data_point(4000, 10.0))
	max_agg:push(create_data_point(3000, 5.0))
	max_agg:push(create_data_point(2000, 15.0))
	max_agg:push(create_data_point(1000, 3.0))

	local result = max_agg:run()
	assert(result.value == 15.0)
end)

-- Test max aggregator with negative values
test_with_aggregators("max_aggregator handles negative values", function(aggregator)
	local max_agg = aggregator()

	max_agg:push(create_data_point(4000, -5.0))
	max_agg:push(create_data_point(3000, -10.0))
	max_agg:push(create_data_point(2000, -15.0))
	max_agg:push(create_data_point(1000, -3.0))

	local result = max_agg:run()
	assert(result.value == -3.0)
end)

-- Test max aggregator with duplicate maximum values
test_with_aggregators("max_aggregator handles duplicate maximum values", function(aggregator)
	local max_agg = aggregator()

	max_agg:push(create_data_point(4000, 5.0))
	max_agg:push(create_data_point(3000, 8.0))
	max_agg:push(create_data_point(2000, 8.0))
	max_agg:push(create_data_point(1000, 3.0))

	local result = max_agg:run()
	assert(result.value == 8.0)
end)

-- Test max aggregator midpoint calculation
test_with_aggregators("max_aggregator calculates correct midpoint timestamp", function(aggregator)
	local max_agg = aggregator()

	max_agg:push(create_data_point(5000, 10.0))
	max_agg:push(create_data_point(1000, 15.0))

	local result = max_agg:run()
	assert(result.value == 15.0)
	-- Midpoint between 1000 and 5000 should be 3000
	assert(result.timestamp == 3000)
end)

-- Test max aggregator pop functionality
test_with_aggregators("max_aggregator pop removes oldest element", function(aggregator)
	local max_agg = aggregator()

	max_agg:push(create_data_point(3000, 10.0))
	max_agg:push(create_data_point(2000, 15.0))
	max_agg:push(create_data_point(1000, 5.0))

	-- Before pop: max should be 15.0
	local result1 = max_agg:run()
	assert(result1.value == 15.0)

	-- Pop the oldest (10.0 at timestamp 1000)
	max_agg:pop()

	-- After pop: max should still be 15.0
	local result2 = max_agg:run()
	assert(result2.value == 15.0)

	-- Verify internal window size is now 2
	assert(#max_agg.window == 2)
	assert(max_agg.window[1].value == 15.0)
	assert(max_agg.window[2].value == 5.0)
end)

-- Test max aggregator pop when maximum is removed
test_with_aggregators("max_aggregator pop updates maximum when maximum is removed", function(aggregator)
	local max_agg = aggregator()

	max_agg:push(create_data_point(3000, 15.0)) -- This will be the maximum
	max_agg:push(create_data_point(2000, 10.0))
	max_agg:push(create_data_point(1000, 7.0))

	-- Before pop: max should be 15.0
	local result1 = max_agg:run()
	assert(result1.value == 15.0)

	-- Pop the oldest (15.0 at timestamp 1000) - this removes the maximum
	max_agg:pop()

	-- After pop: max should now be 10.0
	local result2 = max_agg:run()
	assert(result2.value == 10.0)
end)

-- Test max aggregator with sliding window behavior
test_with_aggregators("max_aggregator maintains sliding window correctly", function(aggregator)
	local max_agg = aggregator()

	-- Add initial data points
	max_agg:push(create_data_point(6000, 8.0))
	max_agg:push(create_data_point(5000, 15.0))
	max_agg:push(create_data_point(4000, 12.0))

	assert(max_agg:run().value == 15.0)

	-- Pop oldest, add new
	max_agg:pop() -- Removes 8.0
	max_agg:push(create_data_point(3000, 20.0))

	assert(max_agg:run().value == 20.0)

	-- Pop oldest, add new
	max_agg:pop() -- Removes 15.0
	max_agg:push(create_data_point(2000, 5.0))

	assert(max_agg:run().value == 20.0)

	-- Pop oldest, add new
	max_agg:pop() -- Removes 12.0
	max_agg:push(create_data_point(1000, 25.0))

	assert(max_agg:run().value == 25.0)
end)

-- Test sliding window with fixed size over a data sequence
test_with_aggregators("max_aggregator correctly handles sliding window over data sequence", function(aggregator)
	local max_agg = aggregator()
	local window_size = 3

	-- Test data sequence: [8, 3, 5, 4, 7, 6, 2, 1, 9]
	local data_sequence = { 8, 3, 5, 4, 7, 6, 2, 1, 9 }
	local timestamps = {}
	for i = 1, #data_sequence do
		timestamps[i] = i * 1000 -- 1000, 2000, 3000, etc.
	end

	-- Expected maximums for each window position:
	-- Window [8, 3, 5] -> max = 8
	-- Window [3, 5, 4] -> max = 5
	-- Window [5, 4, 7] -> max = 7
	-- Window [4, 7, 6] -> max = 7
	-- Window [7, 6, 2] -> max = 7
	-- Window [6, 2, 1] -> max = 6
	-- Window [2, 1, 9] -> max = 9
	local expected_maxs = { 8, 5, 7, 7, 7, 6, 9 }

	local results = {}

	-- Fill initial window
	for i = 1, window_size do
		max_agg:push(create_data_point(timestamps[i], data_sequence[i]))
	end
	table.insert(results, max_agg:run().value)

	-- Slide the window over remaining data
	for i = window_size + 1, #data_sequence do
		max_agg:pop()                                                  -- Remove oldest
		max_agg:push(create_data_point(timestamps[i], data_sequence[i])) -- Add newest
		table.insert(results, max_agg:run().value)
	end

	-- Verify all results match expected maximums
	assert(#results == #expected_maxs,
		string.format("Expected %d results, got %d", #expected_maxs, #results))

	for i = 1, #expected_maxs do
		assert(results[i] == expected_maxs[i],
			string.format("Window %d: expected max=%d, got max=%d",
				i, expected_maxs[i], results[i]))
	end
end)

-- Test sliding window performance characteristics (verify deque optimization)
test_with_aggregators("max_aggregator efficiently handles large sliding window", function(aggregator)
	local max_agg = aggregator()
	local window_size = 5

	-- Create a larger dataset with known maximum pattern
	-- Data: [1, 3, 5, 7, 9, 10, 8, 6, 4, 2, 0, -1]
	-- Windows and their maxs:
	-- [1, 3, 5, 7, 9]  -> max = 9
	-- [3, 5, 7, 9, 10] -> max = 10
	-- [5, 7, 9, 10, 8] -> max = 10
	-- [7, 9, 10, 8, 6] -> max = 10
	-- [9, 10, 8, 6, 4] -> max = 10
	-- [10, 8, 6, 4, 2] -> max = 10
	-- [8, 6, 4, 2, 0]  -> max = 8
	-- [6, 4, 2, 0, -1] -> max = 6
	local data_sequence = { 1, 3, 5, 7, 9, 10, 8, 6, 4, 2, 0, -1 }
	local expected_sequence = { 9, 10, 10, 10, 10, 10, 8, 6 }

	-- Fill initial window
	for i = 1, window_size do
		max_agg:push(create_data_point(i * 1000, data_sequence[i]))
	end

	local results = {}
	table.insert(results, max_agg:run().value)

	-- Slide window and collect results
	for i = window_size + 1, #data_sequence do
		max_agg:pop()
		max_agg:push(create_data_point(i * 1000, data_sequence[i]))
		table.insert(results, max_agg:run().value)
	end

	-- Verify results
	for i = 1, #expected_sequence do
		assert(results[i] == expected_sequence[i],
			string.format("Position %d: expected %d, got %d",
				i, expected_sequence[i], results[i]))
	end
end)

-- Test growing then shrinking window
test_with_aggregators("max_aggregator handles growing then shrinking window correctly", function(aggregator)
	local max_agg = aggregator()

	-- Values to push: [2, 8, 4, 15, 6]
	local values = { 2, 8, 4, 15, 6 }

	-- Expected maxs during growing phase:
	-- After push 2: [2] -> max = 2
	-- After push 8: [2, 8] -> max = 8
	-- After push 4: [2, 8, 4] -> max = 8
	-- After push 15: [2, 8, 4, 15] -> max = 15
	-- After push 6: [2, 8, 4, 15, 6] -> max = 15
	local growing_expected = { 2, 8, 8, 15, 15 }

	-- Expected maxs during shrinking phase (popping from left):
	-- After pop: [8, 4, 15, 6] -> max = 15
	-- After pop: [4, 15, 6] -> max = 15
	-- After pop: [15, 6] -> max = 15
	-- After pop: [6] -> max = 6
	local shrinking_expected = { 15, 15, 15, 6 }

	-- Growing phase: push values and check maxs
	for i, value in ipairs(values) do
		max_agg:push(create_data_point(i * 1000, value))
		local result = max_agg:run()
		assert(result.value == growing_expected[i],
			string.format("Growing phase step %d: expected max=%d, got max=%d",
				i, growing_expected[i], result.value))
	end

	-- Shrinking phase: pop values and check maxs
	for i, expected_max in ipairs(shrinking_expected) do
		max_agg:pop()
		local result = max_agg:run()
		assert(result.value == expected_max,
			string.format("Shrinking phase step %d: expected max=%d, got max=%d",
				i, expected_max, result.value))
	end
end)

-- Test error cases
test_with_aggregators("max_aggregator throws error on empty window", function(aggregator)
	local max_agg = aggregator()

	local ok, err = pcall(function()
		max_agg:run()
	end)

	assert(ok == false)
	assert(err and string.find(tostring(err), "empty window") ~= nil)
end)

-- Summary
helpers.finish_tests()
