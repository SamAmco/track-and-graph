#!/usr/bin/env lua
-- test_avg_aggregation.lua
-- Tests for the aggregator mid point data points

local aggregation = require("src.tng.aggregation")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test
local create_data_point = helpers.create_data_point

print("Testing aggregator midpoints...\n")

-- Test avg aggregator creation
test("avg_aggregator creates aggregator instance", function()
  -- Don't actually care about the aggregator in this case
  local count_agg = aggregation.count_aggregator()
  count_agg:push(create_data_point(1000, 1, 0, "label1", "note1"))

  local curr = count_agg:run()
  assert(curr.label == "label1")
  assert(curr.note == "note1")
  assert(curr.offset == 0)

  count_agg:push(create_data_point(1000, 1, 1, "label2", "note2"))

  curr = count_agg:run()
  assert(curr.label == "label1")
  assert(curr.note == "note1")
  assert(curr.offset == 0)

  count_agg:pop()

  curr = count_agg:run()
  assert(curr.label == "label2")
  assert(curr.note == "note2")
  assert(curr.offset == 1)
end)
