#!/usr/bin/env lua

package.path = "src/examples/calendar-heatmap/?.lua;" .. package.path .. ";src/?.lua;src/?/init.lua"

local helpers = require("src.tng.test.test_helpers")
local test = helpers.test
local render = dofile("src/examples/calendar-heatmap/configuration.lua")

test("calendar configuration requires a feature", function()
	local result = render({ state = nil, feature_map = {} })
	assert(result.configuration_error.en == "Select a feature for the calendar heatmap")
	assert(#result.feature_inputs == 0)
end)

test("calendar configuration restores and updates its normalized feature input", function()
	local existing = { id = 42, type = "FEATURE", name = "Steps", path = "/Health/Steps" }
	local result = render({ state = {}, feature_map = { source = existing } })
	assert(result.configuration_error == nil)
	assert(result.state.source == existing)
	assert(result.feature_inputs[1].name == "source")
	assert(result.feature_inputs[1].feature_id == 42)

	local selector = result.ui.children[1]
	local button_modifiers = selector.children[1].modifier
	for _, modifier in ipairs(button_modifiers) do
		if modifier.callback ~= nil then modifier.callback() end
	end
	local opened = render({ state = result.state, feature_map = { source = existing } })
	local replacement = { id = 99, type = "FEATURE", name = "Sleep", path = "/Health/Sleep" }
	opened.ui.children[1].children[2].on_feature_selected(replacement)
	local updated = render({ state = opened.state, feature_map = { source = existing } })
	assert(updated.state.source == replacement)
	assert(updated.feature_inputs[1].feature_id == 99)
end)
