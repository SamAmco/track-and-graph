#!/usr/bin/env lua
package.path = "src/examples/calendar-heatmap/?.lua;" .. package.path .. ";src/?.lua;src/?/init.lua"
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test
local render = dofile("src/examples/calendar-heatmap/configuration.lua")
local ui = require("tng.ui")

local function find(node, predicate)
	if predicate(node) then return node end
	if node.child then
		local result = find(node.child, predicate)
		if result then return result end
	end
	for _, child in ipairs(node.children or {}) do
		local result = find(child, predicate)
		if result then return result end
	end
end
local function keyed(tree, key)
	return assert(find(tree, function(node) return node.key == key end), key)
end
local function click(node)
	if node.on_click then node.on_click(); return end
	for _, modifier in ipairs(node.modifier or {}) do
		if modifier.callback then modifier.callback(); return end
	end
	error("node has no click handler")
end

test("calendar configuration requires a feature", function()
	local result = render({ feature_map = {} })
	assert(result.configuration_error.en == "Select a feature for the calendar heatmap")
	assert(#result.feature_inputs == 0)
end)

test("calendar source controls retain colours, validate ranges, add and remove inputs", function()
	local existing = { id = 42, path = "/Health/Steps" }
	local result = render({ feature_map = { source = existing } })
	assert(result.configuration_error == nil)
	assert(result.state.sources[1].mode == "BINARY")
	assert(result.feature_inputs[1].feature_id == 42)
	keyed(result.ui, "source_feature_1_dialog").on_feature_selected({ id = 99, path = "/Health/Sleep" })
	keyed(result.ui, "source_color_1_color_7").on_click()
	local source = result.state.sources[1]
	source.mode = "RANGE"
	source.minimum = ui.text_field_value("-3.1253")
	source.maximum = ui.text_field_value("234.12")
	result = render({ state = result.state })
	assert(result.configuration_error == nil)
	assert(source.color == 7)
	assert(result.feature_inputs[1].feature_id == 99)
	keyed(result.ui, "minimum").on_value_change(ui.text_field_value("-"))
	result = render({ state = result.state })
	assert(result.configuration_error ~= nil)
	keyed(result.ui, "minimum").on_value_change(ui.text_field_value("234.12"))
	assert(render({ state = result.state }).configuration_error ~= nil)
	keyed(result.ui, "minimum").on_value_change(ui.text_field_value("-3.1253"))
	assert(render({ state = result.state }).configuration_error == nil)

	click(keyed(result.ui, "add_source_button"))
	result = render({ state = result.state })
	assert(keyed(result.ui, "add_source_dialog").visible)
	keyed(result.ui, "add_source_dialog").on_feature_selected({ id = 123, path = "/Mood" })
	result = render({ state = result.state })
	assert(#result.feature_inputs == 2)
	local delete = find(keyed(result.ui, "source_1"), function(node) return node.type == "ICON_BUTTON" end)
	click(delete)
	result = render({ state = result.state })
	assert(#result.feature_inputs == 1)
	assert(result.feature_inputs[1].feature_id == 123)
	-- Removing an earlier card must not transfer its remembered native fields to this source.
	assert(keyed(result.ui, "source_2") ~= nil)
end)

helpers.finish_tests()
