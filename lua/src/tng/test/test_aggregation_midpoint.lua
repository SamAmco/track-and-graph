#!/usr/bin/env lua
-- test_aggregation_window_point.lua
-- Tests for the aggregator window point data points

local aggregation = require("src.tng.aggregation")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test
local create_data_point = helpers.create_data_point

print("Testing aggregator window points...\n")

-- Test default placement ("mid") preserves backwards compatibility
test("window_point default placement uses temporal midpoint", function()
  local count_agg = aggregation.count_aggregator()
  count_agg:push(create_data_point(1000, 1, 0, "label1", "note1"))

  local curr = count_agg:run()
  assert(curr.label == "label1")
  assert(curr.note == "note1")
  assert(curr.offset == 0)
  assert(curr.timestamp == 1000)

  count_agg:push(create_data_point(500, 1, 1, "label2", "note2"))

  curr = count_agg:run()
  -- Default "mid": oldest(500) + (1000-500)/2 = 750
  assert(curr.timestamp == 750)
  assert(curr.label == "label1")
  assert(curr.note == "note1")
  assert(curr.offset == 1)

  count_agg:pop()

  curr = count_agg:run()
  assert(curr.timestamp == 500)
  assert(curr.label == "label2")
  assert(curr.note == "note2")
  assert(curr.offset == 1)
end)

-- Test "end" placement uses most recent data point (chronologically last)
test("window_point end placement uses most recent data point", function()
  local count_agg = aggregation.count_aggregator("end")
  count_agg:push(create_data_point(1000, 1, 0, "label1", "note1"))
  count_agg:push(create_data_point(500, 1, 1, "label2", "note2"))

  local curr = count_agg:run()
  -- "end" = most recent = window[1] = timestamp 1000
  assert(curr.timestamp == 1000)
  assert(curr.label == "label1")
  assert(curr.note == "note1")
  assert(curr.offset == 0)
end)

-- Test "start" placement uses oldest data point (chronologically first)
test("window_point start placement uses oldest data point", function()
  local count_agg = aggregation.count_aggregator("start")
  count_agg:push(create_data_point(1000, 1, 0, "label1", "note1"))
  count_agg:push(create_data_point(500, 1, 1, "label2", "note2"))

  local curr = count_agg:run()
  -- "start" = oldest = window[#window] = timestamp 500
  assert(curr.timestamp == 500)
  assert(curr.label == "label1") -- label always from newest
  assert(curr.note == "note1")   -- note always from newest
  assert(curr.offset == 1)       -- offset from oldest (start)
end)
