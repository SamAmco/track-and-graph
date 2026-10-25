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

test("calendar configuration restores, adds, recolors, and removes feature inputs", function()
	local existing = { id = 42, type = "FEATURE", name = "Steps", path = "/Health/Steps" }
	local result = render({ state = {}, feature_map = { source = existing } })
	assert(result.configuration_error == nil)
	assert(result.state.sources[1].selection == existing)
	assert(result.state.sources[1].color == 0)
	assert(result.feature_inputs[1].name == "source_1")
	assert(result.feature_inputs[1].feature_id == 42)

	local sources_section = result.ui.children[1]
	assert(sources_section.children[1].text.en == "Data sources")
	local source_controls = sources_section.children[2]
	local selector = source_controls.children[1].child.children[2]
	local button_modifiers = selector.children[1].modifier
	for _, modifier in ipairs(button_modifiers) do
		if modifier.callback ~= nil then modifier.callback() end
	end
	local opened = render({ state = result.state, feature_map = { source = existing } })
	local replacement = { id = 99, type = "FEATURE", name = "Sleep", path = "/Health/Sleep" }
	opened.ui.children[1].children[2].children[1].child.children[2].children[2].on_feature_selected(replacement)
	local updated = render({ state = opened.state, feature_map = { source = existing } })
	assert(updated.state.sources[1].selection == replacement)
	assert(updated.feature_inputs[1].feature_id == 99)

	local color_spinner = updated.ui.children[1].children[2].children[1].child.children[1]
	color_spinner.children[2].children[8].on_click()
	assert(updated.state.sources[1].color == 7)

	local add_control = updated.ui.children[1].children[2].children[2]
	local add_button = add_control.children[1]
	for _, modifier in ipairs(add_button.modifier) do
		if modifier.callback ~= nil then modifier.callback() end
	end
	local adding = render({ state = updated.state, feature_map = {} })
	local add_dialog = adding.ui.children[1].children[2].children[2].children[2]
	assert(add_dialog.visible == true)
	local added = { id = 123, type = "FEATURE", name = "Mood", path = "/Health/Mood" }
	add_dialog.on_feature_selected(added)
	local with_added = render({ state = adding.state, feature_map = {} })
	assert(#with_added.feature_inputs == 2)
	assert(with_added.feature_inputs[2].name == "source_2")
	assert(with_added.feature_inputs[2].feature_id == 123)
	assert(with_added.state.sources[2].color == 7)

	local remove_button = with_added.ui.children[1].children[2].children[1].child.children[3]
	remove_button.on_click()
	local removed = render({ state = with_added.state, feature_map = {} })
	assert(#removed.feature_inputs == 1)
	assert(removed.feature_inputs[1].feature_id == 123)
end)
